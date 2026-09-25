-- ANEXO24_DEV: staging durable y contrato de plantilla de Facturación V2.
-- Migración incremental e idempotente. Ejecutar sólo en ANEXO24_DEV.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaFacturacionFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaFacturacionFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaFacturacionFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaFacturacionFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaFacturacion(id),
        CONSTRAINT CK_CargaFacturacionFila_DatosJson CHECK (ISJSON(datos_json) = 1)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaFacturacionFila_CargaFila'
               AND object_id = OBJECT_ID('app24.CargaFacturacionFila'))
    CREATE UNIQUE NONCLUSTERED INDEX IX_CargaFacturacionFila_CargaFila
        ON app24.CargaFacturacionFila (carga_id, fila);
GO

IF NOT EXISTS (SELECT 1 FROM app24.ConfiguracionPlantilla WHERE nombre = 'FACTURACION' AND version = 'LEGACY-2026-09')
BEGIN
    INSERT INTO app24.ConfiguracionPlantilla (nombre, version, extension, columnas_json, activa)
    VALUES ('FACTURACION', 'LEGACY-2026-09', 'XLSX',
      N'[{"nombre":"Documento","obligatoria":true,"tipo":"TEXTO"},{"nombre":"Fecha","obligatoria":true,"tipo":"FECHA"},{"nombre":"Almacen","obligatoria":false,"tipo":"TEXTO"},{"nombre":"Observaciones","obligatoria":false,"tipo":"TEXTO"},{"nombre":"Descarga","obligatoria":true,"tipo":"TEXTO"},{"nombre":"Tipo","obligatoria":true,"tipo":"TEXTO"},{"nombre":"Linea","obligatoria":true,"tipo":"TEXTO"},{"nombre":"Clave","obligatoria":true,"tipo":"TEXTO"},{"nombre":"Lote","obligatoria":false,"tipo":"TEXTO"},{"nombre":"Cantidad","obligatoria":true,"tipo":"DECIMAL"},{"nombre":"Unidad","obligatoria":true,"tipo":"TEXTO"},{"nombre":"Dirigido","obligatoria":false,"tipo":"TEXTO"},{"nombre":"Cliente","obligatoria":false,"tipo":"TEXTO"}]', 0);
END;
GO

IF NOT EXISTS (SELECT 1 FROM app24.ConfiguracionPlantilla WHERE nombre = 'FACTURACION' AND activa = 1)
BEGIN
    UPDATE app24.ConfiguracionPlantilla
    SET activa = 1
    WHERE nombre = 'FACTURACION' AND version = 'LEGACY-2026-09';
END;
GO

GRANT EXECUTE ON OBJECT::app24.APP24_C_FACTURACION_CARGA_CREAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_FACTURACION_CARGA_POR_HASH TO app24_runtime;

GO
