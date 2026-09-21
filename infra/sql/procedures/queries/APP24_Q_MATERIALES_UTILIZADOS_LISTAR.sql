USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR
    @Desde DATE,
    @Hasta DATE,
    @Material VARCHAR(50) = NULL,
    @Producto VARCHAR(50) = NULL,
    @PedimentoSalida VARCHAR(50) = NULL,
    @ClavePedimentoSalida VARCHAR(5) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Desde IS NULL OR @Hasta IS NULL OR @Desde > @Hasta
    BEGIN
        THROW 50031, 'El rango de fechas es obligatorio y válido.', 1;
    END;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50032, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Material = NULLIF(LTRIM(RTRIM(@Material)), '');
    SET @Producto = NULLIF(LTRIM(RTRIM(@Producto)), '');
    SET @PedimentoSalida = NULLIF(LTRIM(RTRIM(@PedimentoSalida)), '');
    SET @ClavePedimentoSalida = NULLIF(LTRIM(RTRIM(@ClavePedimentoSalida)), '');

    DECLARE @HastaExclusivo DATETIME = NULL;
    DECLARE @Desplazamiento BIGINT;
    DECLARE @FilaInicial BIGINT;
    DECLARE @FilaFinal BIGINT;

    IF @Hasta < '9999-12-31'
    BEGIN
        SET @HastaExclusivo = DATEADD(DAY, 1, CAST(@Hasta AS DATETIME));
    END;

    SET @Desplazamiento = (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT);
    SET @FilaInicial = @Desplazamiento + 1;
    SET @FilaFinal = @Desplazamiento + CAST(@Tamano AS BIGINT);

    SELECT @Total = COUNT_BIG(*)
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
    WHERE s.Fecha >= CAST(@Desde AS DATETIME)
      AND (@HastaExclusivo IS NULL OR s.Fecha < @HastaExclusivo)
      AND (@Material IS NULL OR d.Clave = @Material)
      AND (@Producto IS NULL OR d.PT = @Producto)
      AND (@PedimentoSalida IS NULL OR d.Salida = @PedimentoSalida)
      AND (@ClavePedimentoSalida IS NULL OR s.Cve_pedimento = @ClavePedimentoSalida);

    ;WITH Lineas AS (
        SELECT
            d.Descargakey AS DESCARGA_ID,
            i.Ipedimentokey AS ENTRADA_ID,
            pe.Partidakey AS PARTIDA_ENTRADA_ID,
            s.SalidaKey AS SALIDA_ID,
            ps.Psalidakey AS PARTIDA_SALIDA_ID,
            LTRIM(RTRIM(d.Importacion)) AS PEDIMENTO_ENTRADA,
            LTRIM(RTRIM(d.Salida)) AS PEDIMENTO_SALIDA,
            LTRIM(RTRIM(d.Clave)) AS MATERIAL_CODE,
            LTRIM(RTRIM(pe.Descripcion)) AS MATERIAL_DESCRIPTION,
            LTRIM(RTRIM(d.PT)) AS PRODUCT_CODE,
            LTRIM(RTRIM(ps.Descripcion)) AS PRODUCT_DESCRIPTION,
            CONVERT(DECIMAL(18, 4), d.CantUtil) AS CANTIDAD_INCORPORADA,
            CONVERT(DECIMAL(18, 4), d.Merma) AS CANTIDAD_MERMA,
            CONVERT(DECIMAL(18, 4), d.Desperdicio) AS CANTIDAD_DESPERDICIO,
            CONVERT(DECIMAL(18, 4),
                COALESCE(CONVERT(DECIMAL(18, 4), d.CantUtil), 0)
                + COALESCE(CONVERT(DECIMAL(18, 4), d.Merma), 0)
                + COALESCE(CONVERT(DECIMAL(18, 4), d.Desperdicio), 0))
                AS CANTIDAD_TOTAL_DESCARGADA,
            LTRIM(RTRIM(d.Unidad)) AS UNIDAD,
            s.Fecha AS FECHA,
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
        WHERE s.Fecha >= CAST(@Desde AS DATETIME)
          AND (@HastaExclusivo IS NULL OR s.Fecha < @HastaExclusivo)
          AND (@Material IS NULL OR d.Clave = @Material)
          AND (@Producto IS NULL OR d.PT = @Producto)
          AND (@PedimentoSalida IS NULL OR d.Salida = @PedimentoSalida)
          AND (@ClavePedimentoSalida IS NULL OR s.Cve_pedimento = @ClavePedimentoSalida)
    )
    SELECT
        DESCARGA_ID,
        ENTRADA_ID,
        PARTIDA_ENTRADA_ID,
        SALIDA_ID,
        PARTIDA_SALIDA_ID,
        PEDIMENTO_ENTRADA,
        PEDIMENTO_SALIDA,
        MATERIAL_CODE,
        MATERIAL_DESCRIPTION,
        PRODUCT_CODE,
        PRODUCT_DESCRIPTION,
        CANTIDAD_INCORPORADA,
        CANTIDAD_MERMA,
        CANTIDAD_DESPERDICIO,
        CANTIDAD_TOTAL_DESCARGADA,
        UNIDAD,
        FECHA
    FROM Lineas
    WHERE APP24_ROW_NUMBER BETWEEN @FilaInicial AND @FilaFinal
    ORDER BY APP24_ROW_NUMBER;
END;
GO
