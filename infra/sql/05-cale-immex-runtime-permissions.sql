-- ============================================
-- CALE_IMMEX: permisos mínimos de la identidad runtime
-- RUNTIME IDENTITY HARDENING (no es una migración de base de datos).
--
-- Qué hace (metadata de seguridad únicamente):
--   * exige que el login anexo24_app exista a nivel servidor (no lo crea);
--   * crea el user CALE_IMMEX.anexo24_app FOR LOGIN si falta;
--   * crea el rol cale_immex_runtime si falta y agrega al user como miembro;
--   * otorga EXECUTE por objeto (nunca por schema) a los 28 entry points
--     consumidos por Java (validado por scripts/check-runtime-sql-permissions.py).
--
-- Qué NO hace (política del proyecto):
--   * NO crea bases de datos, tablas ni mueve o duplica datos;
--   * NO crea logins;
--   * NO usa db_owner/db_datareader/db_datawriter/db_ddladmin;
--   * NO otorga SELECT/INSERT/UPDATE/DELETE directo sobre tablas;
--   * NO otorga EXECUTE a nivel schema;
--   * NO modifica los SP ni sus datos.
--
-- Ejecutar con identidad administrativa. Idempotente.
-- ============================================

USE CALE_IMMEX;
GO

-- 1. El login debe existir: este script no crea logins.
IF NOT EXISTS (
    SELECT 1
    FROM sys.server_principals
    WHERE name = 'anexo24_app'
      AND type = 'S'
)
BEGIN
    THROW 50010, 'No existe el login anexo24_app a nivel servidor. Crearlo por proceso autorizado antes de aplicar este script.', 1;
END
GO

-- 2. User de base para el login (sólo si falta).
IF NOT EXISTS (
    SELECT 1
    FROM sys.database_principals
    WHERE name = 'anexo24_app'
      AND type IN ('S', 'U')
)
BEGIN
    CREATE USER anexo24_app FOR LOGIN anexo24_app;
END
GO

-- 3. Retira memberships fijas prohibidas si existieran (hardening idempotente).
IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'db_datareader' AND miembro.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE db_datareader DROP MEMBER anexo24_app;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'db_datawriter' AND miembro.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE db_datawriter DROP MEMBER anexo24_app;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'db_owner' AND miembro.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE db_owner DROP MEMBER anexo24_app;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'db_ddladmin' AND miembro.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE db_ddladmin DROP MEMBER anexo24_app;
END
GO

-- 4. Rol propio del runtime (agrupa los EXECUTE; sin roles fijos).
IF NOT EXISTS (
    SELECT 1
    FROM sys.database_principals
    WHERE name = 'cale_immex_runtime'
      AND type = 'R'
)
BEGIN
    CREATE ROLE cale_immex_runtime;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'cale_immex_runtime'
      AND miembro.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE cale_immex_runtime ADD MEMBER anexo24_app;
END
GO

-- 5. EXECUTE por objeto: 33 entry points consumidos por Java (queries read-only
--    + commands autoritativos de pedimentos, confirmación de materiales, productos,
--    clientes, proveedores y agentes aduanales, actas y constancias). Nunca EXECUTE por schema.
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_AGENTES_ADUANALES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ALMACENES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_CATEGORIAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_CLIENTES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_COMPULSA_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_DATOS_GENERALES_OBTENER TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_DIRIGIDOS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ENTRADAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_ESTRUCTURAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_F4_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_MATERIALES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_PRODUCTOS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_PROVEEDORES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_RECTIFICACIONES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_SALDOS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_SALIDAS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_TIPOS_MATERIAL_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_UNIDADES_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_VENCIMIENTOS_LISTAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_PEDIMENTO_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_PROVEEDOR_CARGA_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_AGENTE_CARGA_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_ACTA_CARGA_CONFIRMAR TO cale_immex_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR TO cale_immex_runtime;
GO

PRINT 'Permisos mínimos cale_immex_runtime aplicados a anexo24_app.';
GO
