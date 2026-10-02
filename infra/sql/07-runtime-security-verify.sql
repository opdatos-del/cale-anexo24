-- ============================================
-- 07 · Verificación READ-ONLY del runtime least privilege
-- RUNTIME IDENTITY HARDENING — no aplica cambios.
--
-- Este script NO hace GRANT, REVOKE, ALTER ni CREATE: sólo SELECT/IF/THROW.
-- Falla (THROW) si detecta cualquier condición insegura o deriva del contrato:
--
--   * memberships en roles fijos (db_owner/db_datareader/db_datawriter/db_ddladmin);
--   * grants directos inesperados al usuario runtime (sólo se admite CONNECT);
--   * grants de rol que no sean EXECUTE por objeto;
--   * set exacto de EXECUTE por objeto distinto al contrato versionado
--     (sobrantes o faltantes) en ambas bases.
--
-- Uso: ejecutar con identidad administrativa DESPUÉS de aplicar 04/05/06.
-- Contrato mantenido en sincronía por scripts/check-runtime-sql-permissions.py.
-- ============================================

USE CALE_IMMEX;
GO

IF USER_ID('anexo24_app') IS NULL
BEGIN
    THROW 50100, 'RUNTIME_SECURITY_VERIFY: no existe anexo24_app en CALE_IMMEX.', 1;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'cale_immex_runtime' AND type = 'R')
BEGIN
    THROW 50101, 'RUNTIME_SECURITY_VERIFY: no existe el rol cale_immex_runtime.', 1;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'cale_immex_runtime' AND miembro.name = 'anexo24_app'
)
BEGIN
    THROW 50102, 'RUNTIME_SECURITY_VERIFY: anexo24_app no es miembro de cale_immex_runtime.', 1;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE miembro.name = 'anexo24_app'
      AND rol.name IN ('db_owner', 'db_datareader', 'db_datawriter', 'db_ddladmin')
)
BEGIN
    THROW 50103, 'RUNTIME_SECURITY_VERIFY: anexo24_app pertenece a un rol fijo prohibido.', 1;
END
GO

-- El usuario runtime sólo admite CONNECT de base; cualquier grant directo es deriva.
IF EXISTS (
    SELECT 1
    FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('anexo24_app')
      AND NOT (class_desc = 'DATABASE' AND permission_name = 'CONNECT' AND state = 'G')
)
BEGIN
    THROW 50104, 'RUNTIME_SECURITY_VERIFY: anexo24_app tiene grants directos inesperados en CALE_IMMEX.', 1;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('cale_immex_runtime')
      AND (permission_name <> 'EXECUTE' OR class_desc <> 'OBJECT_OR_COLUMN' OR state <> 'G')
)
BEGIN
    THROW 50105, 'RUNTIME_SECURITY_VERIFY: cale_immex_runtime tiene permisos que no son EXECUTE por objeto.', 1;
END
GO

DECLARE @esperado TABLE (esquema SYSNAME, objeto SYSNAME, PRIMARY KEY (esquema, objeto));
INSERT INTO @esperado (esquema, objeto) VALUES
    ('dbo', 'APP24_Q_ACTIVOS_FIJOS_LISTAR'),
    ('dbo', 'APP24_Q_AGENTES_ADUANALES_LISTAR'),
    ('dbo', 'APP24_Q_ALMACENES_LISTAR'),
    ('dbo', 'APP24_Q_ANALISIS_DESCARGAS_LISTAR'),
    ('dbo', 'APP24_Q_CATEGORIAS_LISTAR'),
    ('dbo', 'APP24_Q_CLIENTES_LISTAR'),
    ('dbo', 'APP24_Q_COMPULSA_LISTAR'),
    ('dbo', 'APP24_Q_DATOS_GENERALES_OBTENER'),
    ('dbo', 'APP24_Q_DIRIGIDOS_LISTAR'),
    ('dbo', 'APP24_Q_ENTRADAS_LISTAR'),
    ('dbo', 'APP24_Q_ESTRUCTURAS_LISTAR'),
    ('dbo', 'APP24_Q_MATERIALES_LISTAR'),
    ('dbo', 'APP24_Q_MATERIALES_UTILIZADOS_LISTAR'),
    ('dbo', 'APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR'),
    ('dbo', 'APP24_Q_PEDIMENTO_VALIDAR_REGLAS'),
    ('dbo', 'APP24_Q_PRODUCTOS_LISTAR'),
    ('dbo', 'APP24_Q_PROVEEDORES_LISTAR'),
    ('dbo', 'APP24_Q_RECTIFICACIONES_LISTAR'),
    ('dbo', 'APP24_Q_SALIDAS_LISTAR'),
    ('dbo', 'APP24_Q_TIPOS_MATERIAL_LISTAR'),
    ('dbo', 'APP24_Q_UNIDADES_LISTAR'),
    ('dbo', 'APP24_Q_VENCIMIENTOS_LISTAR'),
    ('dbo', 'APP24_C_PEDIMENTO_CONFIRMAR');

IF EXISTS (
    SELECT s.name, o.name
    FROM sys.database_permissions p
    JOIN sys.objects o ON o.object_id = p.major_id
    JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE p.grantee_principal_id = USER_ID('cale_immex_runtime')
      AND p.permission_name = 'EXECUTE'
      AND p.state = 'G'
    EXCEPT
    SELECT esquema, objeto FROM @esperado
)
BEGIN
    THROW 50106, 'RUNTIME_SECURITY_VERIFY: cale_immex_runtime tiene EXECUTE sobrante fuera del contrato.', 1;
END

IF EXISTS (
    SELECT esquema, objeto FROM @esperado
    EXCEPT
    SELECT s.name, o.name
    FROM sys.database_permissions p
    JOIN sys.objects o ON o.object_id = p.major_id
    JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE p.grantee_principal_id = USER_ID('cale_immex_runtime')
      AND p.permission_name = 'EXECUTE'
      AND p.state = 'G'
)
BEGIN
    THROW 50107, 'RUNTIME_SECURITY_VERIFY: faltan EXECUTE del contrato en cale_immex_runtime.', 1;
END
GO

USE ANEXO24_DEV;
GO

IF USER_ID('anexo24_app') IS NULL
BEGIN
    THROW 50110, 'RUNTIME_SECURITY_VERIFY: no existe anexo24_app en ANEXO24_DEV.', 1;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'app24_runtime' AND type = 'R')
BEGIN
    THROW 50111, 'RUNTIME_SECURITY_VERIFY: no existe el rol app24_runtime.', 1;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE rol.name = 'app24_runtime' AND miembro.name = 'anexo24_app'
)
BEGIN
    THROW 50112, 'RUNTIME_SECURITY_VERIFY: anexo24_app no es miembro de app24_runtime.', 1;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals rol ON rol.principal_id = drm.role_principal_id
    JOIN sys.database_principals miembro ON miembro.principal_id = drm.member_principal_id
    WHERE miembro.name = 'anexo24_app'
      AND rol.name IN ('db_owner', 'db_datareader', 'db_datawriter', 'db_ddladmin')
)
BEGIN
    THROW 50113, 'RUNTIME_SECURITY_VERIFY: anexo24_app pertenece a un rol fijo prohibido en ANEXO24_DEV.', 1;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('anexo24_app')
      AND NOT (class_desc = 'DATABASE' AND permission_name = 'CONNECT' AND state = 'G')
)
BEGIN
    THROW 50114, 'RUNTIME_SECURITY_VERIFY: anexo24_app tiene grants directos inesperados en ANEXO24_DEV.', 1;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.database_permissions
    WHERE grantee_principal_id = USER_ID('app24_runtime')
      AND (permission_name <> 'EXECUTE' OR class_desc <> 'OBJECT_OR_COLUMN' OR state <> 'G')
)
BEGIN
    THROW 50115, 'RUNTIME_SECURITY_VERIFY: app24_runtime tiene permisos que no son EXECUTE por objeto.', 1;
END
GO

DECLARE @esperadoApp TABLE (esquema SYSNAME, objeto SYSNAME, PRIMARY KEY (esquema, objeto));
INSERT INTO @esperadoApp (esquema, objeto) VALUES
    ('app24', 'APP24_Q_USUARIO_POR_CLAVE'),
    ('app24', 'APP24_Q_USUARIO_ACCESO'),
    ('app24', 'APP24_Q_USUARIOS_LISTAR'),
    ('app24', 'APP24_Q_USUARIO_OBTENER'),
    ('app24', 'APP24_C_USUARIO_CREAR'),
    ('app24', 'APP24_C_USUARIO_ACTUALIZAR_DATOS'),
    ('app24', 'APP24_C_USUARIO_CAMBIAR_ESTADO'),
    ('app24', 'APP24_C_USUARIO_CAMBIAR_PERFIL'),
    ('app24', 'APP24_C_USUARIO_CAMBIAR_VIGENCIA'),
    ('app24', 'APP24_C_USUARIO_RESTABLECER_PASSWORD'),
    ('app24', 'APP24_Q_PERFILES_LISTAR'),
    ('app24', 'APP24_Q_PERFIL_PERMISOS_LISTAR'),
    ('app24', 'APP24_C_PERFIL_CREAR'),
    ('app24', 'APP24_C_PERFIL_ACTUALIZAR_NOMBRE'),
    ('app24', 'APP24_C_PERFIL_CAMBIAR_ESTADO'),
    ('app24', 'APP24_C_PERFIL_REEMPLAZAR_PERMISOS'),
    ('app24', 'APP24_Q_ACTIVIDADES_LISTAR'),
    ('app24', 'APP24_Q_BITACORA_LISTAR'),
    ('app24', 'APP24_C_BITACORA_REGISTRAR'),
    ('app24', 'APP24_Q_PEDIMENTO_CARGA_POR_HASH'),
    ('app24', 'APP24_Q_PEDIMENTO_CARGA_OBTENER'),
    ('app24', 'APP24_Q_PEDIMENTO_CARGA_ERRORES'),
    ('app24', 'APP24_C_PEDIMENTO_CARGA_CREAR'),
    ('app24', 'APP24_Q_FACTURACION_CARGA_POR_HASH'),
    ('app24', 'APP24_Q_FACTURACION_CARGA_OBTENER'),
    ('app24', 'APP24_C_FACTURACION_CARGA_CREAR'),
    ('app24', 'APP24_Q_FACTURACION_PLANTILLA_ACTIVA'),
    ('app24', 'APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH'),
    ('app24', 'APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER'),
    ('app24', 'APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES'),
    ('app24', 'APP24_C_CATALOGO_MATERIAL_CARGA_CREAR'),
    ('app24', 'APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH'),
    ('app24', 'APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER'),
    ('app24', 'APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES'),
    ('app24', 'APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR');

IF EXISTS (
    SELECT s.name, o.name
    FROM sys.database_permissions p
    JOIN sys.objects o ON o.object_id = p.major_id
    JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE p.grantee_principal_id = USER_ID('app24_runtime')
      AND p.permission_name = 'EXECUTE'
      AND p.state = 'G'
    EXCEPT
    SELECT esquema, objeto FROM @esperadoApp
)
BEGIN
    THROW 50116, 'RUNTIME_SECURITY_VERIFY: app24_runtime tiene EXECUTE sobrante fuera del contrato.', 1;
END

IF EXISTS (
    SELECT esquema, objeto FROM @esperadoApp
    EXCEPT
    SELECT s.name, o.name
    FROM sys.database_permissions p
    JOIN sys.objects o ON o.object_id = p.major_id
    JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE p.grantee_principal_id = USER_ID('app24_runtime')
      AND p.permission_name = 'EXECUTE'
      AND p.state = 'G'
)
BEGIN
    THROW 50117, 'RUNTIME_SECURITY_VERIFY: faltan EXECUTE del contrato en app24_runtime.', 1;
END
GO

PRINT 'RUNTIME_SECURITY_VERIFY|PASS';
GO
