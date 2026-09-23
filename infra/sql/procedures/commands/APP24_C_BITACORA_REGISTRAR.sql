USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_BITACORA_REGISTRAR
    @UsuarioId BIGINT = NULL,
    @Modulo VARCHAR(80),
    @Accion VARCHAR(40),
    @Detalle VARCHAR(500) = NULL,
    @CorrelacionId VARCHAR(40) = NULL,
    @Resultado VARCHAR(20),
    @EventoId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Modulo IS NULL OR LTRIM(RTRIM(@Modulo)) = ''
       OR @Accion IS NULL OR LTRIM(RTRIM(@Accion)) = ''
       OR @Resultado IS NULL OR LTRIM(RTRIM(@Resultado)) = ''
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    INSERT INTO app24.BitacoraEvento (usuario_id, modulo, accion, detalle, correlacion_id, resultado)
    VALUES (@UsuarioId, @Modulo, @Accion, @Detalle, @CorrelacionId, @Resultado);

    SET @EventoId = CONVERT(BIGINT, SCOPE_IDENTITY());
    IF @EventoId IS NULL OR @EventoId <= 0
        THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;
END;
GO
