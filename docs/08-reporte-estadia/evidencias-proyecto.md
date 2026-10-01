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
