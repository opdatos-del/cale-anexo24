USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmación autoritativa de una carga de pedimentos.
-- Vive en CALE_IMMEX.dbo (datos autoritativos); actualiza app24 por nombre de 3 partes.
-- Una sola transacción local: locks, recheck, key allocation, writes, estado y bitácora.
-- SP-FIRST: reutiliza dbo.FACTOR/dbo.VALIDUNIT (read-only) y app24.APP24_C_BITACORA_REGISTRAR.
CREATE OR ALTER PROCEDURE dbo.APP24_C_PEDIMENTO_CONFIRMAR
    @CargaId BIGINT,
    @UsuarioId BIGINT,
    @CorrelacionId VARCHAR(40) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @CargaId IS NULL OR @CargaId < 1 OR @UsuarioId IS NULL OR @UsuarioId < 1
        THROW 51401, 'PARAMETRO_INVALIDO', 1;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Guardia de same-load: sólo coordina solicitudes de nuestra aplicación.
        -- No sustituye TABLOCKX/HOLDLOCK frente al motor legacy.
        DECLARE @LockRecurso VARCHAR(255) = 'ANEXO24_PEDIMENTO_CONFIRMACION_' + CONVERT(VARCHAR(20), @CargaId);
        DECLARE @LockResultado INT;
        EXEC @LockResultado = sp_getapplock @Resource = @LockRecurso, @LockMode = 'Exclusive',
             @LockOwner = 'Transaction', @LockTimeout = 15000;
        IF @LockResultado < 0 THROW 51402, 'CARGA_BLOQUEADA', 1;

        DECLARE @Estado VARCHAR(20),
                @Version VARCHAR(40),
                @FechaConfirmacion DATETIME2(3);
        SELECT @Estado = estado,
               @Version = version_plantilla,
               @FechaConfirmacion = fecha_confirmacion
        FROM ANEXO24_DEV.app24.CargaPedimento WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL THROW 51403, 'CARGA_NO_ENCONTRADA', 1;

        IF @Estado = 'CONFIRMADA'
        BEGIN
            COMMIT TRANSACTION;
            SELECT @CargaId AS CargaId, @Estado AS Estado, NULL AS TipoOperacion,
                   0 AS OperacionesProcesadas, 0 AS PartidasProcesadas,
                   @FechaConfirmacion AS FechaConfirmacion, 'ALREADY_CONFIRMED' AS Resultado;
            RETURN;
        END

        IF @Estado <> 'PREVISUALIZADA' THROW 51404, 'CARGA_NO_CONFIRMABLE', 1;
        IF @Version <> 'LEGACY-STAGE-DERIVED-V2' THROW 51405, 'STAGING_VERSION_NOT_CONFIRMABLE', 1;

        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaPedimento WHERE carga_id = @CargaId)
            THROW 51406, 'CARGA_CON_ERRORES', 1;

        -- Staging tipado (JSON_VALUE funciona desde CALE_IMMEX; OPENJSON no).
        CREATE TABLE #filas (
            fila INT NULL,
            tipo_operacion INT NULL,
            tipo_pedimento INT NULL,
            aduana VARCHAR(50) NULL,
            patente VARCHAR(50) NULL,
            numero_pedimento VARCHAR(50) NULL,
            clave_pedimento VARCHAR(50) NULL,
            tc FLOAT NULL,
            fecha_pago DATETIME2(3) NULL,
            fecha_entrada DATETIME2(3) NULL,
            clave_cp VARCHAR(50) NULL,
            sec INT NULL,
            clave VARCHAR(50) NULL,
            descripcion VARCHAR(250) NULL,
            fraccion VARCHAR(50) NULL,
            categoria VARCHAR(10) NULL,
            cantidad_comercial FLOAT NULL,
            unidad_comercial VARCHAR(50) NULL,
            cantidad_tarifa FLOAT NULL,
            unidad_tarifa VARCHAR(50) NULL,
            pais_od VARCHAR(50) NULL,
            pais_cv VARCHAR(50) NULL,
            valor_dolares FLOAT NULL,
            valor_comercial FLOAT NULL,
            valor_aduanal FLOAT NULL,
            valor_me FLOAT NULL,
            factura VARCHAR(50) NULL,
            fecha_factura DATETIME2(3) NULL,
            pedimento_original VARCHAR(50) NULL,
            descarga_dirigida VARCHAR(50) NULL,
            descarga VARCHAR(10) NULL,
            apartado VARCHAR(2) NULL,
            tasa_igie FLOAT NULL,
            fp_igie INT NULL,
            tasa_iva FLOAT NULL,
            fp_iva INT NULL,
            cnt DECIMAL(38, 10) NULL,
            multas DECIMAL(38, 10) NULL,
            recargos DECIMAL(38, 10) NULL,
            iva_pre DECIMAL(38, 10) NULL,
            igie FLOAT NULL,
            iva FLOAT NULL,
            dta FLOAT NULL,
            prev FLOAT NULL,
            tipo_tasa_igie VARCHAR(50) NULL,
            cove VARCHAR(100) NULL,
            factor_inc FLOAT NULL,
            fme FLOAT NULL,
            peso_bruto FLOAT NULL,
            incoterm VARCHAR(5) NULL,
            nico VARCHAR(5) NULL,
            lote VARCHAR(50) NULL,
            complemento1 VARCHAR(50) NULL,
            complemento2 VARCHAR(50) NULL,
            complemento3 VARCHAR(50) NULL,
            marca VARCHAR(50) NULL,
            modelo VARCHAR(50) NULL,
            serie VARCHAR(50) NULL
        );

        INSERT INTO #filas
        SELECT fila,
               TRY_CAST(JSON_VALUE(datos_json, '$.TipoOperacion') AS INT),
               TRY_CAST(JSON_VALUE(datos_json, '$.TipoPedimento') AS INT),
               JSON_VALUE(datos_json, '$.Aduana'),
               JSON_VALUE(datos_json, '$.Patente'),
               JSON_VALUE(datos_json, '$.NumeroPedimento'),
               JSON_VALUE(datos_json, '$.ClavePedimento'),
               TRY_CAST(JSON_VALUE(datos_json, '$.tc') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.FechaPago') AS DATETIME2(3)),
               TRY_CAST(JSON_VALUE(datos_json, '$.FECHAENTRADA') AS DATETIME2(3)),
               JSON_VALUE(datos_json, '$.ClaveCP'),
               TRY_CAST(JSON_VALUE(datos_json, '$.Sec') AS INT),
               JSON_VALUE(datos_json, '$.Clave'),
               JSON_VALUE(datos_json, '$.Descripcion'),
               JSON_VALUE(datos_json, '$.Fraccion'),
               JSON_VALUE(datos_json, '$.CATEGORIA'),
               TRY_CAST(JSON_VALUE(datos_json, '$.CantidadComercial') AS FLOAT),
               JSON_VALUE(datos_json, '$.UnidadComercial'),
               TRY_CAST(JSON_VALUE(datos_json, '$.CantidadTarifa') AS FLOAT),
               JSON_VALUE(datos_json, '$.UnidadTarifa'),
               JSON_VALUE(datos_json, '$.PaisOD'),
               JSON_VALUE(datos_json, '$.PaisCV'),
               TRY_CAST(JSON_VALUE(datos_json, '$.ValorDolares') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.ValorComercial') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.ValorAduanal') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.ValorME') AS FLOAT),
               JSON_VALUE(datos_json, '$.Factura'),
               TRY_CAST(JSON_VALUE(datos_json, '$.FechaFactura') AS DATETIME2(3)),
               JSON_VALUE(datos_json, '$.PedimentoOriginal'),
               JSON_VALUE(datos_json, '$.DescargaDirigida'),
               JSON_VALUE(datos_json, '$.Descarga'),
               JSON_VALUE(datos_json, '$.APARTADO'),
               TRY_CAST(JSON_VALUE(datos_json, '$.TASAIGIE') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.FPIGIE') AS INT),
               TRY_CAST(JSON_VALUE(datos_json, '$.TASAIVA') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.FPIVA') AS INT),
               TRY_CAST(JSON_VALUE(datos_json, '$.CNT') AS DECIMAL(38, 10)),
               TRY_CAST(JSON_VALUE(datos_json, '$.MULTAS') AS DECIMAL(38, 10)),
               TRY_CAST(JSON_VALUE(datos_json, '$.RECARGOS') AS DECIMAL(38, 10)),
               TRY_CAST(JSON_VALUE(datos_json, '$.IVA_PRE') AS DECIMAL(38, 10)),
               TRY_CAST(JSON_VALUE(datos_json, '$.IGIE') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.IVA') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.DTA') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.PREV') AS FLOAT),
               JSON_VALUE(datos_json, '$.TIPOTASAIGIE'),
               JSON_VALUE(datos_json, '$.COVE'),
               TRY_CAST(JSON_VALUE(datos_json, '$.FACTORINC') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.FME') AS FLOAT),
               TRY_CAST(JSON_VALUE(datos_json, '$.PESOBRUTO') AS FLOAT),
               JSON_VALUE(datos_json, '$.INCOTERM'),
               JSON_VALUE(datos_json, '$.NICO'),
               JSON_VALUE(datos_json, '$.lote'),
               JSON_VALUE(datos_json, '$.complemento1'),
               JSON_VALUE(datos_json, '$.complemento2'),
               JSON_VALUE(datos_json, '$.complemento3'),
               JSON_VALUE(datos_json, '$.marca'),
               JSON_VALUE(datos_json, '$.modelo'),
               JSON_VALUE(datos_json, '$.serie')
        FROM ANEXO24_DEV.app24.CargaPedimentoFila
        WHERE carga_id = @CargaId;

        IF NOT EXISTS (SELECT 1 FROM #filas) THROW 51407, 'CARGA_SIN_FILAS', 1;

        IF EXISTS (SELECT 1 FROM #filas WHERE tipo_operacion NOT IN (1, 2))
            THROW 51408, 'CARGA_TIPO_OPERACION_INVALIDO', 1;

        -- Cargas mixtas soportadas: se procesa cada fila según su TipoOperacion.
        DECLARE @TieneImport BIT = CASE WHEN EXISTS (SELECT 1 FROM #filas WHERE tipo_operacion = 1) THEN 1 ELSE 0 END;
        DECLARE @TieneExport BIT = CASE WHEN EXISTS (SELECT 1 FROM #filas WHERE tipo_operacion = 2) THEN 1 ELSE 0 END;
        DECLARE @TipoOperacion INT = CASE WHEN @TieneImport = 1 AND @TieneExport = 1 THEN NULL
                                          WHEN @TieneImport = 1 THEN 1 ELSE 2 END;
        DECLARE @FechaTexto DATETIME2(3) = SYSUTCDATETIME();
        DECLARE @Operaciones INT = 0, @Partidas INT = 0;

        -- Orden global de locks: IMPORTACIONES, PARTIDAS, SALIDAS, PSALIDAS, DIRIGIDO.
        -- Se adquieren sólo los requeridos, conservando siempre ese orden relativo.
        -- La asignación evita devolver result sets intermedios al cliente.
        DECLARE @DummyLock INT;
        IF @TieneImport = 1
        BEGIN
            SELECT TOP 0 @DummyLock = 1 FROM dbo.IMPORTACIONES WITH (TABLOCKX, HOLDLOCK);
            SELECT TOP 0 @DummyLock = 1 FROM dbo.PARTIDAS WITH (TABLOCKX, HOLDLOCK);
        END
        IF @TieneExport = 1
        BEGIN
            SELECT TOP 0 @DummyLock = 1 FROM dbo.SALIDAS WITH (TABLOCKX, HOLDLOCK);
            SELECT TOP 0 @DummyLock = 1 FROM dbo.PSALIDAS WITH (TABLOCKX, HOLDLOCK);
            SELECT TOP 0 @DummyLock = 1 FROM dbo.DIRIGIDO WITH (TABLOCKX, HOLDLOCK);
        END

        IF @TieneImport = 1
        BEGIN
            IF EXISTS (SELECT 1 FROM #filas f WHERE f.tipo_operacion = 1 AND NOT EXISTS
                       (SELECT 1 FROM dbo.MATERIAL m WHERE m.CLAVE = f.clave))
                THROW 51410, 'PED-003', 1;

            IF EXISTS (SELECT 1 FROM #filas f WHERE f.tipo_operacion = 1 AND EXISTS
                       (SELECT 1 FROM dbo.IMPORTACIONES i WHERE i.NUMERO_PED = f.numero_pedimento))
                THROW 51411, 'DUPLICATE_OPERATION', 1;

            DECLARE @BaseImportacion BIGINT =
                (SELECT ISNULL(MAX(IPEDIMENTOKEY), 0) FROM dbo.IMPORTACIONES);
            DECLARE @BasePartida BIGINT =
                (SELECT ISNULL(MAX(PARTIDAKEY), 0) FROM dbo.PARTIDAS);

            ;WITH Pedimentos AS (
                SELECT numero_pedimento,
                       ROW_NUMBER() OVER (ORDER BY numero_pedimento) AS rn
                FROM (SELECT DISTINCT numero_pedimento FROM #filas WHERE tipo_operacion = 1) AS d
            )
            INSERT INTO dbo.IMPORTACIONES
                (IPEDIMENTOKEY, ADUANA, PATENTE, NUMERO_PED, FECHA, TIPOOPER, CVE_PEDIMENTO, TC,
                 DESCARGA, PEDIMENTOORIGINAL, CVEPROVEEDOR, IVA, DTA, PREVALIDACION, CNT, ADVALOREM,
                 MULTAS, RECARGOS, FECHA_AD, IVA_PRE)
            SELECT @BaseImportacion + p.rn,
                   MIN(f.aduana), MIN(f.patente), p.numero_pedimento, MIN(f.fecha_pago), 'IMPORTACION',
                   MIN(f.clave_pedimento), MIN(f.tc),
                   CASE WHEN MIN(ISNULL(f.descarga, '')) = ''
                        THEN CASE WHEN MIN(ISNULL(f.tipo_pedimento, 1)) = 2 THEN 'NO' ELSE 'SI' END
                        ELSE MIN(f.descarga) END,
                   MIN(f.pedimento_original),
                   (SELECT TOP 1 x.clave_cp FROM #filas x WHERE x.numero_pedimento = p.numero_pedimento),
                   SUM(ISNULL(f.iva, 0)), MAX(ISNULL(f.dta, 0)), MAX(ISNULL(f.prev, 0)),
                   MAX(ISNULL(f.cnt, 0)), SUM(ISNULL(f.igie, 0)),
                   MAX(ISNULL(f.multas, 0)), MAX(ISNULL(f.recargos, 0)),
                   MIN(f.fecha_entrada), MAX(ISNULL(f.iva_pre, 0))
            FROM #filas f
            JOIN Pedimentos p ON p.numero_pedimento = f.numero_pedimento AND f.tipo_operacion = 1
            GROUP BY p.numero_pedimento, p.rn;

            SET @Operaciones = @@ROWCOUNT;

            INSERT INTO dbo.PARTIDAS
                (PARTIDAKEY, CLAVE, DESCRIPCION, FRACCION, CANTIDAD, UNIDAD, VAL_ADUANAL, VAL_DOLARES,
                 ORIGEN, SALDO, CATEGORIA, PROVEEDOR, CANTIDADT, UNIDADT, VAL_COMERCIAL, FACTURA,
                 FECHAFACTURA, PARTIDA, IMPORTACIONLINK, MONTOIGI, MONTOIVA, MONTOCCOMPEN, PAISVENDEDOR,
                 LOTE, COMPLEMENTO1, COMPLEMENTO2, COMPLEMENTO3, valorME, arancel, fpigi, fpiva, tasaiva,
                 tipotasaigie, marca, modelo, serie, COVE, FME, FACINC, ESACTIVO, PESOBRUTO, INCOTERM, NICO)
            SELECT @BasePartida + ROW_NUMBER() OVER (ORDER BY f.numero_pedimento, f.sec),
                   f.clave, f.descripcion, f.fraccion,
                   f.cantidad_comercial * dbo.FACTOR('', f.unidad_comercial, f.clave),
                   (SELECT MIN(m.UNIDAD) FROM dbo.MATERIAL m WHERE m.CLAVE = f.clave),
                   f.valor_aduanal, f.valor_dolares, f.pais_od, f.cantidad_comercial, f.categoria,
                   f.clave_cp, f.cantidad_tarifa, dbo.VALIDUNIT(f.unidad_tarifa), f.valor_comercial,
                   f.factura, f.fecha_factura, f.sec,
                   (SELECT i.IPEDIMENTOKEY FROM dbo.IMPORTACIONES i WHERE i.NUMERO_PED = f.numero_pedimento),
                   f.igie, f.iva, 0, f.pais_cv, ISNULL(f.lote, ''), ISNULL(f.complemento1, ''),
                   ISNULL(f.complemento2, ''), ISNULL(f.complemento3, ''), f.valor_me, f.tasa_igie,
                   f.fp_igie, f.fp_iva, f.tasa_iva, f.tipo_tasa_igie, f.marca, f.modelo, f.serie,
                   f.cove, f.fme, f.factor_inc, 'S', f.peso_bruto, f.incoterm, f.nico
            FROM #filas f
            WHERE f.tipo_operacion = 1;

            SET @Partidas = @@ROWCOUNT;
        END

        IF @TieneExport = 1
        BEGIN
            IF EXISTS (SELECT 1 FROM #filas f WHERE f.tipo_operacion = 2 AND NOT EXISTS
                       (SELECT 1 FROM dbo.PRODUCTOS p WHERE p.CVE_PRODUCTO = f.clave))
                THROW 51412, 'PED-004', 1;

            IF EXISTS (SELECT 1 FROM #filas f WHERE f.tipo_operacion = 2 AND EXISTS
                       (SELECT 1 FROM dbo.SALIDAS s WHERE s.DOCUMENTO = f.numero_pedimento))
                THROW 51413, 'DUPLICATE_OPERATION', 1;

            DECLARE @BaseSalida BIGINT =
                (SELECT ISNULL(MAX(SALIDAKEY), 0) FROM dbo.SALIDAS);
            DECLARE @BasePsalida BIGINT =
                (SELECT ISNULL(MAX(PSALIDAKEY), 0) FROM dbo.PSALIDAS);
            DECLARE @BaseDirigido BIGINT =
                (SELECT ISNULL(MAX(DIRIGIDOKEY), 0) FROM dbo.DIRIGIDO);

            ;WITH Pedimentos AS (
                SELECT numero_pedimento,
                       ROW_NUMBER() OVER (ORDER BY numero_pedimento) AS rn
                FROM (SELECT DISTINCT numero_pedimento FROM #filas WHERE tipo_operacion = 2) AS d
            )
            INSERT INTO dbo.SALIDAS
                (SALIDAKEY, TIPO_OPERACION, ADUANA, AGENTE, DOCUMENTO, FECHA, CVE_PEDIMENTO, TC,
                 DESCARGA, PEDIMENTOORIGINAL, PAIS, ORIGEN, DTA, PREV, CVE_CLIENTE, MULTAS, RECARGOS, IVA_PRE)
            SELECT @BaseSalida + p.rn, 'EXPORTACION DIRECTA', MIN(f.aduana), MIN(f.patente),
                   p.numero_pedimento, MIN(f.fecha_pago), MIN(f.clave_pedimento), MIN(f.tc),
                   CASE WHEN MIN(ISNULL(f.descarga, '')) = ''
                        THEN CASE WHEN MIN(ISNULL(f.tipo_pedimento, 1)) = 2 THEN 'NO' ELSE 'SI' END
                        ELSE MIN(f.descarga) END,
                   MIN(f.pedimento_original), MIN(f.pais_od), MIN(f.pais_cv),
                   MAX(ISNULL(f.dta, 0)), MAX(ISNULL(f.prev, 0)),
                   (SELECT TOP 1 x.clave_cp FROM #filas x WHERE x.numero_pedimento = p.numero_pedimento),
                   MAX(ISNULL(f.multas, 0)), MAX(ISNULL(f.recargos, 0)), MAX(ISNULL(f.iva_pre, 0))
            FROM #filas f
            JOIN Pedimentos p ON p.numero_pedimento = f.numero_pedimento AND f.tipo_operacion = 2
            GROUP BY p.numero_pedimento, p.rn;

            SET @Operaciones = @@ROWCOUNT;

            -- Clasificación de tipo de operación según clave de pedimento (rama activa).
            UPDATE dbo.SALIDAS SET TIPO_OPERACION = 'CAMBIO DE REGIMEN'
            WHERE CVE_PEDIMENTO IN ('F4', 'F5') AND TIPO_OPERACION <> 'CAMBIO DE REGIMEN'
              AND DOCUMENTO IN (SELECT numero_pedimento FROM #filas WHERE tipo_operacion = 2);
            UPDATE dbo.SALIDAS SET TIPO_OPERACION = 'CONST TRANSFERENCIA'
            WHERE CVE_PEDIMENTO = 'CT' AND TIPO_OPERACION <> 'CONST TRANSFERENCIA'
              AND DOCUMENTO IN (SELECT numero_pedimento FROM #filas WHERE tipo_operacion = 2);
            UPDATE dbo.SALIDAS SET TIPO_OPERACION = 'REGULARIZACION'
            WHERE CVE_PEDIMENTO = 'A3' AND TIPO_OPERACION <> 'REGULARIZACION'
              AND DOCUMENTO IN (SELECT numero_pedimento FROM #filas WHERE tipo_operacion = 2);
            UPDATE dbo.SALIDAS SET TIPO_OPERACION = 'Desperdicios', CVE_PEDIMENTO = 'DESP'
            WHERE CVE_PEDIMENTO = 'DE'
              AND DOCUMENTO IN (SELECT numero_pedimento FROM #filas WHERE tipo_operacion = 2);
            UPDATE dbo.SALIDAS SET DESCARGA = 'NO', TIPODESCARGA = 'CTMAPAA'
            WHERE EXISTS (SELECT 1 FROM #filas f
                          WHERE f.numero_pedimento = dbo.SALIDAS.DOCUMENTO AND f.apartado = 'A');

            INSERT INTO dbo.PSALIDAS
                (PSALIDAKEY, CLAVE, DESCRIPCION, FRACCION, CANTIDAD, UNIDAD, VAL_DOLARES, PAISD,
                 CODPROVEEDOR, CANTIDADT, UNIDADT, VAL_PESOS, FACTURA, FECHA, PARTIDA, SALIDALINK,
                 DESCARGADIRIGIDA, BLOQUEADO, CORTE, marca, modelo, serie, ValorAduanaCR, fpiva,
                 montoiva, paisc, ValorME, COVE, NICO)
            SELECT @BasePsalida + ROW_NUMBER() OVER (ORDER BY f.numero_pedimento, f.sec),
                   f.clave, f.descripcion, f.fraccion, f.cantidad_comercial,
                   dbo.VALIDUNIT(f.unidad_comercial), f.valor_dolares, f.pais_od,
                   ISNULL(f.clave_cp, '-'), f.cantidad_tarifa, dbo.VALIDUNIT(f.unidad_tarifa),
                   f.valor_comercial, SUBSTRING(f.factura, 1, 20), f.fecha_factura, f.sec,
                   (SELECT s.SALIDAKEY FROM dbo.SALIDAS s WHERE s.DOCUMENTO = f.numero_pedimento),
                   f.descarga_dirigida, 0, ISNULL(f.lote, ''), f.marca, f.modelo, f.serie,
                   f.valor_aduanal, f.fp_iva, f.iva, f.pais_cv, f.valor_me, f.cove, f.nico
            FROM #filas f
            WHERE f.tipo_operacion = 2;

            SET @Partidas = @@ROWCOUNT;

            -- DIRIGIDO sólo cuando el contrato legacy lo exige (DESCARGADIRIGIDA <> '').
            INSERT INTO dbo.DIRIGIDO
                (DIRIGIDOKEY, DOCUMENTO, CLAVE, INCORPORADO, DESPERDICIO, MERMA, SALIDAKEY, PSALIDAKEY)
            SELECT @BaseDirigido + ROW_NUMBER() OVER (ORDER BY ps.PSALIDAKEY),
                   ps.DESCARGADIRIGIDA, ps.CLAVE, ps.CANTIDAD, 0, 0, ps.SALIDALINK, ps.PSALIDAKEY
            FROM dbo.PSALIDAS ps
            WHERE ps.SALIDAKEY >= @BaseSalida + 1
              AND ISNULL(ps.DESCARGADIRIGIDA, '') <> '';
        END

        UPDATE ANEXO24_DEV.app24.CargaPedimento
        SET estado = 'CONFIRMADA', fecha_confirmacion = @FechaTexto
        WHERE id = @CargaId;

        DECLARE @EventoId BIGINT;
        DECLARE @Detalle VARCHAR(500) = CONCAT('cargaId=', @CargaId, ';tipo=', @TipoOperacion,
            ';operaciones=', @Operaciones, ';partidas=', @Partidas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId, @Modulo = 'OPERACIONES', @Accion = 'PEDIMENTO_CONFIRMADO',
            @Detalle = @Detalle, @CorrelacionId = @CorrelacionId,
            @Resultado = 'EXITO', @EventoId = @EventoId OUTPUT;

        COMMIT TRANSACTION;

        SELECT @CargaId AS CargaId, 'CONFIRMADA' AS Estado, @TipoOperacion AS TipoOperacion,
               @Operaciones AS OperacionesProcesadas, @Partidas AS PartidasProcesadas,
               @FechaTexto AS FechaConfirmacion, 'CONFIRMED' AS Resultado;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
