-- ============================================
-- Firma cross-database del command de pedimentos
-- RUNTIME IDENTITY HARDENING — PREPARACIÓN DE DEPLOYMENT.
--
-- NO ejecutar a ciegas: requiere modo SQLCMD y revisión del runbook
-- (docs/03-diseno/runtime-least-privilege.md, sección de deployment).
--
-- Variables SQLCMD (ejemplo; jamás versionar valores reales):
--   :setvar DmkPassword     <password operativa de la master key de CALE_IMMEX>
--   :setvar CertPublicPath  <ruta temporal ÚNICA por deployment, p. ej.
--                            app24_pedimento_<timestamp>.cer; la genera el
--                            runbook y su limpieza es operativa externa.
--                            El .cer es sólo clave pública: no contiene
--                            clave privada. No se borra vía SQL.>
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
--   3. verificación (bloques 3 y 4: thumbprint, firma y set exacto de permisos);
--   4. smoke test de seguridad cross-db con una carga controlada.
--
-- EJECUCIÓN SQLCMD CON FAIL-FAST:
--   :on error exit
--   :setvar DmkPassword     <password operativa de la master key de CALE_IMMEX>
--   :setvar CertPublicPath  <ruta temporal para el .cer público>
--
-- Si las variables no fueron sustituidas, el script falla ANTES de cualquier
-- DDL (SQLCMD_VARIABLE_GUARD). Las líneas que comienzan con ':' son directivas
-- SQLCMD: un runner que no sea sqlcmd debe sustituirlas o ignorarlas, nunca
-- enviarlas al motor. Los valores operativos no deben contener la secuencia '$('.
-- ============================================

:on error exit

-- Guard SQLCMD: sin variables sustituidas no se ejecuta ningún DDL.
IF N'$(DmkPassword)' LIKE N'%$(%' OR LEN(N'$(DmkPassword)') = 0
BEGIN
    THROW 50030, 'SQLCMD_VARIABLE_GUARD: DmkPassword no fue sustituida. Ejecutar con :setvar en modo SQLCMD. No se aplicó ningún DDL.', 1;
END
GO

IF N'$(CertPublicPath)' LIKE N'%$(%' OR LEN(N'$(CertPublicPath)') = 0
BEGIN
    THROW 50031, 'SQLCMD_VARIABLE_GUARD: CertPublicPath no fue sustituida. Ejecutar con :setvar en modo SQLCMD. No se aplicó ningún DDL.', 1;
END
GO

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
-- La ruta debe ser única por deployment (p. ej. app24_pedimento_<timestamp>.cer
-- generada por el runbook): el motor rechaza sobrescribir. Tras el deployment,
-- el archivo público temporal se elimina por mecanismo operativo externo;
-- este script no borra archivos.
BACKUP CERTIFICATE app24_pedimento_cert TO FILE = N'$(CertPublicPath)';
GO

-- ---------------------------------------------------------------
-- BLOQUE 2 · ANEXO24_DEV: importar certificado y validar el espejo
-- ANTES de crear cualquier principal o permiso.
-- CERTIFICATE_MIRROR_MATCH = REQUIRED: mismo certificado en ambas DB.
-- Fail closed: un certificado inconsistente NO se repara automáticamente.
-- ---------------------------------------------------------------
USE ANEXO24_DEV;
GO

IF NOT EXISTS (SELECT 1 FROM sys.certificates WHERE name = 'app24_pedimento_cert')
BEGIN
    CREATE CERTIFICATE app24_pedimento_cert FROM FILE = N'$(CertPublicPath)';
END
GO

DECLARE @thumbOrigen VARBINARY(32), @thumbDestino VARBINARY(32);

SELECT @thumbOrigen = thumbprint FROM CALE_IMMEX.sys.certificates WHERE name = 'app24_pedimento_cert';
SELECT @thumbDestino = thumbprint FROM ANEXO24_DEV.sys.certificates WHERE name = 'app24_pedimento_cert';

IF @thumbOrigen IS NULL
BEGIN
    THROW 50032, 'CERTIFICATE_MIRROR_MATCH: no existe app24_pedimento_cert en CALE_IMMEX.', 1;
END

IF @thumbDestino IS NULL
BEGIN
    THROW 50033, 'CERTIFICATE_MIRROR_MATCH: no existe app24_pedimento_cert en ANEXO24_DEV.', 1;
END

IF @thumbOrigen <> @thumbDestino
BEGIN
    THROW 50034, 'CERTIFICATE_MIRROR_MATCH: los thumbprints difieren entre CALE_IMMEX y ANEXO24_DEV. Fail closed: no se crean usuarios ni permisos; revisar el deployment y recrear el espejo manualmente.', 1;
END
GO

-- ---------------------------------------------------------------
-- BLOQUE 3 · Principal de contexto y permisos exactos
-- (sólo después de validar la identidad criptográfica del espejo)
-- ---------------------------------------------------------------
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
-- BLOQUE 4 · Verificación de firma y permisos exactos del cert-user
-- (falla si el deployment quedó incompleto)
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

-- CERT_USER_PERMISSION_SET_EXACT = YES: comparación exacta en ambas direcciones
-- (permission_name, esquema, objeto) con state = GRANT y clase OBJECT.
IF EXISTS (
    SELECT p.permission_name, s.name AS esquema, o.name AS objeto
    FROM sys.database_permissions p
    JOIN sys.objects o ON o.object_id = p.major_id
    JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE p.grantee_principal_id = USER_ID('app24_pedimento_cert_user')
      AND p.state = 'G'
      AND p.class_desc = 'OBJECT_OR_COLUMN'
    EXCEPT
    SELECT permission_name, esquema, objeto FROM (VALUES
        ('SELECT', 'app24', 'CargaPedimento'),
        ('UPDATE', 'app24', 'CargaPedimento'),
        ('SELECT', 'app24', 'CargaPedimentoFila'),
        ('SELECT', 'app24', 'ErrorCargaPedimento'),
        ('EXECUTE', 'app24', 'APP24_C_BITACORA_REGISTRAR')
    ) AS esperado(permission_name, esquema, objeto)
)
BEGIN
    THROW 50022, 'CERT_USER_PERMISSION_SET_EXACT: el cert-user tiene grants sobrantes o distintos al contrato mínimo.', 1;
END
GO

IF EXISTS (
    SELECT permission_name, esquema, objeto FROM (VALUES
        ('SELECT', 'app24', 'CargaPedimento'),
        ('UPDATE', 'app24', 'CargaPedimento'),
        ('SELECT', 'app24', 'CargaPedimentoFila'),
        ('SELECT', 'app24', 'ErrorCargaPedimento'),
        ('EXECUTE', 'app24', 'APP24_C_BITACORA_REGISTRAR')
    ) AS esperado(permission_name, esquema, objeto)
    EXCEPT
    SELECT p.permission_name, s.name AS esquema, o.name AS objeto
    FROM sys.database_permissions p
    JOIN sys.objects o ON o.object_id = p.major_id
    JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE p.grantee_principal_id = USER_ID('app24_pedimento_cert_user')
      AND p.state = 'G'
      AND p.class_desc = 'OBJECT_OR_COLUMN'
)
BEGIN
    THROW 50023, 'CERT_USER_PERMISSION_SET_EXACT: faltan grants del contrato mínimo del cert-user.', 1;
END
GO

-- DENY, GRANT_WITH_GRANT_OPTION o permisos fuera del contrato → fail closed.
-- (El único permiso no-object admitido es el CONNECT de base por defecto.)
IF EXISTS (
    SELECT 1
    FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('app24_pedimento_cert_user')
      AND NOT (state = 'G' AND (class_desc = 'OBJECT_OR_COLUMN'
              OR (class_desc = 'DATABASE' AND permission_name = 'CONNECT')))
)
BEGIN
    THROW 50024, 'CERT_USER_PERMISSION_SET_EXACT: el cert-user tiene DENY/WITH GRANT o permisos fuera del contrato (sólo se admite CONNECT de base).', 1;
END
GO

PRINT 'Cross-db signing listo: command firmado y cert-user con permisos mínimos. Ejecutar smoke test de seguridad antes de cerrar el deployment.';
GO
