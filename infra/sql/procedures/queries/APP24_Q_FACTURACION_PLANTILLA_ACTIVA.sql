USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_FACTURACION_PLANTILLA_ACTIVA
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TOP (1) id, nombre, version, extension, columnas_json
    FROM app24.ConfiguracionPlantilla
    WHERE nombre = 'FACTURACION' AND activa = 1
    ORDER BY id DESC;
END;
GO
