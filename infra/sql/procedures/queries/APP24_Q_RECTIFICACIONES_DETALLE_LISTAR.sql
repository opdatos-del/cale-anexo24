USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_RECTIFICACIONES_DETALLE_LISTAR
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
    FROM dbo.v_rectificaciones AS r
    WHERE @Filtro IS NULL
       OR r.[Pedimento] LIKE '%' + @Filtro + '%'
       OR r.[Clave Pedimento] LIKE '%' + @Filtro + '%'
       OR r.[Pedimento Original] LIKE '%' + @Filtro + '%'
       OR r.[Clave Pedimento Original] LIKE '%' + @Filtro + '%'
       OR CONVERT(VARCHAR(60), r.[Existe Pedimento]) LIKE '%' + @Filtro + '%'
       OR r.[Status] LIKE '%' + @Filtro + '%';

    SELECT
        r.[Pedimento] AS PEDIMENTO,
        r.[Clave Pedimento] AS CLAVE_PEDIMENTO,
        r.[Descarga] AS DESCARGA,
        r.[Pedimento Original] AS PEDIMENTO_ORIGINAL,
        r.[Existe Pedimento] AS EXISTE_PEDIMENTO,
        r.[Clave Pedimento Original] AS CLAVE_PEDIMENTO_ORIGINAL,
        r.[Descarga Original] AS DESCARGA_ORIGINAL,
        r.[Status] AS STATUS
    FROM dbo.v_rectificaciones AS r
    WHERE @Filtro IS NULL
       OR r.[Pedimento] LIKE '%' + @Filtro + '%'
       OR r.[Clave Pedimento] LIKE '%' + @Filtro + '%'
       OR r.[Pedimento Original] LIKE '%' + @Filtro + '%'
       OR r.[Clave Pedimento Original] LIKE '%' + @Filtro + '%'
       OR CONVERT(VARCHAR(60), r.[Existe Pedimento]) LIKE '%' + @Filtro + '%'
       OR r.[Status] LIKE '%' + @Filtro + '%'
    ORDER BY r.[Pedimento Original], r.[Pedimento], r.[Clave Pedimento], r.[Clave Pedimento Original],
             r.[Descarga], r.[Descarga Original], r.[Existe Pedimento], r.[Status]
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
