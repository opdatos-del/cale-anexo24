USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_UNIDADES_LISTAR
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
    FROM dbo.unidad
    WHERE @Filtro IS NULL
       OR CVE_UNIDAD LIKE '%' + @Filtro + '%'
       OR NOMBRE LIKE '%' + @Filtro + '%'
       OR ALIAS LIKE '%' + @Filtro + '%';

    SELECT
        LTRIM(RTRIM(CVE_UNIDAD)) AS clave,
        LTRIM(RTRIM(NOMBRE)) AS nombre,
        LTRIM(RTRIM(ALIAS)) AS alias
    FROM dbo.unidad
    WHERE @Filtro IS NULL
       OR CVE_UNIDAD LIKE '%' + @Filtro + '%'
       OR NOMBRE LIKE '%' + @Filtro + '%'
       OR ALIAS LIKE '%' + @Filtro + '%'
    ORDER BY CVE_UNIDAD
    OFFSET (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
