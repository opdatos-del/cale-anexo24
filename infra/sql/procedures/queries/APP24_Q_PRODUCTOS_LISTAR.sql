USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_PRODUCTOS_LISTAR
    @Filtro VARCHAR(250) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- La validación funcional permanece en ListarProductosUseCase. Estas
    -- defensas evitan un OFFSET inválido si el procedimiento se invoca solo.
    IF @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50001, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');

    SELECT @Total = COUNT_BIG(1)
    FROM dbo.productos
    WHERE @Filtro IS NULL
       OR CVE_PRODUCTO LIKE '%' + @Filtro + '%'
       OR NOMBRE LIKE '%' + @Filtro + '%'
       OR fraccion LIKE '%' + @Filtro + '%';

    SELECT
        PRODUCTOKEY,
        CVE_PRODUCTO,
        NOMBRE,
        fraccion,
        UNIDAD,
        UNIDADT
    FROM dbo.productos
    WHERE @Filtro IS NULL
       OR CVE_PRODUCTO LIKE '%' + @Filtro + '%'
       OR NOMBRE LIKE '%' + @Filtro + '%'
       OR fraccion LIKE '%' + @Filtro + '%'
    ORDER BY CVE_PRODUCTO, PRODUCTOKEY
    OFFSET (@Pagina - 1) * @Tamano ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
