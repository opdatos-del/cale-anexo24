USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_FACTURACION_CARGA_CREAR
    @Archivo VARCHAR(255),
    @Hash VARCHAR(64),
    @UsuarioId BIGINT,
    @Estado VARCHAR(20),
    @TotalRegistros INT,
    @RegistrosValidos INT,
    @ErroresJson NVARCHAR(MAX),
    @FilasJson NVARCHAR(MAX),
    @CorrelationId VARCHAR(40),
    @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL
       OR @Estado NOT IN ('PREVISUALIZADA', 'INVALIDA') OR @TotalRegistros < 0
       OR @RegistrosValidos < 0 OR @RegistrosValidos > @TotalRegistros
       OR ISJSON(@ErroresJson) <> 1 OR ISJSON(@FilasJson) <> 1
       OR @CorrelationId IS NULL OR LTRIM(RTRIM(@CorrelationId)) = ''
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaFacturacion (archivo, hash, usuario_id, estado, total_registros,
                                            registros_validos, registros_invalidos)
        VALUES (@Archivo, @Hash, @UsuarioId, @Estado, @TotalRegistros, @RegistrosValidos,
                @TotalRegistros - @RegistrosValidos);
        SET @CargaId = CONVERT(BIGINT, SCOPE_IDENTITY());

        INSERT INTO app24.CargaFacturacionFila (carga_id, hoja, fila, datos_json)
        SELECT @CargaId, hoja, fila, datos_json
        FROM OPENJSON(@FilasJson)
        WITH (hoja VARCHAR(80) '$.hoja', fila INT '$.fila', datos_json NVARCHAR(MAX) '$.datos' AS JSON);

        INSERT INTO app24.ErrorCarga (carga_id, hoja, fila, columna, valor, regla, mensaje)
        SELECT @CargaId, hoja, fila, columna, NULL, regla, mensaje
        FROM OPENJSON(@ErroresJson)
        WITH (hoja VARCHAR(80) '$.hoja', fila INT '$.fila', columna VARCHAR(80) '$.columna',
              regla VARCHAR(120) '$.codigo', mensaje VARCHAR(500) '$.mensaje');

        IF @CargaId IS NULL OR @CargaId <= 0 THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado = 'PREVISUALIZADA'
                                            THEN 'CARGA_VALIDADA' ELSE 'CARGA_CON_ERRORES' END;
        DECLARE @Resultado VARCHAR(20) = CASE WHEN @Estado = 'PREVISUALIZADA'
                                              THEN 'EXITO' ELSE 'FALLO' END;
        DECLARE @Detalle VARCHAR(500) = CONCAT('cargaId=', @CargaId, ';total=', @TotalRegistros,
            ';validos=', @RegistrosValidos, ';invalidos=', @TotalRegistros - @RegistrosValidos);
        DECLARE @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId, @Modulo = 'FACTURACION', @Accion = @Accion,
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
