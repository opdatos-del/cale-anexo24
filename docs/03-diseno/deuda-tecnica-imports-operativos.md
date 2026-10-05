# Deuda técnica — imports operativos (SUBMAQUILA)

Registro de deuda diferida del contrato de constancias de transferencia
(submaquila) y del motor de importaciones. No bloquea la funcionalidad.

## 1. Reutilización del motor común de imports

`SUBMAQUILA` es una **operación**, no un catálogo: su confirmación aísla un stage
legacy (`dbo.TMPSUBMAQUILA`), delega la regla de negocio en
`dbo.CARGA_SUBMAQUILA` y escribe salidas/partidas autoritativas. Sin embargo, en
esta versión reutiliza la infraestructura de importaciones de catálogos
(`catalogs/imports`: parser Excel, staging `app24.CargaSubmaquila*`, command de
confirmación y UI de previsualización).

Decisión: **no** se mueve toda esa infraestructura en esta rama. Cuando los
imports operativos estén cerrados se evaluará extraer un módulo
`shared/imports` (parser + staging genérico + bitácora) si realmente conviene.
Prioridad: no bloquear funcionalidad por pureza estructural.

## 2. Hallazgo de auditoría LIVE (2026-10-05) — réplica de test vs. SP real

La réplica de test `backend/src/test/resources/sql/submaquilas/CARGA_SUBMAQUILA.legacy.sql`
se documenta como "réplica textual" auditada READ-ONLY. La consulta directa a
`CALE_IMMEX.sys.sql_modules` (`OBJECT_DEFINITION`) muestra que **el SP real no
coincide** con esa réplica:

```sql
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
    ,T.FOLIO,T.FECHA,(SELECT fraccion FROM productos P WHERE P.CVE_PRODUCTO = T.CLAVE),DESCRIPCION,CANTIDAD,UNIDAD,CLAVE
    ,(SELECT SALIDAKEY FROM SALIDAS WHERE SALIDAS.Documento = T.FOLIO)
    FROM TMPSUBMAQUILA T
END
```

Diferencias materiales frente a la réplica de test:

| Aspecto | Réplica de test | SP real LIVE |
|---|---|---|
| Columna de tipo en `SALIDAS` | `Tipo_operacion CHAR(25)` = `'SUBMAQUILA'` | `TipoOperacion INT` = `'SUBMAQUILA'` → conversión inválida |
| Submaquilador | `Transfiere CHAR(20)` | `Cve_cliente CHAR(15)` |
| `PSALIDAS.partida` | se asigna `LINEA` | **no se asigna** |
| `PSALIDAS` | `Descripcion`, `Unidad`, `Clave`, `partida` | `FACTURA`, `FECHA`, `Descripcion`, `Unidad`, `Clave` |

Consecuencia: el SP real escribiría `'SUBMAQUILA'` en `SALIDAS.TipoOperacion INT`
(verificado: `max_length = 4`), lo que fallaría en ejecución. La réplica de test
es un contrato **idealizado** que hace pasar la suite, no una copia del legacy.

Recomendación: **reconciliar antes de activar en LIVE**
(`SUBMAQUILA_LIVE_ACTIVATION_PENDING = YES`). Opciones: (a) confirmar que el SP
real es código muerto y autorizar un contrato corregido; (b) corregir la réplica
para que refleje el SP real y rehacer el contrato. No se debe afirmar fidelidad
LIVE mientras no coincidan.
