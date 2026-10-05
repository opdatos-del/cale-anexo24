USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmación técnica de una carga de agentes aduanales mediante el staging legacy existente.
-- El catálogo autoritativo continúa siendo dbo.agentes y sólo dbo.CARGAAgentes
-- aplica sus reglas de negocio. No crea tablas ni lógica paralela de catálogo.
--
-- Comportamiento confirmado del legacy (audit READ ONLY 2026-10-05):
--   * TRUNCATE TABLE ECARGAagentes al inicio (sin transacción propia, sin TRY/CATCH).
--   * INSERT-only en dbo.agentes: omite silenciosamente los agentes cuya CLAVE
--     ya existe en dbo.agentes (NOT EXISTS sobre CLAVE).
--   * Validaciones, todas reportadas en dbo.ECARGAagentes por TMPKEY=AGENTEKEY:
--       1. 'EXISTEN CLAVES VACIAS'          Clave vacía o NULL.
--       2. 'LA PATENTE NO PUEDE ESTAR VACIA' Patente vacía o NULL.
--       3. 'la longitud de la patente es erronea' LEN(Patente) <> 4.
--       4. 'EXISTEN CLAVES DUPLICADAS'      Clave repetida dentro de dbo.TMPagentes.
--   * Sin NOLOCK ni READ UNCOMMITTED; sin SET TRANSACTION ISOLATION LEVEL custom;
--     sin resultadosets ni TRY/CATCH.
--   * dbo.TMPagentes y dbo.ECARGAagentes NO son compartidas: la única dependencia
--     LIVE de ambas es dbo.CARGAAgentes (sys.sql_modules). Se aíslan igual con
--     TABLOCKX+HOLDLOCK y fail-closed para cursorizar ejecuciones no coordinadas.
--   * Restricciones físicas auditadas: TMPagentes(Clave CHAR(10) NOT NULL,
--     Nombre CHAR(40), Domicilio CHAR(60), Rfc CHAR(20), Patente CHAR(10)) y
--     agentes(Clave CHAR(10) NOT NULL, Nombre CHAR(40), Domicilio CHAR(60),
--     Rfc CHAR(20), Patente CHAR(10), ...). Sin ensanchamiento: el parser valida
--     los límites antes de confirmar.
CREATE OR ALTER PROCEDURE dbo.APP24_C_AGENTE_CARGA_CONFIRMAR
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
        carga_agente_key BIGINT NOT NULL,
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

        -- Coordina inserts directos y EJECUCIONES no coordinadas de CARGAAgentes.
        SELECT TOP (1) @Bloqueo = AGENTEKEY
        FROM dbo.TMPagentes WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @Bloqueo = EPKEY
        FROM dbo.ECARGAagentes WITH (TABLOCKX, HOLDLOCK);

        IF EXISTS (SELECT 1 FROM dbo.TMPagentes)
           OR EXISTS (SELECT 1 FROM dbo.ECARGAagentes)
            THROW 51502, 'LEGACY_STAGE_BUSY', 1;

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id,
               @ConfirmadaEn = fecha_confirmacion
        FROM ANEXO24_DEV.app24.CargaCatalogoAgente WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL
            THROW 51503, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA'
            THROW 51504, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA'
            THROW 51505, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaAgente WHERE carga_id = @CargaId)
            THROW 51506, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaCatalogoAgenteFila WHERE carga_id = @CargaId)
            THROW 51507, 'CARGA_SIN_FILAS', 1;

        MERGE dbo.TMPagentes AS destino
        USING (
            SELECT f.hoja,
                   f.fila,
                   JSON_VALUE(f.datos_json, '$.Clave') AS clave,
                   JSON_VALUE(f.datos_json, '$.Nombre') AS nombre,
                   JSON_VALUE(f.datos_json, '$.Domicilio') AS domicilio,
                   JSON_VALUE(f.datos_json, '$.Rfc') AS rfc,
                   JSON_VALUE(f.datos_json, '$.Patente') AS patente
            FROM ANEXO24_DEV.app24.CargaCatalogoAgenteFila f
            WHERE f.carga_id = @CargaId
        ) AS origen
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (Clave, Nombre, Domicilio, Rfc, Patente)
            VALUES (LEFT(origen.clave, 10),
                    origen.nombre,
                    origen.domicilio,
                    origen.rfc,
                    origen.patente)
        OUTPUT inserted.AGENTEKEY, origen.hoja, origen.fila
            INTO @Mapa (carga_agente_key, hoja, fila);

        SELECT @TotalFilas = COUNT(*) FROM @Mapa;

        EXEC dbo.CARGAAgentes;

        INSERT INTO @Errores (hoja, fila, codigo, mensaje)
        SELECT mapa.hoja, mapa.fila, 'LEGACY_VALIDATION', error_legacy.ERROR
        FROM dbo.ECARGAagentes error_legacy
        JOIN @Mapa mapa ON mapa.carga_agente_key = error_legacy.TMPKEY;

        IF EXISTS (SELECT 1 FROM @Errores)
        BEGIN
            ROLLBACK TRANSACTION;

            SELECT @FilasConError = COUNT(*)
            FROM (SELECT DISTINCT hoja, fila FROM @Errores) AS filas_error;

            BEGIN TRANSACTION;
            INSERT INTO ANEXO24_DEV.app24.ErrorCargaAgente
                (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
            SELECT @CargaId,
                   hoja,
                   fila,
                   'CargaAgente',
                   'no almacenado',
                   'LEGACY_VALIDATION',
                   LEFT(STRING_AGG(mensaje, ' | '), 500)
            FROM @Errores
            GROUP BY hoja, fila;

            UPDATE ANEXO24_DEV.app24.CargaCatalogoAgente
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
                @Modulo = 'CATALOGOS_AGENTES',
                @Accion = 'AGENTE_CARGA_CONFIRMACION_ERROR',
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

        DELETE error_legacy
        FROM dbo.ECARGAagentes error_legacy
        JOIN @Mapa mapa ON mapa.carga_agente_key = error_legacy.TMPKEY;
        DELETE carga_legacy
        FROM dbo.TMPagentes carga_legacy
        JOIN @Mapa mapa ON mapa.carga_agente_key = carga_legacy.AGENTEKEY;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaCatalogoAgente
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'CATALOGOS_AGENTES',
            @Accion = 'AGENTE_CARGA_CONFIRMADA',
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