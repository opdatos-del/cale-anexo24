-- ANEXO24_DEV: garantiza una sola plantilla activa de Facturación por nombre.
-- Migración incremental e idempotente. Ejecutar sólo en ANEXO24_DEV.
USE ANEXO24_DEV;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF EXISTS (
    SELECT nombre
    FROM app24.ConfiguracionPlantilla
    WHERE activa = 1
    GROUP BY nombre
    HAVING COUNT(*) > 1
)
    THROW 51170, 'CONFIGURACION_PLANTILLA_ACTIVA_DUPLICADA', 1;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_ConfiguracionPlantilla_NombreActiva'
      AND object_id = OBJECT_ID('app24.ConfiguracionPlantilla')
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_ConfiguracionPlantilla_NombreActiva
        ON app24.ConfiguracionPlantilla (nombre)
        WHERE activa = 1;
END;
GO
