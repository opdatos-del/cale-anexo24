# Mapeo de carga de actas (CARGAACTAS) - revalidacion READ-ONLY

Fuente: CALE_IMMEX LIVE 2026-10-05, OBJECT_DEFINITION + sys.sql_expression_dependencies.
Ninguna ejecucion mutable; solo metadata.

## 1. Objeto

    dbo.CARGAACTAS = SQL_STORED_PROCEDURE (create/modify 2026-09-14)
    clasificacion preliminar = BUSINESS_LOGIC_CONFIRMED / STAGE_CONTRACT_CONFIRMED /
                               SP_EXISTING_REUSABLE

## 2. Contrato observado (sin parametros)

    DECLARE ACTA CURSOR FAST_FORWARD FOR
        SELECT ACTA.FOLIO, ACTA.FECHA, ACTA.CLAVE, ACTA.LINEA, ACTA.CANTIDAD,
               ACTA.UMC, ACTA.DESCARGADIRIGIDA, ACTA.VALORCOMERCIAL
        FROM DBO.ACTA ORDER BY FOLIO

    DELETE FROM GENERADORES WHERE TABLA = 'ACTA'      -- side effect de negocio

    por cada fila del cursor:
      IF no existe SALIDAS con DOCUMENTO=@FOLIO y TIPO_OPERACION='DESPERDICIOS':
          INSERT INTO SALIDAS
          (SALIDAKEY, DOCUMENTO, FECHA, TIPO_OPERACION, CVE_PEDIMENTO, CVE_CLIENTE,
           ADUANA, AGENTE, PAIS, TC, BLOQUEADO)
          VALUES (MAX(SALIDAKEY)+1, @FOLIO, @FECHA, 'DESPERDICIOS','DESP','-',
                  '-','-','-',0,0)
      IF no existe PSALIDAS para (SALIDAS.DOCUMENTO=@FOLIO AND PSALIDAS.PARTIDA=@LINEA):
          descripcion/fraccion/unidad desde MATERIAL o PRODUCTOS por CLAVE
          INSERT INTO PSALIDAS
          (PSALIDAKEY, CLAVE, DESCRIPCION, CANTIDAD, FRACCION, UNIDAD, SALIDALINK,
           PARTIDA, DESCARGADIRIGIDA, FECHA, VAL_PESOS)
          VALUES (MAX(PSALIDAKEY)+1, ..., @SALIDAKEY, @LINEA, @DESCARGADIRIGIDA,
                  @FECHA, @VALORCOMERCIAL)

    al terminar:
      @DIRIGIDOKEY = MAX(DIRIGIDOKEY)+1
      INSERT INTO DIRIGIDO (DIRIGIDOKEY, DOCUMENTO, CLAVE, INCORPORADO, DESPERDICIO,
                            MERMA, SALIDAKEY, PSALIDAKEY)
      SELECT ROW_NUMBER() OVER(ORDER BY PSALIDAKEY)+@DIRIGIDOKEY, DESCARGADIRIGIDA,
             CLAVE, CANTIDAD, 0, 0, SALIDALINK, PSALIDAKEY
      FROM PSALIDAS
      WHERE DESCARGADIRIGIDA <> '' AND PSALIDAKEY NOT IN (SELECT PSALIDAKEY FROM DIRIGIDO)

## 3. Clasificacion de contrato

    parametros        = NINGUNO
    transaccion       = NINGUNA (sin BEGIN TRAN / TRY-CATCH / XACT_ABORT)
    resultsets        = NINGUNO de negocio (solo PRINT @SALIDAKEY de debug)
    NOLOCK/READUNCOMM = NINGUNO
    validaciones      = NINGUNA
    error stage       = NINGUNO (dbo.ERRORACTA NO EXISTE en LIVE)
    stage compartido  = NO (dbo.ACTA: unico writer = dbo.CARGAACTAS)
    claves            = MAX()+1 en SALIDAKEY, PSALIDAKEY, DIRIGIDOKEY
    idempotencia      = parcial (comprueba existencia por folio/partida, a diferencia
                        de CARGA_SUBMAQUILA que inserta a ciegas)
    side effect       = DELETE FROM GENERADORES WHERE TABLA='ACTA'

Columnas LIVE del stage dbo.ACTA (para el parser):

    actakey BIGINT IDENTITY PK
    Folio VARCHAR(50), fecha DATETIME, clave VARCHAR(50), linea INT,
    cantidad FLOAT, umc VARCHAR(50), descargadirigida VARCHAR(50),
    VALORCOMERCIAL NUMERIC(18,10)

## 4. Lock order plan

Orden global compartido (identico a APP24_C_PEDIMENTO_CONFIRMAR y al hardening de
submaquila):

    IMPORTACIONES -> PARTIDAS -> SALIDAS -> PSALIDAS -> DIRIGIDO

CARGAACTAS no toca IMPORTACIONES/PARTIDAS. Subset requerido, con el stage primero:

    ACTA (stage exclusivo) -> SALIDAS -> PSALIDAS -> DIRIGIDO -> GENERADORES

GENERADORES se incluye porque el legacy lo borra; se agrega al final para no romper
el orden relativo de las tablas compartidas.

## 5. Plan de implementacion (no ejecutado en este turno)

    - migration 18-acta-staging-confirmation-v1.sql:
        app24.CargaActa / CargaActaFila / ErrorCargaActa (errores del parser moderno,
        no validaciones legacy inventadas), APP24_C_ACTA_CARGA_CREAR,
        APP24_Q_ACTA_CARGA_OBTENER / _POR_HASH, actividades ACTA_CARGAR/ACTA_CONFIRMAR.
    - wrapper dbo.APP24_C_ACTA_CARGA_CONFIRMAR @CargaId:
        locks (orden de arriba) -> stage app24 -> INSERT dbo.ACTA -> EXEC dbo.CARGAACTAS
        -> DELETE dbo.ACTA -> estado CONFIRMADA -> bitacora; CATCH: rollback + evento
        ACTA_CARGA_CONFIRMACION_ERROR.
    - Java: CatalogImportType.ACTA + parser + controller/usecase/port/adapter (patron
      constancias).
    - Frontend: pestana "Actas" dentro de Operaciones (no llamarla catalogo).
    - IT ActaLegacyStageConcurrencyTest con fixture CARGAACTAS.legacy.sql textual.

    NEW_BUSINESS_SP = 0

## 6. Regla de fidelidad de fixture (leccion de submaquila)

    CARGAACTAS.legacy.sql debe ser copia textual del OBJECT_DEFINITION LIVE
    (SOURCE=LIVE OBJECT_DEFINITION, AUDITED_AT, LEGACY_EXECUTION_SAFE=si procede).
    Prohibido escribir una version "equivalente" o idealizada.

Estado: branch feature/operations-acts-import-v1 creada desde origin/dev (2e4b8d8).
Implementacion pendiente de ejecucion.

## 7. Contrato A.1 - side effects y ambiguedades heredadas (2026-10-05)

    SP_EXISTING_REUSABLE = YES
    LEGACY_SIDE_EFFECTS_DOCUMENTED = YES

    ACTA_DIRIGIDO_SIDE_EFFECT = GLOBAL_PENDING_PSALIDAS_SWEEP

El cierre de dbo.CARGAACTAS NO esta limitado al folio ACTA procesado. Inserta en
dbo.DIRIGIDO TODA fila de dbo.PSALIDAS con DESCARGADIRIGIDA <> '' que aun no exista en
dbo.DIRIGIDO:

    INSERT INTO DIRIGIDO (...)
    SELECT ... FROM PSALIDAS
    WHERE DESCARGADIRIGIDA <> '' AND PSALIDAKEY NOT IN (SELECT PSALIDAKEY FROM DIRIGIDO)

Es un barrido global de PSALIDAS pendientes. Es comportamiento de negocio legacy: no se
corrige, no se limita en el wrapper y no se reimplementa. Debe registrarse en la
aceptacion operativa, porque confirmar un acta puede crear filas DIRIGIDO de PSALIDAS
ajenas al acta.

Riesgos heredados (fail closed, no se "arreglan" en el SP):

- SALIDAS scalar ambiguity: la salida de desperdicios se busca con un subquery escalar por
  DOCUMENTO + TIPO_OPERACION='DESPERDICIOS'. Si el folio tiene DOS salidas de desperdicios,
  SQL Server lanza "subquery returned more than 1 value"; el wrapper revierte y deja la
  carga en PREVISUALIZADA con evento de fallo registrado.
- PSALIDAS scalar ambiguity: la partida se busca con un subquery escalar por DOCUMENTO +
  PARTIDA. Si devuelve mas de una fila, ocurre el mismo fallo cerrado con rollback.

Ambos caminos se cubren en ActaLegacyStageConcurrencyTest
(folioConDosSalidasDesperdiciosRevierte, folioConDosPsalidasMismaPartidaRevierte). No se
clasifican como blocker: el camino normal es ejecutable en Testcontainers.

Preservacion de NULL: el wrapper materializa celdas modernas vacias como NULL legacy
(NULLIF(JSON_VALUE(...), '')) para que '' no se convierta en 0 / 1900-01-01 cuando la
columna LIVE es nullable.

Variables de lock tipadas segun metadata LIVE (nunca reutilizar un tipo mas estrecho):
@BloqueoActa BIGINT (actakey), @BloqueoSalida/@BloqueoPsalida NUMERIC(18,0),
@BloqueoDirigido BIGINT (dirigidokey), @BloqueoGenerador INT (consecutivo).

VALORCOMERCIAL: el stage LIVE es NUMERIC(18,10); el legacy declara
@VALORCOMERCIAL NUMERIC(18,4) y termina en PSALIDAS.Val_pesos NUMERIC(18,4).
LEGACY_VALORCOMERCIAL_EFFECTIVE_SCALE = 4 (reduccion de precision del legacy, no del parser).

## 8. Implementación integrada en dev

El plan de las secciones anteriores se implementó posteriormente y debe leerse
como antecedente de discovery, no como estado vigente. La superficie moderna es
/operaciones/actas y cubre dos trazabilidades de paridad sin duplicar flujo:

- LEGACY-025: operación y procesamiento de actas;
- LEGACY-060: interfaz segura de importación de actas.

El flujo implementado es archivo XLS/XLSX -> staging app24.CargaActa -> preview y
errores -> confirmación explícita con ACTAS_CONFIRMAR ->
dbo.APP24_C_ACTA_CARGA_CONFIRMAR -> dbo.CARGAACTAS dentro de una transacción.
El wrapper conserva las reglas y efectos legacy documentados, bloquea las tablas
compartidas en orden determinista, limpia el stage temporal y deja auditoría.

Evidencia de integración: ActaImportController,
ActaImportConfirmationController, ConfirmarCargaActaUseCase,
ConfirmacionCargaActaJdbcAdapter, migration 18-acta-staging-confirmation-v1.sql,
APP24_C_ACTA_CARGA_CONFIRMAR.sql, fixture textual CARGAACTAS.legacy.sql y tests
de parser, API, adapter, use case y concurrencia SQL.

LEGACY_025 = IMPLEMENTED_REDESIGNED
LEGACY_060 = IMPLEMENTED_REDESIGNED
NEW_BUSINESS_SP = 0
