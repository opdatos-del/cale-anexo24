USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmacion tecnica de actas reutilizando el SP legacy dbo.CARGAACTAS.
-- Las tablas autoritativas siguen siendo dbo.SALIDAS / dbo.PSALIDAS / dbo.DIRIGIDO y
-- solo dbo.CARGAACTAS aplica reglas de negocio. No crea logica de negocio paralela.
--
-- Contrato LIVE confirmado (READ-ONLY 2026-10-05, LEGACY_EXECUTION_SAFE = YES):
--   - sin parametros; cursor sobre dbo.ACTA; sin transaccion/TRY-CATCH/NOLOCK;
--   - sin error stage (dbo.ERRORACTA no existe);
--   - claves por MAX()+1 en SALIDAS/PSALIDAS/DIRIGIDO;
--   - side effect de negocio: DELETE FROM dbo.GENERADORES WHERE TABLA='ACTA'.
--
-- Orden determinista de locks dentro de la MISMA transaccion:
--   1. dbo.ACTA (stage exclusivo)
--   2. dbo.SALIDAS   3. dbo.PSALIDAS   4. dbo.DIRIGIDO   5. dbo.GENERADORES
-- El orden relativo SALIDAS -> PSALIDAS -> DIRIGIDO coincide con el orden global
-- IMPORTACIONES -> PARTIDAS -> SALIDAS -> PSALIDAS -> DIRIGIDO de
-- dbo.APP24_C_PEDIMENTO_CONFIRMAR. GENERADORES se agrega al final porque CARGAACTAS
-- lo modifica (DELETE) y debe participar atomicamente en la transaccion.
CREATE OR ALTER PROCEDURE dbo.APP24_C_ACTA_CARGA_CONFIRMAR
    @CargaId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @CargaId IS NULL OR @CargaId < 1
        THROW 51601, 'PARAMETRO_INVALIDO', 1;

    DECLARE @Estado VARCHAR(20),
            @UsuarioId BIGINT,
            @CorrelacionId VARCHAR(40),
            @ConfirmadaEn DATETIME2(3),
            @Bloqueo INT,
            @BloqueoSalida NUMERIC(18,0),
            @BloqueoPsalida NUMERIC(18,0),
            @BloqueoDirigido BIGINT,
            @TotalFilas INT,
            @DetalleBitacora VARCHAR(500),
            @EventoId BIGINT;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Coordina inserts directos y EXEC no coordinados de CARGAACTAS.
        SELECT TOP (1) @Bloqueo = actakey FROM dbo.ACTA WITH (TABLOCKX, HOLDLOCK);
        IF EXISTS (SELECT 1 FROM dbo.ACTA) THROW 51602, 'LEGACY_STAGE_BUSY', 1;

        SELECT TOP (1) @BloqueoSalida = SalidaKey FROM dbo.SALIDAS WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoPsalida = Psalidakey FROM dbo.PSALIDAS WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoDirigido = dirigidokey FROM dbo.DIRIGIDO WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @Bloqueo = consecutivo FROM dbo.GENERADORES WITH (TABLOCKX, HOLDLOCK);

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id
        FROM ANEXO24_DEV.app24.CargaActa WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL THROW 51603, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA' THROW 51604, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA' THROW 51605, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaActa WHERE carga_id = @CargaId) THROW 51606, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaActaFila WHERE carga_id = @CargaId) THROW 51607, 'CARGA_SIN_FILAS', 1;

        INSERT INTO dbo.ACTA (Folio, fecha, clave, linea, cantidad, umc, descargadirigida, VALORCOMERCIAL)
        SELECT JSON_VALUE(f.datos_json, '$.Folio'),
               CONVERT(DATETIME, JSON_VALUE(f.datos_json, '$.Fecha'), 126),
               JSON_VALUE(f.datos_json, '$.Clave'),
               CONVERT(INT, JSON_VALUE(f.datos_json, '$.Linea')),
               CONVERT(FLOAT, JSON_VALUE(f.datos_json, '$.Cantidad')),
               JSON_VALUE(f.datos_json, '$.Umc'),
               JSON_VALUE(f.datos_json, '$.DescargaDirigida'),
               CONVERT(NUMERIC(18,10), JSON_VALUE(f.datos_json, '$.ValorComercial'))
        FROM ANEXO24_DEV.app24.CargaActaFila f
        WHERE f.carga_id = @CargaId
        ORDER BY f.fila;

        SELECT @TotalFilas = COUNT(*) FROM dbo.ACTA;
        IF @TotalFilas = 0 THROW 51607, 'CARGA_SIN_FILAS', 1;

        EXEC dbo.CARGAACTAS;

        DELETE FROM dbo.ACTA;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaActa
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'OPERACIONES_ACTAS',
            @Accion = 'ACTA_CARGA_CONFIRMADA',
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
                    @Modulo = 'OPERACIONES_ACTAS',
                    @Accion = 'ACTA_CARGA_CONFIRMACION_ERROR',
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
