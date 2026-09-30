USE ANEXO24_DEV;
GO

-- Fuente canónica versionada: infra/sql/migrations/09-pedimentos-staging-v1.sql.
-- Este archivo conserva el contrato del command; no ejecuta confirmación legacy.
CREATE OR ALTER PROCEDURE app24.APP24_C_PEDIMENTO_CARGA_CREAR
    @Archivo VARCHAR(255),
    @Hash VARCHAR(64),
    @UsuarioId BIGINT,
    @Estado VARCHAR(20),
    @TotalFilas INT,
    @FilasValidas INT,
    @VersionPlantilla VARCHAR(40),
    @CorrelationId VARCHAR(40),
    @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX),
    @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL
       OR @Estado NOT IN ('PREVISUALIZADA', 'CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas
       OR @VersionPlantilla IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1
        THROW 51201, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaPedimento (archivo, hash, usuario_id, estado, total_filas,
            filas_validas, filas_invalidas, version_plantilla, correlation_id)
        VALUES (@Archivo, @Hash, @UsuarioId, @Estado, @TotalFilas, @FilasValidas,
            @TotalFilas - @FilasValidas, @VersionPlantilla, @CorrelationId);
        SET @CargaId = CONVERT(BIGINT, SCOPE_IDENTITY());
        INSERT INTO app24.CargaPedimentoFila (carga_id, hoja, fila, datos_json)
        SELECT @CargaId, hoja, fila, datos_json FROM OPENJSON(@FilasJson)
        WITH (hoja VARCHAR(80) '$.hoja', fila INT '$.fila', datos_json NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaPedimento (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
        SELECT @CargaId, hoja, fila, columna, valor_enmascarado, codigo, mensaje FROM OPENJSON(@ErroresJson)
        WITH (hoja VARCHAR(80) '$.hoja', fila INT '$.fila', columna VARCHAR(80) '$.columna',
              valor_enmascarado VARCHAR(80) '$.valorEnmascarado', codigo VARCHAR(120) '$.codigo', mensaje VARCHAR(500) '$.mensaje');
        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado = 'PREVISUALIZADA'
            THEN 'CARGA_PEDIMENTO_VALIDADA' ELSE 'CARGA_PEDIMENTO_CON_ERRORES' END;
        DECLARE @Resultado VARCHAR(20) = CASE WHEN @Estado = 'PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END;
        DECLARE @Detalle VARCHAR(500) = CONCAT('cargaId=', @CargaId, ';total=', @TotalFilas,
            ';validas=', @FilasValidas, ';invalidas=', @TotalFilas - @FilasValidas);
        DECLARE @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId, @Modulo = 'OPERACIONES', @Accion = @Accion,
            @Detalle = @Detalle, @CorrelacionId = @CorrelationId,
            @Resultado = @Resultado, @EventoId = @EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
