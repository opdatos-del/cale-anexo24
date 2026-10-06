# Interfaces especializadas - discovery SP-FIRST (Servicios, Ordenes de fabricacion, Procesos)

## Alcance y controles

Discovery read-only de LEGACY-059, LEGACY-061 y LEGACY-062. No se crean endpoint, permiso, UI, stored procedure ni migracion. Separa la capa restringida de evidencia versionada y la lectura LIVE no ejecutada por falta de credencial autorizada.

- LIVE writes = 0.
- LIVE mutable executions = 0.
- SQL de negocio inline en Java = 0.
- Nuevo query SP = 0.
- Nuevo business SP = 0.
- LIVE reads = 0 (sin credencial autorizada).

## Distincion de evidencia

Este discovery nunca afirma ausencia absoluta de objeto LIVE. La sesion carecio de credencial autorizada sobre `CALE_IMMEX`, por lo que toda conclusion sobre fuentes se emite en forma de:

- `REPO_VERSIONED_SQL_SOURCE`: lo que aparece en los artefactos versionados actuales en Git.
- `PROJECT_SQL_DUMP_EVIDENCE`: lo que se observa en el dump SQL del proyecto (fuera del repo).
- `PRIOR_LIVE_EVIDENCE`: lo que se observo en rondas previas de auditoria LIVE.
- `CURRENT_LIVE_REVALIDATION`: lo que podria confirmarse con credencial LIVE autorizada; no ejecutado en esta sesion.

- `NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE`: el objeto no aparece en los artefactos versionados actuales.
- `NOT_FOUND`: no aparece en la cobertura versionada disponible (sin afirmar `PROVEN_NOT_TO_EXIST`).
- `NOT_REVALIDATED`: no se confirmo en la sesion actual.
- `NOT_CONFIRMED`: estado por defecto cuando no se demostro aun.

CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
VERSIONED_PRIOR_EVIDENCE = AVAILABLE

## Inventario versionado

### Procedimientos (docs/03-diseno/procedimientos-almacenados.md)

- Familia CARGA documentada: CARGA_SUBMAQUILA, CARGAACTAS, CARGA_MATERIALES, CARGA_PRODUCTOS, CARGA_FACTURAS, CARGACONSTANCIAS, CARGAPEDIMENTOS, CARGA_ENCABEZADOS, CARGA_ESTRUCTURADESENSAMBLE, CARGAAgentes, CARGACLIENTES, CARGAPROVEEDORES, CARGAFACTURASENPSALIDAS. Ningun CARGA_SERVICIOS, CARGA_ORDENES o CARGA_PROCESOS aparece en la version actual.
- Familia PR_INFORME: cubre importaciones, exportaciones, estructuras y saldos. No aparece PR_INFORME_SERVICIOS.
- Familia INSERT: pedimentos, dirigidos y facturaciones.
- Resto: descargos, saldos, compulsa, CTM, A31, compulsa DS y reportes; sin coincidencias con los tres IDs en la version actual.

### Archivos fisicos

Sin archivos dedicados en backend/src/test/resources, docs o infra/sql ademas de los ya clasificados. Los terminos servicios, orden_fabric, fabric, proceso y process no producen archivos adicionales. La busqueda recursiva confirma que `V_AJUSTE`, `V_AJUSTEA2012`, `SP_AJUSTE` y nombres `4.3.16` no aparecen en la version actual.

### Codigo y frontend

Busquedas sobre Servicio, ServicioImport, Orden Fabric, Order Manufact, Proceso, ProcessImport, UUID en backend/src/main y frontend/src/app devuelven solo coincidencias genericas (errores, traducciones, bitacora). No hay paquetes dedicados.

### Matriz y backlog

Los tres IDs siguen NOT_CAPTURED en la matriz y P2 en el backlog. Ninguna fila de la matriz de paridad aporta una pista cuantitativa al contrato; el backlog solo enuncia su existencia a nivel de capacidad.

## LEGACY-059 - Servicios

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | UNKNOWN (no se conserva pantalla versionada) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| REPO_VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin CARGA_SERVICIOS, PR_INFORME_SERVICIOS, dbo.servicios ni V_SERVICIOS en la version actual) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| STAGE_CONTRACT | NOT_CONFIRMED (no se localiza permiso legacy asignado a la capacidad) |
| SP_REUSABLE | NOT_CONFIRMED (no se identifico SP read-only reutilizable) |
| WRITERS / DEPENDENCIAS | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. El dominio cualitativo (montos, impuestos, moneda/tipo de cambio, paises) no es contrato versionado |

```text
LEGACY-059_SCREEN_CONTRACT_CONFIRMED = NO
LEGACY-059_SOURCE_CONTRACT_CONFIRMED = NO
LEGACY-059_INPUT_OR_READ_CONTRACT_CONFIRMED = NO
LEGACY-059_GRAIN_CONFIRMED = NO
LEGACY-059_FILTER_CONTRACT_CONFIRMED = NO
LEGACY-059_MUTABLE_PRECONDITION_REQUIRED = UNKNOWN
LEGACY_059 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
```

La matriz conserva UNKNOWN con nota CONTRACT_INCOMPLETE; esta reauditoria aporta NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE sin convertir la fila en MISSING (la auditoria funcional previa lo enuncio).

## LEGACY-061 - Ordenes de fabricacion

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | UNKNOWN (no se conserva pantalla versionada) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| REPO_VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin CARGA_ORDEN, PR_INFORME_ORDEN, dbo.ordenes ni V_ORDEN en la version actual) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| STAGE_CONTRACT | NOT_CONFIRMED |
| SP_REUSABLE | NOT_CONFIRMED |
| WRITERS / DEPENDENCIAS | UNKNOWN; el dominio reusa campos semanticos de descarga y saldo ya excluidos de V1 |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE; reintroduciria reglas fiscales ya separadas por decision documentada |

```text
LEGACY-061_SCREEN_CONTRACT_CONFIRMED = NO
LEGACY-061_SOURCE_CONTRACT_CONFIRMED = NO
LEGACY-061_INPUT_OR_READ_CONTRACT_CONFIRMED = NO
LEGACY-061_GRAIN_CONFIRMED = NO
LEGACY-061_FILTER_CONTRACT_CONFIRMED = NO
LEGACY-061_MUTABLE_PRECONDITION_REQUIRED = UNKNOWN
LEGACY_061 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
```

La matriz conserva UNKNOWN con nota CONTRACT_INCOMPLETE; esta reauditoria aporta NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE sin convertir la fila en MISSING.

## LEGACY-062 - Procesos

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | UNKNOWN (no se conserva pantalla versionada) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| REPO_VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin CARGA_PROCESOS, PR_INFORME_PROCESOS, dbo.procesos ni V_PROCESOS en la version actual) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| LIVE_EXISTENCE | NOT_REVALIDATED |
| STAGE_CONTRACT | NOT_CONFIRMED |
| SP_REUSABLE | NOT_CONFIRMED |
| WRITERS / DEPENDENCIAS | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE; el patron de staging propio existe para facturacion y pedimentos pero no es reutilizable sin evidencia de layout y reglas |

```text
LEGACY-062_SCREEN_CONTRACT_CONFIRMED = NO
LEGACY-062_SOURCE_CONTRACT_CONFIRMED = NO
LEGACY-062_INPUT_OR_READ_CONTRACT_CONFIRMED = NO
LEGACY-062_GRAIN_CONFIRMED = NO
LEGACY-062_FILTER_CONTRACT_CONFIRMED = NO
LEGACY-062_MUTABLE_PRECONDITION_REQUIRED = UNKNOWN
LEGACY_062 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
```

La matriz conserva UNKNOWN con nota CONTRACT_INCOMPLETE; esta reauditoria aporta NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE sin convertir la fila en MISSING.

## Gate de implementacion por capacidad

| Requisito | LEGACY-059 | LEGACY-061 | LEGACY-062 |
|---|---|---|---|
| SCREEN_CONTRACT_CONFIRMED | NO | NO | NO |
| INPUT_OR_READ_CONTRACT_CONFIRMED | NO | NO | NO |
| SOURCE_CONTRACT_CONFIRMED | NO | NO | NO |
| GRAIN_CONFIRMED | NO | NO | NO |
| FILTER_CONTRACT_CONFIRMED | NO | NO | NO |
| VALIDATION_CONTRACT_CONFIRMED | NO | NO | NO |
| MUTABLE_PRECONDITION_REQUIRED = NO | UNKNOWN | UNKNOWN | UNKNOWN |
| Implementable ahora | NO | NO | NO |

### Blocker separado por capacidad

LEGACY-059: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. La pista cualitativa no es contrato.

LEGACY-061: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. El dominio reusa campos semanticos ya excluidos por decision documentada.

LEGACY-062: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. El patron de staging propio existe para facturacion y pedimentos pero no es reutilizable sin evidencia.

Sin wrappers APP24_Q ni procedimientos de negocio: NEW_QUERY_SP = 0 y NEW_BUSINESS_SP = 0.

## Estado

LEGACY_059 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_061 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_062 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
MATRIX_NOTE_059 = UNKNOWN + CONTRACT_INCOMPLETE (sin cambio de categoria global)
MATRIX_NOTE_061 = UNKNOWN + CONTRACT_INCOMPLETE (sin cambio de categoria global)
MATRIX_NOTE_062 = UNKNOWN + CONTRACT_INCOMPLETE (sin cambio de categoria global)
NEW_QUERY_SP = 0
NEW_BUSINESS_SP = 0
INLINE_BUSINESS_SQL_JAVA = 0
LIVE_ACTIVATION_PENDING = YES
LIVE reads = 0
LIVE writes = 0
LIVE mutable executions = 0
DISCOVERY_059_061_062_VERSIONED = YES

LEGACY_059_TERMINOLOGY_FIXED = YES
LEGACY_061_TERMINOLOGY_FIXED = YES
LEGACY_062_TERMINOLOGY_FIXED = YES
