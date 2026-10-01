USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_VENCIMIENTOS_LISTAR
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
    FROM dbo.vDESPERDICIOS AS v
    WHERE @Filtro IS NULL
       OR v.numero_ped LIKE '%' + @Filtro + '%'
       OR v.cve_pedimento LIKE '%' + @Filtro + '%'
       OR v.clave LIKE '%' + @Filtro + '%'
       OR v.FACTURA LIKE '%' + @Filtro + '%';

    SELECT
        v.numero_ped AS PEDIMENTO,
        v.fecha AS FECHA_BASE,
        v.cve_pedimento AS CLAVE_PEDIMENTO,
        v.clave AS CLAVE,
        v.unidad AS UNIDAD,
        v.unidadt AS UNIDAD_T,
        v.Desperdicio AS DESPERDICIO,
        v.DesperdicioT AS DESPERDICIO_T,
        v.VALORDESPERDICIO AS VALOR_DESPERDICIO,
        v.Aplicado AS APLICADO,
        v.FACTURA AS FACTURA,
        v.VENCIMIENTO AS VENCIMIENTO
    FROM dbo.vDESPERDICIOS AS v
    WHERE @Filtro IS NULL
       OR v.numero_ped LIKE '%' + @Filtro + '%'
       OR v.cve_pedimento LIKE '%' + @Filtro + '%'
       OR v.clave LIKE '%' + @Filtro + '%'
       OR v.FACTURA LIKE '%' + @Filtro + '%'
    ORDER BY v.numero_ped, v.fecha, v.cve_pedimento, v.clave, v.unidad, v.unidadt,
             v.FACTURA, v.VENCIMIENTO, v.Desperdicio, v.DesperdicioT,
             v.VALORDESPERDICIO, v.Aplicado, v.linea
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
