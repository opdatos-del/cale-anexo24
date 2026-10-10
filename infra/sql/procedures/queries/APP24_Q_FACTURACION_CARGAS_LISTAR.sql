USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_FACTURACION_CARGAS_LISTAR
    @UsuarioId BIGINT,
    @Estado VARCHAR(20) = NULL,
    @Desde DATE = NULL,
    @Hasta DATE = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @Estado = NULLIF(LTRIM(RTRIM(@Estado)), '');
    IF @UsuarioId IS NULL OR NOT (@UsuarioId > 0) OR NOT (@Pagina >= 1) OR NOT (@Tamano BETWEEN 1 AND 100)
       OR (@Estado IS NOT NULL AND @Estado NOT IN ('PREVISUALIZADA', 'INVALIDA'))
       OR (@Desde IS NULL AND @Hasta IS NOT NULL) OR (@Desde IS NOT NULL AND @Hasta IS NULL)
       OR (@Desde IS NOT NULL AND @Desde > @Hasta)
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    SELECT @Total = COUNT_BIG(*)
    FROM app24.CargaFacturacion
    WHERE usuario_id = @UsuarioId
      AND (@Estado IS NULL OR estado = @Estado)
      AND (@Desde IS NULL OR CONVERT(DATE, fecha) >= @Desde)
      AND (@Hasta IS NULL OR CONVERT(DATE, fecha) <= @Hasta);

    SELECT id, archivo, hash, fecha, estado, total_registros, registros_validos, registros_invalidos
    FROM app24.CargaFacturacion
    WHERE usuario_id = @UsuarioId
      AND (@Estado IS NULL OR estado = @Estado)
      AND (@Desde IS NULL OR CONVERT(DATE, fecha) >= @Desde)
      AND (@Hasta IS NULL OR CONVERT(DATE, fecha) <= @Hasta)
    ORDER BY fecha DESC, id DESC
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
