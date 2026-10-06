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
| PRIOR_LIVE_EVIDENCE | AVAILABLE: 3,866 filas / 44 columnas (conteos historicos, no revalidados) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SCREEN_SCOPE_NOT_FOUND | No se demuestra mapping 1:1 entre la UI observada y V_AJUSTE sin OBJECT_DEFINITION en esta sesion |
| MAPPING_065_TO_V_AJUSTE | NOT_CONFIRMED |
| IMPLEMENTABLE_NOW | NO (sin revalidacion LIVE; sin mapping UI-a-columnas demostrable) |
| BLOCKER | CURRENT_LIVE_REVALIDATION no disponible; PROJECT_SQL_DUMP_EVIDENCE no auditable localmente |

LEGACY_065 = BLOCKED_NO_LIVE_REVALIDATION

### LEGACY-066 - Informe almacen

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin view/SP read-only de almacen para ajuste anual |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: V_AJUSTE y V_AJUSTEA2012 incluyen referencias a almacen (no se demuestra exclusivo sin lectura) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (fragmentado; no se asigna unico ID) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SCREEN_SCOPE_NOT_FOUND | No se demuestra que la UI observada informe almacen corresponda a alguna view del dump |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | CURRENT_LIVE_REVALIDATION no disponible; sin mapping UI-a-columnas demostrable |

LEGACY_066 = BLOCKED_NO_LIVE_REVALIDATION

### LEGACY-067 - Informe ventas

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin view/SP read-only de ventas para ajuste anual |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: V_AJUSTE/V_AJUSTEA2012 incluyen referencias a ventas, CTM y clientes (no exclusivo) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (fragmentado) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SCREEN_SCOPE_NOT_FOUND | No se demuestra mapping exclusivo a la UI informe ventas |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | CURRENT_LIVE_REVALIDATION no disponible; sin mapping exclusivo demostrable |

LEGACY_067 = BLOCKED_NO_LIVE_REVALIDATION

### LEGACY-068 - Secciones 4.3.16 I / II

| Capa | Estado |
|---|---|
| REPO_VERSIONED_EVIDENCE | AVAILABLE: sin V_AJUSTEA2012, PR_INFORME_AJUSTE_4316 o SP read-only equivalente |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE: V_AJUSTEA2012 observada como view read-only (sufijo A2012 indica variante historica) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (fragmentado) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| SCREEN_SCOPE_NOT_FOUND | No se demuestra mapping a secciones 4.3.16 I y II; el sufijo A2012 sugiere variante historica, no contrato vigente |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | Variante A2012 no es contrato vigente sin revalidacion LIVE |

LEGACY_068 = BLOCKED_NO_LIVE_REVALIDATION

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

Los cuatro IDs quedan bloqueados por la ausencia de CURRENT_LIVE_REVALIDATION. ANNUAL_ADJUSTMENT_DISCOVERY_CORRECTED = YES; ningun LEGACY queda IMPLEMENTABLE_NOW = YES.

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
| REPO_VERSIONED_EVIDENCE | AVAILABLE: HOJATRABAJODESCARGOSA31(@ANIO, @PERIODO, @CLAVE), HOJATRABAJOCTMDESPA31(@ANIO, @PERIODO), SP_G6(@ANIO, @PERIODO, @SUSTITUYE, @clave_destino) |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (anio, periodo, clave, sustituion, destino aduanero como filtros del UI Anexo 30) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| PARAMETERS_TO_QUERY_PARAMETERS_NOT_AVAILABLE | sin revalidacion LIVE no se demuestra que algun SP reciba explicitamente destino aduanero |
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
| REPO_VERSIONED_EVIDENCE | AVAILABLE: tablas snapshot A31_ENTRADAS, A31_DESCARGAS, A31_TRAZO, A31_COMPARATIVADESCARGA, DIFERENCIASA31, etc. (no view ni SP read-only especifico) |
| PROJECT_SQL_DUMP_EVIDENCE | AVAILABLE (no se audito LIVE; no se demuestra snapshot lifecycle) |
| PRIOR_LIVE_EVIDENCE | AVAILABLE (referencia historica; sin READ_ONLY_PATH_CONFIRMED) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| PERSISTED_SOURCE_CONFIRMED | NOT_CONFIRMED (sin revalidacion LIVE) |
| SOURCE_GRAIN_CONFIRMED | NOT_CONFIRMED |
| FILTERS_CONFIRMED | NOT_CONFIRMED (anio, periodo, clave, destino, sustituion, pero sin SP read-only) |
| STALE_DATA_SEMANTICS_KNOWN | NO (sin CURRENT_LIVE_REVALIDATION) |
| NO_GENERATION_REQUIRED_FOR_READ | NOT_CONFIRMED |
| REVIEW_SCREEN_CONTRACT_CONFIRMED | NOT_CONFIRMED (mapea a tablas A31, pero sin mapping demostrable) |
| MUTABLE_PRECONDITION_REQUIRED | NO_PRESUMED (snapshots A31, no confirmado sin regeneracion) |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | Sin revalidacion LIVE no se demuestra que las tablas A31 sean consultables sin regeneracion; sin OBJECT_DEFINITION no se confirman fuentes ni consumers |

LEGACY_073 = BLOCKED_NO_LIVE_REVALIDATION

### Gate por capacidad (069..073)

| Requisito | LEGACY-069 | LEGACY-070 | LEGACY-071 | LEGACY-072 | LEGACY-073 |
|---|---|---|---|---|---|
| SCREEN_CONTRACT_CONFIRMED | PARTIAL | PARTIAL | PARTIAL | PARTIAL | PARTIAL |
| SOURCE_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| SOURCE_GRAIN_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| VISIBLE_FIELDS_MAPPED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| FILTER_CONTRACT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED | NOT_CONFIRMED |
| READ_ONLY_PATH_CONFIRMED | NO (no SP read-only equivalente) | NO | NO | NO | NO |
| MUTABLE_PRECONDITION_REQUIRED | YES | YES | YES | YES | NO_PRESUMED |
| NO_GENERATION_REQUIRED_FOR_READ | NO (precondicion mutable) | YES (solo si 069 regenera) | YES (hojas de trabajo mutables) | YES (precondicion mutable) | NOT_CONFIRMED |
| Implementable ahora | NO | NO | NO | NO | NO |

### Conclusion del bloque

Ninguna capacidad de Anexo 30 pasa el gate completo en esta sesion. El motivo comun es CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE: sin revalidacion LIVE no se puede demostrar mapping UI-a-columnas, ni lifecycle de snapshots, ni READ_ONLY_PATH de los mutables A31. Cualquier implementacion read-only para LEGACY-073 requeriria:

- OBJECT_DEFINITION en vivo de las tablas o vistas A31 snapshot.
- Demostracion de que las tablas A31_ENTRADAS, A31_DESCARGAS, A31_TRAZO, A31_COMPARATIVADESCARGA y DIFERENCIASA31 son consultables sin regeneracion.
- Snapshot lifecycle claro (periodicidad, last run, semantica de staleness).
- Mapping UI-a-columnas para los campos observados (documento, operacion, pedimento, fechas, fraccion, valores, IVA, partida, ESAF).

## Estado global

ANNUAL_ADJUSTMENT_DISCOVERY_CORRECTED = YES
ANEXO30_DISCOVERY_COMPLETE = YES
READY_FOR_NEXT_IMPLEMENTATION = NO
NEW_QUERY_SP = 0
NEW_BUSINESS_SP = 0
INLINE_BUSINESS_SQL_JAVA = 0
LIVE reads = 0
LIVE writes = 0
LIVE mutable executions = 0

LEGACY_064 = BLOCKED_CONTRACT
LEGACY_065 = BLOCKED_NO_LIVE_REVALIDATION
LEGACY_066 = BLOCKED_NO_LIVE_REVALIDATION
LEGACY_067 = BLOCKED_NO_LIVE_REVALIDATION
LEGACY_068 = BLOCKED_NO_LIVE_REVALIDATION
LEGACY_069 = BLOCKED_MUTABLE_BY_DESIGN
LEGACY_070 = BLOCKED_BY_MUTABLE_GENERATORS
LEGACY_071 = BLOCKED_WORKTABLE_LIFECYCLE
LEGACY_072 = BLOCKED_NO_READONLY_DATASET
LEGACY_073 = BLOCKED_NO_LIVE_REVALIDATION
