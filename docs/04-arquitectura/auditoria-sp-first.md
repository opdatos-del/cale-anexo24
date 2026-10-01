# Auditoría SP-FIRST y endurecimiento de acceso a SQL

## A) Java compliance

```text
DATABASE_ACCESS_POLICY = SP_FIRST_STRICT
JAVA_FUNCTIONAL_INLINE_SQL = 0
DIRECT_TABLE_ACCESS_JAVA = 0
DIRECT_VIEW_ACCESS_JAVA = 0
INLINE_WRITE_SQL_JAVA = 0
TECHNICAL_EXCEPTIONS = 1 (TECHNICAL_SQL_EXCEPTION_001)
```

Todo acceso funcional a SQL Server pasa por Stored Procedure. Java no contiene
SQL funcional inline ni accede a tablas o vistas de negocio directamente.

No existe JPA, Hibernate, `EntityManager`, `@Query` ni consultas nativas
anotadas. Los 35 adapters usan `JdbcTemplate` con `call(...)` o
`query(PreparedStatementCreator, RowMapper)` sobre `{call <SP>}`. Los nombres de
SP (`"dbo.APP24_Q_X"`) y la plantilla `"{call " + PROCEDURE + "(?, ?)}"` no son
SQL inline: son invocación de SP.

### Inventario

```text
persistence classes (Adapter) = 35
DB access methods (Java con BD) = 54 (53 SP + 1 technical)
SP call sites (prepareCall) = 53, un sitio por método
direct SQL call sites = 1
```

Tabla de adapters: todos los métodos listados en la primera pasada siguen
clasificados `SP_APP24_JUSTIFIED`; ninguno ejecuta SQL de negocio inline. El
detalle por adapter no cambió. `SystemStatusController.checkDatabase` es el único
`TECHNICAL_EXCEPTION`.

## B) APP24 SP safety

```text
APP24_Q total = 42 (22 dbo + 20 app24)
direct DML violations = 0
mutable callees = 0
dynamic SQL = 0
APP24_Q_TRANSITIVE_READ_ONLY = PASS
```

Se reconstruyó el call graph transitivo desde `sys.sql_modules`. Los 42 `APP24_Q_*`
tienen DML directo `= 0`, cero callees (`callees=[]` en cada uno), cero llamadas a
SP mutable y cero SQL dinámico. No hay `INSERT`/`UPDATE`/`DELETE`/`MERGE`/
`TRUNCATE`/`EXEC` en su definición.

Los 15 `APP24_C_*` son mutables por diseño y sólo los invocan casos de uso de
escritura.

```text
APP24_C total = 15
boundary findings = 0
```

Verificación de frontera: los commands de staging escriben en `app24`
(`ANEXO24_DEV`); los de dominio usan `APP24_C_*` de `app24`. No se observó ningún
`APP24_C_*` que escriba de forma inesperada en `CALE_IMMEX` ni DML fuera de su
esquema esperado. Todos versionados.

## C) Legacy call graph

Se auditó **todo** `CALE_IMMEX.dbo` (111 procedimientos) construyendo el grafo de
llamadas con `sys.sql_expression_dependencies` y el texto de `sys.sql_modules`
(para `EXEC`/`EXECUTE` y `sp_executesql`). No se ejecutó ningún SP.

Clasificación directa (DML + `EXEC`, incluyendo `SELECT ... INTO` permanente y
DDL de objeto permanente; se excluyeron DML sobre temporales `#` y el header
`CREATE PROCEDURE`) y luego propagación transitiva por el grafo.

```text
procedures = 111
edges = 26
READ_ONLY = 23
WRITE = 0
MIXED = 84
MIXED_TRANSITIVE = 4
DYNAMIC_SQL_UNKNOWN = 0
UNKNOWN = 0
```

`READ_ONLY` = 22 `APP24_Q_*` propios + `dbo.NUEVOFOLIOB`.

La primera pasada (búsqueda superficial sólo de DML directo) reportó 27 `READ` y
84 `MIXED`. El recálculo transitivo corrige 4 procedimientos:

| Procedimiento | Superficial | Final transitivo | Evidencia de callees |
|---|---|---|---|
| `DESCARGATSALIDA` | READ (incorrecto) | MIXED_TRANSITIVE | `EXEC DESCARGATSALIDA1` (MIXED), `EXEC descargatsalida2` |
| `DESCDIRIGIDA` | READ (incorrecto) | MIXED_TRANSITIVE | `EXEC SALDOSDIRIGIDOS` (MIXED) |
| `HOJATRABAJOCTMDESPA31` | READ (incorrecto) | MIXED_TRANSITIVE | `EXEC PROC_DESCARGASF4CTMA` |
| `HOJATRABAJODESCARGOSA31` | READ (incorrecto) | MIXED_TRANSITIVE | `EXEC PROC_HISTORIADESCARGASALIDA` |

Chequeos manuales:

```text
DESCARGATSALIDA = MIXED_TRANSITIVE (aplica descargas de material; no reusable)
DESCDIRIGIDA = MIXED_TRANSITIVE (cursor de operaciones dirigidas; no reusable)
SALDOSDIRIGIDOS = MIXED (DML directo)
DESCARGATSALIDA1 = MIXED
DESCARGASALIDAPEPS = MIXED (EXEC DESCDIRIGIDA, SALDOS)
DESCARGATODOSDIRIGIDOS = MIXED (EXEC DESCDIRIGIDA)
HOJATRABAJODESCARGOSA31 = MIXED_TRANSITIVE
HOJATRABAJOCTMDESPA31 = MIXED_TRANSITIVE
NUEVOFOLIOB = READ_ONLY (212 chars; calcula MAX(folio)+1 sobre DESCARGOSBLOQUEADOS,
  sin INSERT/UPDATE/DELETE/EXEC/SET de tabla, sin cursor)
```

```text
LEGACY_CALL_GRAPH_CLASSIFIED = PASS
```

Ningún procedimiento de descargos/dirigidos queda `READ_ONLY`. `NUEVOFOLIOB`
permanece `READ_ONLY` con evidencia directa: sólo lee y proyecta un folio.

## D) Reuse decisions

Sólo los procedimientos finales `READ_ONLY` son candidatos a reutilización. Los
23 `READ_ONLY` son 22 `APP24_Q_*` (propios) y `NUEVOFOLIOB`. El único legacy
read-only (`NUEVOFOLIOB`) calcula el siguiente folio de `DESCARGOSBLOQUEADOS` y
no cubre ninguna necesidad de catálogo/reporte; no es reutilizable para las
consultas V1.

| APP24 SP | Need | Legacy candidate | Final classification | Contract match | Decision |
|---|---|---|---|---|---|
| `APP24_Q_*_LISTAR` (catálogos, operaciones, reportes) | consulta read-only | ninguno `READ_ONLY` compatible | MIXED / MIXED_TRANSITIVE / otro dominio | NO | KEEP_APP24 |
| `APP24_Q_DATOS_GENERALES_OBTENER` | ficha singleton | ninguno | — | NO | KEEP_APP24 |
| `APP24_Q_PEDIMENTO_VALIDAR_REGLAS` | validación read-only de staging | `VALIDAPEDIMENTO`, `VALIDA_I_DETALLENP` | MIXED | NO | KEEP_APP24 |
| `APP24_C_*` | comandos atómicos `app24` | `CARGA_*`, `CARGAPEDIMENTOS` | MIXED/legacy compartido | NO | KEEP_APP24 |
| `APP24_Q_*` staging `app24` | staging aislado | no aplica | — | NO | KEEP_APP24 |

```text
REUSE_LEGACY = 0
KEEP_APP24 = 57
REFACTOR_APP24 = 0
NEEDS_ANALYSIS = 0
```

Motivo del `KEEP_APP24` (concreto, no por nombre): los `APP24_Q_*` aportan
paginación, `COUNT_BIG`, proyección explícita sin `SELECT *`, normalización de
filtros, orden determinista, aislamiento de esquema (staging en `app24`) y
contrato estable de DTO; ninguno de los candidatos legacy `READ_ONLY` cubre esos
requisitos ni cumple contrato+parámetros+resultado.

## E) Version/LIVE reconciliation

Comparación `sys.sql_modules` (LIVE) contra `infra/sql/procedures/*` con
normalización estricta (comentarios, `USE`/`GO`, `SET ANSI_NULLS`/
`QUOTED_IDENTIFIER`, `CREATE|ALTER PROCEDURE`, espaciado).

```text
DEFINITION_MATCH = PASS (57/57)
GRANT_EXTERNAL_TO_DEFINITION = YES
NOT_APPLIED = 0
```

Diferencias clasificadas:

- `COSMETIC_WHITESPACE`: espaciado tras comas y `CREATE` vs `CREATE OR ALTER`.
- `ENCODING_ONLY`: acentos en literales de mensaje se comparan por su texto
  normalizado; no cambian el contrato.
- `GRANT_EXTERNAL_TO_DEFINITION`: los archivos
  `APP24_C_CATALOGO_MATERIAL_CARGA_CREAR.sql` y
  `APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR.sql` incluyen un
  `GRANT EXECUTE ... TO app24_runtime` que no forma parte de
  `OBJECT_DEFINITION`. No es mismatch de definición.
- `SEMANTIC_DIFF`: ninguna tras el análisis.

### APP24_Q_PEDIMENTO_VALIDAR_REGLAS

```text
repo/live difference = sólo CREATE vs CREATE OR ALTER + USE/SET (normalización)
authoritative contract = repositorio (idéntico a LIVE en cuerpo)
action = ninguna (no se reaplica ni se edita)
semantic match = PASS
PEDIMENTO_VALIDAR_REGLAS_RECONCILED = PASS
```

El cuerpo versionado conserva PED-001 (unidad comercial), PED-002 (unidad
tarifa), PED-003 (material para `TipoOperacion='1'`), PED-004 (producto para
`TipoOperacion='2'`) y el contrato XML `@FilasXml` probado. La comparación
exacta tras normalización devuelve `EQUAL=true` (1573 = 1573). No hubo divergencia
funcional real; el hallazgo previo fue artefacto del normalizador.

## SP usage accounting

```text
DB access methods = 54 (53 SP + 1 technical)
call sites = 54 (53 prepareCall + 1 queryForObject)
unique SP references = 57 (22 dbo + 20 app24_Q + 15 app24_C)
generic call sites = 4
SP_USAGE_ACCOUNTING = PASS
```

Explicación de `57 SP != 53 prepareCall sites`: 4 sitios genéricos en
`CatalogImportJdbcAdapter` seleccionan el SP por tipo
(`MATERIAL`/`PRODUCTO`):

- `existsByHash` → `..._MATERIAL_CARGA_POR_HASH` / `..._PRODUCTO_CARGA_POR_HASH`
- `save` → `..._CARGA_CREAR` (2)
- `findById`/`callDetail` → `..._CARGA_OBTENER` (2)
- `findErrors` → `..._CARGA_ERRORES` (2)

49 sitios fijos (1 SP cada uno) + 8 SP en 4 sitios genéricos = 57 SP únicos en 53
sitios. Cada método con `prepareCall` tiene exactamente un sitio (53 métodos = 53
sitios), más el método técnico con `queryForObject`.

## F) Enforcement

Se creó un gate automático (ya no es audit-only):

```text
scanner = scripts/check-inline-sql.py
scanner tests = scripts/tests/check_inline_sql_test.py (+ fixtures pass/fail)
CI gate = job sp-first-gate en .github/workflows/ci.yml (antes de backend)
INLINE_SQL_CI_GATE = PASS
allowlist =
  backend/.../shared/api/SystemStatusController.java
    INLINE_SQL: "SELECT 1" (exacto)
    DIRECT_JDBC: permitido (health-check)
```

El scanner:
- escanea `backend/src/main/java/**/*.java`;
- distingue literales de código y comentarios (ignora Javadoc/comentarios);
- marca `INLINE_SQL` sólo si el literal parece sentencia (evita verbos HTTP como
  `"DELETE"` de CORS);
- marca `DIRECT_JDBC` para `query(`, `queryForObject(`, `queryForList(`,
  `update(`, `batchUpdate(`, `execute(`, `createStatement(`, `prepareStatement(`;
- NO marca `connection.prepareCall("{call ...}")` ni `jdbcTemplate.call(...)`;
- NO marca `.query(PreparedStatementCreator, mapper)` cuando el sitio usa
  `prepareCall` (invocación de SP).

Resultado real: `SP_FIRST_GATE|PASS|violations=0`. Los tests de fixtures validan
PASS (`prepareCall` SP, `SELECT 1` allowlisted) y FAIL (`SELECT FROM`,
`UPDATE`, `query`, `StringBuilder` SQL).

### TECHNICAL_SQL_EXCEPTION_001

```text
archivo = backend/src/main/java/com/jovycandy/anexo24/shared/api/SystemStatusController.java
literal = "SELECT 1"
scope = health only
business data = none
side effects = none
```

Única excepción permitida. Cualquier futura excepción requiere documentación,
justificación, allowlist exacta y revisión explícita. No se permite libremente el
archivo completo.

## Estado final

```text
JAVA_FUNCTIONAL_INLINE_SQL = 0
APP24_Q_TRANSITIVE_READ_ONLY = PASS
LEGACY_CALL_GRAPH_CLASSIFIED = PASS
SP_USAGE_ACCOUNTING = PASS
PEDIMENTO_VALIDAR_REGLAS_RECONCILED = PASS
SP_FIRST_AUDIT_COMPLETE = YES
SP_FIRST_COMPLIANT = YES
LEGACY_SP_EXECUTED = 0
CALE_IMMEX_WRITES = 0
```
