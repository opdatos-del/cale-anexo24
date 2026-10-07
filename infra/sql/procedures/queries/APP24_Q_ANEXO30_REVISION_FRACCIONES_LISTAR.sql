USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR
    @Filtro VARCHAR(60) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51000, 'La paginación no es válida.', 1;

    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');

    SELECT @Total = COUNT_BIG(*)
    FROM dbo.A31_DESCARGASF f
    WHERE @Filtro IS NULL
       OR f.TIPO LIKE '%' + @Filtro + '%'
       OR f.CLAVEPEDIMENTO LIKE '%' + @Filtro + '%'
       OR f.EJERCICIO LIKE '%' + @Filtro + '%'
       OR f.PERIODO LIKE '%' + @Filtro + '%'
       OR f.FRACCION LIKE '%' + @Filtro + '%'
       OR f.AF LIKE '%' + @Filtro + '%'
       OR f.ARCHIVO LIKE '%' + @Filtro + '%';

    SELECT f.A31_FRACCIONKEY AS A31_FRACCION_KEY, f.TIPO,
           f.CLAVEPEDIMENTO AS CLAVE_PEDIMENTO, f.EJERCICIO, f.PERIODO,
           f.FRACCION, f.VALOR, f.AF, f.ARCHIVO
    FROM dbo.A31_DESCARGASF f
    WHERE @Filtro IS NULL
       OR f.TIPO LIKE '%' + @Filtro + '%'
       OR f.CLAVEPEDIMENTO LIKE '%' + @Filtro + '%'
       OR f.EJERCICIO LIKE '%' + @Filtro + '%'
       OR f.PERIODO LIKE '%' + @Filtro + '%'
       OR f.FRACCION LIKE '%' + @Filtro + '%'
       OR f.AF LIKE '%' + @Filtro + '%'
       OR f.ARCHIVO LIKE '%' + @Filtro + '%'
    ORDER BY TRY_CONVERT(INT, f.EJERCICIO) DESC, TRY_CONVERT(INT, f.PERIODO) DESC,
             f.CLAVEPEDIMENTO, f.FRACCION, f.A31_FRACCIONKEY
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
