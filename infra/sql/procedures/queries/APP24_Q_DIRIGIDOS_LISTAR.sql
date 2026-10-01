USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_DIRIGIDOS_LISTAR
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

    ;WITH Base AS (
        SELECT
            s.SALIDAKEY AS SALIDA_KEY,
            s.PSALIDAKEY AS PSALIDA_KEY,
            s.DOCUMENTO,
            s.[FECHA SALIDA] AS FECHA_SALIDA,
            s.[CLAVE PEDIMENTO] AS CLAVE_PEDIMENTO,
            s.Sec AS SECUENCIA,
            s.PRODUCTO,
            s.cantidad AS CANTIDAD,
            s.[NUMERO FACTURA] AS FACTURA,
            s.DESCARGO,
            s.DIRIGIDO,
            s.[VALOR DESCARGA DOLARES] AS VALOR_DESCARGA_DOLARES,
            s.[VALOR DESCARGA PESOS] AS VALOR_DESCARGA_PESOS,
            s.[TIENE ESTRUCTURA] AS TIENE_ESTRUCTURA,
            s.[NUMERO MATERIALES] AS NUMERO_MATERIALES
        FROM dbo.V_STATUS_DESCARGAS AS s
        WHERE s.DIRIGIDO = 'SI'
    )
    SELECT @Total = COUNT_BIG(*)
    FROM Base
    WHERE @Filtro IS NULL
       OR DOCUMENTO LIKE '%' + @Filtro + '%'
       OR CLAVE_PEDIMENTO LIKE '%' + @Filtro + '%'
       OR PRODUCTO LIKE '%' + @Filtro + '%'
       OR FACTURA LIKE '%' + @Filtro + '%';

    ;WITH Base AS (
        SELECT
            s.SALIDAKEY AS SALIDA_KEY,
            s.PSALIDAKEY AS PSALIDA_KEY,
            s.DOCUMENTO,
            s.[FECHA SALIDA] AS FECHA_SALIDA,
            s.[CLAVE PEDIMENTO] AS CLAVE_PEDIMENTO,
            s.Sec AS SECUENCIA,
            s.PRODUCTO,
            s.cantidad AS CANTIDAD,
            s.[NUMERO FACTURA] AS FACTURA,
            s.DESCARGO,
            s.DIRIGIDO,
            s.[VALOR DESCARGA DOLARES] AS VALOR_DESCARGA_DOLARES,
            s.[VALOR DESCARGA PESOS] AS VALOR_DESCARGA_PESOS,
            s.[TIENE ESTRUCTURA] AS TIENE_ESTRUCTURA,
            s.[NUMERO MATERIALES] AS NUMERO_MATERIALES
        FROM dbo.V_STATUS_DESCARGAS AS s
        WHERE s.DIRIGIDO = 'SI'
    )
    SELECT
        SALIDA_KEY,
        PSALIDA_KEY,
        DOCUMENTO,
        FECHA_SALIDA,
        CLAVE_PEDIMENTO,
        SECUENCIA,
        PRODUCTO,
        CANTIDAD,
        FACTURA,
        DESCARGO,
        DIRIGIDO,
        VALOR_DESCARGA_DOLARES,
        VALOR_DESCARGA_PESOS,
        TIENE_ESTRUCTURA,
        NUMERO_MATERIALES
    FROM Base
    WHERE @Filtro IS NULL
       OR DOCUMENTO LIKE '%' + @Filtro + '%'
       OR CLAVE_PEDIMENTO LIKE '%' + @Filtro + '%'
       OR PRODUCTO LIKE '%' + @Filtro + '%'
       OR FACTURA LIKE '%' + @Filtro + '%'
    ORDER BY SALIDA_KEY, PSALIDA_KEY, SECUENCIA, DOCUMENTO, PRODUCTO, FACTURA
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
