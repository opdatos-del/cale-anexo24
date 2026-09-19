USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ENTRADAS_LISTAR
    @Desde DATE,
    @Hasta DATE,
    @Pedimento VARCHAR(20) = NULL,
    @ClavePedimento VARCHAR(5) = NULL,
    @Fraccion VARCHAR(15) = NULL,
    @NumeroParte VARCHAR(50) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Desde IS NULL OR @Hasta IS NULL OR @Desde > @Hasta
    BEGIN
        THROW 50011, 'El rango de fechas es obligatorio y válido.', 1;
    END;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50012, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Pedimento = NULLIF(LTRIM(RTRIM(@Pedimento)), '');
    SET @ClavePedimento = NULLIF(LTRIM(RTRIM(@ClavePedimento)), '');
    SET @Fraccion = NULLIF(LTRIM(RTRIM(@Fraccion)), '');
    SET @NumeroParte = NULLIF(LTRIM(RTRIM(@NumeroParte)), '');

    SELECT @Total = COUNT_BIG(1)
    FROM dbo.Importaciones AS i
    INNER JOIN dbo.Partidas AS p
        ON p.Importacionlink = i.Ipedimentokey
    WHERE i.Fecha >= @Desde
      AND i.Fecha < DATEADD(DAY, 1, @Hasta)
      AND (@Pedimento IS NULL OR i.Numero_ped = @Pedimento)
      AND (@ClavePedimento IS NULL OR i.Cve_pedimento = @ClavePedimento)
      AND (@Fraccion IS NULL OR p.Fraccion = @Fraccion)
      AND (@NumeroParte IS NULL OR p.Clave = @NumeroParte);

    SELECT
        i.Ipedimentokey AS IMPORTACION_ID,
        p.Partidakey AS PARTIDA_ID,
        LTRIM(RTRIM(i.Numero_ped)) AS PEDIMENTO,
        LTRIM(RTRIM(i.Cve_pedimento)) AS CLAVE_PEDIMENTO,
        i.Fecha_ad AS FECHA_ENTRADA,
        LTRIM(RTRIM(p.Fraccion)) AS FRACCION,
        LTRIM(RTRIM(p.Unidad)) AS UNIDAD_COMERCIAL,
        p.Cantidad AS CANTIDAD_COMERCIAL,
        LTRIM(RTRIM(p.Clave)) AS NUMERO_PARTE,
        i.Fecha AS FECHA_PAGO
    FROM dbo.Importaciones AS i
    INNER JOIN dbo.Partidas AS p
        ON p.Importacionlink = i.Ipedimentokey
    WHERE i.Fecha >= @Desde
      AND i.Fecha < DATEADD(DAY, 1, @Hasta)
      AND (@Pedimento IS NULL OR i.Numero_ped = @Pedimento)
      AND (@ClavePedimento IS NULL OR i.Cve_pedimento = @ClavePedimento)
      AND (@Fraccion IS NULL OR p.Fraccion = @Fraccion)
      AND (@NumeroParte IS NULL OR p.Clave = @NumeroParte)
    ORDER BY
        i.Fecha,
        i.Numero_ped,
        i.Ipedimentokey,
        p.partida,
        p.Partidakey
    OFFSET (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
