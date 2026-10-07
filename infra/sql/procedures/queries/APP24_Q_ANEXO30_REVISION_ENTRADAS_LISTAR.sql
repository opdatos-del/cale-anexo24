-- ============================================
-- Revisión Anexo 30 - Entradas (LEGACY-073 parcial).
-- Fuente: dbo.A31_ENTRADAS (snapshot persistido de entradas Anexo 30).
-- Sin side effects: solo SELECT sobre tabla persistida.
-- No ejecuta procesos mutables A31 (DESCARGAS_A31, A31_SALDOS, etc.).
-- Consumido por GET /api/v1/reportes/anexo30-revision-entradas.
-- LIVE_ACTIVATION_PENDING = YES (deploy no incluido en este commit).
-- ============================================
USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR
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
    FROM dbo.A31_ENTRADAS AS e
    WHERE @Filtro IS NULL
       OR e.Pedimentoarmado LIKE '%' + @Filtro + '%'
       OR e.PEDIMENTOORIGINAL LIKE '%' + @Filtro + '%'
       OR e.Fracccion LIKE '%' + @Filtro + '%'
       OR e.Clavepedimento LIKE '%' + @Filtro + '%'
       OR e.ESAF LIKE '%' + @Filtro + '%'
       OR CAST(e.OPERACION AS VARCHAR(20)) LIKE '%' + @Filtro + '%';

    SELECT
        e.Entradaskey AS ENTRADA_KEY,
        e.Descarga AS DESCARGA,
        e.Tipooperacion AS TIPO_OPERACION,
        e.Pedimentoarmado AS PEDIMENTO,
        e.PEDIMENTOORIGINAL AS PEDIMENTO_ORIGINAL,
        e.Fecha AS FECHA,
        e.FECHAORIGINAL AS FECHA_ORIGINAL,
        e.Clavepedimento AS CLAVE_PEDIMENTO,
        e.Fracccion AS FRACCION,
        e.Valocomercial AS VALOR_COMERCIAL,
        e.IVAFP21 AS IVA_FP21,
        e.IVAFP22 AS IVA_FP22,
        e.SALDO AS SALDO,
        e.OPERACION AS OPERACION,
        e.PARTIDA AS PARTIDA,
        e.ESAF AS ESAF
    FROM dbo.A31_ENTRADAS AS e
    WHERE @Filtro IS NULL
       OR e.Pedimentoarmado LIKE '%' + @Filtro + '%'
       OR e.PEDIMENTOORIGINAL LIKE '%' + @Filtro + '%'
       OR e.Fracccion LIKE '%' + @Filtro + '%'
       OR e.Clavepedimento LIKE '%' + @Filtro + '%'
       OR e.ESAF LIKE '%' + @Filtro + '%'
       OR CAST(e.OPERACION AS VARCHAR(20)) LIKE '%' + @Filtro + '%'
    ORDER BY e.Fecha DESC, e.Pedimentoarmado, e.Fracccion, e.Entradaskey
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
