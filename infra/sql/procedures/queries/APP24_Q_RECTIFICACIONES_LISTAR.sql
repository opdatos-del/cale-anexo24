USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_RECTIFICACIONES_LISTAR
    @Filtro VARCHAR(60) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51000, 'La paginacion no es valida.', 1;

    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');

    SELECT @Total = COUNT_BIG(*)
    FROM dbo.v_total_rectificaciones AS r
    WHERE @Filtro IS NULL
       OR r.pedimento LIKE '%' + @Filtro + '%';

    SELECT
        r.pedimento AS PEDIMENTO,
        r.[Total] AS TOTAL_RECTIFICACIONES
    FROM dbo.v_total_rectificaciones AS r
    WHERE @Filtro IS NULL
       OR r.pedimento LIKE '%' + @Filtro + '%'
    ORDER BY CASE WHEN r.pedimento IS NULL THEN 0 ELSE 1 END, r.pedimento
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
