USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_COMPULSA_DETALLE_LISTAR
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
    FROM dbo.v_compulsa AS c
    WHERE @Filtro IS NULL
       OR c.[PedimentoGlosa] LIKE '%' + @Filtro + '%'
       OR c.[PedimentoA24] LIKE '%' + @Filtro + '%'
       OR c.[Clave Pedimento Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Clave Pedimento A24] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion A24] LIKE '%' + @Filtro + '%'
       OR c.[Pais OD Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Pais OD A24] LIKE '%' + @Filtro + '%'
       OR c.[Pais CV Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Pais CV A24] LIKE '%' + @Filtro + '%'
       OR c.[STATUS CLAVE PEDIMENTO] LIKE '%' + @Filtro + '%'
       OR c.[STATUS FECHAS] LIKE '%' + @Filtro + '%'
       OR c.[STATUS FRACCION] LIKE '%' + @Filtro + '%'
       OR c.[STATUS PAIS OD] LIKE '%' + @Filtro + '%'
       OR c.[STATUS PAIS CV] LIKE '%' + @Filtro + '%'
       OR c.[STATUS VALOR ADUANAL] LIKE '%' + @Filtro + '%'
       OR c.[STATUS VALOR COMERCIAL] LIKE '%' + @Filtro + '%'
       OR c.[STATUS CANTIDAD COMERCIAL] LIKE '%' + @Filtro + '%'
       OR c.[STATUS CANTIDAD TARIFA] LIKE '%' + @Filtro + '%';

    SELECT
        c.[PedimentoGlosa] AS PEDIMENTO_GLOSA,
        c.[SEC GLOSA] AS SEC_GLOSA,
        c.[PedimentoA24] AS PEDIMENTO_A24,
        c.[SEC A24] AS SEC_A24,
        c.[Clave Pedimento Glosa] AS CLAVE_PEDIMENTO_GLOSA,
        c.[Clave Pedimento A24] AS CLAVE_PEDIMENTO_A24,
        c.[STATUS CLAVE PEDIMENTO] AS STATUS_CLAVE_PEDIMENTO,
        c.[FechaGlosa] AS FECHA_GLOSA,
        c.[FechaA24] AS FECHA_A24,
        c.[STATUS FECHAS] AS STATUS_FECHAS,
        c.[Fraccion Glosa] AS FRACCION_GLOSA,
        c.[Fraccion A24] AS FRACCION_A24,
        c.[STATUS FRACCION] AS STATUS_FRACCION,
        c.[Pais OD Glosa] AS PAIS_OD_GLOSA,
        c.[Pais OD A24] AS PAIS_OD_A24,
        c.[STATUS PAIS OD] AS STATUS_PAIS_OD,
        c.[Pais CV Glosa] AS PAIS_CV_GLOSA,
        c.[Pais CV A24] AS PAIS_CV_A24,
        c.[STATUS PAIS CV] AS STATUS_PAIS_CV,
        c.[Valor Aduana Glosa] AS VALOR_ADUANA_GLOSA,
        c.[Valor Aduana A24] AS VALOR_ADUANA_A24,
        c.[STATUS VALOR ADUANAL] AS STATUS_VALOR_ADUANA,
        c.[Valor Comercial Glosa] AS VALOR_COMERCIAL_GLOSA,
        c.[Valor Comercial A24] AS VALOR_COMERCIAL_A24,
        c.[STATUS VALOR COMERCIAL] AS STATUS_VALOR_COMERCIAL,
        c.[Cantidad UMC Glosa] AS CANTIDAD_UMC_GLOSA,
        c.[Cantidad UMC A24] AS CANTIDAD_UMC_A24,
        c.[STATUS CANTIDAD COMERCIAL] AS STATUS_CANTIDAD_COMERCIAL,
        c.[Cantidad UMT Glosa] AS CANTIDAD_UMT_GLOSA,
        c.[Cantidad UMT A24] AS CANTIDAD_UMT_A24,
        c.[STATUS CANTIDAD TARIFA] AS STATUS_CANTIDAD_TARIFA,
        c.[toper] AS TIPO_OPERACION_GLOSA,
        c.[tipoped] AS TIPO_PEDIMENTO_GLOSA
    FROM dbo.v_compulsa AS c
    WHERE @Filtro IS NULL
       OR c.[PedimentoGlosa] LIKE '%' + @Filtro + '%'
       OR c.[PedimentoA24] LIKE '%' + @Filtro + '%'
       OR c.[Clave Pedimento Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Clave Pedimento A24] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Fraccion A24] LIKE '%' + @Filtro + '%'
       OR c.[Pais OD Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Pais OD A24] LIKE '%' + @Filtro + '%'
       OR c.[Pais CV Glosa] LIKE '%' + @Filtro + '%'
       OR c.[Pais CV A24] LIKE '%' + @Filtro + '%'
       OR c.[STATUS CLAVE PEDIMENTO] LIKE '%' + @Filtro + '%'
       OR c.[STATUS FECHAS] LIKE '%' + @Filtro + '%'
       OR c.[STATUS FRACCION] LIKE '%' + @Filtro + '%'
       OR c.[STATUS PAIS OD] LIKE '%' + @Filtro + '%'
       OR c.[STATUS PAIS CV] LIKE '%' + @Filtro + '%'
       OR c.[STATUS VALOR ADUANAL] LIKE '%' + @Filtro + '%'
       OR c.[STATUS VALOR COMERCIAL] LIKE '%' + @Filtro + '%'
       OR c.[STATUS CANTIDAD COMERCIAL] LIKE '%' + @Filtro + '%'
       OR c.[STATUS CANTIDAD TARIFA] LIKE '%' + @Filtro + '%'
    ORDER BY
        c.[FechaGlosa], c.[FechaA24], c.[PedimentoGlosa], c.[PedimentoA24],
        c.[SEC GLOSA], c.[SEC A24], c.[Clave Pedimento Glosa], c.[Clave Pedimento A24],
        c.[Fraccion Glosa], c.[Fraccion A24], c.[Pais OD Glosa], c.[Pais OD A24],
        c.[Pais CV Glosa], c.[Pais CV A24], c.[Valor Aduana Glosa], c.[Valor Aduana A24],
        c.[Valor Comercial Glosa], c.[Valor Comercial A24], c.[Cantidad UMC Glosa],
        c.[Cantidad UMC A24], c.[Cantidad UMT Glosa], c.[Cantidad UMT A24],
        c.[STATUS CLAVE PEDIMENTO], c.[STATUS FECHAS], c.[STATUS FRACCION],
        c.[STATUS PAIS OD], c.[STATUS PAIS CV], c.[STATUS VALOR ADUANAL],
        c.[STATUS VALOR COMERCIAL], c.[STATUS CANTIDAD COMERCIAL],
        c.[STATUS CANTIDAD TARIFA], c.[toper], c.[tipoped]
    OFFSET (@Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
