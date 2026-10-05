-- CARGA_SUBMAQUILA.expected-contract.sql
--
-- NOT_LIVE_REPLICA - NO es una copia del SP LIVE.
--
-- Este archivo representa el CONTRATO ESPERADO/IDEALIZADO del SP de negocio que
-- los tests de aislamiento (locks, timeout, rollback, MAX()+1) necesitan para poder
-- ejercitar el wrapper endurecido. NO debe usarse como evidencia de fidelidad LIVE.
--
-- Evidencia LIVE 2026-10-05 (ver docs/03-diseno/deuda-tecnica-imports-operativos.md):
-- dbo.CARGA_SUBMAQUILA real escribe 'SUBMAQUILA' en SALIDAS.TipoOperacion INT (falla),
-- usa SALIDAS.Cve_cliente y PSALIDAS.FACTURA/FECHA, y no asigna PSALIDAS.partida.
-- Por eso dbo.CARGA_SUBMAQUILA se clasifica SP_EXISTING_UNSAFE y el archivo fiel al
-- LIVE es CARGA_SUBMAQUILA.legacy.sql (usado por SubmaquilaLiveContractTest).
--
-- La prueba de fidelidad LIVE vive en SubmaquilaLiveContractTest; estos tests NO
-- demuestran que el flujo business LIVE funcione.
--
-- Notas preservadas del contrato esperado:
--   - Sin parametros: consume la totalidad de dbo.TMPSUBMAQUILA.
--   - SIN validacion y SIN error stage. Cualquier error del motor aborta la
--     transaccion del wrapper completo.
--   - INSERT-only en dbo.SALIDAS: una fila por grupo FOLIO/FECHA/SUBMAQUILADOR.
--   - INSERT-only en dbo.PSALIDAS: una fila por renglon del stage.
--   - Claves por MAX()+1 (el wrapper aisla el stage y las tablas compartidas).
--   - SALIDALINK resuelto con subquery escalar sobre dbo.SALIDAS por DOCUMENTO.

USE [CALE_IMMEX];
GO

CREATE PROCEDURE [dbo].[CARGA_SUBMAQUILA]
AS
BEGIN

    INSERT INTO [dbo].[SALIDAS]
               ([SalidaKey]
               ,[Tipo_operacion]
               ,[Documento]
               ,[Fecha]
               ,[Cve_pedimento]
               ,[Transfiere]
               )
    SELECT ROW_NUMBER() OVER (ORDER BY A.FOLIO)
         + (SELECT ISNULL(MAX([SalidaKey]), 0) FROM [dbo].[SALIDAS])
         ,'SUBMAQUILA'
         ,A.FOLIO
         ,CAST(A.FECHA AS DATETIME)
         ,'SUB'
         ,A.SUBMAQUILADOR
    FROM [dbo].[TMPSUBMAQUILA] A
    GROUP BY A.FOLIO, A.FECHA, A.SUBMAQUILADOR

    INSERT INTO [dbo].[PSALIDAS]
               ([Psalidakey]
               ,[Fraccion]
               ,[Descripcion]
               ,[Cantidad]
               ,[Unidad]
               ,[Salidalink]
               ,[Clave]
               ,[partida]
               )
    SELECT ROW_NUMBER() OVER (ORDER BY T.LINEA)
         + (SELECT ISNULL(MAX([Psalidakey]), 0) FROM [dbo].[PSALIDAS])
         ,(SELECT ISNULL(P.FRACCION, '') FROM [dbo].[PRODUCTOS] P WHERE P.CVE_PRODUCTO = T.CLAVE)
         ,ISNULL(T.DESCRIPCION, '')
         ,ISNULL(T.CANTIDAD, 0)
         ,ISNULL(T.UNIDAD, '')
         ,(SELECT S.SalidaKey FROM [dbo].[SALIDAS] S WHERE S.Documento = T.FOLIO)
         ,T.CLAVE
         ,T.LINEA
    FROM [dbo].[TMPSUBMAQUILA] T

END
GO
