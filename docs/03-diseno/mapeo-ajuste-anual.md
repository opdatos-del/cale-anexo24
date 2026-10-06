# Ajuste anual - discovery SP-FIRST (LEGACY-064 a LEGACY-068)

## Alcance y controles

Discovery read-only del bloque de Ajuste anual sobre LEGACY-064 a LEGACY-068. No crea endpoint, permiso, UI, stored procedure ni migracion. Ordena la auditoria por capacidad potencialmente read-only (065..068) antes que el subproceso de carga (064).

- LIVE writes = 0.
- LIVE mutable executions = 0.
- SQL de negocio inline en Java = 0.
- Nuevo query SP = 0.
- Nuevo business SP = 0.
- LIVE reads = 0 (sin credencial autorizada).

## Distincion de evidencia

La sesion carecio de credencial autorizada sobre `CALE_IMMEX`. Toda conclusion sobre fuentes SQL se emite como `NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE` o `NOT_FOUND`; `UNKNOWN` se aplica a la UI sin contrato visible. Este discovery no afirma ausencia absoluta de objeto LIVE; en su lugar reporta lo que la cobertura versionada confirma.

CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
VERSIONED_PRIOR_EVIDENCE = AVAILABLE

## Inventario versionado

### Procedimientos (docs/03-diseno/procedimientos-almacenados.md)

Familia CARGA_W: no aparece `CARGA_AJUSTE`, `SP_AJUSTE*`, `PR_INFORME_AJUSTE*`, `V_AJUSTE*`, `V_AJUSTEA2012`, `V_AJUSTE2012`, ni `CargaVentasInventario` en la version actual. Familia INSERT_W: pedimentos, dirigidos y facturaciones. Resto: descargos, saldos, compulsa, CTM, A31, reportes; sin coincidencias con 064..068.

### Tablas de inventario inicial documentadas

`infra/sql/01-seed-cale-immex-test.sql` contiene `DELETE FROM ErroresValidacionInventarioInicial;` y `DELETE FROM ValidacionInventarioInicial;` (lineas 81-86), usadas por el arnes de pruebas. No hay wrappers APP24 que las exploten; no hay evidencia de UI ni SP sobre ellas.

### Codigo y frontend

Busquedas sobre Ajuste, Annual, V_AJUSTE, SP_AJUSTE, 4.3.16, 4316, VentasInventario en backend/src/main y frontend/src/app devuelven solo coincidencias genericas (errores, traducciones, bitacora). No hay paquetes dedicados.

### Auditoria E2E previa (evidencia UI)

`docs/05-pruebas/auditoria-e2e-v1.md` y `docs/08-reporte-estadia/evidencias-proyecto.md` registran como inventario observado:

- Pantallas de carga: seis controles de archivo (ventas, CTM, inventario final, inventario inicial).
- Pantallas de informes: principal, almacen, ventas, 4.3.16 I y 4.3.16 II.
- Datos visibles: planta, factura, parte cliente, parte compania, cantidad, UMC, precio, cliente, periodo.

Esa evidencia de UI esta versionada en el backlog y la matriz; conserva nombres de pantallas pero no rutas ASPX, columnas, filtros, orden ni acciones.

### Matriz y backlog

Las cinco filas siguen `UNKNOWN` en la matriz; el backlog priorizado las asigna a P1 (065) y P2 (066..068, 064). Reauditorias futuras deben demostrar contrato, no asignacion por nombre.

## LEGACY-064 - Carga ajuste anual

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | CONFIRMED_AT_AUDIT_LEVEL (auditoria E2E enumera seis controles de archivo para ventas, CTM, inventario final e inventario inicial) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas por archivo) |
| VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin CARGA_AJUSTE o SP_AJUSTE en artefactos; ValidacionInventarioInicial y ErroresValidacionInventarioInicial aparecen solo en 01-seed-cale-immex-test.sql sin wrapper APP24 ni SP que las consuma) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| Stage / permissions | NOT_FOUND (sin permiso legacy asignado a la capacidad) |
| SP legacy reutilizable | NONE |
| Writers / dependencias | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Falta layout, reglas de validacion, mapeo contra tablas autoritativas, contrato de errores y autoridad de confirmacion |
| Requisitos faltantes | FILE_LAYOUT, STAGE_CONTRACT, TARGET_CONTRACT, LEGACY_PIPELINE, VALIDATION_CONTRACT, TRANSACTION_BOUNDARY, RETRY_BEHAVIOR |

```text
SCREEN_PRESENCE = CONFIRMED_AT_AUDIT_LEVEL (auditoria E2E enumera seis controles de archivo para ventas, CTM, inventario final e inventario inicial)
VISIBLE_CONTRACT = UNKNOWN (sin columnas, filtros, orden ni acciones documentadas por archivo)
VERSIONED_SQL_SOURCE = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin CARGA_AJUSTE o SP_AJUSTE en artefactos; ValidacionInventarioInicial y ErroresValidacionInventarioInicial aparecen solo en 01-seed-cale-immex-test.sql sin wrapper APP24 ni SP que las consuma)
CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
Stage = NOT_FOUND (sin permiso legacy asignado a la capacidad)
SP = NONE
Writers = UNKNOWN
IMPLEMENTABLE_NOW = NO
BLOCKER = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Falta layout, reglas de validacion, mapeo contra tablas autoritativas, contrato de errores y autoridad de confirmacion
Requisitos = FILE_LAYOUT, STAGE_CONTRACT, TARGET_CONTRACT, LEGACY_PIPELINE, VALIDATION_CONTRACT, TRANSACTION_BOUNDARY, RETRY_BEHAVIOR
```

La matriz conserva UNKNOWN; esta reauditoria aporta NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. La pista de ValidacionInventarioInicial es antecedente historico y no contrato de archivo. Cualquier carga requeriria un wrapper que ejecute un SP mutable no versionado.

## LEGACY-065 - Informe principal

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | CONFIRMED_AT_AUDIT_LEVEL (informe principal listado por la auditoria E2E) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin V_AJUSTE, V_AJUSTEA2012, PR_INFORME_AJUSTE o SP read-only equivalente en artefactos versionados) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| Stage / permissions | NOT_FOUND |
| SP legacy reutilizable | NONE |
| Writers / dependencias | UNKNOWN (sin fuente de datos versionada para detalle) |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin view, sin parametros, sin granularidad ni consumidores identificables |
| Requisitos faltantes | SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH |

```text
SCREEN_PRESENCE = CONFIRMED_AT_AUDIT_LEVEL (informe principal listado por la auditoria E2E)
VISIBLE_CONTRACT = UNKNOWN (sin columnas, filtros, orden ni acciones documentadas)
VERSIONED_SQL_SOURCE = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin V_AJUSTE, V_AJUSTEA2012, PR_INFORME_AJUSTE o SP read-only equivalente en artefactos versionados)
CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
Stage = NOT_FOUND
SP = NONE
Writers = UNKNOWN (sin fuente de datos versionada para detalle)
IMPLEMENTABLE_NOW = NO
BLOCKER = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin view, sin parametros, sin granularidad ni consumidores identificables
Requisitos = SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH
```

La matriz conserva UNKNOWN con nota UNKNOWN_NEEDS_AUDIT; esta reauditoria mantiene UNKNOWN y aporta NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. No hay evidencia para consolidar reporte read-only.

## LEGACY-066 - Informe almacen

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | CONFIRMED_AT_AUDIT_LEVEL (informe almacen listado por la auditoria E2E) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin V_AJUSTE, view de almacen ni PR_INFORME_AJUSTE_ALMACEN) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| Stage / permissions | NOT_FOUND |
| SP legacy reutilizable | NONE |
| Writers / dependencias | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Misma falta de fuente read-only que LEGACY-065 |
| Requisitos faltantes | SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH |

```text
SCREEN_PRESENCE = CONFIRMED_AT_AUDIT_LEVEL (informe almacen listado por la auditoria E2E)
VISIBLE_CONTRACT = UNKNOWN (sin columnas, filtros, orden ni acciones documentadas)
VERSIONED_SQL_SOURCE = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin V_AJUSTE, view de almacen ni PR_INFORME_AJUSTE_ALMACEN)
CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
Stage = NOT_FOUND
SP = NONE
Writers = UNKNOWN
IMPLEMENTABLE_NOW = NO
BLOCKER = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Misma falta de fuente read-only que LEGACY-065
Requisitos = SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH
```

La matriz conserva UNKNOWN; esta reauditoria mantiene UNKNOWN con NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE.

## LEGACY-067 - Informe ventas

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | CONFIRMED_AT_AUDIT_LEVEL (informe ventas listado por la auditoria E2E) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin view de ventas para ajuste anual ni PR_INFORME_AJUSTE_VENTAS) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| Stage / permissions | NOT_FOUND |
| SP legacy reutilizable | NONE |
| Writers / dependencias | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. app24.CargaFacturacion cubre facturacion, no ajuste |
| Requisitos faltantes | SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH |

```text
SCREEN_PRESENCE = CONFIRMED_AT_AUDIT_LEVEL (informe ventas listado por la auditoria E2E)
VISIBLE_CONTRACT = UNKNOWN (sin columnas, filtros, orden ni acciones documentadas)
VERSIONED_SQL_SOURCE = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin view de ventas para ajuste anual ni PR_INFORME_AJUSTE_VENTAS)
CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
Stage = NOT_FOUND
SP = NONE
Writers = UNKNOWN
IMPLEMENTABLE_NOW = NO
BLOCKER = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. app24.CargaFacturacion cubre facturacion, no ajuste
Requisitos = SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH
```

La matriz conserva UNKNOWN; esta reauditoria mantiene UNKNOWN con NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE.

## LEGACY-068 - Secciones 4.3.16 I / II

| Campo | Evidencia / estado |
|---|---|
| SCREEN_PRESENCE | CONFIRMED_AT_AUDIT_LEVEL (secciones 4.3.16 I y II listadas por la auditoria E2E) |
| VISIBLE_CONTRACT | UNKNOWN (sin columnas, filtros, orden ni acciones documentadas) |
| VERSIONED_SQL_SOURCE | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin V_AJUSTEA2012 ni PR_INFORME_AJUSTE_4316) |
| CURRENT_LIVE_REVALIDATION | NOT_AVAILABLE |
| Stage / permissions | NOT_FOUND |
| SP legacy reutilizable | NONE |
| Writers / dependencias | UNKNOWN |
| IMPLEMENTABLE_NOW | NO |
| BLOCKER | NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. El antecedente historico V_AJUSTEA2012 no aparece en artefactos versionados |
| Requisitos faltantes | SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH |

```text
SCREEN_PRESENCE = CONFIRMED_AT_AUDIT_LEVEL (secciones 4.3.16 I y II listadas por la auditoria E2E)
VISIBLE_CONTRACT = UNKNOWN (sin columnas, filtros, orden ni acciones documentadas)
VERSIONED_SQL_SOURCE = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE (sin V_AJUSTEA2012 ni PR_INFORME_AJUSTE_4316)
CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
Stage = NOT_FOUND
SP = NONE
Writers = UNKNOWN
IMPLEMENTABLE_NOW = NO
BLOCKER = NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. El antecedente historico V_AJUSTEA2012 no aparece en artefactos versionados
Requisitos = SCREEN_CONTRACT, READ_SOURCE, GRAIN, FILTER_CONTRACT, READ_ONLY_PATH
```

La matriz conserva UNKNOWN; esta reauditoria mantiene UNKNOWN con NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE.

## Gate de implementacion por capacidad (read-only: 065..068)

| Requisito | LEGACY-065 | LEGACY-066 | LEGACY-067 | LEGACY-068 |
|---|---|---|---|---|
| SCREEN_CONTRACT_CONFIRMED | NO | NO | NO | NO |
| READ_SOURCE_CONFIRMED | NO | NO | NO | NO |
| GRAIN_CONFIRMED | NO | NO | NO | NO |
| FILTER_CONTRACT_CONFIRMED | NO | NO | NO | NO |
| READ_ONLY_PATH_CONFIRMED | NO | NO | NO | NO |
| MUTABLE_PRECONDITION_REQUIRED = NO | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN |
| Implementable ahora | NO | NO | NO | NO |

## Gate de implementacion para LEGACY-064 (carga)

| Requisito | Estado |
|---|---|
| FILE_LAYOUT_CONFIRMED | NO |
| STAGE_CONTRACT_CONFIRMED | NO |
| TARGET_CONTRACT_CONFIRMED | NO |
| LEGACY_PIPELINE_CONFIRMED | NO |
| VALIDATION_CONTRACT_CONFIRMED | NO |
| TRANSACTION_BOUNDARY_CONFIRMED | NO |
| RETRY_BEHAVIOR_CONFIRMED | NO |
| Implementable ahora | NO |

### Blocker separado por capacidad

LEGACY-064: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin layout, stage, SP, view ni permiso versionados; ValidacionInventarioInicial es solo tabla de arnes de pruebas. Cualquier carga requeriria wrapper sobre SP mutable no versionado.

LEGACY-065: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin view/SP read-only; la UI principal figura en inventario pero sin columnas ni filtros.

LEGACY-066: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin view/SP read-only de almacen para ajuste anual.

LEGACY-067: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin view/SP read-only de ventas para ajuste anual; app24.CargaFacturacion cubre facturacion, no ajuste.

LEGACY-068: NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE. Sin V_AJUSTEA2012 ni PR_INFORME_AJUSTE_4316; el antecedente historico V_AJUSTEA2012 no aparece en la cobertura actual.

Sin wrappers APP24_Q ni procedimientos de negocio: NEW_QUERY_SP = 0 y NEW_BUSINESS_SP = 0.

## Estado

LEGACY_064 = BLOCKED_CONTRACT
LEGACY_065 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_066 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_067 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_068 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
MATRIX_NOTE_064 = UNKNOWN + CONTRACT_INCOMPLETE (sin cambio de categoria global)
MATRIX_NOTE_065 = UNKNOWN + UNKNOWN_NEEDS_AUDIT
MATRIX_NOTE_066 = UNKNOWN
MATRIX_NOTE_067 = UNKNOWN
MATRIX_NOTE_068 = UNKNOWN
NEW_QUERY_SP = 0
NEW_BUSINESS_SP = 0
INLINE_BUSINESS_SQL_JAVA = 0
LIVE_ACTIVATION_PENDING = YES
LIVE reads = 0
LIVE writes = 0
LIVE mutable executions = 0
DISCOVERY_064_068_VERSIONED = YES
