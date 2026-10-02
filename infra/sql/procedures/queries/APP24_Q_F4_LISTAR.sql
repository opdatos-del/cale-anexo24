-- ============================================
-- Líneas dirigidas F4 (CTM APAA y desperdicio) — consulta read-only.
-- Fuente: dbo.V_F4CTMA y dbo.V_F4DESP (vistas legacy read-only).
-- Sin side effects: sólo SELECT; no ejecuta procesos mutables.
-- Consumido por GET /api/v1/reportes/f4 (LEGACY-041, parcial).
-- ============================================
USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_F4_LISTAR
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
            v.TipoDescarga AS TIPO_DESCARGA,
            v.F4 AS F4,
            v.Fecha AS FECHA,
            v.IMPORTACION AS IMPORTACION,
            v.clave AS CLAVE,
            v.incorporado AS INCORPORADO,
            v.saldo AS SALDO,
            v.dirigidokey AS DIRIGIDOKEY
        FROM dbo.V_F4CTMA AS v
        UNION ALL
        SELECT
            v.TipoDescarga, v.F4, v.Fecha, v.IMPORTACION, v.clave, v.incorporado, v.saldo, v.dirigidokey
        FROM dbo.V_F4DESP AS v
    )
    SELECT @Total = COUNT_BIG(*)
    FROM Base
    WHERE @Filtro IS NULL
       OR TIPO_DESCARGA LIKE '%' + @Filtro + '%'
       OR F4 LIKE '%' + @Filtro + '%'
       OR IMPORTACION LIKE '%' + @Filtro + '%'
       OR CLAVE LIKE '%' + @Filtro + '%';

    ;WITH Base AS (
        SELECT
            v.TipoDescarga AS TIPO_DESCARGA,
            v.F4 AS F4,
            v.Fecha AS FECHA,
            v.IMPORTACION AS IMPORTACION,
            v.clave AS CLAVE,
            v.incorporado AS INCORPORADO,
            v.saldo AS SALDO,
            v.dirigidokey AS DIRIGIDOKEY
        FROM dbo.V_F4CTMA AS v
        UNION ALL
        SELECT
            v.TipoDescarga, v.F4, v.Fecha, v.IMPORTACION, v.clave, v.incorporado, v.saldo, v.dirigidokey
        FROM dbo.V_F4DESP AS v
    )
    SELECT
        TIPO_DESCARGA,
        F4,
        FECHA,
        IMPORTACION,
        CLAVE,
        INCORPORADO,
        SALDO
    FROM Base
    WHERE @Filtro IS NULL
       OR TIPO_DESCARGA LIKE '%' + @Filtro + '%'
       OR F4 LIKE '%' + @Filtro + '%'
       OR IMPORTACION LIKE '%' + @Filtro + '%'
       OR CLAVE LIKE '%' + @Filtro + '%'
    ORDER BY FECHA DESC, F4, CLAVE, DIRIGIDOKEY
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
