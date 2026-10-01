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

### Contexto para el capítulo 3 (métodos y técnicas)

Auditoría funcional legacy; análisis de procedimientos almacenados y call graph;
arquitectura hexagonal con puertos/adaptadores; staging aislado; política SP-FIRST
con gate automatizado; reconciliación read-only contra LIVE.

### Contexto para el capítulo 4 (análisis de resultados)

7 procedimientos y 21 tablas auditados; 4 flujos de escritura identificados; 1 regla
de duplicado (`PED-007`) confirmada; 6 riesgos técnicos documentados; clasificación
de paridad sin cambios (`LEGACY-016/017/019 = PARTIAL`, `LEGACY-057 = MISSING`).

### Anexos potenciales

Diagrama del pipeline legacy, call graph simplificado, matriz SP→tabla/acción,
matriz de validaciones (`PED-001..007`), resultados del gate SP-FIRST.
