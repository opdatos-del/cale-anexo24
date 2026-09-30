USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ALMACENES_LISTAR
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
    FROM dbo.almacen
    WHERE @Filtro IS NULL
       OR CONVERT(VARCHAR(50), ALMACENKEY) LIKE '%' + @Filtro + '%'
       OR ALMACEN LIKE '%' + @Filtro + '%'
       OR DESCRIPCION LIKE '%' + @Filtro + '%';

    SELECT
        ALMACENKEY AS almacen_key,
        LTRIM(RTRIM(ALMACEN)) AS clave,
        LTRIM(RTRIM(DESCRIPCION)) AS descripcion
    FROM dbo.almacen
    WHERE @Filtro IS NULL
       OR CONVERT(VARCHAR(50), ALMACENKEY) LIKE '%' + @Filtro + '%'
       OR ALMACEN LIKE '%' + @Filtro + '%'
       OR DESCRIPCION LIKE '%' + @Filtro + '%'
    ORDER BY ALMACENKEY
    OFFSET (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
