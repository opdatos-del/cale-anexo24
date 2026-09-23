-- ============================================
-- ANEXO24_DEV: permisos mínimos de cuenta runtime
-- Ejecutar con identidad administrativa después de 02-app-schema.sql
-- y 03-app-seed-security.sql.
-- ============================================

USE ANEXO24_DEV;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_principals
    WHERE name = 'anexo24_app'
      AND type IN ('S', 'U')
)
BEGIN
    THROW 50001, 'No existe el usuario de base de datos anexo24_app en ANEXO24_DEV.', 1;
END
GO

-- Retira memberships heredados de bootstrap amplio, si existen.
IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals user_principal ON user_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = 'db_datareader'
      AND user_principal.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE db_datareader DROP MEMBER anexo24_app;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals user_principal ON user_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = 'db_datawriter'
      AND user_principal.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE db_datawriter DROP MEMBER anexo24_app;
END
GO

-- Revoca el permiso global heredado; futuros EXECUTE deben ser por objeto.
REVOKE EXECUTE TO anexo24_app;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_principals
    WHERE name = 'app24_runtime'
      AND type = 'R'
)
BEGIN
    CREATE ROLE app24_runtime;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals user_principal ON user_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = 'app24_runtime'
      AND user_principal.name = 'anexo24_app'
)
BEGIN
    ALTER ROLE app24_runtime ADD MEMBER anexo24_app;
END
GO

-- Permisos directos se conservan temporalmente: ownership chain LIVE incompatible
-- y runtime role/membership aún no están desplegados. Revocación queda pendiente.
GRANT SELECT ON OBJECT::app24.UsuarioApp TO app24_runtime;
GRANT SELECT ON OBJECT::app24.PerfilApp TO app24_runtime;
GRANT SELECT ON OBJECT::app24.PerfilActividad TO app24_runtime;
GRANT SELECT ON OBJECT::app24.Actividad TO app24_runtime;
GRANT INSERT ON OBJECT::app24.UsuarioApp TO app24_runtime;
GRANT UPDATE (nombre, correo) ON OBJECT::app24.UsuarioApp TO app24_runtime;
GRANT UPDATE (estado, perfil_id, vigencia) ON OBJECT::app24.UsuarioApp TO app24_runtime;
GRANT SELECT, INSERT ON OBJECT::app24.BitacoraEvento TO app24_runtime;

-- Queries read-only de SP-1B.
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_POR_CLAVE TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_ACCESO TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIOS_LISTAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_BITACORA_LISTAR TO app24_runtime;

-- Commands atómicos de SP-1C.
GRANT EXECUTE ON OBJECT::app24.APP24_C_USUARIO_CREAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_USUARIO_ACTUALIZAR_DATOS TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_USUARIO_CAMBIAR_ESTADO TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_USUARIO_CAMBIAR_PERFIL TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_USUARIO_CAMBIAR_VIGENCIA TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_BITACORA_REGISTRAR TO app24_runtime;
GO

PRINT 'Permisos mínimos app24_runtime aplicados a anexo24_app.';
GO
