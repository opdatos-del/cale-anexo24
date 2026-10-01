-- Permiso de confirmación autoritativa de pedimentos, separado de PEDIMENTOS_CARGAR.
-- BD: ANEXO24_DEV · esquema app24
--
-- No se asigna automáticamente a ningún perfil productivo: el administrador lo
-- otorga desde el módulo de Perfiles. La confirmación autoritativa se implementará
-- en una fase posterior; este archivo sólo declara la actividad.

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'PEDIMENTOS_CONFIRMAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('PEDIMENTOS_CONFIRMAR', 'Confirmar pedimentos', 'pedimentos', 'CONFIRMAR');
GO
