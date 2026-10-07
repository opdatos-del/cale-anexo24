USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR
    @Filtro VARCHAR(60) = NULL, @Pagina INT = 1, @Tamano INT = 20, @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    IF @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51000, 'La paginación no es válida.', 1;
    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');
    SELECT @Total = COUNT_BIG(*) FROM dbo.A31_DESCARGAS d
    LEFT JOIN dbo.A31_ENTRADAS e ON e.Entradaskey = d.ENTRADALINK
    LEFT JOIN dbo.A31_DESCARGASF f ON f.A31_FRACCIONKEY = d.A31_FRACCIONLINK
    WHERE @Filtro IS NULL OR e.Pedimentoarmado LIKE '%' + @Filtro + '%' OR e.PEDIMENTOORIGINAL LIKE '%' + @Filtro + '%'
       OR e.Clavepedimento LIKE '%' + @Filtro + '%' OR e.Fracccion LIKE '%' + @Filtro + '%' OR e.ESAF LIKE '%' + @Filtro + '%'
       OR e.PARTIDA LIKE '%' + @Filtro + '%' OR d.FRACCION LIKE '%' + @Filtro + '%' OR f.CLAVEPEDIMENTO LIKE '%' + @Filtro + '%'
       OR f.EJERCICIO LIKE '%' + @Filtro + '%' OR f.PERIODO LIKE '%' + @Filtro + '%' OR f.FRACCION LIKE '%' + @Filtro + '%'
       OR f.AF LIKE '%' + @Filtro + '%' OR f.ARCHIVO LIKE '%' + @Filtro + '%';
    SELECT d.A31_DESCARGAKEY AS DESCARGA_KEY, d.ENTRADALINK AS ENTRADA_KEY, d.A31_FRACCIONLINK AS A31_FRACCION_KEY,
           e.Pedimentoarmado AS PEDIMENTO, e.PEDIMENTOORIGINAL AS PEDIMENTO_ORIGINAL, e.Fecha AS FECHA_ENTRADA,
           e.Clavepedimento AS CLAVE_PEDIMENTO_ENTRADA, e.Fracccion AS FRACCION_ENTRADA,
           e.Valocomercial AS VALOR_COMERCIAL_ENTRADA, e.SALDO AS SALDO_PERSISTIDO_A31, e.ESAF, e.PARTIDA,
           d.FRACCION AS FRACCION_DESCARGA, d.VALORDESCARGADO AS VALOR_DESCARGADO, f.TIPO AS TIPO_A31,
           f.CLAVEPEDIMENTO AS CLAVE_PEDIMENTO_A31, f.EJERCICIO, f.PERIODO, f.FRACCION AS FRACCION_A31,
           f.VALOR AS VALOR_A31, f.AF, f.ARCHIVO
    FROM dbo.A31_DESCARGAS d
    LEFT JOIN dbo.A31_ENTRADAS e ON e.Entradaskey = d.ENTRADALINK
    LEFT JOIN dbo.A31_DESCARGASF f ON f.A31_FRACCIONKEY = d.A31_FRACCIONLINK
    WHERE @Filtro IS NULL OR e.Pedimentoarmado LIKE '%' + @Filtro + '%' OR e.PEDIMENTOORIGINAL LIKE '%' + @Filtro + '%'
       OR e.Clavepedimento LIKE '%' + @Filtro + '%' OR e.Fracccion LIKE '%' + @Filtro + '%' OR e.ESAF LIKE '%' + @Filtro + '%'
       OR e.PARTIDA LIKE '%' + @Filtro + '%' OR d.FRACCION LIKE '%' + @Filtro + '%' OR f.CLAVEPEDIMENTO LIKE '%' + @Filtro + '%'
       OR f.EJERCICIO LIKE '%' + @Filtro + '%' OR f.PERIODO LIKE '%' + @Filtro + '%' OR f.FRACCION LIKE '%' + @Filtro + '%'
       OR f.AF LIKE '%' + @Filtro + '%' OR f.ARCHIVO LIKE '%' + @Filtro + '%'
    ORDER BY e.Fecha DESC, e.Pedimentoarmado, f.EJERCICIO DESC, f.PERIODO DESC, d.A31_DESCARGAKEY
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
