USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmacion tecnica de constancias reutilizando el SP legacy dbo.CARGACONSTANCIAS.
-- La operacion autoritativa queda en dbo.productos / dbo.salidas / dbo.psalidas / dbo.dirigido
-- y solo dbo.CARGACONSTANCIAS aplica reglas de negocio. NEW_BUSINESS_SP = 0.
--
-- Contrato LIVE confirmado (READ-ONLY 2026-10-06, LEGACY_EXECUTION_SAFE = YES):
--   - sin parametros; sin transaccion/TRY-CATCH/NOLOCK; sin cursor ni SQL dinamico;
--   - stage: dbo.Constanciatransf (CONSTANCIAKEY IDENTITY);
--   - error stage COMPARTIDO: dbo.errorcarga (tambien usada por CARGAPEDIMENTOS);
--   - DELETE FROM errorcarga al inicio: barrido GLOBAL de errores ajenos;
--   - dependencia mutable de dbo.settings: I_CARGAPRODUCTOSCONST (default 'SI')
--     dispara insercion de nuevos dbo.productos por MAX(PRODUCTOKEY)+1;
--   - targets: productos, salidas, psalidas, dirigido; claves por MAX()+1;
--   - side effects globales heredados (NO se corrigen, se documentan):
--       * UPDATE dbo.salidas SET TIPO_OPERACION='CONST TRANSFERENCIA' WHERE CVE_PEDIMENTO='CT'
--         (afecta salidas ajenas, no solo las de esta carga);
--       * DELETE FROM dbo.generadores (barrido global, sin WHERE TABLA).
--
-- Orden determinista de locks dentro de la MISMA transaccion (derivado del orden global
-- IMPORTACIONES -> PARTIDAS -> SALIDAS -> PSALIDAS -> DIRIGIDO de APP24_C_PEDIMENTO_CONFIRMAR
-- y de APP24_C_ACTA_CARGA_CONFIRMAR; PRODUCTOS es nueva para los wrappers, sin inversion):
--   Constanciatransf (stage propio) -> errorcarga (error stage compartido) -> productos
--   -> salidas -> psalidas -> dirigido -> generadores.
-- dbo.Importaciones/partidas/clientes/Proveedores solo se leen (MAX); no se bloquean ni escriben.
--
-- El wrapper FALLA CERRADO si dbo.errorcarga tiene filas: el DELETE global del legacy las
-- destruiria, incluidos errores de Pedimentos. Nunca borra errores ajenos.
--
-- Variables de lock tipadas segun metadata LIVE (nunca un tipo mas estrecho):
--   Constanciatransf.CONSTANCIAKEY = BIGINT -> @BloqueoStage BIGINT
--   errorcarga.errorkey = BIGINT -> @BloqueoError BIGINT
--   productos.PRODUCTOKEY/SalidaKey/Psalidakey = NUMERIC(18,0); dirigidokey BIGINT; consecutivo INT
CREATE OR ALTER PROCEDURE dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR
    @CargaId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @CargaId IS NULL OR @CargaId < 1
        THROW 51701, 'PARAMETRO_INVALIDO', 1;

    DECLARE @Estado VARCHAR(20),
            @UsuarioId BIGINT,
            @CorrelacionId VARCHAR(40),
            @ConfirmadaEn DATETIME2(3),
            @BloqueoStage BIGINT,
            @BloqueoError BIGINT,
            @BloqueoProducto NUMERIC(18,0),
            @BloqueoSalida NUMERIC(18,0),
            @BloqueoPsalida NUMERIC(18,0),
            @BloqueoDirigido BIGINT,
            @BloqueoGenerador INT,
            @TotalFilas INT,
            @FilasConError INT,
            @DetalleBitacora VARCHAR(500),
            @EventoId BIGINT;
    DECLARE @Mapa TABLE (
        constancia_key BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL
    );
    DECLARE @Errores TABLE (
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        mensaje VARCHAR(250) NOT NULL
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Coordina inserts directos y EXEC no coordinados de CARGACONSTANCIAS.
        SELECT TOP (1) @BloqueoStage = CONSTANCIAKEY FROM dbo.Constanciatransf WITH (TABLOCKX, HOLDLOCK);
        IF EXISTS (SELECT 1 FROM dbo.Constanciatransf) THROW 51702, 'LEGACY_STAGE_BUSY', 1;

        SELECT TOP (1) @BloqueoError = errorkey FROM dbo.errorcarga WITH (TABLOCKX, HOLDLOCK);
        IF EXISTS (SELECT 1 FROM dbo.errorcarga) THROW 51708, 'LEGACY_ERROR_STAGE_BUSY', 1;

        SELECT TOP (1) @BloqueoProducto = PRODUCTOKEY FROM dbo.productos WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoSalida = SalidaKey FROM dbo.salidas WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoPsalida = Psalidakey FROM dbo.psalidas WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoDirigido = dirigidokey FROM dbo.dirigido WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoGenerador = consecutivo FROM dbo.generadores WITH (TABLOCKX, HOLDLOCK);

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id
        FROM ANEXO24_DEV.app24.CargaConstancia WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL THROW 51703, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA' THROW 51704, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA' THROW 51705, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaConstancia WHERE carga_id = @CargaId) THROW 51706, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaConstanciaFila WHERE carga_id = @CargaId) THROW 51707, 'CARGA_SIN_FILAS', 1;

        -- MERGE ON 1=0 conserva el vinculo entre identidad legacy (CONSTANCIAKEY) y fila de origen.
        MERGE dbo.Constanciatransf AS destino
        USING (
            SELECT f.hoja,
                   f.fila,
                   -- Columnas de texto: se conserva '' (no NULL) porque CARGACONSTANCIAS valida
                   -- LEN(DESCRIPCION) <= 1 y LEN(PROV) <= 1 sin ISNULL; con NULL el chequeo legacy
                   -- no dispara y se desviaria el contrato. Solo numericos/fechas usan NULLIF.
                   ISNULL(JSON_VALUE(f.datos_json, '$.NUMERODEFOLIO'), '') AS numerodefolio,
                   ISNULL(JSON_VALUE(f.datos_json, '$.SEC'), '') AS sec,
                   CONVERT(DATETIME, NULLIF(JSON_VALUE(f.datos_json, '$.FECHACREACION'), ''), 126) AS fechacreacion,
                   ISNULL(JSON_VALUE(f.datos_json, '$.PROV'), '') AS prov,
                   CONVERT(NUMERIC(18,0), NULLIF(JSON_VALUE(f.datos_json, '$.LIN'), '')) AS lin,
                   ISNULL(JSON_VALUE(f.datos_json, '$.NOPARTE'), '') AS noparte,
                   ISNULL(JSON_VALUE(f.datos_json, '$.DESCRIPCION'), '') AS descripcion,
                   CONVERT(NUMERIC(18,0), NULLIF(JSON_VALUE(f.datos_json, '$.CANTIDAD'), '')) AS cantidad,
                   ISNULL(JSON_VALUE(f.datos_json, '$.PEDIMENTO'), '') AS pedimento,
                   ISNULL(JSON_VALUE(f.datos_json, '$.ADUANA'), '') AS aduana,
                   ISNULL(JSON_VALUE(f.datos_json, '$.MER'), '') AS mer,
                   CONVERT(DATETIME, NULLIF(JSON_VALUE(f.datos_json, '$.PERIODO'), ''), 126) AS periodo,
                   CONVERT(DECIMAL(18,10), NULLIF(JSON_VALUE(f.datos_json, '$.Val_dolares'), '')) AS val_dolares,
                   CONVERT(DECIMAL(18,10), NULLIF(JSON_VALUE(f.datos_json, '$.Val_Comercial'), '')) AS val_comercial
            FROM ANEXO24_DEV.app24.CargaConstanciaFila f
            WHERE f.carga_id = @CargaId
        ) AS origen
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (NUMERODEFOLIO, SEC, FECHACREACION, PROV, LIN, NOPARTE, DESCRIPCION, CANTIDAD,
                    PEDIMENTO, ADUANA, MER, PERIODO, Val_dolares, Val_Comercial)
            VALUES (origen.numerodefolio, origen.sec, origen.fechacreacion, origen.prov, origen.lin,
                    origen.noparte, origen.descripcion, origen.cantidad, origen.pedimento, origen.aduana,
                    origen.mer, origen.periodo, origen.val_dolares, origen.val_comercial)
        OUTPUT inserted.CONSTANCIAKEY, origen.hoja, origen.fila
            INTO @Mapa (constancia_key, hoja, fila);

        SELECT @TotalFilas = COUNT(*) FROM @Mapa;

        EXEC dbo.CARGACONSTANCIAS;

        INSERT INTO @Errores (hoja, fila, mensaje)
        SELECT mapa.hoja, mapa.fila, error_legacy.error
        FROM dbo.errorcarga error_legacy
        JOIN @Mapa mapa ON mapa.constancia_key = error_legacy.cargakey;

        IF EXISTS (SELECT 1 FROM @Errores)
        BEGIN
            ROLLBACK TRANSACTION;

            SELECT @FilasConError = COUNT(*)
            FROM (SELECT DISTINCT hoja, fila FROM @Errores) AS filas_error;

            BEGIN TRANSACTION;
            -- Normaliza unicamente los errores de ESTE lote; error_legacy ya no existe tras el rollback.
            INSERT INTO ANEXO24_DEV.app24.ErrorCargaConstancia
                (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
            SELECT @CargaId,
                   hoja,
                   fila,
                   'Constanciatransf',
                   'no almacenado',
                   'LEGACY_VALIDATION',
                   LEFT(STRING_AGG(mensaje, ' | '), 500)
            FROM @Errores
            GROUP BY hoja, fila;

            UPDATE ANEXO24_DEV.app24.CargaConstancia
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
                @Modulo = 'OPERACIONES_CONSTANCIAS',
                @Accion = 'CONSTANCIA_CARGA_CONFIRMACION_ERROR',
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

        -- El precheck garantiza el error stage ajeno vacio; solo se limpian identidades de este lote.
        DELETE error_legacy
        FROM dbo.errorcarga error_legacy
        JOIN @Mapa mapa ON mapa.constancia_key = error_legacy.cargakey;
        DELETE carga_legacy
        FROM dbo.Constanciatransf carga_legacy
        JOIN @Mapa mapa ON mapa.constancia_key = carga_legacy.CONSTANCIAKEY;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaConstancia
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'OPERACIONES_CONSTANCIAS',
            @Accion = 'CONSTANCIA_CARGA_CONFIRMADA',
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

        DECLARE @DetalleFallo VARCHAR(500) = CONCAT('cargaId=', @CargaId, ';fase=LEGACY_CONFIRMATION');
        DECLARE @EventoFallo BIGINT = NULL;
        IF @UsuarioId IS NOT NULL AND @CorrelacionId IS NOT NULL
        BEGIN
            BEGIN TRY
                EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
                    @UsuarioId = @UsuarioId,
                    @Modulo = 'OPERACIONES_CONSTANCIAS',
                    @Accion = 'CONSTANCIA_CARGA_CONFIRMACION_ERROR',
                    @Detalle = @DetalleFallo,
                    @CorrelacionId = @CorrelacionId,
                    @Resultado = 'FALLO',
                    @EventoId = @EventoFallo OUTPUT;
            END TRY
            BEGIN CATCH
                SET @EventoFallo = NULL;
            END CATCH;
        END;
        SET @DetalleFallo = @DetalleFallo;

        THROW;
    END CATCH;
END;
GO
