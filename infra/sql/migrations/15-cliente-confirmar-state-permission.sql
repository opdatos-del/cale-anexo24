-- Habilita la confirmación autoritativa de cargas de clientes y declara su permiso separado.
-- La operación autoritativa se ejecuta exclusivamente en CALE_IMMEX.dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR.
USE ANEXO24_DEV;
GO

DECLARE @RestriccionEstado SYSNAME = (
    SELECT TOP (1) name
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('app24.CargaCatalogoCliente')
      AND definition LIKE '%estado%'
    ORDER BY name
);

IF @RestriccionEstado IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.check_constraints
       WHERE parent_object_id = OBJECT_ID('app24.CargaCatalogoCliente')
         AND name = @RestriccionEstado
         AND definition LIKE '%CONFIRMADA%'
   )
BEGIN
    DECLARE @SqlDrop NVARCHAR(500) =
        N'ALTER TABLE app24.CargaCatalogoCliente DROP CONSTRAINT ' + QUOTENAME(@RestriccionEstado);
    EXEC sp_executesql @SqlDrop;
    SET @RestriccionEstado = NULL;
END;

IF @RestriccionEstado IS NULL
BEGIN
    ALTER TABLE app24.CargaCatalogoCliente WITH CHECK
        ADD CONSTRAINT CK_CargaCatalogoCliente_estado
        CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES', 'CONFIRMADA'));
END;
GO

IF COL_LENGTH('app24.CargaCatalogoCliente', 'fecha_confirmacion') IS NULL
BEGIN
    ALTER TABLE app24.CargaCatalogoCliente ADD fecha_confirmacion DATETIME2(3) NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'CLIENTES_CONFIRMAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('CLIENTES_CONFIRMAR', 'Confirmar clientes', 'clientes', 'CONFIRMAR');
GO

INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave = 'CLIENTES_CONFIRMAR'
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id
        AND pa.actividad_id = a.id
  );
GO

PRINT 'Migration 15-cliente-confirmar-state-permission aplicada.';
GO