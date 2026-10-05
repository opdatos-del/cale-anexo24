/*
 * Réplica textual del SP legacy dbo.CARGA_SUBMAQUILA auditado READ-ONLY contra
 * CALE_IMMEX LIVE el 2026-10-05. Se carga en SQL Testcontainers para preservar
 * el comportamiento real durante la prueba de aislamiento.
 *
 * Notas preservadas del legacy:
 *   * Sin parámetros: consume la totalidad de dbo.TMPSUBMAQUILA.
 *   * SIN validación y SIN error stage (a diferencia de dbo.CARGAAgentes, que sí
 *     truncaba y llenaba dbo.ECARGAagentes). Cualquier error del motor aborta la
 *     transacción del wrapper completo.
 *   * INSERT-only en dbo.SALIDAS: una fila por grupo FOLIO/FECHA/SUBMAQUILADOR con
 *     TIPO_OPERACION='SUBMAQUILA' y CVE_PEDIMENTO='SUB'.
 *   * INSERT-only en dbo.PSALIDAS: una fila por renglón del stage.
 *   * Claves por MAX()+1 (no identity, no seguro ante concurrencia: el wrapper aísla
 *     el stage con TABLOCKX/HOLDLOCK para serializar las ejecuciones).
 *   * SALIDALINK resuelto con subquery escalar sobre dbo.SALIDAS por DOCUMENTO: si el
 *     folio ya tuviera más de una salida, el motor lanzaría "subquery returned more
 *     than 1 value". Es una restricción heredada y el wrapper no la silencia.
 *   * Sin NOLOCK ni READ UNCOMMITTED; sin resultadosets, sin TRY/CATCH y sin
 *     transacción propia.
 */

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
