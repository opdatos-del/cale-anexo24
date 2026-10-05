-- =====================================================================
-- SOURCE           = LIVE OBJECT_DEFINITION
-- AUDITED_AT       = 2026-10-05
-- LEGACY_EXECUTION_SAFE = NO
-- =====================================================================
-- Copia textual fiel de CALE_IMMEX.dbo.CARGA_SUBMAQUILA obtenida READ-ONLY
-- (sys.sql_modules.definition / OBJECT_DEFINITION). NO se corrigio nada:
-- columnas, tipos, nombres, ordering, MAX()+1 ni subqueries.
--
-- La fixture reproduce el defecto actual: SALIDAS.TipoOperacion es INT en LIVE
-- y el SP intenta escribir 'SUBMAQUILA' en esa columna, por lo que la ejecucion
-- falla en el schema LIVE. Por eso este archivo se carga con el schema LIVE
-- (01-submaquila-live-schema.sql) en SubmaquilaLiveContractTest.
--
-- Prohibido editar este archivo para "arreglar" el SP: es evidencia.

USE [CALE_IMMEX];
GO

CREATE PROCEDURE [dbo].[CARGA_SUBMAQUILA]
AS
BEGIN
	DECLARE @SKEY BIGINT
	DECLARE @PSKEY BIGINT

	SET @SKEY = ISNULL((SELECT MAX(SALIDAKEY) FROM SALIDAS),0)+1
	SET @PSKEY = ISNULL((SELECT MAX(PSALIDAKEY) FROM PSALIDAS),0)+1


	INSERT INTO SALIDAS
	(SALIDAKEY,TipoOperacion,Documento,FECHA,Cve_cliente,CVE_PEDIMENTO)

	SELECT ROW_NUMBER()OVER(ORDER BY FOLIO)+@SKEY
	,'SUBMAQUILA',FOLIO,FECHA,SUBMAQUILADOR,'SUB'
	FROM TMPSUBMAQUILA
	GROUP BY FOLIO,FECHA,SUBMAQUILADOR

	INSERT INTO PSALIDAS
	(PSALIDAKEY,FACTURA,FECHA,FRACCION,Descripcion,CANTIDAD,Unidad,Clave,Salidalink)

	SELECT ROW_NUMBER()OVER(ORDER BY CLAVE)+@PSKEY
	,T.FOLIO,T.FECHA,(SELECT fraccion  FROM productos P WHERE P.CVE_PRODUCTO = T.CLAVE),DESCRIPCION,CANTIDAD,UNIDAD,CLAVE
	,(SELECT SALIDAKEY FROM SALIDAS WHERE SALIDAS.Documento = T.FOLIO)
	FROM TMPSUBMAQUILA T




END
GO
