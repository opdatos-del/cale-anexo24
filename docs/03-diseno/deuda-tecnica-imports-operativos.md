# Deuda tecnica - imports operativos (SUBMAQUILA)

Registro de deuda diferida y bloqueos del contrato de constancias de transferencia
(submaquila) y del motor de importaciones.

## 1. Reutilizacion del motor comun de imports

SUBMAQUILA es una operacion, no un catalogo: su confirmacion aisla un stage legacy
(dbo.TMPSUBMAQUILA), delega la regla de negocio en dbo.CARGA_SUBMAQUILA y escribe
salidas/partidas autoritativas. En esta version reutiliza la infraestructura de
importaciones de catalogos (parser Excel, staging app24.CargaSubmaquila*, command de
confirmacion y UI de previsualizacion).

Decision: NO se mueve toda esa infraestructura en esta rama. Cuando los imports
operativos esten cerrados se evaluara extraer un modulo shared/imports si conviene.
Prioridad: no bloquear funcionalidad por pureza estructural.

## 2. Clasificacion LIVE: dbo.CARGA_SUBMAQUILA = SP_EXISTING_UNSAFE

La replica de test anterior afirmaba ser copia textual del SP LIVE. La consulta
READ-ONLY a CALE_IMMEX (sys.sql_modules / OBJECT_DEFINITION, 2026-10-05) muestra que
el SP real diverge materialmente del contrato usado por la fixture Testcontainers.

SP real LIVE (resumen):

    CREATE PROCEDURE [dbo].[CARGA_SUBMAQUILA]
    AS
    BEGIN
        DECLARE @SKEY BIGINT, @PSKEY BIGINT
        SET @SKEY  = ISNULL((SELECT MAX(SALIDAKEY)  FROM SALIDAS),0)+1
        SET @PSKEY = ISNULL((SELECT MAX(PSALIDAKEY) FROM PSALIDAS),0)+1
        INSERT INTO SALIDAS
        (SALIDAKEY,TipoOperacion,Documento,FECHA,Cve_cliente,CVE_PEDIMENTO)
        SELECT ROW_NUMBER()OVER(ORDER BY FOLIO)+@SKEY,
               'SUBMAQUILA',FOLIO,FECHA,SUBMAQUILADOR,'SUB'
        FROM TMPSUBMAQUILA GROUP BY FOLIO,FECHA,SUBMAQUILADOR
        INSERT INTO PSALIDAS
        (PSALIDAKEY,FACTURA,FECHA,FRACCION,Descripcion,CANTIDAD,Unidad,Clave,Salidalink)
        SELECT ROW_NUMBER()OVER(ORDER BY CLAVE)+@PSKEY, T.FOLIO,T.FECHA,
               (SELECT fraccion FROM productos P WHERE P.CVE_PRODUCTO = T.CLAVE),
               DESCRIPCION,CANTIDAD,UNIDAD,CLAVE,
               (SELECT SALIDAKEY FROM SALIDAS WHERE SALIDAS.Documento = T.FOLIO)
        FROM TMPSUBMAQUILA T
    END

Divergencias confirmadas:

| Aspecto | Fixture previa | SP real LIVE |
|---|---|---|
| Columna de tipo en SALIDAS | Tipo_operacion CHAR(25) | TipoOperacion INT |
| Submaquilador | Transfiere CHAR(20) | Cve_cliente CHAR(15) |
| PSALIDAS | partida = LINEA | FACTURA / FECHA, sin partida |

Consecuencia critica: el SP LIVE escribe 'SUBMAQUILA' en SALIDAS.TipoOperacion,
que en CALE_IMMEX es INT (verificado: max_length = 4). La insercion falla por
conversion de tipo. La fixture idealizada hacia pasar la suite; NO era fiel a LIVE.

Clasificacion:

    SUBMAQUILA_LEGACY_SP = SP_EXISTING_UNSAFE
    SUBMAQUILA_TECHNICAL_IMPLEMENTATION = COMPLETE (wrapper, staging, parser, UI)
    SUBMAQUILA_IMPLEMENTATION_COMPLETE = NO
    SUBMAQUILA_LEGACY_CONTRACT_BLOCKED = YES
    SUBMAQUILA_LIVE_ACTIVATION_PENDING = YES

## 3. SUBMAQUILA_BLOCKER

    dbo.CARGA_SUBMAQUILA LIVE diverge del schema/contrato esperado: escribe
    'SUBMAQUILA' en SALIDAS.TipoOperacion INT y no es ejecutable tal cual.

Evidencia requerida para desbloquear (NO asumir ninguna):

    A) confirmacion del responsable de BD de que el SP debe corregirse; o
    B) identificacion de otro SP realmente utilizado por el WebForms/legacy; o
    C) evidencia de que otra base/version contiene el SP operativo correcto.

Prohibido: crear APP24_C_SUBMAQUILA_V2 / CARGA_SUBMAQUILA_CORREGIDO / nuevo SP de
negocio / SQL inline Java para SALIDAS/PSALIDAS, ni reinterpretar la intencion del
SP. SP-FIRST obliga a distinguir business intent de actual executable contract.

## 4. Forensics del punto de entrada (READ-ONLY 2026-10-05)

    sys.sql_expression_dependencies (referenced_entity_name='CARGA_SUBMAQUILA')
        -> sin modulos que lo referencien
    sys.sql_modules.definition LIKE '%CARGA_SUBMAQUILA%'
        -> solo el propio dbo.CARGA_SUBMAQUILA
    sys.triggers (CALE_IMMEX)                 -> 0
    msdb.dbo.sysjobsteps LIKE '%SUBMAQUILA%'  -> 0
    sys.objects LIKE '%SUBMAQUILA%' en CALE_PVU_FS, DATA_STAGE_CALE,
        StoredProceduresADW_DEV, PROCESADORA_DE_ALIMENTOS_CALE_JOVYADW_DEV,
        JovyDataADW_DEV                        -> 0 (solo CALE_IMMEX lo contiene)

    SUBMAQUILA_RUNTIME_ENTRY_POINT = UNKNOWN
    ALTERNATIVE_SP_FOUND = NO

El unico candidato es dbo.CARGA_SUBMAQUILA, pero ningun modulo SQL/job/trigger lo
invoca: el llamador seria un cliente externo (WebForms) no evidenciable desde la
metadata disponible. Se mantiene el blocker.

## 5. Fidelidad de fixtures (Submaquila)

    CARGA_SUBMAQUILA.legacy.sql            = copia textual fiel de LIVE
                                             (SOURCE=LIVE OBJECT_DEFINITION,
                                              AUDITED_AT=2026-10-05,
                                              LEGACY_EXECUTION_SAFE=NO)
    01-submaquila-live-schema.sql          = schema fiel a metadata LIVE
    SubmaquilaLiveContractTest             = prueba reproducible del bloqueo
    CARGA_SUBMAQUILA.expected-contract.sql = NOT_LIVE_REPLICA (contrato idealizado
                                             que necesitan los tests de aislamiento)
    01-submaquila-confirm-fixture.sql      = schema idealizado (referencia, NO LIVE)

Los tests de aislamiento (SubmaquilaLegacyStageConcurrencyTest) validan locks,
timeout, rollback y MAX()+1 contra el contrato esperado; NO demuestran que el flujo
business LIVE funcione.
