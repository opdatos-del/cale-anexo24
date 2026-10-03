USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmación técnica de una carga de productos mediante el staging legacy existente.
-- El catálogo autoritativo continúa siendo dbo.productos y sólo dbo.CARGA_PRODUCTOS
-- aplica sus reglas de negocio. No crea tablas ni lógica paralela de catálogo.
--
-- Comportamiento confirmado del legacy (audit READ ONLY 2026-10-03):
--   * DELETE FROM ECARGAPRODUCTO al inicio (sin transacción, sin TRY/CATCH).
--   * Inserta en dbo.productos sólo cuando CVE_PRODUCTO no existe y la fila
--     no aparece en dbo.ECargaProducto. Es INSERT-only; no actualiza productos
--     existentes ni los reporta como error.
--   * Validaciones: unidad válida, longitud de fracción = 8, longitud CVE_PRODUCTO
--     >= 3, longitud CVE_PRODUCTO_CLIENTE >= 3 cuando no es vacío, longitud
--     nombre >= 3 y duplicados internos marcados como error.
--   * Sin NOLOCK ni READ UNCOMMITTED; sin SET TRANSACTION ISOLATION LEVEL custom.
CREATE OR ALTER PROCEDURE dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR
    @CargaId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @CargaId IS NULL OR @CargaId < 1
        THROW 51501, 'PARAMETRO_INVALIDO', 1;

    DECLARE @Estado VARCHAR(20),
            @UsuarioId BIGINT,
            @CorrelacionId VARCHAR(40),
            @ConfirmadaEn DATETIME2(3),
            @Bloqueo BIGINT,
            @TotalFilas INT,
            @FilasConError INT,
            @DetalleBitacora VARCHAR(500),
            @EventoId BIGINT;
    DECLARE @Mapa TABLE (
        carga_producto_key BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL
    );
    DECLARE @Errores TABLE (
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Coordina tanto inserciones directas como ejecuciones no coordinadas de CARGA_PRODUCTOS.
        SELECT TOP (1) @Bloqueo = tmpPRODUCTOKEY
        FROM dbo.tmpproductos WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @Bloqueo = ECargaProductokey
        FROM dbo.ECargaProducto WITH (TABLOCKX, HOLDLOCK);

        IF EXISTS (SELECT 1 FROM dbo.tmpproductos)
           OR EXISTS (SELECT 1 FROM dbo.ECargaProducto)
            THROW 51502, 'LEGACY_STAGE_BUSY', 1;

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id,
               @ConfirmadaEn = fecha_confirmacion
        FROM ANEXO24_DEV.app24.CargaCatalogoProducto WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL
            THROW 51503, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA'
            THROW 51504, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA'
            THROW 51505, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaProducto WHERE carga_id = @CargaId)
            THROW 51506, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaCatalogoProductoFila WHERE carga_id = @CargaId)
            THROW 51507, 'CARGA_SIN_FILAS', 1;

        -- MERGE ON 1=0 conserva el vínculo entre identidad legacy y fila de origen.
        MERGE dbo.tmpproductos AS destino
        USING (
            SELECT f.hoja,
                   f.fila,
                   JSON_VALUE(f.datos_json, '$.CveProducto') AS cve_producto,
                   JSON_VALUE(f.datos_json, '$.Nombre') AS nombre,
                   JSON_VALUE(f.datos_json, '$.Unidad') AS unidad,
                   JSON_VALUE(f.datos_json, '$.Fraccion') AS fraccion,
                   JSON_VALUE(f.datos_json, '$.Division') AS division,
                   JSON_VALUE(f.datos_json, '$.CveProductoCliente') AS cve_producto_cliente,
                   JSON_VALUE(f.datos_json, '$.Auxiliar') AS auxiliar
            FROM ANEXO24_DEV.app24.CargaCatalogoProductoFila f
            WHERE f.carga_id = @CargaId
        ) AS origen
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion, DIVISION, CVE_PRODUCTO_CLIENTE, AUXILIAR)
            VALUES (origen.cve_producto, origen.nombre, origen.unidad, origen.fraccion,
                    origen.division, origen.cve_producto_cliente, origen.auxiliar)
        OUTPUT inserted.tmpPRODUCTOKEY, origen.hoja, origen.fila
            INTO @Mapa (carga_producto_key, hoja, fila);

        SELECT @TotalFilas = COUNT(*) FROM @Mapa;

        EXEC dbo.CARGA_PRODUCTOS;

        INSERT INTO @Errores (hoja, fila, codigo, mensaje)
        SELECT mapa.hoja, mapa.fila, 'LEGACY_VALIDATION', error_legacy.error
        FROM dbo.ECargaProducto error_legacy
        JOIN @Mapa mapa ON mapa.carga_producto_key = error_legacy.CargaProductoKey;

        IF EXISTS (SELECT 1 FROM @Errores)
        BEGIN
            -- Los cambios autoritativos y el staging legacy sólo sobreviven si todas las filas son válidas.
            ROLLBACK TRANSACTION;

            SELECT @FilasConError = COUNT(*)
            FROM (SELECT DISTINCT hoja, fila FROM @Errores) AS filas_error;

            BEGIN TRANSACTION;
            INSERT INTO ANEXO24_DEV.app24.ErrorCargaProducto
                (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
            SELECT @CargaId,
                   hoja,
                   fila,
                   'CargaProducto',
                   'no almacenado',
                   'LEGACY_VALIDATION',
                   LEFT(STRING_AGG(mensaje, ' | '), 500)
            FROM @Errores
            GROUP BY hoja, fila;

            UPDATE ANEXO24_DEV.app24.CargaCatalogoProducto
            SET estado = 'CON_ERRORES',
                total_filas = @TotalFilas,
                filas_validas = @TotalFilas - @FilasConError,
                filas_invalidas = @FilasConError,
                fecha_confirmacion = NULL
            WHERE id = @CargaId;

            SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas,
                ';filasConError=', @FilasConError);
            EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
                @UsuarioId = @UsuarioId,
                @Modulo = 'CATALOGOS_PRODUCTOS',
                @Accion = 'PRODUCTO_CARGA_CONFIRMACION_ERROR',
                @Detalle = @DetalleBitacora,
                @CorrelacionId = @CorrelacionId,
                @Resultado = 'FALLO',
                @EventoId = @EventoId OUTPUT;
            COMMIT TRANSACTION;

            SELECT @CargaId AS CargaId,
                   'CON_ERRORES' AS Estado,
                   @TotalFilas AS TotalFilas,
                   @TotalFilas - @FilasConError AS FilasValidas,
                   @FilasConError AS FilasConError,
                   CAST(NULL AS DATETIME2(3)) AS ConfirmadaEn,
                   'BUSINESS_ERRORS' AS Resultado;
            RETURN;
        END;

        -- El precheck garantiza una etapa ajena vacía; sólo se eliminan identidades de este lote.
        DELETE error_legacy
        FROM dbo.ECargaProducto error_legacy
        JOIN @Mapa mapa ON mapa.carga_producto_key = error_legacy.CargaProductoKey;
        DELETE carga_legacy
        FROM dbo.tmpproductos carga_legacy
        JOIN @Mapa mapa ON mapa.carga_producto_key = carga_legacy.tmpPRODUCTOKEY;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaCatalogoProducto
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'CATALOGOS_PRODUCTOS',
            @Accion = 'PRODUCTO_CARGA_CONFIRMADA',
            @Detalle = @DetalleBitacora,
            @CorrelacionId = @CorrelacionId,
            @Resultado = 'EXITO',
            @EventoId = @EventoId OUTPUT;
        COMMIT TRANSACTION;

        SELECT @CargaId AS CargaId,
               'CONFIRMADA' AS Estado,
               @TotalFilas AS TotalFilas,
               @TotalFilas AS FilasValidas,
               0 AS FilasConError,
               @ConfirmadaEn AS ConfirmadaEn,
               'CONFIRMED' AS Resultado;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO