-- ============================================
-- ANEXO24_DEV: Bootstrap database + app user
-- ============================================

-- 1. Create database
IF DB_ID('ANEXO24_DEV') IS NULL
BEGIN
    CREATE DATABASE ANEXO24_DEV;
END
GO

USE ANEXO24_DEV;
GO

-- 2. Create login (server-level)
IF NOT EXISTS (
    SELECT 1
    FROM sys.server_principals
    WHERE name = 'anexo24_app'
)
BEGIN
    CREATE LOGIN anexo24_app
    WITH PASSWORD = 'CAMBIAR_PASSWORD_LOCAL';
END
GO

-- 3. Create user (database-level)
IF NOT EXISTS (
    SELECT 1
    FROM sys.database_principals
    WHERE name = 'anexo24_app'
)
BEGIN
    CREATE USER anexo24_app
    FOR LOGIN anexo24_app;
END
GO

-- 4. Grant permissions
IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals user_principal ON user_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = 'db_datareader' AND user_principal.name = 'anexo24_app'
)
    ALTER ROLE db_datareader ADD MEMBER anexo24_app;

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals user_principal ON user_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = 'db_datawriter' AND user_principal.name = 'anexo24_app'
)
    ALTER ROLE db_datawriter ADD MEMBER anexo24_app;

GRANT EXECUTE TO anexo24_app;
GO

PRINT 'ANEXO24_DEV bootstrap completed.';
GO
