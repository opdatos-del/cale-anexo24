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
ALTER ROLE db_datareader ADD MEMBER anexo24_app;
ALTER ROLE db_datawriter ADD MEMBER anexo24_app;
GRANT EXECUTE TO anexo24_app;
GO

PRINT 'ANEXO24_DEV bootstrap completed.';
GO
