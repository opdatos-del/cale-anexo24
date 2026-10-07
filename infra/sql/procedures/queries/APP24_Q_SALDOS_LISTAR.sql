-- ============================================
-- Saldos legacy (LEGACY-038 Reporte Saldos) - consulta read-only.
-- Fuente: dbo.PR_INFORME_SALDOS (SP legacy read-only; definicion completa
--   conocida via dump db.sql del proyecto).
-- Sin side effects: solo materializa el resultset del SP legacy en un table
--   variable local y pagina. No ejecuta SALDOS*, DESCARGAS, INSERTAPEDIMENTO
--   ni ningun generador mutable.
-- Consumido por GET /api/v1/reportes/saldos y /saldos/exportacion.
-- LIVE_ACTIVATION_PENDING = YES (deploy no incluido en este commit).
-- Paridad exacta contra UI legacy: pendiente de segunda auditoria
--   comparativa original vs nuevo (XLSX_PARITY = PENDING_FINAL_COMPARATIVE_AUDIT).
-- Reglas de filtro demostradas:
--   DOCUMENT_OVERRIDES_DATE_RANGE = YES (si @documento no es vacio, anula @DESDE/@HASTA)
--   DATE_FILTER_COLUMN = Importaciones.Fecha (BETWEEN inclusive)
--   DOCUMENT_FILTER_COLUMN = Importaciones.Numero_ped (igualdad exacta)
-- ============================================
USE [CALE_IMMEX];
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_SALDOS_LISTAR
    @DESDE DATETIME = NULL,
    @HASTA DATETIME = NULL,
    @documento VARCHAR(50) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51000, 'La paginacion no es valida.', 1;

    -- Materializa el resultset del SP legacy en una tabla variable local.
    -- El orden de columnas debe coincidir EXACTAMENTE con el SELECT final de
    -- dbo.PR_INFORME_SALDOS (37 columnas confirmadas via
    -- sys.dm_exec_describe_first_result_set_for_object).
    DECLARE @res TABLE (
        [Documento] CHAR(20),
        [Fecha de Pago] DATETIME,
        [Clave Pedimento] CHAR(5),
        [Tipo de Operacion] CHAR(25),
        [tc] FLOAT,
        [Clave] VARCHAR(50),
        [Descripcion] VARCHAR(250),
        [Fraccion] VARCHAR(15),
        [Cant. Importado] NUMERIC(18,4),
        [Unidad] VARCHAR(10),
        [Saldo] NUMERIC(18,4),
        [Valor Aduanal de Saldo] NUMERIC(38,6),
        [Valor dolares del saldo] NUMERIC(38,6),
        [Pais origen] VARCHAR(10),
        [Temporalidad(Meses)] INT,
        [Categoria] CHAR(5),
        [Fecha de Vencimiento] DATETIME,
        [PedimentoOriginal] VARCHAR(19),
        [Descarga] VARCHAR(2),
        [lote] VARCHAR(2),
        [Complemento 1] VARCHAR(50),
        [Complemento 2] VARCHAR(50),
        [Complemento 3] VARCHAR(50),
        [Desperdiciado] NUMERIC(38,4),
        [Saldodesperdicio] NUMERIC(38,4),
        [COVE] VARCHAR(100),
        [Factura] CHAR(50),
        [Tipo Material] VARCHAR(150),
        [pu_vad] NUMERIC(38,10),
        [pu_vdo] NUMERIC(38,10),
        [val_aduanal] NUMERIC(18,4),
        [val_dolares] NUMERIC(18,4),
        [saldo en UMT] FLOAT,
        [unidadt] CHAR(5),
        [valor en pesos] FLOAT,
        [Saldo en valor pesos] FLOAT,
        [NICO] VARCHAR(10)
    );

    INSERT INTO @res
    EXEC dbo.PR_INFORME_SALDOS @DESDE = @DESDE, @HASTA = @HASTA, @documento = @documento;

    SELECT @Total = COUNT_BIG(*) FROM @res;

    -- Proyeccion identica al resultset legacy; orden por Fecha de Pago + Documento
    -- (preservado del PR original). Sin alias ni transformaciones.
    SELECT
        [Documento],
        [Fecha de Pago],
        [Clave Pedimento],
        [Tipo de Operacion],
        [tc],
        [Clave],
        [Descripcion],
        [Fraccion],
        [Cant. Importado],
        [Unidad],
        [Saldo],
        [Valor Aduanal de Saldo],
        [Valor dolares del saldo],
        [Pais origen],
        [Temporalidad(Meses)],
        [Categoria],
        [Fecha de Vencimiento],
        [PedimentoOriginal],
        [Descarga],
        [lote],
        [Complemento 1],
        [Complemento 2],
        [Complemento 3],
        [Desperdiciado],
        [Saldodesperdicio],
        [COVE],
        [Factura],
        [Tipo Material],
        [pu_vad],
        [pu_vdo],
        [val_aduanal],
        [val_dolares],
        [saldo en UMT],
        [unidadt],
        [valor en pesos],
        [Saldo en valor pesos],
        [NICO]
    FROM @res
    ORDER BY [Fecha de Pago], [Documento], [Clave], [Fraccion], [Factura], [NICO],
             [Cant. Importado], [Saldo], [Clave Pedimento], [Tipo de Operacion], [tc],
             [Descripcion], [Unidad], [Valor Aduanal de Saldo], [Valor dolares del saldo],
             [Pais origen], [Temporalidad(Meses)], [Categoria], [Fecha de Vencimiento],
             [PedimentoOriginal], [Descarga], [lote], [Complemento 1], [Complemento 2],
             [Complemento 3], [Desperdiciado], [Saldodesperdicio], [COVE], [Tipo Material],
             [pu_vad], [pu_vdo], [val_aduanal], [val_dolares], [saldo en UMT], [unidadt],
             [valor en pesos], [Saldo en valor pesos]
    OFFSET CONVERT(BIGINT, @Pagina - 1) * @Tamano ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
