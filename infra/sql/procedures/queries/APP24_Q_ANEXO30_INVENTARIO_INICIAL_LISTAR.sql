-- Revisión Anexo 30 - Inventario inicial (LEGACY-073, snapshot read-only).
-- Fuente canónica: dbo.INVENTARIOINICIAL, vista sobre dbo.v_saldos.
-- No ejecuta A31_SALDOS ni recalcula saldos.
USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ANEXO30_INVENTARIO_INICIAL_LISTAR
    @Filtro VARCHAR(100) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51000, 'La paginacion de inventario inicial no es valida.', 1;

    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');

    SELECT @Total = COUNT_BIG(*)
    FROM dbo.INVENTARIOINICIAL AS i
    WHERE @Filtro IS NULL
       OR i.Patente LIKE '%' + @Filtro + '%'
       OR i.[Número de pedimento] LIKE '%' + @Filtro + '%'
       OR i.[Clave sección aduanera] LIKE '%' + @Filtro + '%'
       OR i.[Fracción arancelaria o subpartida] LIKE '%' + @Filtro + '%'
       OR i.[Identificador de Activo fijo] LIKE '%' + @Filtro + '%';

    SELECT
        i.Patente AS PATENTE,
        i.[Número de pedimento] AS NUMERO_PEDIMENTO,
        i.[Clave sección aduanera] AS CLAVE_SECCION_ADUANERA,
        i.[Fecha de selección del pedimento] AS FECHA_SELECCION,
        i.[Fracción arancelaria o subpartida] AS FRACCION,
        i.[Valor comercial histórico] AS VALOR_COMERCIAL_HISTORICO,
        i.[Identificador de Activo fijo] AS IDENTIFICADOR_ACTIVO_FIJO
    FROM dbo.INVENTARIOINICIAL AS i
    WHERE @Filtro IS NULL
       OR i.Patente LIKE '%' + @Filtro + '%'
       OR i.[Número de pedimento] LIKE '%' + @Filtro + '%'
       OR i.[Clave sección aduanera] LIKE '%' + @Filtro + '%'
       OR i.[Fracción arancelaria o subpartida] LIKE '%' + @Filtro + '%'
       OR i.[Identificador de Activo fijo] LIKE '%' + @Filtro + '%'
    ORDER BY i.[Fecha de selección del pedimento] DESC,
             i.[Número de pedimento],
             i.[Fracción arancelaria o subpartida],
             i.Patente,
             i.[Clave sección aduanera],
             i.[Identificador de Activo fijo]
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
