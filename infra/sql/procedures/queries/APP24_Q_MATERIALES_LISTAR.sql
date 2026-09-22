USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_MATERIALES_LISTAR
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
    FROM dbo.material
    WHERE @Filtro IS NULL
       OR clave LIKE '%' + @Filtro + '%'
       OR descripcion LIKE '%' + @Filtro + '%'
       OR fraccion LIKE '%' + @Filtro + '%';

    SELECT
        materialkey,
        clave,
        descripcion,
        fraccion,
        unidad,
        unidadt,
        tipomaterial,
        tipo,
        FactorUM,
        IGIE
    FROM dbo.material
    WHERE @Filtro IS NULL
       OR clave LIKE '%' + @Filtro + '%'
       OR descripcion LIKE '%' + @Filtro + '%'
       OR fraccion LIKE '%' + @Filtro + '%'
    ORDER BY clave, materialkey
    OFFSET (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
