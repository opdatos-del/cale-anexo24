USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_CATEGORIAS_LISTAR
    @Filtro VARCHAR(250) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50051, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');

    SELECT @Total = COUNT_BIG(1)
    FROM dbo.categorias
    WHERE @Filtro IS NULL
       OR categoria LIKE '%' + @Filtro + '%'
       OR descripcion LIKE '%' + @Filtro + '%';

    SELECT
        LTRIM(RTRIM(categoria)) AS clave,
        LTRIM(RTRIM(descripcion)) AS descripcion,
        Dias_Validos AS dias_validos,
        meses
    FROM dbo.categorias
    WHERE @Filtro IS NULL
       OR categoria LIKE '%' + @Filtro + '%'
       OR descripcion LIKE '%' + @Filtro + '%'
    ORDER BY categoria
    OFFSET (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
