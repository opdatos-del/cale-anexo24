-- ============================================
-- Firma cross-database del command de pedimentos
-- RUNTIME IDENTITY HARDENING — PREPARACIÓN DE DEPLOYMENT.
--
-- NO ejecutar a ciegas: requiere modo SQLCMD y revisión del runbook
-- (docs/03-diseno/runtime-least-privilege.md, sección de deployment).
--
-- Variables SQLCMD (ejemplo; jamás versionar valores reales):
--   :setvar DmkPassword     <password operativa de la master key de CALE_IMMEX>
--   :setvar CertPublicPath  <ruta temporal para el .cer público>
--
-- Alcance (sólo metadata de seguridad):
--   * CALE_IMMEX: master key si falta, certificado de firma, ADD SIGNATURE
--     del command y exportación del certificado PÚBLICO (sin clave privada).
--   * ANEXO24_DEV: certificado espejo (clave pública), cert-user y permisos
--     mínimos exactos: SELECT+UPDATE CargaPedimento (sin INSERT), SELECT
--     CargaPedimentoFila, SELECT ErrorCargaPedimento, EXECUTE bitácora.
--     El INSERT a BitacoraEvento lo resuelve la cadena de propiedad intra-DB.
--
-- Qué NO hace:
--   * no toca datos de negocio ni crea/mueve tablas;
--   * no modifica el cuerpo del command;
--   * no otorga DML directo al runtime ni schema grants;
--   * no usa TRUSTWORTHY ni DB_CHAINING;
--   * no usa EXECUTE AS.
--
-- ORDEN OBLIGATORIO DE DEPLOYMENT DEL COMMAND
-- (CREATE OR ALTER / ALTER PROCEDURE elimina la firma):
--   1. desplegar el procedure;
--   2. ejecutar este script (firma + certificado espejo + permisos);
--   3. verificación del bloque 3 (falla si la firma no existe);
--   4. smoke test de seguridad cross-db con una carga controlada.
-- ============================================

-- ---------------------------------------------------------------
-- BLOQUE 1 · CALE_IMMEX: certificado y firma del command
-- ---------------------------------------------------------------
USE CALE_IMMEX;
GO

IF NOT EXISTS (SELECT 1 FROM sys.symmetric_keys WHERE name = '##MS_DatabaseMasterKey##')
BEGIN
    CREATE MASTER KEY ENCRYPTION BY PASSWORD = N'$(DmkPassword)';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.certificates WHERE name = 'app24_pedimento_cert')
BEGIN
    CREATE CERTIFICATE app24_pedimento_cert
        WITH SUBJECT = N'Runtime hardening: firma cross-db de APP24_C_PEDIMENTO_CONFIRMAR';
END
GO

-- Idempotente: sólo firma si no hay firma vigente (re-firmar tras ALTER es
-- parte del deployment normal).
IF NOT EXISTS (
    SELECT 1
    FROM sys.crypt_properties
    WHERE class_desc = 'OBJECT_OR_COLUMN'
      AND major_id = OBJECT_ID('dbo.APP24_C_PEDIMENTO_CONFIRMAR')
)
BEGIN
    ADD SIGNATURE TO dbo.APP24_C_PEDIMENTO_CONFIRMAR BY CERTIFICATE app24_pedimento_cert;
END
GO

-- Exporta SOLO la clave pública (el .cer no contiene clave privada).
-- Si el archivo destino ya existe, eliminarlo antes o usar otra ruta:
-- el motor rechaza sobrescribir. No versionar el .cer generado.
BACKUP CERTIFICATE app24_pedimento_cert TO FILE = N'$(CertPublicPath)';
GO

-- ---------------------------------------------------------------
-- BLOQUE 2 · ANEXO24_DEV: certificado espejo y permisos mínimos
-- ---------------------------------------------------------------
USE ANEXO24_DEV;
GO

IF NOT EXISTS (SELECT 1 FROM sys.certificates WHERE name = 'app24_pedimento_cert')
BEGIN
    CREATE CERTIFICATE app24_pedimento_cert FROM FILE = N'$(CertPublicPath)';
END
GO

IF USER_ID('app24_pedimento_cert_user') IS NULL
BEGIN
    CREATE USER app24_pedimento_cert_user FOR CERTIFICATE app24_pedimento_cert;
END
GO

-- Permisos EXACTOS del principal de contexto del command.
GRANT SELECT, UPDATE ON OBJECT::app24.CargaPedimento TO app24_pedimento_cert_user;
GRANT SELECT ON OBJECT::app24.CargaPedimentoFila TO app24_pedimento_cert_user;
GRANT SELECT ON OBJECT::app24.ErrorCargaPedimento TO app24_pedimento_cert_user;
GRANT EXECUTE ON OBJECT::app24.APP24_C_BITACORA_REGISTRAR TO app24_pedimento_cert_user;
GO

-- ---------------------------------------------------------------
-- BLOQUE 3 · Verificación (falla si el deployment quedó incompleto)
-- ---------------------------------------------------------------
USE CALE_IMMEX;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.crypt_properties cp
    JOIN sys.certificates c ON c.thumbprint = cp.thumbprint
    WHERE cp.class_desc = 'OBJECT_OR_COLUMN'
      AND cp.major_id = OBJECT_ID('dbo.APP24_C_PEDIMENTO_CONFIRMAR')
      AND c.name = 'app24_pedimento_cert'
)
BEGIN
    THROW 50020, 'La firma de dbo.APP24_C_PEDIMENTO_CONFIRMAR no existe o no corresponde a app24_pedimento_cert. Revisar el bloque 1 (ADD SIGNATURE) antes de continuar.', 1;
END
GO

USE ANEXO24_DEV;
GO

IF USER_ID('app24_pedimento_cert_user') IS NULL
BEGIN
    THROW 50021, 'No existe app24_pedimento_cert_user en ANEXO24_DEV: el comando cross-db fallará.', 1;
END
GO

IF (SELECT COUNT(*) FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('app24_pedimento_cert_user')) <> 5
BEGIN
    THROW 50022, 'app24_pedimento_cert_user no tiene exactamente los 5 GRANT esperados (SELECT/UPDATE CargaPedimento, SELECT CargaPedimentoFila, SELECT ErrorCargaPedimento, EXECUTE bitácora).', 1;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('app24_pedimento_cert_user')
      AND (permission_name IN ('INSERT', 'DELETE') OR class_desc <> 'OBJECT_OR_COLUMN')
)
BEGIN
    THROW 50023, 'app24_pedimento_cert_user tiene permisos fuera del contrato mínimo (INSERT/DELETE o fuera de OBJECT).', 1;
END
GO

PRINT 'Cross-db signing listo: command firmado y cert-user con permisos mínimos. Ejecutar smoke test de seguridad antes de cerrar el deployment.';
GO
