-- ============================================
-- ANEXO24 - Seed inicial de seguridad
-- BD: ANEXO24_DEV · Esquema app24
-- Perfiles, actividades (permisos) y usuario administrador
-- ============================================

-- Perfiles
IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WHERE nombre = 'ADMINISTRADOR')
    INSERT INTO app24.PerfilApp (nombre) VALUES ('ADMINISTRADOR');
IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WHERE nombre = 'CONSULTA')
    INSERT INTO app24.PerfilApp (nombre) VALUES ('CONSULTA');
GO

-- Actividades / permisos (recurso_ACCION = clave de permiso de API)
INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
SELECT v.clave, v.nombre, v.recurso, v.accion
FROM (VALUES
    ('MATERIALES_CONSULTAR',    'Consultar materiales',    'materiales',    'CONSULTAR'),
    ('PRODUCTOS_CONSULTAR',     'Consultar productos',     'productos',     'CONSULTAR'),
    ('ESTRUCTURAS_CONSULTAR',   'Consultar estructuras',   'estructuras',   'CONSULTAR'),
    ('OPERACIONES_CONSULTAR',   'Consultar operaciones',   'operaciones',   'CONSULTAR'),
    ('REPORTES_GENERAR',        'Generar reportes',        'reportes',      'GENERAR'),
    ('REPORTES_EXPORTAR',       'Exportar reportes',       'reportes',      'EXPORTAR'),
    ('FACTURACION_CARGAR',      'Cargar facturación',      'facturacion',   'CARGAR'),
    ('FACTURACION_GUARDAR',     'Guardar facturación',     'facturacion',   'GUARDAR'),
    ('BITACORA_CONSULTAR',      'Consultar bitácora',      'bitacora',      'CONSULTAR'),
    ('USUARIOS_ADMINISTRAR',    'Administrar usuarios',    'usuarios',      'ADMINISTRAR'),
    ('PERFILES_ADMINISTRAR',   'Administrar perfiles',    'perfiles',      'ADMINISTRAR'),
    ('ACTIVIDADES_ADMINISTRAR','Administrar actividades', 'actividades',   'ADMINISTRAR')
) AS v(clave, nombre, recurso, accion)
WHERE NOT EXISTS (SELECT 1 FROM app24.Actividad a WHERE a.clave = v.clave);
GO

-- ADMINISTRADOR: todos los permisos faltantes
INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
CROSS JOIN app24.Actividad a
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id
        AND pa.actividad_id = a.id
  );
GO

-- CONSULTA: solo lectura de catálogos
INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave IN (
    'MATERIALES_CONSULTAR',
    'PRODUCTOS_CONSULTAR',
    'ESTRUCTURAS_CONSULTAR',
    'OPERACIONES_CONSULTAR'
)
WHERE p.nombre = 'CONSULTA'
  AND NOT EXISTS (
      SELECT 1
      FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id
        AND pa.actividad_id = a.id
  );
GO

-- Usuario administrador inicial.
-- Debe cambiarse la contraseña/hash antes de utilizar el ambiente productivo.
IF NOT EXISTS (SELECT 1 FROM app24.UsuarioApp WHERE clave = 'admin')
BEGIN
    INSERT INTO app24.UsuarioApp (clave, nombre, correo, password_hash, perfil_id)
    SELECT 'admin', 'Administrador del Sistema', 'admin@anexo24.mx',
           '$2b$10$n9NvR0IcyoA0SnbZbiYi2ezi3wyOfj26dpQNaIFBgfOJXUYKhn3.K',
           p.id
    FROM app24.PerfilApp p
    WHERE p.nombre = 'ADMINISTRADOR';
END;
GO

PRINT '=== SEED SEGURIDAD app24 CARGADO ===';
GO
