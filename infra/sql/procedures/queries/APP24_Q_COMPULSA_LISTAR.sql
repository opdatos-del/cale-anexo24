USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_COMPULSA_LISTAR
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
    FROM dbo.v_compulsa_gen AS c
    WHERE @Filtro IS NULL
       OR c.[Pedimento Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Pedimento A24] LIKE '%' + @Filtro + '%'
       OR c.[Clave Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Clave A24] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion A24] LIKE '%' + @Filtro + '%';

    SELECT
        c.[Pedimento Glosa] AS PEDIMENTO_GLOSA,
        c.[Pedimento A24] AS PEDIMENTO_ANEXO24,
        c.[Fecha Glosa] AS FECHA_GLOSA,
        c.[Fecha A24] AS FECHA_ANEXO24,
        c.[Clave Glosa] AS CLAVE_GLOSA,
        c.[Clave A24] AS CLAVE_ANEXO24,
        c.[Fraccion Glosa] AS FRACCION_GLOSA,
        c.[Fraccion A24] AS FRACCION_ANEXO24
    FROM dbo.v_compulsa_gen AS c
    WHERE @Filtro IS NULL
       OR c.[Pedimento Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Pedimento A24] LIKE '%' + @Filtro + '%'
       OR c.[Clave Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Clave A24] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion A24] LIKE '%' + @Filtro + '%'
    ORDER BY c.[Pedimento A24], c.[Pedimento Glosa], c.[Fecha A24], c.[Fecha Glosa], c.[Clave A24], c.[Clave Glosa], c.[Fraccion A24], c.[Fraccion Glosa]
    OFFSET (@Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
