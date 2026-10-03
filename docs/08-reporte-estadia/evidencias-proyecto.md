# Evidencias del proyecto (reporte de estadía)

Registro acumulativo de evidencia verificable por fase. No sustituye al reporte
final; sólo conserva problema, método, resultados y limitaciones para los
capítulos 3 (métodos y técnicas) y 4 (análisis de resultados).

No se registran secretos, credenciales, cadenas de conexión ni datos completos de
negocio. Los pedimentos, RFC y documentos reales no se copian; se usan conteos,
diagramas y datos sintéticos.

---

## Fase — Importación autoritativa de pedimentos

**Fecha / fase:** discovery V1, rama `feature/legacy-pediment-authoritative-import-v1`.

**Problema técnico abordado:** el staging/preview de pedimentos (V1) no explica
cómo el sistema legado convierte un pedimento en operaciones reales
(`IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `PSALIDAS`). `LEGACY-057` estaba `MISSING`
y su frontera con `LEGACY-016/017/019` no estaba reconciliada.

**Objetivo de la fase:** reconstruir el pipeline autoritativo legacy y decidir, con
evidencia y bajo SP-FIRST estricto, si se puede reutilizar o si se requiere un
command `APP24_*` nuevo. Sin implementar confirmación.

**Método utilizado:**

- auditoría funcional legacy sobre metadata (`sys.objects`, `sys.columns`,
  `sys.indexes`, `sys.sql_expression_dependencies`);
- lectura de `OBJECT_DEFINITION` para clasificar DML directo y construir el call
  graph transitivo;
- conteos agregados (nunca filas de negocio);
- análisis de transacción, idempotencia y generación de claves;
- reconciliación con la matriz de paridad y los mapeos existentes;
- sin ejecutar procedimientos mutables.

**Objetos auditados:** `CARGAPEDIMENTOS`, `INSERTAPEDIMENTO`, `CARGA_ENCABEZADOS`,
`VALIDAPEDIMENTO`, `VALIDA_I_DETALLENP`, `INDICAOPERACION`, `INSERTERROR`; tablas
`CARGAPEDIMENTOSIE`, `ERRORCARGA`, `PEDIMENTOS` (ausente), `INVENTARIO` (ausente),
`NP` (ausente), `I_PEDIMENTO`, `I_DETALLENP`, `I_ERROR`, `I_FACTURA`,
`ERRORVALIDACION`, `GENERADORES`, `A31_ENTRADAS`, `IMPORTACIONES`, `PARTIDAS`,
`SALIDAS`, `PSALIDAS`, `DIRIGIDO`, `DESCARGA`, `RECTIFICACIONIMPORT`,
`RECTIFICACIONEXPORT`.

**Decisiones tomadas:**

- `PEDIMENT_LEGACY_CALL_GRAPH = COMPLETE` (2 aristas SP→SP, sin SQL dinámico).
- `LEGACY_PEDIMENT_TRANSACTION = NONE`; `PARTIAL_WRITE_RISK = YES`.
- `KEY_GENERATION_STRATEGY = MAX_PLUS_ONE_WITH_ROW_NUMBER`; `CONCURRENCY_SAFE = NO`.
- `PED-007` confirmada como omisión silenciosa (no error) contra operación existente; `PED_007_MODERN_IDEMPOTENCY_KEY = UNKNOWN`.
- Ningún SP legacy es reutilizable para confirmación
  (`LEGACY_REUSABLE_FOR_CONFIRMATION = 0`); se propone
  `SAFE_IMPLEMENTATION_STRATEGY = PROVISIONAL_NEW_AUTHORITATIVE_COMMAND`, con
  `AUTHORITATIVE_COMMAND_DATABASE = UNKNOWN`.
- No se implementa `POST /confirmar`; `LEGACY-057` permanece `MISSING`.

**Riesgos encontrados:** ausencia de transacción y rollback; claves `MAX+1` no
seguras en concurrencia; `GENERADORES` como scratch global borrado por varios SP;
auto-creación de `MATERIAL`/`PRODUCTOS`; flujos que dependen de tablas ausentes
(`PEDIMENTOS`, `INVENTARIO`, `NP`); reglas `PED-005/006` sin fuente de inventario.

**Resultado:** pipeline reconstruido; contrato autoritativo `PARTIAL`;
`READY_FOR_IMPLEMENTATION = NO` en esta fase.

**Pruebas / evidencias:** conteos LIVE (2 `CARGAPEDIMENTOSIE`, 2 `IMPORTACIONES`,
2 `PARTIDAS`, 660 `SALIDAS`, 3392 `PSALIDAS`, 5 `GENERADORES`, 0 `DIRIGIDO`);
`OBJETO_DEFINITION` de los 7 SP; ausencia confirmada de `PEDIMENTOS`/`INVENTARIO`/
`NP`; gate SP-FIRST `PASS` (`violations=0`).

**Limitaciones:** sin datos de negocio suficientes para validar 1:N; permiso de
confirmación sin evidencia; contrato de inventario pendiente; no se ejecutó el
pipeline.

**Artefactos producidos:** `docs/03-diseno/mapeo-importacion-pedimentos-autoritativa.md`;
este registro.

### Cierre V2 — contrato, cobertura y enforcement

- Dependencias legacy ausentes clasificadas como `MISSING_DEPLOYMENT_OBJECT` /
  `DEAD_LEGACY_BRANCH` (`PEDIMENTOS`, `NP`, `INVENTARIO`): no hay synonyms, linked
  servers ni referencias cross-database.
- Única rama legacy ejecutable: `CARGAPEDIMENTOSIE → CARGAPEDIMENTOS`
  (`ACTIVE_PEDIMENT_PIPELINE`). Las ramas (B) y (C) son ramas muertas.
- Cobertura staging moderno → operación: `IMPORT_FIELD_COVERAGE = PARTIAL (54/61)`,
  `EXPORT_FIELD_COVERAGE = PARTIAL (42/45)`; faltan `IGIE`, `IVA`, `DTA`, `PREV`,
  `TIPOTASAIGIE` (clase C: vienen del archivo legacy, hoy no se almacenan).
- Identidad operacional de una columna (`NUMERO_PED` / `DOCUMENTO`),
  `OPERATIONAL_IDENTITY_CONTRACT = PARTIAL` (0 duplicados / 0 nulos LIVE, sin
  constraint DDL).
- `PED-007` implementado read-only en `APP24_Q_PEDIMENTO_VALIDAR_REGLAS`
  (extensión, no SP nuevo), con `EXISTS` NULL-safe; smoke LIVE sintético `PASS`.
- `DATABASES_SAME_INSTANCE = YES`; `CROSS_DB_ATOMIC_COMMAND_FEASIBLE = YES`;
  `AUTHORITATIVE_RUNTIME_PERMISSION = INSUFFICIENT`; `AUTHORITATIVE_COMMAND_DATABASE = UNKNOWN`.
- Sin auto-creación de catálogo (`UNKNOWN_CATALOG_ITEM_POLICY = REJECT`, propuesta).
- Permiso de confirmación propuesto `PEDIMENTOS_CONFIRMAR` (no implementado).

Métricas V2 para el reporte: 4 SP re-auditados por dependencia de runtime; 3
objetos LIVE ausentes; 61 columnas de destino analizadas (import) y 45 (export);
5 campos sin cobertura; 1 regla `PED-007` implementada; 2 tests nuevos;
1 smoke LIVE sintético; política SP-FIRST revalidada (`violations=0`).

### Cierre V3 — precisión metodológica

- Se distinguió **objeto ausente** (`OBJECT_STATUS = NOT_PRESENT_LIVE`) de
  **flujo muerto** (`FLOW_STATUS = BROKEN_IN_CURRENT_SCHEMA`); no se usó la doble
  etiqueta ni se afirmó `DEAD_CONFIRMED` sin probar ausencia de entrypoints.
- Se verificó la **identidad efectiva del runtime** con `SUSER_SNAME()`/`USER_NAME()`:
  `opdatos`/`dbo` en ambos datasources; `CURRENT_RUNTIME_PERMISSION = SUFFICIENT`
  para el runtime actual, con `anexo24_app` como objetivo de hardening pendiente.
- Se reconciliaron **61 campos de importación** y **45 de exportación** por
  categoría (`STORED/DERIVED/CATALOG_LOOKUP/DEFAULT_CONFIRMED/MISSING_FROM_STAGING`),
  sumando exactamente el total y explicitando `FIELD_COVERAGE_ACCOUNTING = PASS`.
- Se implementó `PED-007` **reutilizando** el SP read-only existente
  (`PED_007_SP_REUSE = PASS`), sin crear SP redundante.
- Se mantuvo la separación entre **validación preventiva** (`PED-007`) e
  **idempotencia transaccional** futura (`MODERN_OPERATIONAL_IDENTITY_CONTRACT = PARTIAL`).

Métricas V3: 3 objetos ausentes reclasificados con status separado; 2 datasources
identificados; 106 columnas de destino categorizadas (61 + 45); 1 contrato de
runtime confirmado; 0 cambios de paridad.

---

## Fase — Contrato de confirmación de pedimentos (staging V2 + concurrencia)

**Fecha / fase:** `feature/pediment-confirmation-contract-v1`, primera pasada (sin push).

**Problema técnico abordado:** cerrar el contrato previo a la confirmación
autoritativa: campos fiscales faltantes en staging, identidad operacional,
compatibilidad de generación de claves `MAX+1` con el motor legacy, ubicación
transaccional del command y separación de permiso.

**Método utilizado:** extensión del contrato JSON de staging (sin `ALTER TABLE`);
mapping fiscal línea por línea de `CARGAPEDIMENTOS`; prueba de concurrencia aislada
con dos conexiones (tabla temporal global en `tempdb`); auditoría de permisos del
principal efectivo; diseño transaccional y de bloqueo.

**Decisiones:** `UNKNOWN_CATALOG_ITEM_POLICY = REJECT`;
`PEDIMENT_CONFIRM_PERMISSION = PEDIMENTOS_CONFIRMAR` (sin asignación a perfiles);
`CONFIRMED_STATE_TERMINAL = YES`; `LEGACY_FISCAL_FIELD_POLICY = PRESERVE_INPUT`;
`AUTHORITATIVE_COMMAND_DATABASE = CALE_IMMEX`; `KEY_ALLOCATION_STRATEGY_V1 =
TABLE_LOCKED_MAX_PLUS_ONE`.

**Riesgos encontrados:** `MAX+1` concurrente colisiona; `sp_getapplock` no coordina
con el legacy; el motor legacy no toma locks de aplicación.

**Resultado:**

- staging V2 (IGIE/IVA/DTA/PREV/TIPOTASAIGIE), `STAGING_V1_BACKWARD_COMPATIBILITY = PASS`;
- `IMPORT_FIELD_COVERAGE = COMPLETE (61/61)`, `EXPORT_FIELD_COVERAGE = COMPLETE (45/45)`;
- `FISCAL_FIELD_MAPPING = CONFIRMED`;
- `KEY_ALLOCATION_COMPATIBLE_WITH_LEGACY = CONFIRMED_FOR_V1` con prueba real;
- `IDEMPOTENCY_TWO_PHASE_GUARD = DESIGNED`;
- permiso `PEDIMENTOS_CONFIRMAR` versionado, 0 asignaciones productivas.

**Pruebas / evidencias:** prueba de concurrencia aislada (`SIN_LOCK_COLISION = true`,
`CON_LOCK_BLOQUEO_LEGACY = true`, `CON_LOCK_CLAVES_DISTINTAS = true`,
`CON_LOCK_FILAS = 2`); test reproducible en CI con Testcontainers SQL Server;
tests de parser V2 (fiscal presente/vacío/inválido); backend `test build` PASS;
SP-FIRST `violations = 0`.

**Limitaciones:** la prueba de concurrencia usó una tabla temporal sintética (no
tablas reales de `CALE_IMMEX`); el Testcontainers no arranca en el Docker Desktop
local (endpoint npipe incompatible con docker-java 1.20.4) y se ejecuta en CI.

**Artefactos producidos:** parser V2 + tests; prueba de concurrencia; migración de
permiso; actualización de `mapeo-importacion-pedimentos-autoritativa.md` y
`mapeo-carga-pedimentos.md`.

### Métricas para el reporte

coverage antes/después = 54/61 → 61/61 (import) y 42/45 → 45/45 (export); campos
agregados = 5; tests añadidos = 4 (2 parser V2, 2 concurrencia); collision test y
lock test = 1 caso cada uno; reglas PED cerradas = `PED-007` (implementada),
`PED-001..004` ya confirmadas.

### Anexos candidatos

matriz staging→target; diagrama transaccional; diagrama PRECHECK→RECHECK; prueba de
concurrencia `MAX+1`; lista `PED-001..007`.

---

## Fase — Confirmación autoritativa de pedimentos (backend)

**Fecha / fase:** `feature/pediment-authoritative-confirmation-v1`, backend expuesto tras SQL gate verde.

**Problema técnico abordado:** faltaba la operación autoritativa posterior al preview:
convertir el staging validado en `IMPORTACIONES`/`PARTIDAS` o `SALIDAS`/`PSALIDAS` con
atomicidad, idempotencia y coordinación con el motor legacy.

**Método utilizado:** command SQL único (`dbo.APP24_C_PEDIMENTO_CONFIRMAR`) que lee el
staging por nombre de 3 partes y concentra locks, recheck, asignación de claves, writes,
estado y bitácora en una sola transacción; backend hexagonal que sólo invoca el SP;
tests de integración SQL en contenedor efímero aplicando el DDL versionado real.

**Resultado:**

- endpoint `POST /api/v1/operaciones/pedimentos/cargas/{id}/confirmacion` con permiso `PEDIMENTOS_CONFIRMAR`;
- cargas mixtas (import + export) en una única transacción, con conteos acumulados
  (`OperacionesProcesadas` = headers, `PartidasProcesadas` = líneas);
- idempotencia: mismo `CargaId` → `ALREADY_CONFIRMED` (200); otra carga con el mismo
  documento → `DUPLICATE_OPERATION` (409);
- rollback total ante fallo forzado, sin escrituras parciales;
- coordinación con legacy mediante `TABLOCKX + HOLDLOCK` y `sp_getapplock` para same-load;
- auditoría de éxito dentro de la transacción (`APP24_C_BITACORA_REGISTRAR`), auditoría
  de fallo fuera del rollback con infraestructura existente.

**Pruebas / evidencias (CI real, commit `4bd8776`):**

```text
sp-first-gate = SUCCESS · backend = SUCCESS · frontend = SUCCESS
PedimentoConfirmacionSqlIT = 15 PASSED / 0 skipped / 0 failed
PedimentKeyAllocationConcurrencyTest = 2 PASSED
endpoint: 401 / 403 / 200 CONFIRMED / 200 ALREADY_CONFIRMED / 404 / 409 / 422
SP-FIRST: violations = 0 · INLINE_BUSINESS_SQL_JAVA = 0
```

**Limitaciones:** el comando se validó en contenedor efímero con funciones `FACTOR`/
`VALIDUNIT` de prueba; el despliegue LIVE del command y las migrations sigue pendiente;
no se ejecutó el command sobre datos empresariales.

**Artefactos producidos:** `APP24_C_PEDIMENTO_CONFIRMAR.sql`, migration
`12-pedimento-confirmada-state.sql`, adapter/puerto/caso de uso/DTO/endpoint, tests de
adaptador, caso de uso y seguridad, `PedimentoConfirmacionSqlIT`.

### Métricas para el reporte (backend)

tests nuevos = 3 clases (adaptador, caso de uso, seguridad) + 15 casos SQL;
mapeo de errores SQL = 12 códigos controlados; endpoints = 1; permisos nuevos = 1
(`PEDIMENTOS_CONFIRMAR`, 0 asignaciones a perfiles);
`LIVE command executions = 0` y `business writes = 0`.

### Anexos candidatos (backend)

diagrama de la transacción del command; matriz de errores SQL → HTTP; matriz
401/403/200/404/409/422; captura de la futura UI de confirmación.

---

## Fase — Confirmación autoritativa: interfaz, despliegue controlado y paridad

**Fecha / fase:** `feature/pediment-authoritative-confirmation-v1`, cierre V1.

**Problema técnico abordado:** exponer la confirmación en la UI sin revelar la acción
a quien no puede ejecutarla, y desplegar el DDL autorizado sin ejecutar el command
sobre datos empresariales.

**Método utilizado:** reutilización del `ConfirmService` existente (MatDialog) en vez
de un diálogo paralelo; `canConfirm` derivado de permiso + estado + errores + filas
inválidas; refresco del recurso por `GET` tras confirmar; despliegue LIVE del DDL
versionado con verificación de definición repo↔LIVE normalizada.

**Resultados:**

- UI con botón `Confirmar` sólo con `PEDIMENTOS_CONFIRMAR` y carga previsualizada sin
  errores; `CONFIRMADA` muestra estado terminal sin botón;
- `ALREADY_CONFIRMED` tratado como éxito informativo (no error), con refresco;
- errores 404/409/422 con mensajes funcionales, sin detalles SQL;
- migraciones 11 y 12 aplicadas a `ANEXO24_DEV` (idempotentes, 0 asignaciones de perfil,
  0 filas modificadas);
- `dbo.APP24_C_PEDIMENTO_CONFIRMAR` desplegado en `CALE_IMMEX`;
- `COMMAND_REPO_LIVE_MATCH = PASS` (15107 = 15107 caracteres normalizados);
- `business command executions = 0` y `business writes = 0` durante el despliegue.

**Pruebas / evidencias:**

```text
frontend: 113 tests PASS · lint PASS · build PASS · parent-relative imports = 0
backend:  SQL IT 15 PASSED / 0 skipped / 0 failed · concurrencia 2 PASSED
SP-FIRST: violations = 0
paridad:  LEGACY-016/019/057 → IMPLEMENTED_REDESIGNED; LEGACY-017 sigue PARTIAL
          total 79 = 5 equivalentes + 26 rediseñadas + 12 parciales + 6 faltantes
                     + 5 bloqueadas + 6 consolidadas + 19 desconocidas
                     (recuento corregido en el cierre técnico V1: LEGACY-041 en PARTIAL)
```

**Limitaciones:** el runtime sigue siendo `opdatos` (`TARGET_LEAST_PRIVILEGE_MIGRATION =
PENDING`); la confirmación no ejecuta descargos/PEPS/saldos.

**Artefactos producidos:** UI de confirmación (modelo, repositorio, API, casos de uso,
página y tests), migrations aplicadas, command desplegado, matriz de paridad actualizada.

### Anexos candidatos (cierre)

captura de la UI de confirmación; matriz de errores SQL → HTTP; diagrama del pipeline
final (staging → validación → confirmación autoritativa); resultado de la prueba de
concurrencia `MAX+1`.

### CI en feature branches

Se habilitó validación pre-integración para ramas `feature/**`
(`FEATURE_BRANCH_CI = PRE_INTEGRATION_VALIDATION`) y se hizo obligatoria la prueba
de concurrencia en CI (skip prohibido cuando `CI=true`).

Evidencia observada en GitHub Actions (commit `466e64c`):

```text
sp-first-gate = SUCCESS
backend = SUCCESS
frontend = SUCCESS
CONCURRENCY_TESTS_EXECUTED = 2
CONCURRENCY_TESTS_SKIPPED = 0
CONCURRENCY_TESTS_FAILED = 0
maxPlusOneSinBloqueoPuedeColisionar = PASSED
bloqueoDeTablaCoordinaConSesionLegacySinConocerElBloqueo = PASSED
```

La prueba se ejecutó en un contenedor SQL Server efímero del runner Linux; no usa
`backend/.env`, ni `CALE_IMMEX`, ni red corporativa. En Windows local se omite
cuando el endpoint Docker es incompatible (`SKIP LOCAL` documentado).

### Contexto para el capítulo 3 (métodos y técnicas)

Auditoría funcional legacy; análisis de procedimientos almacenados y call graph;
arquitectura hexagonal con puertos/adaptadores; staging aislado; política SP-FIRST
con gate automatizado; reconciliación read-only contra LIVE.

### Contexto para el capítulo 4 (análisis de resultados)

7 procedimientos y 21 tablas auditados; 4 flujos de escritura identificados; 1 regla
de duplicado (`PED-007`) confirmada; 6 riesgos técnicos documentados. La
clasificación de paridad evolucionó y su estado vigente vive en la sección
«Cierre V1 — confirmación autoritativa integrada en `dev`» de este documento
(`LEGACY-016/019/057 = IMPLEMENTED_REDESIGNED`, `LEGACY-017 = PARTIAL`,
total 79).

### Anexos potenciales

Diagrama del pipeline legacy, call graph simplificado, matriz SP→tabla/acción,
matriz de validaciones (`PED-001..007`), resultados del gate SP-FIRST.

---

## Cierre V1 — confirmación autoritativa integrada en `dev`

Evidencia factual consolidada (sin narrativa adicional; reemplaza cualquier
conteo anterior que contradiga esta distribución):

```text
commit dev                 = a64434a72e5799605098afd4a9481a479f4bf367
CI final dev (run)         = 37014148994 — SUCCESS (sp-first-gate, backend, frontend)
frontend                   = 113/113 tests PASS · lint PASS · build PASS
PedimentoConfirmacionSqlIT = 15/15 PASS · 0 skipped · 0 failed
concurrencia MAX+1         = 2/2 PASS · 0 skipped · 0 failed
SP-FIRST                   = violations = 0
paridad vigente            = 5 equivalentes + 26 rediseñadas + 12 parciales
                             + 6 faltantes + 5 bloqueadas + 6 consolidadas
                             + 19 desconocidas = 79
```

Nota de corrección (cierre técnico V1): el conteo anterior de parciales y
desconocidas quedó desactualizado al no reflejar `LEGACY-041` en `PARTIAL`; el
recuento fila por fila vigente es `12 parciales / 19 desconocidas` (ver
`docs/05-pruebas/v1-technical-closure.md`).

El detalle de las decisiones de negocio pendientes y las métricas de cierre
viven en `docs/01-requerimientos/cierre-alcance-v1.md`.

---

## Fase — Reauditoría LEGACY-048 (scrap / desperdicio read-only)

**Fecha / fase:** paridad V1, rama `feature/legacy-scrap-report-v1`.

**Problema técnico abordado:** `LEGACY-048` seguía `UNKNOWN`; existía una
auditoría previa (rama `feature/legacy-waste-report-v1`) sin contrato
suficiente. Se reauditó para decidir si existe una consulta read-only
implementable.

**Método utilizado:** barrido read-only LIVE sobre `CALE_IMMEX` (`sys.objects`,
`sys.sql_modules`, `sys.columns`, `sys.sql_expression_dependencies`,
`sys.parameters`, `OBJECT_DEFINITION`); conteos agregados; ningún objeto mutable
ejecutado.

**Resultados:**

- sin objeto ni definición que use el nombre `Scrap`; sin SP read-only de
  desperdicio;
- 15 datasets de desperdicio con 0 filas; `descarga.Desperdicio` NULL en
  3.866/3.866 filas;
- única reutilización read-only existente: `APP24_Q_VENCIMIENTOS_LISTAR →
  vDESPERDICIOS` (LEGACY-046);
- `NEW_SP_REQUIRED = NOT_PROVEN`: falta contrato de pantalla (columnas,
  filtros, grano, caso de aceptación).

**Resultado:** `SCRAP_CONTRACT = PARTIAL` y `LEGACY-048 = UNKNOWN` (sin
cambio); no se implementó SP, endpoint ni frontend.

**Limitaciones:** la evidencia del repositorio y de la metadata LIVE no
incluye una pantalla legacy denominada Scrap.

**Artefactos producidos:** `docs/03-diseno/mapeo-desperdicios.md` (segunda
pasada); `docs/01-requerimientos/matriz-paridad-legacy-v1.md` (nota de
reauditoría).

---

## Fase — Reauditoría LEGACY-060 (actas de destrucción, sin implementación)

**Fecha / fase:** paridad V1, rama `feature/legacy-destruction-acts-staging-v1`.

**Problema técnico abordado:** `LEGACY-060` seguía `MISSING`; la auditoría previa
de operaciones especiales no cerró layout. Se reauditó para decidir si existía
una V1 de carga/preview implementable.

**Método utilizado:** barrido read-only LIVE sobre `CALE_IMMEX` (`sys.objects`,
`sys.columns`, `sys.sql_modules`, `sys.sql_expression_dependencies`,
`OBJECT_DEFINITION`, conteos); ningún objeto mutable ejecutado.

**Resultados:**

- `CARGAACTAS` (sin parámetros) lee el stage global `dbo.Acta` y escribe
  `salidas`/`psalidas`/`dirigido` con claves `MAX+1`, sin transacción y sin
  validaciones;
- `ERRORACTA` no existe en LIVE y su limpieza está comentada: sin contrato de
  errores;
- no existe superficie de archivo (tabla de carga por archivo/hash/lote);
- `dbo.Acta` = 0 filas.

**Resultado:** `ACTAS_LAYOUT_CONTRACT = NOT_SUFFICIENT`; `LEGACY-060 = MISSING`
(sin cambio); no se implementó staging, API ni UI.

**Limitaciones:** la evidencia LIVE no incluye el archivo/pantalla de entrada ni
reglas de validación legacy (inexistentes en el SP).

**Artefactos producidos:** `docs/03-diseno/mapeo-operaciones-especiales.md`
(segunda pasada); `docs/01-requerimientos/matriz-paridad-legacy-v1.md` (nota).

---

## Fase — Reporte read-only F4 (CTM / desperdicio, LEGACY-041 parcial)

**Fecha / fase:** paridad V1, rama `feature/legacy-ctm-f4-report-v1`.

**Problema técnico abordado:** `LEGACY-041` (`CTM/F4/HDE`) estaba `UNKNOWN` sin
proyección read-only aprobada; se auditó `V_F4CTMA`/`V_F4DESP` para decidir si
existía al menos una consulta implementable.

**Método utilizado:** barrido read-only LIVE (objetos, definiciones, columnas,
conteos, dependencias, parámetros) sobre `CALE_IMMEX`; sin ejecutar objetos
mutables; pruebas SQL sobre SQL Server efímero con fixtures sintéticos.

**Resultados:**

- `V_F4CTMA`/`V_F4DESP` son vistas read-only de líneas dirigidas F4/A3
  (`TipoDescarga` CTMAPAA/DESP) con grano `dirigidokey` y 0 filas en LIVE;
- todo el resto del subsistema CTM (`LIGACTMA`, `SALDOSCTM`, `CTMDESCARGA`,
  `DESCARGACTMF`, `V_INFORME_F4_CTMAPAA`) es mutable o sin contrato; no existe
  objeto HDE (`HDE_CONTRACT = NOT_FOUND`);
- implementado `GET /api/v1/reportes/f4` + exportación XLSX vía
  `dbo.APP24_Q_F4_LISTAR` (nuevo SP read-only) y opción `F4 (CTM / desperdicio)`
  en `/reportes`.

**Resultado:** `LEGACY-041`: `UNKNOWN → PARTIAL`; `LIVE_ROWS = 0` documentado.

**Limitaciones:** el proceso CTM, HDE y el informe `V_INFORME_F4_CTMAPAA`
quedan fuera de alcance; el SP requiere deployment LIVE controlado antes de
usarse en runtime.

**Artefactos producidos:** `infra/sql/procedures/queries/APP24_Q_F4_LISTAR.sql`;
`docs/03-diseno/mapeo-reportes-extendidos.md` (sección F4 V1);
`docs/01-requerimientos/matriz-paridad-legacy-v1.md` (LEGACY-041).

---

## Fase — Cierre técnico V1 (auditoría E2E de la app nueva + correcciones + inventario)

**Fecha / fase:** cierre V1. Ramas `test/functional-e2e-v1` y
`feature/e2e-v1-fixes` integradas a `dev` por fast-forward (`3c00e19`);
consolidación documental en `release/v1-technical-closure`.

**Problema técnico abordado:** verificar de punta a punta la aplicación nueva
(no el legacy), corregir los hallazgos P1/P2 encontrados y determinar si V1
queda técnicamente listo para entrega bajo los contratos actuales, separando
pendientes técnicos de decisiones de negocio y de evidencia externa.

**Método utilizado:** sweep E2E interactivo con Playwright/Edge headless sobre
backend aislado `18082` y frontend `4300` con proxy, autenticando con JWT
HS256 efímeros (TTL 30 min, no impresos) por perfil de permisos y fixtures
sintéticos `E2E_*`; validación de XLSX descargados con `openpyxl`; RBAC por API
y browser; regresión focalizada post-fix y post-merge; verificación read-only
de objetos SQL LIVE (`sys.objects` vía sqlcmd, sin DDL); gates locales SP-FIRST,
runtime permission, backend `test build` y frontend `test lint build`; CI de
`dev`.

**Resultados:**

- E2E: rutas 20/21 PASS (el único desvío era el hallazgo P1); API 25/25
  esperado (23×200, 401 esperado, 400 esperado, 0×5xx); reportes 11/11 con
  paginación real en 6 y empty states limpios en los vacíos; uploads staging
  con hash y duplicados 409 en pedimentos/materiales/productos/facturación, sin
  ninguna confirmación ejecutada; exports operativos descargados y abiertos;
  responsive 40/40 sin overflow ni errores de consola/página.
- Hallazgos: `FUN-E2E-003` (P1, guard de rutas hijas permitía
  `/catalogos/importaciones` con perfil de sólo lectura) y `FUN-E2E-002` (P2,
  partidas `"4.0"` visibles) corregidos con tests y verificados; `FUN-E2E-001`
  y `FUN-E2E-004` (P3) diferidos y documentados.
- Integración: CI `dev` en run `37123546803` SUCCESS; regresión post-merge
  4/4 RBAC y partidas sin sufijo `.0`.
- Inventario de despliegue LIVE: `CALE_IMMEX` 24/24 SP, `ANEXO24_DEV` 35/35 SP
  y 18/18 tablas `app24`; 0 missing / 0 extra.
- Snapshot de matriz recalculado fila por fila: 5 equivalentes + 26 rediseñadas
  + 12 parciales + 6 faltantes + 5 bloqueadas + 6 consolidadas + 19 desconocidas
  = 79; corrige el agregado anterior (11/20) que no reflejaba `LEGACY-041`
  `PARTIAL`.

**Métricas:** frontend 131/131 tests; backend `test build` PASS con ITs;
SP-FIRST violations 0; runtime permission CALE 24/24, APP 35/35;
`P0_TECHNICAL_ACTIONABLE_REMAINING = 0`; `P1_TECHNICAL_ACTIONABLE_REMAINING = 0`;
pendientes de negocio 4 temas / 25 ítems; evidencia externa 12; POST_V1 9;
P3 2; `CALE_IMMEX` writes 0 y SP mutables ejecutados 0 durante la fase.

**Limitaciones:** login con credenciales reales queda
`OPERATOR_ACCEPTANCE_PENDING`; CRUD de administración no se mutó en E2E; el
fixture sintético de pedimentos no referencia un material real (`PED-003`
esperado y correcto); los P3 `FUN-E2E-001/004` permanecen abiertos por decisión.

**Artefactos producidos:** `docs/05-pruebas/auditoria-funcional-e2e-v1.md`;
`docs/05-pruebas/v1-technical-closure.md`; specs nuevas
(`permission.guard.spec.ts`, `operation-formatters.spec.ts`,
`report-list.page.spec.ts`); fixes `permission.guard.ts`,
`operation-formatters.ts` y `report-list.page.ts`; corrección de conteos en
`docs/01-requerimientos/matriz-paridad-legacy-v1.md`.

---

## Cobertura documental del reporte de estadía

Estado de la evidencia disponible por capítulo (fuente consolidada: este
archivo + documentos citados). Los gaps marcados son reales y no se cubren con
resultados inventados.

| Capítulo | Evidencia disponible | Estado |
|---|---|---|
| Introducción / Antecedentes | `antecedentes.md`, `alcance.md`; fases tempranas de este archivo (auditorías legacy, descubrimiento SQL) | Cubierto |
| Problema | `planteamiento-problema.md`; fases de auditoría read-only y bloqueos de negocio | Cubierto |
| Objetivos | `objetivos.md`; criterios de salida en `v1-technical-closure.md` | Cubierto |
| Método | Fases de este archivo (método por fase); `docs/05-pruebas/plan-pruebas.md`; gates SP-FIRST/runtime | Cubierto |
| Resultados | Matriz de paridad recalculada; `v1-technical-closure.md` (módulos, reportes, uploads, SQL LIVE); `auditoria-funcional-e2e-v1.md`; runs CI | Cubierto |
| Conclusiones | Pendiente de redacción final | GAP intencional: depende de respuestas de negocio (`decisiones-alcance-v1.md`) |
| Anexos | `anexos-candidatos.md` (15 candidatos) | Parcial: faltan capturas UI versionadas y fixtures XLSX (regenerables) |

Gaps adicionales declarados:

1. Aceptación de operador pendiente: login con credenciales reales
   (`OPERATOR_ACCEPTANCE_PENDING`) y sesión de validación con usuarios de
   negocio.
2. Sin evidencia de uso en producción real: toda la verificación es en
   ambiente DEV/local con datos LIVE de solo lectura y staging sintético.
3. Respuestas de negocio pendientes (Saldos, Descargos/PEPS, Facturación
   confirmación, Dashboard, PED-005/006) — sin ellas no se redactan
   conclusiones de alcance cerrado.
4. Capturas de pantalla UI no recolectadas como anexo.

---

## Fase — Evidencia visual y anexos RC1

**Fecha / fase:** preparación de entrega, ramas `docs/reporte-estadia-annexes`
(borrador del reporte integrado a `dev` previamente, `dev` = `cb1e918`, CI run
`37125329238` SUCCESS).

**Problema técnico abordado:** el reporte requería evidencia visual y anexos
reproducibles sin exponer datos empresariales ni afirmar uso productivo.

**Método utilizado:** capturas reproducibles con Playwright/Edge headless en
entorno local (`1440x900`), sesión JWT sintética (usuario “Anexos Demo”);
interceptación de los GET sensibles con respuestas sintéticas o páginas vacías
para listados; fixtures `.xlsx` 100 % sintéticos validados con `openpyxl`;
uploads de los fixtures contra staging (`ANEXO24_DEV`) sin confirmaciones;
revisión de privacidad por muestreo visual de las capturas de mayor riesgo.

**Resultados:**

- 22 capturas sanitizadas en `docs/08-reporte-estadia/anexos/capturas/`
  (login, dashboard, catálogos, operaciones, flujos de carga, reportes,
  administración).
- 5 fixtures sintéticos validados en `docs/08-reporte-estadia/anexos/fixtures/`
  (apertura, hojas, headers y filas verificados; sin datos sensibles).
- Índice de anexos actualizado (`anexos-candidatos.md`) y figuras candidatas
  para el cuerpo (`figuras-candidatas.md`, 7 figuras).
- Diagramas revisados: arquitectura y flujo de carga `USE`; ERv3 `USE` como
  propuesta; ERv1/v2 `NOT_NEEDED`; flujo de confirmación y hexagonal
  `REGENERATE`.

**Flags:**

```text
ANNEX_SCREENSHOTS_COMPLETE = YES
SYNTHETIC_FIXTURES_COMPLETE = YES
PRIVACY_REVIEW = PASS
FINAL_BUSINESS_CONCLUSION_PENDING = YES
```

**Limitaciones:** las capturas de listados muestran datos sintéticos o estados
vacíos (no datos LIVE); los uploads de evidencia crearon cargas sintéticas en
staging `app24` (retenidas para auditoría, sin confirmación).

**Artefactos producidos:** `docs/08-reporte-estadia/anexos/` (capturas y
fixtures), `figuras-candidatas.md`, `anexos-candidatos.md` actualizado. No se
modificó funcionalidad ni SQL; `CALE_IMMEX` sin escrituras.

---

## Fase — Congelación RC1 (integración documental y punto de referencia)

**Fecha / fase:** cierre de entrega. Ramas `docs/v1-delivery-preparation`
integrada a `dev` por fast-forward; rama `docs/reporte-estadia-v1` para el
borrador del reporte.

**Problema técnico abordado:** integrar el paquete documental de entrega
(decisiones ejecutivas, anexos candidatos, cobertura documental) y congelar un
punto de referencia verificable de V1 sin mover el alcance funcional.

**Método utilizado:** fast-forward de `dev` sobre la rama documental, CI de
`dev`, verificación del snapshot consolidado (recuento de matriz fila por fila),
creación de tag anotado sobre `origin/dev` y arranque del borrador del reporte
de estadía a partir de evidencia consolidada.

**Resultados:**

- `dev` = `fb77be0` con CI run `37124761648` SUCCESS (sp-first-gate, backend,
  frontend).
- Tag `v1.0.0-rc1` = `ecb2e3510ea63ea0b3b76597df3848c090f169fe` (anotado, no
  release productiva).
- Snapshot verificado: 79 capacidades (5/26/12/6/5/6/19),
  `P0/P1_technical_remaining = 0`, SQL LIVE 24/24 + 35/35 + 18/18, E2E con
  P0/P1/P2 = 0 y P3 = 2.
- Paquete ejecutivo de decisiones y checklist de capturas/fixtures listos.

**Limitaciones:** desarrollo funcional V1 congelado hasta recibir decisión de
negocio o evidencia externa; aceptación de operador pendiente; sin evidencia de
uso productivo.

**Artefactos producidos:** `docs/01-requerimientos/decisiones-alcance-v1.md`
(paquete ejecutivo); `docs/08-reporte-estadia/anexos-candidatos.md`
(checklist de capturas y fixtures); borrador del reporte en
`docs/08-reporte-estadia/`.
