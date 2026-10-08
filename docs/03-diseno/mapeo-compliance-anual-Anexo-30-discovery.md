# Ajuste anual y Anexo 30 - discovery SP-FIRST reforzado (LEGACY-064..073)

## Alcance y controles

Reauditoria documental read-only que incorpora la evidencia SQL externa del dump del proyecto, separada de la cobertura versionada y de la revalidacion LIVE actual. No crea endpoint, permiso, UI, stored procedure ni migracion. La meta sigue siendo encontrar la siguiente capacidad funcional implementable, no producir documentacion por si misma.
- LIVE writes = 0.
- LIVE mutable executions = 0.
- SQL de negocio inline en Java = 0.
- Nuevo query SP = 0.
- Nuevo business SP = 0.
- LIVE reads = 0 (sin credencial autorizada).

## Distincion de evidencia

Toda conclusion sobre fuentes se separa en capas.

- REPO_VERSIONED_EVIDENCE: artefactos versionados en Git.
- PROJECT_SQL_DUMP_EVIDENCE: dump SQL fuera del repo.
- PRIOR_LIVE_EVIDENCE: rondas previas de auditoria LIVE.
- CURRENT_LIVE_REVALIDATION: credencial LIVE autorizada no disponible.
Estados de evidencia validos:
- NOT_FOUND: no aparece en cobertura disponible sin afirmar PROVEN_NOT_TO_EXIST.
- NOT_REVALIDATED: no se confirmo en sesion actual.
- NOT_CONFIRMED: estado por defecto cuando no se demostro aun.
- AVAILABLE: presente en la capa correspondiente.
- UNAVAILABLE: no disponible por falta de credencial, version o auditoria.

CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
REPO_VERSIONED_EVIDENCE = AVAILABLE
PROJECT_SQL_DUMP_EVIDENCE = AVAILABLE (fuera del repo)
PRIOR_LIVE_EVIDENCE = AVAILABLE (referenciado en auditorias previas)

## Bloque Ajuste anual (LEGACY-064..068)

### Evidencia UI (auditoria E2E previa)

La auditoria E2E enumero pantallas y campos observados del bloque:

- Pantallas de carga (LEGACY-064): seis controles de archivo para ventas, CTM, inventario final, inventario inicial.
- Pantallas de informes (LEGACY-065..068): principal, almacen, ventas, 4.3.16 I y 4.3.16 II.
- Campos observados: planta, factura, parte cliente, parte compania, cantidad, UMC, precio, cliente, periodo.

Las rutas ASPX exactas, filtros y orden no fueron conservados por la auditoria previa; los nombres de pantallas y campos son evidencia UI valida.

### Inventario SQL versionado (repo)

docs/03-diseno/procedimientos-almacenados.md no incluye ningun CARGA_AJUSTE, SP_AJUSTE, PR_INFORME_AJUSTE, V_AJUSTE, V_AJUSTEA2012, V_AJUSTE2012, ni tablas INVENTARIOINICIAL, INVENTARIOFINAL, VentasCTM o Almacen_VentasCTM en la version actual. La cobertura de infra/sql/procedures/queries se limita a catalogos, pedimentos, facturacion y reportes read-only versionados (Vencimientos, F4, Compulsa, Rectificaciones, Analisis de descargas, Operaciones bloqueadas).

Las tablas ValidacionInventarioInicial y ErroresValidacionInventarioInicial solo aparecen en infra/sql/01-seed-cale-immex-test.sql (DELETE en lineas 83-84) como parte del arnes de pruebas; ningun wrapper APP24 ni SP de negocio las consume.

### Evidencia SQL del dump del proyecto (fuera del repo)

Segun la auditoria SQL-FIRST externa (referenciada en prompts previos), el dump del proyecto conserva dbo.V_AJUSTE y dbo.V_AJUSTEA2012 como views read-only. Esa evidencia esta fuera del repo actual y no es auditable localmente. Se mantiene el estado AVAILABLE para PROJECT_SQL_DUMP_EVIDENCE sin presentar como LIVE.

### LEGACY-064 - Carga ajuste anual

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin layout, stage, SP, view ni permiso versionados para carga de ajuste anual |
| PROJECT_SQL_DUMP_EVIDENCE | NOT_FOUND_IN_DUMP_SCOPE: ningun SP CARGA_AJUSTE o equivalente aparece en el alcance auditado |
| PRIOR_LIVE_EVIDENCE | NOT_FOUND |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| STAGE_CONTRACT | NOT_CONFIRMED |
| SP_REUSABLE | NOT_CONFIRMED |
| MUTABLE_PRECONDITION_REQUIRED | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | FILE_LAYOUT/STAGE_CONTRACT/TARGET_CONTRACT/LEGACY_PIPELINE/VALIDATION_CONTRACT/TRANSACTION_BOUNDARY/RETRY_BEHAVORY todos faltan |

LEGACY_064 = BLOCKED_CONTRACT

### LEGACY-065 - Informe principal

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin view/SP read-only de informe principal en la version actual |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: dbo.V_AJUSTE observada como view read-only (fuera del repo) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE: definicion referenciada de la auditoria SQL-FIRST externa; sin OBJECT_DEFINITION local |
| PRIOR_LIVE_ROW_COUNT | NOT_CONFIRMED (conteos previos no atribuibles a V_AJUSTE sin revalidacion LIVE) |
| PRIOR_LIVE_COLUMN_COUNT | NOT_CONFIRMED (idem) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SOURCE_DEFINITION_AVAILABLE | YES (referenciada fuera del repo) |
| SOURCE_TO_SCREEN_MAPPING | NOT_CONFIRMED |
| MAPPING_065_TO_V_AJUSTE | NOT_CONFIRMED |
| IMPLEMENTABLE_NOW | NO (mapping UI-a-columnas no demostrable sin OBJECT_DEFINITION local) |
| BLOCKER | SOURCE_TO_SCREEN_MAPPING no confirmado |

LEGACY_065 = BLOCKED_SOURCE_TO_SCREEN_MAPPING

### LEGACY-066 - Informe almacen

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin view/SP read-only de almacen para ajuste anual |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: V_AJUSTE y V_AJUSTEA2012 observadas en el dump (fuera del repo); la definicion SQL disponible no presenta una referencia ALMACEN demostrada |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (fragmentado; no se asigna unico ID) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SOURCE_DEFINITION_AVAILABLE | YES (fuera del repo) |
| LEGACY_066_SOURCE_MAPPING | NOT_CONFIRMED |
| SCREEN_SCOPE_NOT_FOUND | No se demuestra que la UI observada informe almacen corresponda a alguna view del dump |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | SOURCE_TO_SCREEN_MAPPING no confirmado |

LEGACY_066 = BLOCKED_SOURCE_TO_SCREEN_MAPPING

### LEGACY-067 - Informe ventas

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin view/SP read-only de ventas para ajuste anual |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: V_AJUSTE/V_AJUSTEA2012 observadas en el dump (fuera del repo) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (fragmentado) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SOURCE_DEFINITION_AVAILABLE | YES (fuera del repo) |
| SOURCE_TO_SCREEN_MAPPING | NOT_CONFIRMED (mapping exclusivo a la UI informe ventas no demostrable) |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | SOURCE_TO_SCREEN_MAPPING no confirmado |

LEGACY_067 = BLOCKED_SOURCE_TO_SCREEN_MAPPING

### LEGACY-068 - Secciones 4.3.16 I / II

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin V_AJUSTEA2012, PR_INFORME_AJUSTE_4316 o SP read-only equivalente |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: V_AJUSTEA2012 observada como view read-only (fuera del repo) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (fragmentado) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SOURCE_DEFINITION_AVAILABLE | YES (fuera del repo) |
| V_AJUSTEA2012_TEMPORAL_SEMANTICS | NOT_CONFIRMED (el sufijo A2012 no prueba por si mismo variante historica ni contrato vigente) |
| SOURCE_TO_SCREEN_MAPPING | NOT_CONFIRMED (mapping a secciones 4.3.16 I y II no demostrable) |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | SOURCE_TO_SCREEN_MAPPING no confirmado; semantica temporal de V_AJUSTEA2012 no determinada |

LEGACY_068 = BLOCKED_SOURCE_TO_SCREEN_MAPPING

### Gate por capacidad (065..068)

| Requisito | LEGACY-065 | LEGACY-066 | LEGACY-067 | LEGACY-068 |
|---|---|---|---|---|
| SCREEN_SCOPE_CONFIRMED | PARTIAL | PARTIAL | PARTIAL | PARTIAL |
| SOURCE_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| SOURCE_GRAIN_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| VISIBLE_FIELDS_MAPPED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| FILTER_CONTRACT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| READ_ONLY_PATH_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| MUTABLE_PRECONDITION_REQUIRED = NO | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN |
| Implementable ahora | NO | NO | NO | NO |

Los cuatro IDs quedan bloqueados por SOURCE_TO_SCREEN_MAPPING no confirmado. La revalidacion LIVE sigue pendiente para aceptacion final, pero la definicion SQL existe fuera del repo; el blocker principal es la ausencia de mapping UI-a-columnas demostrable, no la falta absoluta de fuente. ANNUAL_ADJUSTMENT_DISCOVERY_CORRECTED = YES; ningun LEGACY queda IMPLEMENTABLE_NOW = YES.

## Bloque Anexo 30 (LEGACY-069..073)

### Evidencia UI (auditoria E2E previa)

La auditoria E2E enumero pantallas y campos:

- Informe de descargos Anexo 30 (LEGACY-069): destino aduanero, clave, anio, sustituio; hoja de trabajo; errores; operaciones faltantes; TXT; Excel.
- Parametros (LEGACY-070): destino aduanero, clave, anio, sustituio.
- Worksheet y errores (LEGACY-071): hoja de trabajo, errores, faltantes, validaciones, comparativas.
- TXT/Excel (LEGACY-072): formatos regulatorios (TXT) y Excel de consulta.
- Revision Anexo 30 (LEGACY-073): entradas, inventario inicial, descargas, saldos, comparativa, vencimientos; campos documento, operacion, pedimento, fechas, fraccion, valores, IVA, partida, ESAF.

ANEXO30_SCREEN_PRESENCE = CONFIRMED_AT_AUDIT_LEVEL (auditoria E2E).

### Inventario SQL versionado (repo)

SPs versionados relevantes al bloque:

- dbo.A31_SALDOS: inserta en A31_DESCARGAS, actualiza A31_ENTRADAS.SALDO, inserta en A31_TRAZO. Mutacion clasificada preliminarmente como SP_EXISTING_MUTABLE_REQUIRES_CONFIRMATION.
- dbo.DESCARGAS_A31: trunca A31_TRAZO y A31_DESCARGAS, actualiza A31_ENTRADAS SET SALDO = VALORCOMERCIAL, ejecuta A31_SALDOS. Mutacion clasificada preliminarmente como SP_EXISTING_UNSAFE.
- dbo.DESCAGARA31 / dbo.DESCARGASCTM / dbo.DESCARGASCTMF / dbo.HOJATRABAJOCTMDESPA31 / dbo.HOJATRABAJODESCARGOSA31: familia A31/CTM mutable.
- dbo.HOJATRABAJOCTMDESPA31: ejecuta PROC_DESCARGASF4CTMA.
- dbo.HOJATRABAJODESCARGOSA31: ejecuta PROC_HISTORIADESCARGASALIDA.
- dbo.INSERTAFALTANTESA31: mutable; cross-DB en DATA_STAGE_CALE.
- dbo.SP_GENERA_TXT_COMPLETO: NO es un exportador read-only. UPDATE sobre tablas operativas; construye BCP; utiliza xp_cmdshell; genera archivos externos. Clasificacion preliminar: SP_EXISTING_UNSAFE / EXTERNAL_SIDE_EFFECT. No usarlo desde API web; no ejecutarlo LIVE.
- dbo.SP_G6 / dbo.SP_G6_F4: familia G6/Anexo 30 mutable.

Tablas A31 registradas en el inventario (no se localizan views A31 read-only equivalentes a V_AJUSTE o V_AJUSTEA2012 en el repo):

A31_ENTRADAS, A31_DESCARGAS, A31_DESCARGASF, A31_TRAZO, A31_HISTORIADESCARGAS, A31_COMPARATIVADESCARGA, A31_PEDIMENTOSS, A31_DESC, DIFERENCIASA31, DIFERENCIASA31_DETALLE, TEMPORALIDADES_A31, COMPARATIVA31, COMPARATIVA31_DETALLE, DESCARGA31, DESCARGA31_DETALLE, SalidasA31Key.

Estas tablas NO aparecen con CREATE VIEW ni con wrappers APP24 en la version actual. Las definiciones A31 (si existen) forman parte de CALE_IMMEX y no son artefactos versionados.

### LEGACY-069 - Generacion Anexo 30

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: familia A31 mutables documentada (A31_SALDOS, DESCARGAS_A31, HOJATRABAJO*, SP_GENERA_TXT_COMPLETO) |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: SPs mutables A31 (auditados por SP-FIRST en otros turnos) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| MUTABLE_PRECONDITION_REQUIRED | YES (DESCARGAS_A31 trunca, A31_SALDOS escribe, SP_GENERA_TXT_COMPLETO usa xp_cmdshell) |
| IMPLEMENTABLE_NOW | NO (generacion mutable bloqueada por diseno) |
| BLOCKER | MUTABLE; no se ejecuta desde HTTP. Si las snapshots de A31_ENTRADAS, A31_DESCARGAS, A31_TRAZO, A31_COMPARATIVADESCARGA y DIFERENCIASA31 ya estan generadas, LEGACY-073 podria implementarse read-only |

LEGACY_069 = BLOCKED_MUTABLE_BY_DESIGN

### LEGACY-070 - Parametros del informe

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: SPs A31 con parametros documentados |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (anio, periodo, clave, sustituion, destino aduanero como filtros del UI Anexo 30) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| ANIO_PARAMETER | CONFIRMED (SP_G6 @ANIO VARCHAR(4); HOJATRABAJODESCARGOSA31 @ANIO; HOJATRABAJOCTMDESPA31 @ANIO) |
| PERIODO_PARAMETER | CONFIRMED (SP_G6 @PERIODO VARCHAR(10); HOJATRABAJODESCARGOSA31 @PERIODO; HOJATRABAJOCTMDESPA31 @PERIODO) |
| CLAVE_PARAMETER | CONFIRMED_IN_HOJATRABAJODESCARGOSA31 (SP_G6 @clave_destino; HOJATRABAJODESCARGOSA31 @CLAVE) |
| SUSTITUYE_PARAMETER | CONFIRMED (SP_G6 @SUSTITUYE VARCHAR(60)) |
| DESTINO_PARAMETER | CONFIRMED_IN_SP_G6 (@clave_destino no es sinonimo univoco de destino aduanero UI sin revalidacion LIVE) |
| UI_TO_SQL_PARAMETER_MAPPING | PARTIAL_CONFIRMED (anio, periodo, clave, sustituion; destino aduanero pendiente de mapping 1:1) |
| MUTABLE_PRECONDITION_REQUIRED | YES (los SPs A31 mutan al ejecutar la generacion) |
| IMPLEMENTABLE_NOW | NO (los parametros son entrada de SPs mutables) |
| BLOCKER | Parametros de entrada de mutables; no se exponen como endpoint |

LEGACY_070 = BLOCKED_BY_MUTABLE_GENERATORS

### LEGACY-071 - Worksheet / errores

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: HOJATRABAJO* e INSERTAFALTANTESA31 trabajan con tablas de hoja de trabajo; cross-DB en DATA_STAGE_CALE |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (hojas de trabajo persistidas) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| WORKTABLE_LIFECYCLE_NOT_CONFIRMED | sin revalidacion LIVE no se puede demostrar que HOJATRABAJO* no sea global scratch |
| MUTABLE_PRECONDITION_REQUIRED | YES |
| IMPLEMENTABLE_NOW | NO (sin lifecycle read-only demostrado) |
| BLOCKER | Posible scratch o worktable multiusuario sin lifecycle claro |

LEGACY_071 = BLOCKED_WORKTABLE_LIFECYCLE

### LEGACY-072 - TXT / Excel

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: SP_GENERA_TXT_COMPLETO (UPDATE + BCP + xp_cmdshell) |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE |
| PRIOR_LIVE_EVIDENCE | AVAILABLE |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| SP_FIRST_CLASSIFICATION | SP_EXISTING_UNSAFE / EXTERNAL_SIDE_EFFECT (xp_cmdshell) |
| MUTABLE_PRECONDITION_REQUIRED | YES |
| IMPLEMENTABLE_NOW | NO (no se reutiliza desde HTTP; el Excel de consulta puede regenerarse con ExportadorXlsxReportes solo si el dataset read-only esta confirmado) |
| BLOCKER | Sin dataset read-only confirmado para XLSX moderno; layout TXT regulatorio no inventable |

LEGACY_072 = BLOCKED_NO_READONLY_DATASET

### LEGACY-073 - Revision Anexo 30

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: tablas snapshot A31_ENTRADAS, A31_DESCARGAS, A31_TRAZO, A31_COMPARATIVADESCARGA, DIFERENCIASA31; ningun SP read-only ni view equivalente versionado en infra/sql |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: CREATE TABLE dbo.A31_ENTRADAS (Entradaskey bigint IDENTITY PK + columnas mostradas en evidencia forense) + tablas snapshot restantes en dump externo al repo |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (esquema documentado; sin OBJECT_DEFINITION local ni conteos LIVE current rows) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| ENTRADAS_GRAIN_CONFIRMED | YES (Entradaskey bigint IDENTITY PK; pedimentoarmado + fecha + fraccion definen el pedimento y su fraccion; saldo se reasigna por row en cada corrida de DESCARGAS_A31) |
| READ_SUBCAPABILITY_ENTRADAS | IMPLEMENTED_READ_ONLY |
| READ_SUBCAPABILITY_FRACCIONES | IMPLEMENTED_READ_ONLY |
| READ_SUBCAPABILITY_DESCARGAS | IMPLEMENTED_READ_ONLY_LAST_PERSISTED_SNAPSHOT |
| READ_SUBCAPABILITY_TRAZO | UNKNOWN (idem A31_DESCARGAS; truncada y reconstruida por corrida) |
| READ_SUBCAPABILITY_COMPARATIVA | PENDING_SEPARATE_FORENSIC |
| SNAPSHOT_LIFECYCLE | DEMOSTRABLE_DESDE_WRITERS: A31_ENTRADAS no se trunca (writer INSERTAFALTANTESA31 actualiza SALDO y FECHA por fila), A31_DESCARGAS/A31_TRAZO truncadas y reconstruidas por DESCARGAS_A31, A31_COMPARATIVADESCARGA truncada y reconstruida por COMPARADESCARGAA31, DIFERENCIASA31 truncada y reconstruida por COMPARATIVADESCARGA31 |
| TECHNICAL_FIELD_MAPPING | SUFFICIENT_FOR_PARTIAL_IMPLEMENTATION: A31_ENTRADAS expone columnas fisicas suficientes para construir la subcapacidad entradas (Pedimentoarmado, PEDIMENTOORIGINAL, Fecha, FECHAORIGINAL, Fracccion, Clavepedimento, Valocomercial, IVAFP21, IVAFP22, SALDO, OPERACION, PARTIDA, ESAF, Descarga, Tipooperacion). |
| TECHNICAL_FILTER_IMPLEMENTED | YES (filtro unico LIKE sobre pedimentoarmado, pedimentooriginal, fraccion, clavepedimento, esaf y operacion; validado por SQL IT con fixtures sinteticos) |
| PARITY_FILTER_CONTRACT | NOT_CONFIRMED: el filtro moderno esta implementado pero la UI legacy no fue auditada con contrato explicito de filtros; la segunda auditoria E2E comparativa decidira si coincide, debe ajustarse o es una mejora aceptada |
| NO_MUTABLE_EXECUTION_REQUIRED | YES (la consulta no llama a DESCARGAS_A31, A31_SALDOS, COMPARADESCARGAA31, COMPARATIVADESCARGA31, SP_G6 ni ningun generador) |
| READ_ONLY_QUERY_CONFIRMED | YES (SELECT con OFFSET/FETCH sobre dbo.A31_ENTRADAS + @Total; contrato del SP aplicado al SQL IT) |
| SOURCE_SCHEMA_CONFIRMED | YES (esquema verificado contra el dump del proyecto; replicado en el fixture del SQL IT) |
| SNAPSHOT_SEMANTICS_DOCUMENTED | YES (A31_ENTRADAS refleja las entradas acumuladas con SALDO del ultimo calculo DESCARGAS_A31; las demas subcapacidades dependen de la corrida mas reciente del generador y no se demuestran como snapshot estable sin CURRENT_LIVE_REVALIDATION) |
| QUERY_MUTABLE_PRECONDITION_REQUIRED | NO (la consulta es SELECT puro sobre dbo.A31_ENTRADAS; no llama a DESCARGAS_A31, A31_SALDOS, COMPARADESCARGAA31, COMPARATIVADESCARGA31, SP_G6, HOJATRABAJO* ni ningun generador) |
| DATA_FRESHNESS_MUTABLE_DEPENDENCY | YES (el contenido, en especial SALDO y los timestamps Fecha/FECHAORIGINAL, es producido/actualizado por procesos A31 mutables DESCARGAS_A31, A31_SALDOS e INSERTAFALTANTESA31 ejecutados fuera de la aplicacion; el endpoint solo lee lo que esos procesos ya dejaron persistido) |
| TECHNICAL_IMPLEMENTATION_READY | YES (cumple SOURCE_SCHEMA_CONFIRMED, SOURCE_GRAIN_CONFIRMED, TECHNICAL_FIELD_MAPPING, TECHNICAL_FILTER_IMPLEMENTED, READ_ONLY_QUERY_CONFIRMED, NO_MUTABLE_EXECUTION_REQUIRED, SNAPSHOT_SEMANTICS_DOCUMENTED para la subcapacidad entradas; UI_FIELD_MAPPING_CONFIRMED ya no se usa porque fue renombrado) |
| PARITY_FIELD_MAPPING | PARTIAL_CONFIRMED: la auditoria legacy conserva unicamente etiquetas funcionales (documento, operacion, pedimento, fechas, fraccion, valores, IVA, saldo, partida, ESAF). A31_ENTRADAS expone columnas fisicas homonimas pero la asignacion etiqueta-a-columna no esta auditada formalmente (ej. documento vs pedimento vs operacion). Sera confirmada en la segunda auditoria E2E comparativa. |
| PARITY_ACCEPTANCE_PENDING | YES |
| SALDO_SEMANTICS | LAST_PERSISTED_VALUE_FROM_MUTABLE_A31_PROCESS (no se calcula en linea; se lee tal como lo dejo DESCARGAS_A31 / A31_SALDOS / INSERTAFALTANTESA31) |
| CURRENT_BALANCE | NOT_GUARANTEED: el valor refleja el estado persistido por procesos A31 no ejecutados por la aplicacion nueva. No equivale a saldo fiscal recalculado. |
| SNAPSHOT_RUN_TIMESTAMP | NOT_AVAILABLE (sin columna de timestamp / run-id explicito en A31_ENTRADAS; los writers no registranlo) |
| STALENESS_DETECTABLE | NOT_CONFIRMED: sin CURRENT_LIVE_REVALIDATION no se demuestra cuando quedo escrito el ultimo SALDO por entrada |
| UI_INTERPRETATION | READ-ONLY_REVISION: la UI debe interpretarse como revision del estado persistido, no como generacion ni como calculo actual del saldo A31 |
| API_FIELDS | 16 (ENTRADA_KEY, DESCARGA, TIPO_OPERACION, PEDIMENTO, PEDIMENTO_ORIGINAL, FECHA, FECHA_ORIGINAL, CLAVE_PEDIMENTO, FRACCION, VALOR_COMERCIAL, IVA_FP21, IVA_FP22, SALDO, OPERACION, PARTIDA, ESAF) |
| UI_VISIBLE_COLUMNS | 13 (excluye ENTRADA_KEY, DESCARGA, TIPO_OPERACION; no renderizados porque no fueron observados como etiquetas en la auditoria legacy) |
| LIVE_ACCEPTANCE_READY | NO (CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE; el deploy del SP queda pendiente hasta revalidar LIVE) |
| IMPLEMENTABLE_NOW | YES (entradas, fracciones y descargas read-only; comparativa y otras subcapacidades pendientes) |
| BLOCKER | PARITY_ACCEPTANCE_PENDING y CURRENT_LIVE_REVALIDATION; no se ejecutan generadores mutables |

LEGACY_073 = PARTIAL

### LEGACY-073 forensic focalizado y subcapacidad implementada

#### Esquema fisico A31 (proveniente del dump del proyecto)

> dbo.A31_ENTRADAS (Entradaskey BIGINT IDENTITY PK, Descarga, Tipooperacion, Pedimentoarmado, Fecha, Fracccion, Valocomercial, Clavepedimento, IVAFP21, IVAFP22, SALDO, PEDIMENTOORIGINAL, FECHAORIGINAL, ESAF, OPERACION, PARTIDA)

> dbo.A31_DESCARGASF (A31_FRACCIONKEY PK, TIPO, CLAVEPEDIMENTO, EJERCICIO, PERIODO, FRACCION, VALOR, AF, ARCHIVO)

> dbo.A31_DESCARGAS (A31_DESCARGAKEY PK, ENTRADALINK, FRACCION, VALORDESCARGADO, A31_FRACCIONLINK)

> dbo.A31_TRAZO (A31_TRAZOKEY PK, FRACCION, VALORCOMERCIAL, FALTO, A31_FRACCIONKEY, DESCARGO)

> dbo.A31_COMPARATIVADESCARGA (clavepedimento, ejercicio, periodo, fraccion, valor A31, valor A24, diferencia, iva21total, iva22total, valortotal, iva descargado A31, iva descargado A24)

> dbo.DIFERENCIASA31 (fraccionA31, valorA31, periodoA31, ejercicioA31, fraccionA24, valorA24, periodoA24, ejercicioA24, diferencias)

#### Joins inferidos (sin FK fisica declarada)

- A31_DESCARGAS.ENTRADALINK -> A31_ENTRADAS.Entradaskey (REFERENCE_CANDIDATE; FK_PHYSICAL = NO; CARDINALITY_ENFORCED = NO; EXPECTED_CHILD_TO_PARENT = N:1 segun la logica de aplicacion, pendiente de confirmacion por paridad audit).
- A31_DESCARGAS.A31_FRACCIONLINK -> A31_DESCARGASF.A31_FRACCIONKEY (REFERENCE_CANDIDATE; FK_PHYSICAL = NO; CARDINALITY_ENFORCED = NO; EXPECTED_CHILD_TO_PARENT = N:1).
- A31_TRAZO.A31_FRACCIONKEY -> A31_DESCARGASF.A31_FRACCIONKEY (REFERENCE_CANDIDATE; FK_PHYSICAL = NO; CARDINALITY_ENFORCED = NO; EXPECTED_CHILD_TO_PARENT = N:1).

No se declaran constraints FK en la fuente. La aplicacion NO debe inferir cardinalidad contractual ni joins multiplicativos hasta revalidacion LIVE / paridad audit. Los enlaces de A31_DESCARGAS se usan por la ruta read-only de descargas mediante LEFT JOIN, preservando enlaces huerfanos y nulos. FK_PHYSICAL = NO; CARDINALITY_ENFORCED = NO.

#### Lifecycle de snapshots (desde writers declarados)

- A31_ENTRADAS: NO se trunca. Filas acumuladas; SALDO reasignado por DESCARGAS_A31 (UPDATE A31_ENTRADAS SET SALDO = VALOCOMERCIAL). INSERTAFALTANTESA31 inserta/actualiza filas.
- A31_DESCARGAS / A31_TRAZO: truncadas y reconstruidas por DESCARGAS_A31 cada corrida. Representan el ultimo snapshot del generador.
- A31_COMPARATIVADESCARGA: truncada y reconstruida por COMPARADESCARGAA31.
- DIFERENCIASA31 / DIFERENCIASA31_DETALLE: truncadas y reconstruidas por COMPARATIVADESCARGA31 / COMPARATIVADESCARGA31_DETALLE.
- Sin columna run_id / timestamp explicito en A31_ENTRADAS. La identificacion de la corrida depende de los timestamps Fecha/FECHAORIGINAL por fila.

#### Candidate technical mapping UI -> physical columns

Este mapping es CANDIDATO. La auditoria legacy solo conserva etiquetas funcionales (documento, operacion, pedimento, fechas, fraccion, valores, IVA, saldo, partida, ESAF). La segunda auditoria E2E original vs nuevo decidira que label legacy corresponde a que columna fisica.

- documento / pedimento
  -> candidate: A31_ENTRADAS.Pedimentoarmado
  PARITY_MAPPING = NOT_YET_DISAMBIGUATED (documento podria equivaler a Pedimentoarmado o PEDIMENTOORIGINAL)
- pedimento original
  -> candidate: A31_ENTRADAS.PEDIMENTOORIGINAL
  PARITY_MAPPING = STRONG_CANDIDATE
- fecha
  -> candidate: A31_ENTRADAS.Fecha
  PARITY_MAPPING = STRONG_CANDIDATE
- fecha original
  -> candidate: A31_ENTRADAS.FECHAORIGINAL
  PARITY_MAPPING = STRONG_CANDIDATE
- operacion
  -> candidates: A31_ENTRADAS.OPERACION (bigint) / A31_ENTRADAS.Tipooperacion (varchar(2))
  PARITY_MAPPING = NOT_YET_DISAMBIGUATED
- fraccion
  -> candidate: A31_ENTRADAS.Fracccion
  PARITY_MAPPING = STRONG_CANDIDATE
- clave pedimento
  -> candidate: A31_ENTRADAS.Clavepedimento
  PARITY_MAPPING = STRONG_CANDIDATE
- valor comercial
  -> candidate: A31_ENTRADAS.Valocomercial
  PARITY_MAPPING = STRONG_CANDIDATE
- IVA
  -> candidates: A31_ENTRADAS.IVAFP21 / A31_ENTRADAS.IVAFP22
  PARITY_MAPPING = STRONG_CANDIDATE
- saldo
  -> candidate: A31_ENTRADAS.SALDO
  PARITY_MAPPING = STRONG_CANDIDATE (pero SALDO_SEMANTICS = LAST_PERSISTED_VALUE_FROM_MUTABLE_A31_PROCESS)
- partida
  -> candidate: A31_ENTRADAS.PARTIDA
  PARITY_MAPPING = STRONG_CANDIDATE
- ESAF
  -> candidate: A31_ENTRADAS.ESAF
  PARITY_MAPPING = STRONG_CANDIDATE
- descarga
  -> candidate: A31_ENTRADAS.Descarga
  PARITY_MAPPING = NOT_YET_DISAMBIGUATED

Conjunto: PARITY_FIELD_MAPPING = PARTIAL_CONFIRMED.

#### Decisiones de implementacion (entradas)

- SP tecnico versionado: dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR(@Filtro, @Pagina, @Tamano, @Total OUTPUT). Solo SELECT sobre dbo.A31_ENTRADAS. Paginacion validada, OFFSET/FETCH con orden determinista (Fecha DESC, Pedimentoarmado, Fracccion, Entradaskey).
- Permiso reusado: REPORTES_GENERAR (sin crear permisos nuevos).
- Endpoint: GET /api/v1/reportes/anexo30-revision-entradas con filtro opcional, paginacion 1..100.
- Frontend: nueva opcion en report-list.page.ts (Revision Anexo 30 - Entradas). API expone 16 campos (ENTRADA_KEY, DESCARGA, TIPO_OPERACION, PEDIMENTO, PEDIMENTO_ORIGINAL, FECHA, FECHA_ORIGINAL, CLAVE_PEDIMENTO, FRACCION, VALOR_COMERCIAL, IVA_FP21, IVA_FP22, SALDO, OPERACION, PARTIDA, ESAF); la tabla Angular renderiza 13 columnas visibles (excluye ENTRADA_KEY, DESCARGA, TIPO_OPERACION porque no fueron observados como etiquetas en la auditoria legacy). Sin XLSX (no demostrado en legacy para revision).
- Hexagonal: domain/port/adapter/application/query + api/dto + controller. Cero SQL de negocio inline en Java.
- LIVE_ACTIVATION_PENDING = YES: el SP no se despliega en LIVE en este commit; queda versionado para que el controlador lo aplique cuando se revalide el acceso a CALE_IMMEX.
- Entradas, fracciones y descargas cuentan con una ruta read-only implementada. Trazo, comparativa, vencimientos y cualquier mapeo legacy no confirmado permanecen pendientes. FRACCIONES_PARITY_MAPPING = NOT_CONFIRMED; descargas representa unicamente el ultimo snapshot persistido.

### Gate por capacidad (069..073)

| Requisito | LEGACY-069 | LEGACY-070 | LEGACY-071 | LEGACY-072 | LEGACY-073 |
|---|---|---|---|---|---|
| SCREEN_CONTRACT_CONFIRMED | PARTIAL | PARTIAL | PARTIAL | PARTIAL | PARTIAL |
| SOURCE_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | YES_FOR_ENTRADAS_FRACCIONES_DESCARGAS_FROM_PROJECT_SQL_DUMP |
| SOURCE_GRAIN_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | YES_FOR_ENTRADAS_FRACCIONES_DESCARGAS_PHYSICAL_GRAIN |
| VISIBLE_FIELDS_MAPPED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | PARTIAL_CONFIRMED |
| PARITY_FILTER_CONTRACT | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| READ_ONLY_PATH_CONFIRMED | NO (no SP read-only equivalente) | NO | NO | NO | YES_FOR_ENTRADAS_FRACCIONES_DESCARGAS |
| QUERY_MUTABLE_PRECONDITION_REQUIRED | YES | YES | YES | YES | NO |
| DATA_FRESHNESS_MUTABLE_DEPENDENCY | YES | YES | YES | YES | YES |
| NO_GENERATION_REQUIRED_FOR_READ | NO | NO_OR_UNKNOWN | NO | NO_FOR_TXT / UNKNOWN_FOR_READONLY_XLSX | YES_FOR_EXISTING_PERSISTED_ROWS |
| Implementable ahora | NO | NO | NO | NO | YES_FOR_ENTRADAS_FRACCIONES_DESCARGAS_PARTIAL |
### Conclusion del bloque

LEGACY-073 implementa como read-only las subcapacidades entradas, fracciones y descargas (SP + endpoint + frontend + SQL IT). Descargas lee unicamente el ultimo snapshot persistido y no ejecuta recalculo. Las capacidades 069-072 siguen bloqueadas por la naturaleza mutable de sus SPs. Las subcapacidades restantes de LEGACY-073 (trazo, comparativa, vencimientos y otros mapeos legacy aun no confirmados) permanecen pendientes; su ciclo de vida y paridad requieren evidencia adicional sin ejecutar generadores. Para revisarlas se requeriria:

- OBJECT_DEFINITION en vivo de las tablas o vistas A31 snapshot.
- Demostracion de que las tablas A31_ENTRADAS, A31_DESCARGAS, A31_TRAZO, A31_COMPARATIVADESCARGA y DIFERENCIASA31 son consultables sin regeneracion.
- Snapshot lifecycle claro (periodicidad, last run, semantica de staleness).
- Mapping UI-a-columnas para los campos observados (documento, operacion, pedimento, fechas, fraccion, valores, IVA, partida, ESAF).

## Estado global

ANNUAL_ADJUSTMENT_DISCOVERY_CORRECTED = YES
ANEXO30_DISCOVERY_COMPLETE = YES
READY_FOR_NEXT_IMPLEMENTATION = NO (capacidad recien implementada requiere CI verde y revision del controlador antes de continuar)
READY_FOR_NEXT_DISCOVERY = YES
NEW_QUERY_SP = 3 (APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR, APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR, APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR)
NEW_BUSINESS_SP = 0
INLINE_BUSINESS_SQL_JAVA = 0
LIVE reads = 0
LIVE writes = 0
LIVE mutable executions = 0
LIVE_ACTIVATION_PENDING = YES

LEGACY_064 = BLOCKED_CONTRACT
LEGACY_065 = BLOCKED_SOURCE_TO_SCREEN_MAPPING
LEGACY_066 = BLOCKED_SOURCE_TO_SCREEN_MAPPING
LEGACY_067 = BLOCKED_SOURCE_TO_SCREEN_MAPPING
LEGACY_068 = BLOCKED_SOURCE_TO_SCREEN_MAPPING
LEGACY_069 = BLOCKED_MUTABLE_BY_DESIGN
LEGACY_070 = BLOCKED_BY_MUTABLE_GENERATORS
LEGACY_071 = BLOCKED_WORKTABLE_LIFECYCLE
LEGACY_072 = BLOCKED_NO_READONLY_DATASET
LEGACY_073 = PARTIAL


### LEGACY-073 — extensión read-only: fracciones y descargas

PROJECT_SQL_DUMP_DEFINITION = CONFIRMED
CURRENT_LIVE_DEFINITION_MATCH = NOT_REVALIDATED

A31_DESCARGASF_SCHEMA = CONFIRMED
A31_DESCARGASF_WRITER_IN_PROJECT_DUMP = NOT_FOUND
A31_DESCARGASF_TRUNCATED_BY = NONE_FOUND_IN_PROJECT_DUMP
A31_DESCARGASF_DELETED_BY = NONE_FOUND_IN_PROJECT_DUMP
A31_DESCARGASF_READERS = CONFIRMED
A31_DESCARGASF_LIFECYCLE = EXTERNALLY_POPULATED_OR_WRITER_NOT_PRESENT_IN_DUMP

A31_DESCARGAS_SCHEMA = CONFIRMED
A31_DESCARGAS_LIFECYCLE = LAST_PERSISTED_GENERATOR_SNAPSHOT
QUERY_MUTABLE_PRECONDITION_REQUIRED = NO
DATA_FRESHNESS_MUTABLE_DEPENDENCY = YES_FOR_DESCARGAS
CURRENT_DATA_GUARANTEED = NO
SNAPSHOT_RUN_ID = NOT_AVAILABLE
SNAPSHOT_TIMESTAMP = NOT_AVAILABLE
STALENESS_DETECTABLE = NO

ENTRADALINK_SEMANTICS = CONFIRMED_BY_LEGACY_WRITER
A31_FRACCIONLINK_SEMANTICS = CONFIRMED_BY_LEGACY_WRITER_AND_READER
FK_PHYSICAL = NO
CARDINALITY_ENFORCED = NO

READ_SUBCAPABILITY_ENTRADAS = IMPLEMENTED_READ_ONLY
READ_SUBCAPABILITY_FRACCIONES = IMPLEMENTED_READ_ONLY
READ_SUBCAPABILITY_DESCARGAS = IMPLEMENTED_READ_ONLY_LAST_PERSISTED_SNAPSHOT
READ_SUBCAPABILITY_COMPARATIVA = PENDING_SEPARATE_FORENSIC
FRACCIONES_PARITY_MAPPING = NOT_CONFIRMED
PARITY_ACCEPTANCE_PENDING = YES

Las fracciones son un rediseño moderno read-only de registros fuente persistidos; no se afirma equivalencia con la pestaña legacy de archivos. Descargas conserva enlaces huérfanos mediante LEFT JOIN y no ejecuta ni recalcula generadores A31.
