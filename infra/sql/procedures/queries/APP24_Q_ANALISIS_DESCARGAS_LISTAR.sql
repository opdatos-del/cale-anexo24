USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR
    @Filtro VARCHAR(60) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50041, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Filtro = NULLIF(LTRIM(RTRIM(@Filtro)), '');

    DECLARE @Desplazamiento BIGINT = (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT);
    DECLARE @FilaInicial BIGINT = @Desplazamiento + 1;
    DECLARE @FilaFinal BIGINT = @Desplazamiento + CAST(@Tamano AS BIGINT);

    ;WITH Base AS (
        SELECT
            d.Descargakey AS DESCARGA_ID,
            i.Numero_ped AS PEDIMENTO_ENTRADA,
            pe.Partida AS PARTIDA_ENTRADA,
            CASE WHEN s.Cve_pedimento <> 'ct' THEN s.Documento END AS PEDIMENTO_SALIDA,
            ps.Partida AS PARTIDA_SALIDA,
            pe.Clave AS MATERIAL,
            ps.Clave AS PRODUCTO,
            i.Fecha AS FECHA_IMPORTACION,
            s.Fecha AS FECHA_SALIDA,
            DATEADD(MONTH, c.Meses, i.Fecha) AS FECHA_VENCIMIENTO,
            pe.Cantidad AS CANTIDAD_IMPORTADA,
            ps.Cantidad AS CANTIDAD_EXPORTADA,
            d.CantUtil AS CANTIDAD_INCORPORADA,
            d.Merma AS CANTIDAD_MERMA,
            d.Desperdicio AS CANTIDAD_DESPERDICIO,
            d.Unidad AS UNIDAD,
            ROW_NUMBER() OVER (
                ORDER BY s.Fecha DESC, s.SalidaKey DESC, ps.Psalidakey DESC, d.Descargakey DESC
            ) AS APP24_ROW_NUMBER
        FROM dbo.DESCARGA AS d
        INNER JOIN dbo.PARTIDAS AS pe
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Pentradalink > -1.0E18
                    AND d.Pentradalink < 1.0E18
                    AND d.Pentradalink = FLOOR(d.Pentradalink)
                   THEN FLOOR(d.Pentradalink)
               END) = pe.Partidakey
        INNER JOIN dbo.IMPORTACIONES AS i
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Entradalink > -1.0E18
                    AND d.Entradalink < 1.0E18
                    AND d.Entradalink = FLOOR(d.Entradalink)
                   THEN FLOOR(d.Entradalink)
               END) = i.Ipedimentokey
        INNER JOIN dbo.PSALIDAS AS ps
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Psalidalink > -1.0E18
                    AND d.Psalidalink < 1.0E18
                    AND d.Psalidalink = FLOOR(d.Psalidalink)
                   THEN FLOOR(d.Psalidalink)
               END) = ps.Psalidakey
        INNER JOIN dbo.SALIDAS AS s
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Salidalink > -1.0E18
                    AND d.Salidalink < 1.0E18
                    AND d.Salidalink = FLOOR(d.Salidalink)
                   THEN FLOOR(d.Salidalink)
               END) = s.SalidaKey
        LEFT JOIN dbo.CATEGORIAS AS c ON c.Categoria = pe.Categoria
        WHERE @Filtro IS NULL
           OR i.Numero_ped LIKE '%' + @Filtro + '%'
           OR s.Documento LIKE '%' + @Filtro + '%'
           OR pe.Clave LIKE '%' + @Filtro + '%'
           OR ps.Clave LIKE '%' + @Filtro + '%'
           OR d.Clave LIKE '%' + @Filtro + '%'
           OR d.PT LIKE '%' + @Filtro + '%'
    )
    SELECT @Total = COUNT_BIG(*)
    FROM Base;

    ;WITH Base AS (
        SELECT
            d.Descargakey AS DESCARGA_ID,
            i.Numero_ped AS PEDIMENTO_ENTRADA,
            pe.Partida AS PARTIDA_ENTRADA,
            CASE WHEN s.Cve_pedimento <> 'ct' THEN s.Documento END AS PEDIMENTO_SALIDA,
            ps.Partida AS PARTIDA_SALIDA,
            pe.Clave AS MATERIAL,
            ps.Clave AS PRODUCTO,
            i.Fecha AS FECHA_IMPORTACION,
            s.Fecha AS FECHA_SALIDA,
            DATEADD(MONTH, c.Meses, i.Fecha) AS FECHA_VENCIMIENTO,
            pe.Cantidad AS CANTIDAD_IMPORTADA,
            ps.Cantidad AS CANTIDAD_EXPORTADA,
            d.CantUtil AS CANTIDAD_INCORPORADA,
            d.Merma AS CANTIDAD_MERMA,
            d.Desperdicio AS CANTIDAD_DESPERDICIO,
            d.Unidad AS UNIDAD,
            ROW_NUMBER() OVER (
                ORDER BY s.Fecha DESC, s.SalidaKey DESC, ps.Psalidakey DESC, d.Descargakey DESC
            ) AS APP24_ROW_NUMBER
        FROM dbo.DESCARGA AS d
        INNER JOIN dbo.PARTIDAS AS pe
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Pentradalink > -1.0E18
                    AND d.Pentradalink < 1.0E18
                    AND d.Pentradalink = FLOOR(d.Pentradalink)
                   THEN FLOOR(d.Pentradalink)
               END) = pe.Partidakey
        INNER JOIN dbo.IMPORTACIONES AS i
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Entradalink > -1.0E18
                    AND d.Entradalink < 1.0E18
                    AND d.Entradalink = FLOOR(d.Entradalink)
                   THEN FLOOR(d.Entradalink)
               END) = i.Ipedimentokey
        INNER JOIN dbo.PSALIDAS AS ps
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Psalidalink > -1.0E18
                    AND d.Psalidalink < 1.0E18
                    AND d.Psalidalink = FLOOR(d.Psalidalink)
                   THEN FLOOR(d.Psalidalink)
               END) = ps.Psalidakey
        INNER JOIN dbo.SALIDAS AS s
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN d.Salidalink > -1.0E18
                    AND d.Salidalink < 1.0E18
                    AND d.Salidalink = FLOOR(d.Salidalink)
                   THEN FLOOR(d.Salidalink)
               END) = s.SalidaKey
        LEFT JOIN dbo.CATEGORIAS AS c ON c.Categoria = pe.Categoria
        WHERE @Filtro IS NULL
           OR i.Numero_ped LIKE '%' + @Filtro + '%'
           OR s.Documento LIKE '%' + @Filtro + '%'
           OR pe.Clave LIKE '%' + @Filtro + '%'
           OR ps.Clave LIKE '%' + @Filtro + '%'
           OR d.Clave LIKE '%' + @Filtro + '%'
           OR d.PT LIKE '%' + @Filtro + '%'
    )
    SELECT
        DESCARGA_ID,
        PEDIMENTO_ENTRADA,
        PARTIDA_ENTRADA,
        PEDIMENTO_SALIDA,
        PARTIDA_SALIDA,
        MATERIAL,
        PRODUCTO,
        FECHA_IMPORTACION,
        FECHA_SALIDA,
        FECHA_VENCIMIENTO,
        CANTIDAD_IMPORTADA,
        CANTIDAD_EXPORTADA,
        CANTIDAD_INCORPORADA,
        CANTIDAD_MERMA,
        CANTIDAD_DESPERDICIO,
        UNIDAD
    FROM Base
    WHERE APP24_ROW_NUMBER BETWEEN @FilaInicial AND @FilaFinal
    ORDER BY APP24_ROW_NUMBER;
END;
GO
