USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_SALIDAS_LISTAR
    @Desde DATE,
    @Hasta DATE,
    @Pedimento VARCHAR(60) = NULL,
    @ClavePedimento VARCHAR(5) = NULL,
    @Fraccion VARCHAR(12) = NULL,
    @NumeroParte VARCHAR(50) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Desde IS NULL OR @Hasta IS NULL OR @Desde > @Hasta
    BEGIN
        THROW 50021, 'El rango de fechas es obligatorio y válido.', 1;
    END;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50022, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Pedimento = NULLIF(LTRIM(RTRIM(@Pedimento)), '');
    SET @ClavePedimento = NULLIF(LTRIM(RTRIM(@ClavePedimento)), '');
    SET @Fraccion = NULLIF(LTRIM(RTRIM(@Fraccion)), '');
    SET @NumeroParte = NULLIF(LTRIM(RTRIM(@NumeroParte)), '');

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

    SELECT @Total = COUNT_BIG(1)
    FROM dbo.PSALIDAS AS p
    INNER JOIN dbo.SALIDAS AS s
        ON CONVERT(NUMERIC(18, 0), CASE
               WHEN p.Salidalink > -1.0E18
                AND p.Salidalink < 1.0E18
                AND p.Salidalink = FLOOR(p.Salidalink)
               THEN p.Salidalink
           END) = s.SalidaKey
    WHERE s.Fecha >= CAST(@Desde AS DATETIME)
      AND (@HastaExclusivo IS NULL OR s.Fecha < @HastaExclusivo)
      AND (@Pedimento IS NULL OR s.Documento = @Pedimento)
      AND (@ClavePedimento IS NULL OR s.Cve_pedimento = @ClavePedimento)
      AND (@Fraccion IS NULL OR p.Fraccion = @Fraccion)
      AND (@NumeroParte IS NULL OR p.Clave = @NumeroParte);

    ;WITH Lineas AS (
        SELECT
            s.SalidaKey AS SALIDA_ID,
            p.Psalidakey AS PARTIDA_ID,
            LTRIM(RTRIM(s.Documento)) AS PEDIMENTO,
            LTRIM(RTRIM(s.Cve_pedimento)) AS CLAVE_PEDIMENTO,
            LTRIM(RTRIM(p.Fraccion)) AS FRACCION,
            LTRIM(RTRIM(p.Unidad)) AS UNIDAD_COMERCIAL,
            p.Cantidad AS CANTIDAD,
            LTRIM(RTRIM(p.Clave)) AS NUMERO_PARTE,
            s.Fecha AS FECHA_PAGO,
            ROW_NUMBER() OVER (
                ORDER BY s.Fecha, s.Documento, s.SalidaKey, p.partida, p.Psalidakey
            ) AS APP24_ROW_NUMBER
        FROM dbo.PSALIDAS AS p
        INNER JOIN dbo.SALIDAS AS s
            ON CONVERT(NUMERIC(18, 0), CASE
                   WHEN p.Salidalink > -1.0E18
                    AND p.Salidalink < 1.0E18
                    AND p.Salidalink = FLOOR(p.Salidalink)
                   THEN p.Salidalink
               END) = s.SalidaKey
        WHERE s.Fecha >= CAST(@Desde AS DATETIME)
          AND (@HastaExclusivo IS NULL OR s.Fecha < @HastaExclusivo)
          AND (@Pedimento IS NULL OR s.Documento = @Pedimento)
          AND (@ClavePedimento IS NULL OR s.Cve_pedimento = @ClavePedimento)
          AND (@Fraccion IS NULL OR p.Fraccion = @Fraccion)
          AND (@NumeroParte IS NULL OR p.Clave = @NumeroParte)
    )
    SELECT
        SALIDA_ID,
        PARTIDA_ID,
        PEDIMENTO,
        CLAVE_PEDIMENTO,
        FRACCION,
        UNIDAD_COMERCIAL,
        CANTIDAD,
        NUMERO_PARTE,
        FECHA_PAGO
    FROM Lineas
    WHERE APP24_ROW_NUMBER BETWEEN @FilaInicial AND @FilaFinal
    ORDER BY APP24_ROW_NUMBER;
END;
GO
