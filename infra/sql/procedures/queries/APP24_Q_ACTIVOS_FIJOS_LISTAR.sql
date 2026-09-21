USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR
    @Desde DATE = NULL,
    @Hasta DATE = NULL,
    @Pedimento VARCHAR(20) = NULL,
    @ClavePedimento VARCHAR(5) = NULL,
    @NumeroParte VARCHAR(50) = NULL,
    @Descripcion VARCHAR(250) = NULL,
    @Serie VARCHAR(50) = NULL,
    @Marca VARCHAR(50) = NULL,
    @Modelo VARCHAR(50) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF (@Desde IS NULL AND @Hasta IS NOT NULL)
       OR (@Desde IS NOT NULL AND @Hasta IS NULL)
       OR (@Desde IS NOT NULL AND @Hasta IS NOT NULL AND @Desde > @Hasta)
    BEGIN
        THROW 50041, 'Los parámetros desde y hasta deben informarse juntos y formar un rango válido.', 1;
    END;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50042, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Pedimento = NULLIF(LTRIM(RTRIM(@Pedimento)), '');
    SET @ClavePedimento = NULLIF(LTRIM(RTRIM(@ClavePedimento)), '');
    SET @NumeroParte = NULLIF(LTRIM(RTRIM(@NumeroParte)), '');
    SET @Descripcion = NULLIF(LTRIM(RTRIM(@Descripcion)), '');
    SET @Serie = NULLIF(LTRIM(RTRIM(@Serie)), '');
    SET @Marca = NULLIF(LTRIM(RTRIM(@Marca)), '');
    SET @Modelo = NULLIF(LTRIM(RTRIM(@Modelo)), '');

    DECLARE @HastaExclusivo DATETIME = NULL;
    DECLARE @Desplazamiento BIGINT;
    DECLARE @FilaInicial BIGINT;
    DECLARE @FilaFinal BIGINT;

    IF @Hasta IS NOT NULL AND @Hasta < '9999-12-31'
    BEGIN
        SET @HastaExclusivo = DATEADD(DAY, 1, CAST(@Hasta AS DATETIME));
    END;

    SET @Desplazamiento = (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT);
    SET @FilaInicial = @Desplazamiento + 1;
    SET @FilaFinal = @Desplazamiento + CAST(@Tamano AS BIGINT);

    SELECT @Total = COUNT_BIG(*)
    FROM dbo.Partidas AS p
    INNER JOIN dbo.Importaciones AS i
        ON i.Ipedimentokey = p.Importacionlink
    WHERE UPPER(LTRIM(RTRIM(p.EsActivo))) = 'S'
      AND (
          @Desde IS NULL
          OR (
              i.Fecha >= CAST(@Desde AS DATETIME)
              AND (
                  @Hasta = '9999-12-31'
                  OR i.Fecha < @HastaExclusivo
              )
          )
      )
      AND (@Pedimento IS NULL OR i.Numero_ped = @Pedimento)
      AND (@ClavePedimento IS NULL OR i.Cve_pedimento = @ClavePedimento)
      AND (@NumeroParte IS NULL OR p.Clave = @NumeroParte)
      AND (@Descripcion IS NULL OR p.Descripcion = @Descripcion)
      AND (@Serie IS NULL OR p.serie = @Serie)
      AND (@Marca IS NULL OR p.marca = @Marca)
      AND (@Modelo IS NULL OR p.modelo = @Modelo);

    ;WITH ActivosFijos AS (
        SELECT
            p.Partidakey AS PARTIDA_ENTRADA_ID,
            i.Ipedimentokey AS IMPORTACION_ID,
            LTRIM(RTRIM(i.Numero_ped)) AS PEDIMENTO,
            LTRIM(RTRIM(i.Cve_pedimento)) AS CLAVE_PEDIMENTO,
            i.Fecha AS FECHA_IMPORTACION,
            LTRIM(RTRIM(p.Clave)) AS NUMERO_PARTE,
            LTRIM(RTRIM(p.Descripcion)) AS DESCRIPCION,
            LTRIM(RTRIM(p.Fraccion)) AS FRACCION,
            p.Cantidad AS CANTIDAD,
            LTRIM(RTRIM(p.Unidad)) AS UNIDAD,
            LTRIM(RTRIM(p.serie)) AS NUMERO_SERIE,
            LTRIM(RTRIM(p.marca)) AS MARCA,
            LTRIM(RTRIM(p.modelo)) AS MODELO,
            ROW_NUMBER() OVER (
                ORDER BY i.Fecha DESC, i.Ipedimentokey DESC, p.Partidakey DESC
            ) AS APP24_ROW_NUMBER
        FROM dbo.Partidas AS p
        INNER JOIN dbo.Importaciones AS i
            ON i.Ipedimentokey = p.Importacionlink
        WHERE UPPER(LTRIM(RTRIM(p.EsActivo))) = 'S'
          AND (
              @Desde IS NULL
              OR (
                  i.Fecha >= CAST(@Desde AS DATETIME)
                  AND (
                      @Hasta = '9999-12-31'
                      OR i.Fecha < @HastaExclusivo
                  )
              )
          )
          AND (@Pedimento IS NULL OR i.Numero_ped = @Pedimento)
          AND (@ClavePedimento IS NULL OR i.Cve_pedimento = @ClavePedimento)
          AND (@NumeroParte IS NULL OR p.Clave = @NumeroParte)
          AND (@Descripcion IS NULL OR p.Descripcion = @Descripcion)
          AND (@Serie IS NULL OR p.serie = @Serie)
          AND (@Marca IS NULL OR p.marca = @Marca)
          AND (@Modelo IS NULL OR p.modelo = @Modelo)
    )
    SELECT
        PARTIDA_ENTRADA_ID,
        IMPORTACION_ID,
        PEDIMENTO,
        CLAVE_PEDIMENTO,
        FECHA_IMPORTACION,
        NUMERO_PARTE,
        DESCRIPCION,
        FRACCION,
        CANTIDAD,
        UNIDAD,
        NUMERO_SERIE,
        MARCA,
        MODELO
    FROM ActivosFijos
    WHERE APP24_ROW_NUMBER BETWEEN @FilaInicial AND @FilaFinal
    ORDER BY APP24_ROW_NUMBER;
END;
GO
