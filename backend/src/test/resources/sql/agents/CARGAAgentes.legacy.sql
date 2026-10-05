/*
 * Réplica textual del SP legacy dbo.CARGAAgentes auditado READ-ONLY contra
 * CALE_IMMEX LIVE el 2026-10-05. Se carga en SQL Testcontainers para preservar
 * el comportamiento real durante la prueba de aislamiento.
 *
 * Notas preservadas:
 *   * TRUNCATE TABLE ECARGAagentes al inicio.
 *   * INSERT-only en dbo.agentes: omite silenciosamente agentes cuya CLAVE ya
 *     existe (NOT EXISTS sobre CLAVE).
 *   * Validaciones reportadas por AGENTEKEY: clave vacía, patente vacía,
 *     longitud de patente <> 4 y claves duplicadas dentro del stage.
 *   * Sin NOLOCK ni READ UNCOMMITTED; sin SET TRANSACTION ISOLATION LEVEL custom.
 *   * Sin resultadosets, sin TRY/CATCH y sin transacción propia.
 */

USE [CALE_IMMEX];
GO

CREATE  PROCEDURE [dbo].[CARGAAgentes]
AS
BEGIN
TRUNCATE TABLE [ECARGAagentes]


INSERT INTO [dbo].[ECARGAagentes]
           ([TMPKEY]
           ,[ERROR]
           ,[CLAVE])


SELECT [agentekey],'EXISTEN CLAVES VACIAS',''
FROM [dbo].[TMPagentes]
WHERE ISNULL([Clave],'')=''

INSERT INTO [dbo].[ECARGAagentes]
           ([TMPKEY]
           ,[ERROR]
           ,[CLAVE])
SELECT [agentekey],'LA PATENTE NO PUEDE ESTAR VACIA',''
FROM [dbo].[TMPagentes]
WHERE ISNULL([patente],'')=''

INSERT INTO [dbo].[ECARGAagentes]
           ([TMPKEY]
           ,[ERROR]
           ,[CLAVE])
SELECT [agentekey],'la longitud de la patente es erronea',''
FROM [dbo].[TMPagentes]
WHERE len(ISNULL([patente],''))<>4

INSERT INTO [dbo].[ECARGAagentes]
           ([TMPKEY]
           ,[ERROR]
           ,[CLAVE])


SELECT [agentekey],'EXISTEN CLAVES DUPLICADAS',CLAVE
FROM [dbo].[TMPagentes]
WHERE CLAVE IN (SELECT CLAVE FROM [dbo].[TMPagentes] GROUP BY CLAVE HAVING COUNT(*)>1)


INSERT INTO [dbo].[agentes]
           ([Clave]
           ,[Nombre]
           ,[Domicilio]
           ,[Rfc]
           ,[Patente]
           )
SELECT [Clave]
           ,[Nombre]
           ,[Domicilio]
           ,[Rfc]
           ,[Patente]
FROM [TMPagentes]
WHERE NOT EXISTS (SELECT * FROM ECARGAagentes E WHERE E.TMPKEY = [TMPagentes].[AGENTEKEY])
AND NOT EXISTS (SELECT CLAVE FROM AGENTES WHERE AGENTES.CLAVE = TMPAGENTES.CLAVE)

END

GO