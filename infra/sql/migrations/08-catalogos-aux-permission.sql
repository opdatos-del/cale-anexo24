-- Permiso único para la superficie read-only de catálogos auxiliares.
-- BD: ANEXO24_DEV · esquema app24

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'CATALOGOS_AUX_CONSULTAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('CATALOGOS_AUX_CONSULTAR', 'Consultar catálogos auxiliares', 'catalogos', 'CONSULTAR');
GO

INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave = 'CATALOGOS_AUX_CONSULTAR'
WHERE p.nombre IN ('ADMINISTRADOR', 'CONSULTA')
  AND NOT EXISTS (
      SELECT 1
      FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id
        AND pa.actividad_id = a.id
  );
GO
