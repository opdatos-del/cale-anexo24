USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmación técnica de una carga de constancias de transferencia (submaquila)
-- mediante el staging legacy existente. Las tablas autoritativas siguen siendo
-- dbo.SALIDAS y dbo.PSALIDAS y sólo dbo.CARGA_SUBMAQUILA aplica la regla de negocio.
-- No crea tablas ni lógica paralela de salidas.
--
-- Comportamiento confirmado del legacy (auditoría READ ONLY 2026-10-05):
--   * No recibe parámetros: consume la totalidad de dbo.TMPSUBMAQUILA.
--   * No valida nada y NO usa error stage (no existe tabla de errores para
--     submaquila; el CARGAAgentes equivalente sí la tenía). Por eso este wrapper
--     no puede normalizar errores de negocio: cualquier error del legacy se
--     propaga y la transacción se revierte completa.
--   * INSERT-only:
--       1. INSERT en dbo.SALIDAS con TIPO_OPERACION='SUBMAQUILA',
--          CVE_PEDIMENTO='SUB', una fila por grupo FOLIO/FECHA/SUBMAQUILADOR,
--          con SALIDAKEY = ROW_NUMBER() OVER (ORDER BY FOLIO) + MAX(SALIDAKEY) + 1.
--       2. INSERT en dbo.PSALIDAS con una fila por renglón de dbo.TMPSUBMAQUILA,
--          FRACCION resuelta desde dbo.PRODUCTOS por CVE_PRODUCTO = CLAVE,
--          SALIDALINK resuelto como SALIDAS.SALIDAKEY donde DOCUMENTO = FOLIO.
--   * Sin NOLOCK, sin TRY/CATCH, sin transacción propia, sin resultado de negocio.
--   * dbo.TMPSUBMAQUILA no es compartida: se aísla con TABLOCKX+HOLDLOCK y
--     fail-closed para cursorizar inserciones o ejecuciones no coordinadas.
--   * dbo.SALIDAS y dbo.PSALIDAS se aíslan con TABLOCKX+HOLDLOCK porque el legacy
--     asigna claves por MAX(SALIDAKEY)+1 y MAX(PSALIDAKEY)+1: sin ese lock, otro
--     writer concurrente (p. ej. CARGA_FACTURAS, CARGAACTAS, CARGACONSTANCIAS,
--     CARGAPEDIMENTOS o APP24_C_PEDIMENTO_CONFIRMAR) podría calcular el mismo MAX
--     y provocar colisión de clave primaria o reutilización de clave.
--   * Restricciones físicas auditadas de dbo.TMPSUBMAQUILA (compat level 100):
--     TMPSKEY BIGINT IDENTITY PK, FOLIO VARCHAR(50), FECHA DATE,
--     SUBMAQUILADOR VARCHAR(50), CLAVE VARCHAR(50), CANTIDAD NUMERIC(18,4),
--     UNIDAD VARCHAR(5), DESCRIPCION VARCHAR(250), LINEA INT.
--     El parser valida los límites antes de confirmar; aquí no hay ensanchamiento.
--
-- CONTRAPUNTO AUDITADO (riesgo heredado, no introducido aquí): el legacy resuelve
-- SALIDALINK con un subquery escalar sobre dbo.SALIDAS por DOCUMENTO. Si el folio
-- ya existiera con más de una salida, SQL Server lanzaría "subquery returned more
-- than 1 value". El wrapper no lo silencia: revierte y propaga el error.
CREATE OR ALTER PROCEDURE dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR
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
            @BloqueoSalida NUMERIC(18,0),
            @BloqueoPsalida NUMERIC(18,0),
            @TotalFilas INT,
            @DetalleBitacora VARCHAR(500),
            @EventoId BIGINT;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Coordina inserts directos y EJECUCIONES no coordinadas de CARGA_SUBMAQUILA.
        SELECT TOP (1) @Bloqueo = TMPSKEY
        FROM dbo.TMPSUBMAQUILA WITH (TABLOCKX, HOLDLOCK);

        IF EXISTS (SELECT 1 FROM dbo.TMPSUBMAQUILA)
           THROW 51502, 'LEGACY_STAGE_BUSY', 1;

        -- Orden determinista de locks dentro de la MISMA transacción:
        --   1. dbo.TMPSUBMAQUILA  (stage del contrato submaquila)
        --   2. dbo.SALIDAS        (clave por MAX(SalidaKey)+1)
        --   3. dbo.PSALIDAS       (clave por MAX(PsalidaKey)+1)
        -- El orden relativo SALIDAS -> PSALIDAS coincide con el orden global
        -- IMPORTACIONES -> PARTIDAS -> SALIDAS -> PSALIDAS -> DIRIGIDO de
        -- dbo.APP24_C_PEDIMENTO_CONFIRMAR, de modo que dos wrappers nunca toman
        -- estas tablas en orden inverso. La asignación TOP (1) evita devolver
        -- result sets intermedios y garantiza el lock real de tabla del motor.
        SELECT TOP (1) @BloqueoSalida = SalidaKey FROM dbo.SALIDAS WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @BloqueoPsalida = Psalidakey FROM dbo.PSALIDAS WITH (TABLOCKX, HOLDLOCK);

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id
        FROM ANEXO24_DEV.app24.CargaSubmaquila WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL
            THROW 51503, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA'
            THROW 51504, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA'
            THROW 51505, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaSubmaquila WHERE carga_id = @CargaId)
            THROW 51506, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaSubmaquilaFila WHERE carga_id = @CargaId)
            THROW 51507, 'CARGA_SIN_FILAS', 1;

        INSERT INTO dbo.TMPSUBMAQUILA (FOLIO, FECHA, SUBMAQUILADOR, CLAVE, CANTIDAD, UNIDAD, DESCRIPCION, LINEA)
        SELECT origen.folio,
               origen.fecha,
               origen.submaquilador,
               origen.clave,
               origen.cantidad,
               origen.unidad,
               LEFT(origen.descripcion, 250),
               origen.linea
        FROM (
            SELECT f.hoja,
                   f.fila,
                   JSON_VALUE(f.datos_json, '$.Folio')         AS folio,
                   JSON_VALUE(f.datos_json, '$.Fecha')         AS fecha,
                   JSON_VALUE(f.datos_json, '$.Submaquilador') AS submaquilador,
                   JSON_VALUE(f.datos_json, '$.Clave')         AS clave,
                   JSON_VALUE(f.datos_json, '$.Cantidad')      AS cantidad,
                   JSON_VALUE(f.datos_json, '$.Unidad')        AS unidad,
                   JSON_VALUE(f.datos_json, '$.Descripcion')   AS descripcion,
                   JSON_VALUE(f.datos_json, '$.Linea')         AS linea
            FROM ANEXO24_DEV.app24.CargaSubmaquilaFila f
            WHERE f.carga_id = @CargaId
        ) AS origen
        ORDER BY origen.folio, origen.linea;

        SELECT @TotalFilas = COUNT(*) FROM dbo.TMPSUBMAQUILA;

        IF @TotalFilas = 0
            THROW 51507, 'CARGA_SIN_FILAS', 1;

        EXEC dbo.CARGA_SUBMAQUILA;

        DELETE FROM dbo.TMPSUBMAQUILA;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaSubmaquila
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'OPERACIONES_SUBMAQUILA',
            @Accion = 'SUBMAQUILA_CARGA_CONFIRMADA',
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

        -- Trazabilidad del intento fallido (p. ej. SALIDALINK ambiguo). Se ejecuta
        -- sólo si ya se conocen usuario y correlación (si el fallo ocurre antes de
        -- leerlos, no se inventan). Nunca oculta el error original: el registro de
        -- bitácora va en su propio TRY/CATCH y se relanza con THROW vacío.
        DECLARE @DetalleFallo VARCHAR(500) = CONCAT('cargaId=', @CargaId, ';fase=LEGACY_CONFIRMATION');
        DECLARE @EventoFallo BIGINT = NULL;
        IF @UsuarioId IS NOT NULL AND @CorrelacionId IS NOT NULL
        BEGIN
            BEGIN TRY
                EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
                    @UsuarioId = @UsuarioId,
                    @Modulo = 'OPERACIONES_SUBMAQUILA',
                    @Accion = 'SUBMAQUILA_CARGA_CONFIRMACION_ERROR',
                    @Detalle = @DetalleFallo,
                    @CorrelacionId = @CorrelacionId,
                    @Resultado = 'FALLO',
                    @EventoId = @EventoFallo OUTPUT;
            END TRY
            BEGIN CATCH
                SET @EventoFallo = NULL;
            END CATCH;
        END;
        -- Terminador explícito: la sentencia previa a THROW debe cerrar con ';'.
        SET @DetalleFallo = @DetalleFallo;

        THROW;
    END CATCH;
END;
GO
