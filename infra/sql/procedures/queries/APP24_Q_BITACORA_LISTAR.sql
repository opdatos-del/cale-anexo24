USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_BITACORA_LISTAR
    @Desde DATETIME2(3),
    @Hasta DATETIME2(3),
    @UsuarioId BIGINT = NULL,
    @Modulo VARCHAR(80) = NULL,
    @Resultado VARCHAR(20) = NULL,
    @CorrelacionId VARCHAR(40) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Desde IS NULL OR @Hasta IS NULL
        THROW 50001, 'Desde y Hasta son obligatorios.', 1;
    IF @Desde > @Hasta
        THROW 50002, 'Desde no puede ser posterior a Hasta.', 1;
    IF @Pagina < 1
        THROW 50003, 'Pagina debe ser mayor o igual que 1.', 1;
    IF @Tamano < 1 OR @Tamano > 100
        THROW 50004, 'Tamano debe estar entre 1 y 100.', 1;

    SELECT @Total = COUNT_BIG(*)
    FROM app24.BitacoraEvento b
    WHERE b.fecha >= @Desde
      AND b.fecha <= @Hasta
      AND (@UsuarioId IS NULL OR b.usuario_id = @UsuarioId)
      AND (@Modulo IS NULL OR b.modulo = @Modulo)
      AND (@Resultado IS NULL OR b.resultado = @Resultado)
      AND (@CorrelacionId IS NULL OR b.correlacion_id = @CorrelacionId);

    SELECT
        b.id,
        b.fecha,
        b.usuario_id,
        u.clave AS usuario,
        b.modulo,
        b.accion,
        b.detalle,
        b.resultado,
        b.correlacion_id
    FROM app24.BitacoraEvento b
    LEFT JOIN app24.UsuarioApp u ON u.id = b.usuario_id
    WHERE b.fecha >= @Desde
      AND b.fecha <= @Hasta
      AND (@UsuarioId IS NULL OR b.usuario_id = @UsuarioId)
      AND (@Modulo IS NULL OR b.modulo = @Modulo)
      AND (@Resultado IS NULL OR b.resultado = @Resultado)
      AND (@CorrelacionId IS NULL OR b.correlacion_id = @CorrelacionId)
    ORDER BY b.fecha DESC, b.id DESC
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * CONVERT(BIGINT, @Tamano) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
