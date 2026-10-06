# Cambios de régimen y regularizaciones — discovery SP-FIRST

## Alcance y controles

Este documento registra el discovery read-only de LEGACY-023 y LEGACY-024. No
crea endpoint, permiso, UI, stored procedure ni migración. La revisión separa la
evidencia de pantalla, el mapeo de cada identificador y las fuentes SQL; no
deduce que una view con un nombre parecido cubra alguna de las dos capacidades.

- LIVE writes = 0.
- LIVE mutable executions = 0.
- SQL de negocio inline en Java = 0.
- Nuevo query SP = 0.
- Nuevo business SP = 0.

## Disponibilidad de evidencia LIVE

Se intentó una consulta de metadata con sqlcmd contra CALE_IMMEX. El servidor fue
alcanzable, pero el inicio de sesión integrado no tiene acceso a esa base; por
tanto el SELECT no se ejecutó. No fue un fallo TLS.

| Métrica | Resultado |
|---|---|
| LIVE reads exitosos | 0 |
| LIVE reads intentados, no ejecutados por autenticación | 1 |
| LIVE writes | 0 |
| LIVE mutable executions | 0 |
| CURRENT_LIVE metadata/definition/cardinality | NOT_AVAILABLE |

CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
VERSIONED_PRIOR_EVIDENCE = AVAILABLE

Todo resultado que sigue proviene de documentación versionada o de evidencia
histórica indicada para esta fase. Los conteos históricos no se presentan como
una revalidación actual.

## Mapeo funcional y contrato UI

La matriz de paridad identifica inequívocamente el mapping: LEGACY-023 es
Cambios de régimen y LEGACY-024 es Regularizaciones. La auditoría funcional
ampliada confirma ambas pantallas a nivel de inventario, pero no conservó las
rutas ASPX ni el contrato visible por pantalla.

### LEGACY-023 — Cambios de régimen

| Campo | Evidencia / estado |
|---|---|
| Nombre funcional | CONFIRMED: Cambios de régimen |
| Pantalla | CONFIRMED_AT_AUDIT_LEVEL |
| Ruta ASPX | NOT_CAPTURED / UNKNOWN |
| Menú | UNKNOWN |
| Propósito visible | UNKNOWN: no se demostró si consulta, proceso o ambas cosas |
| Grano | UNKNOWN |
| Filtros, rango de fechas, documento/pedimento | UNKNOWN |
| Columnas y orden | UNKNOWN |
| Botones y acciones | UNKNOWN |
| Exportación | UNKNOWN |
| Comportamiento vacío / con datos | UNKNOWN |

LEGACY_023_SCREEN = CONFIRMED_AT_AUDIT_LEVEL
LEGACY_023_PURPOSE = UNKNOWN
LEGACY_023_GRAIN = UNKNOWN
LEGACY_023_FILTERS = UNKNOWN
SCREEN_CONTRACT_CONFIRMED = NO

### LEGACY-024 — Regularizaciones

| Campo | Evidencia / estado |
|---|---|
| Nombre funcional | CONFIRMED: Regularizaciones |
| Pantalla | CONFIRMED_AT_AUDIT_LEVEL |
| Ruta ASPX | NOT_CAPTURED / UNKNOWN |
| Menú | UNKNOWN |
| Propósito visible | UNKNOWN: no se demostró si consulta, proceso o ambas cosas |
| Grano | UNKNOWN |
| Filtros, rango de fechas, documento/pedimento | UNKNOWN |
| Columnas y orden | UNKNOWN |
| Botones y acciones | UNKNOWN |
| Exportación | UNKNOWN |
| Comportamiento vacío / con datos | UNKNOWN |

LEGACY_024_SCREEN = CONFIRMED_AT_AUDIT_LEVEL
LEGACY_024_PURPOSE = UNKNOWN
LEGACY_024_GRAIN = UNKNOWN
LEGACY_024_FILTERS = UNKNOWN
SCREEN_CONTRACT_CONFIRMED = NO

No se atribuye F4/F5, A3, RT, SICE o SACI a una pantalla concreta. La
clasificación de claves que aparece abajo prueba una rama de persistencia, no el
contrato visible de una pantalla legacy.

## Evidencia SQL versionada

### Fuentes y clasificación

| Objeto | Tipo / evidencia | Clasificación | Decisión |
|---|---|---|---|
| dbo.V_CambioRegimen_Sice_SACI | VIEW en evidencia técnica previa; 3,866 filas y 44 columnas son conteos históricos, no revalidados | READ_SOURCE_CANDIDATE | La definición, columnas, grano, filtros, dependencias y consumidores no están disponibles en evidencia versionada; no usar como fuente canónica |
| dbo.SALIDAS / dbo.PSALIDAS | Persistencia de salida y detalle; las ramas de pedimentos clasifican las operaciones | PERSISTED_PROCESS_OUTPUT | Puede contener resultados de proceso, no es contrato de consulta por sí sola |
| dbo.CARGAPEDIMENTOSIE | Stage global legado | MUTABLE_WORKTABLE | No leer como contrato de UI ni reutilizar |
| dbo.CARGAPEDIMENTOS | SP con DELETE, INSERT y UPDATE sobre operación, catálogos y scratch global | SP_EXISTING_UNSAFE | No ejecutar ni reutilizar para GET |
| dbo.INSERTAPEDIMENTO | SP con DELETE/INSERT/UPDATE y religa rectificaciones | SP_EXISTING_UNSAFE | No ejecutar ni reutilizar para GET |
| dbo.CARGA_ENCABEZADOS | SP de inserción y normalización | SP_EXISTING_UNSAFE | No ejecutar ni reutilizar para GET |
| dbo.APP24_C_PEDIMENTO_CONFIRMAR | Command versionado mutable | SP_EXISTING_MUTABLE_REQUIRES_CONFIRMATION | Clasifica la salida creada, pero no es una consulta ni sustituye el contrato legacy |
| dbo.NUEVOFOLIOB | Único SP legacy read-only del inventario SP-FIRST; calcula folio de descargos bloqueados | FALSE_POSITIVE | No cubre régimen ni regularización |

La única evidencia concreta de clasificación operativa versionada proviene de
APP24_C_PEDIMENTO_CONFIRMAR: F4 y F5 se persisten como CAMBIO DE REGIMEN, A3
como REGULARIZACION, CT como CONST TRANSFERENCIA y DE como desperdicios. Es una
regla de persistencia de la rama de salida; no demuestra la ruta, filtros,
columnas ni semántica completa de LEGACY-023 o LEGACY-024.

### Dependencias, writers y precondiciones

Las fuentes persistidas dependen de que un proceso anterior haya creado las
operaciones de salida y sus detalles. La documentación de importación autoritativa
registra como writers: CARGAPEDIMENTOS, INSERTAPEDIMENTO y CARGA_ENCABEZADOS; el
command moderno APP24_C_PEDIMENTO_CONFIRMAR también escribe SALIDAS y PSALIDAS
cuando confirma una carga. Los tres procedimientos legacy tienen DML y efectos
globales documentados; no fueron ejecutados.

La evidencia no permite determinar para dbo.V_CambioRegimen_Sice_SACI:

- definición completa y dependencias transitivas;
- claves, cardinalidad y grano;
- filtros seguros y orden estable;
- si proyecta sólo cambios de régimen, regularizaciones o ambos;
- si sólo refleja una salida ya creada por procesos mutables;
- consumers existentes o un SP legacy que entregue su mismo contrato.

MUTABLE_PRECONDITION_REQUIRED para ambas capacidades = UNKNOWN. No se marca NO
porque no se pudo inspeccionar la definición ni el flujo productor de la view.

## Gate de implementación por capacidad

| Requisito | LEGACY-023 | LEGACY-024 |
|---|---|---|
| SCREEN_CONTRACT_CONFIRMED | NO | NO |
| SOURCE_CONTRACT_CONFIRMED | NO | NO |
| GRAIN_CONFIRMED | NO | NO |
| FILTER_CONTRACT_CONFIRMED | NO | NO |
| READ_ONLY_PATH_CONFIRMED | NO | NO |
| MUTABLE_PRECONDITION_REQUIRED = NO | NO: UNKNOWN | NO: UNKNOWN |
| Implementable ahora | NO | NO |

### Blocker separado

LEGACY-023 queda bloqueado por el contrato visible perdido y por no tener una
fuente read-only verificada que represente cambios de régimen, su grano y sus
filtros.

LEGACY-024 queda bloqueado por el contrato visible perdido y por no tener una
fuente read-only verificada que represente regularizaciones, su grano y sus
filtros. La clasificación A3 en la persistencia no elimina ese blocker.

No se crean wrapper APP24_Q ni procedimiento de negocio: NEW_QUERY_SP = 0 y
NEW_BUSINESS_SP = 0. Si una revisión futura confirma una view canónica y su
contrato, el único cambio permitido antes de activar runtime sería un wrapper
técnico APP24_Q de sólo lectura, versionado y sin despliegue LIVE.

## Continuidad de backlog

La selección previa de LEGACY-025 queda superada por la reconciliación de
paridad: Actas ya está IMPLEMENTED_REDESIGNED en dev. La selección del siguiente
candidato se registra después de integrar esta corrección; no se reinicia Actas.

## Estado

LEGACY_023 = PARTIAL_EVIDENCE_BLOCKED_CONTRACT
LEGACY_024 = PARTIAL_EVIDENCE_BLOCKED_CONTRACT
NEW_QUERY_SP = 0
NEW_BUSINESS_SP = 0
INLINE_BUSINESS_SQL_JAVA = 0
LIVE_ACTIVATION_PENDING = YES
LIVE reads = 0
LIVE writes = 0
LIVE mutable executions = 0
