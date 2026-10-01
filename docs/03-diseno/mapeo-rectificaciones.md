# Mapeo de Rectificaciones Read V1

## Alcance y seguridad

La V1 implementa únicamente la consulta paginada y filtrable del resumen
read-only de rectificaciones. No implementa alta, aplicación, confirmación,
procesamiento, ajuste de pedimentos ni exportación XLSX.

La auditoría LIVE se realizó sobre metadata, definiciones, dependencias y
conteos agregados de `CALE_IMMEX`. No se ejecutaron procedimientos legacy
mutables ni se escribieron tablas de negocio.

```text
RECTIFICATIONS_SUMMARY_CONTRACT = CONFIRMED
RECTIFICATIONS_DETAIL_CONTRACT = NOT_IMPLEMENTED / DATASET_EMPTY
RECTIFICATION_VIEW_RELATION = PARTIAL
```

## Fuentes legacy

| Fuente | Clasificación | Contrato observado |
|---|---|---|
| `dbo.v_total_rectificaciones` | READ SOURCE / resumen | Una fila por `pedimento` distinto de `dbo.v_operaciones`; `Total` cuenta pedimentos distintos cuyo `pedimentooriginal` coincide con ese pedimento |
| `dbo.v_rectificaciones` | READ SOURCE / detalle | Filas distintas con pedimento rectificado, clave, descarga, pedimento original, existencia, clave/descarga original y `Status` calculado |
| `dbo.v_operaciones` | READ SOURCE / vista derivada | Depende de `Importaciones`, `partidas`, `psalidas` y `salidas` |
| `rectificacionImport` | MUTABLE PROCESS SOURCE | Tabla relacionada identificada por metadata; no se modifica |
| `rectificacionExport` | MUTABLE PROCESS SOURCE | Tabla relacionada identificada por metadata; no se modifica |

Las dos vistas dependen de `v_operaciones`, pero no son detalle y agregado de
una misma proyección 1:1. `v_rectificaciones` filtra pedimentos con
`Pedimento Original` no vacío, mientras `v_total_rectificaciones` conserva la
proyección distinta de pedimentos y calcula el agregado de relaciones.

## Contrato de columnas

### `dbo.v_total_rectificaciones`

| Columna | Tipo | Nullable | Significado | Estado |
|---|---|---:|---|---|
| `pedimento` | `varchar(60)` | Sí | Identidad textual del pedimento proyectado por `v_operaciones` | `CONFIRMED_MEANING` |
| `Total` | `int` | No | Conteo de pedimentos distintos cuyo `pedimentooriginal` referencia el pedimento de la fila | `CONFIRMED_MEANING` |

### `dbo.v_rectificaciones`

| Columna | Tipo | Nullable | Significado | Estado |
|---|---|---:|---|---|
| `Pedimento` | `varchar(60)` | Sí | Pedimento de la operación rectificada | `CONFIRMED_MEANING` |
| `Clave Pedimento` | `varchar(5)` | Sí | Clave de la operación rectificada | `CONFIRMED_MEANING` |
| `Descarga` | `varchar(2)` | Sí | Indicador de descarga de la operación rectificada | `PARTIAL` |
| `Pedimento Original` | `varchar(20)` | Sí | Pedimento original referenciado | `CONFIRMED_MEANING` |
| `Existe Pedimento` | `varchar(10)` | No | Resultado de búsqueda del pedimento original en `v_operaciones` | `CONFIRMED_MEANING` |
| `Clave Pedimento Original` | `varchar(5)` | No | Clave encontrada para el pedimento original | `CONFIRMED_MEANING` |
| `Descarga Original` | `varchar(2)` | No | Descarga encontrada para el pedimento original | `PARTIAL` |
| `Status` | `varchar(23)` | No | Advertencia cuando descarga y descarga original coinciden | `CONFIRMED_MEANING` |

El detalle tuvo `0` filas LIVE durante la auditoría. Sus columnas se conservan
como evidencia, pero no se exponen en esta V1 ni se infiere un contrato de
consulta detalle a partir de un dataset vacío.

## Procedimientos relacionados

| Procedimiento | Clasificación | Decisión |
|---|---|---|
| `CARGAPEDIMENTOS` | `MIXED` / proceso | No ejecutar |
| `CARGA_ENCABEZADOS` | `MIXED` / proceso | No ejecutar |
| `INDICAOPERACION` | `MIXED` / proceso | Sólo auditado; no ejecutar |
| `INSERTAPEDIMENTO` | `MIXED` / escritura | No ejecutar; proceso autoritativo fuera de V1 |
| `PR_CompulsaDSA24` | `MIXED` | No relacionado con la consulta V1; no ejecutar |

La búsqueda de objetos relacionados también encontró vistas y funciones de
lectura, pero ninguna se incorporó al contrato porque no aporta campos o
filtros necesarios para el resumen.

## Nueva consulta read-only

- SP versionado: `dbo.APP24_Q_RECTIFICACIONES_LISTAR`.
- Fuente: `dbo.v_total_rectificaciones`.
- API: `GET /api/v1/reportes/rectificaciones`.
- Permiso: `REPORTES_GENERAR`.
- Filtro: `filtro` aplicado sólo a `pedimento`.
- Paginación: `pagina` 1-based; `tamano` entre 1 y 100.
- Total: `@Total` cuenta después del filtro y antes de paginar.
- Orden: `CASE WHEN pedimento IS NULL THEN 0 ELSE 1 END, pedimento`; no se inventa una PK.
- XLSX: `NOT_IMPLEMENTED`.
- Procesamiento: no hay command ni endpoint de confirmación.

## UI

La opción `Rectificaciones` vive dentro de `/reportes`, comparte la superficie
consolidada y muestra:

- filtro opcional por pedimento;
- total y paginación;
- columna de pedimento;
- número de rectificaciones relacionadas;
- estados de carga, vacío y error mediante la infraestructura existente.

No se agregó ruta ni item de sidebar independiente.

## Reconciliación LIVE

Auditoría previa a la aplicación del SP:

```text
v_rectificaciones = 0
v_total_rectificaciones = 662
rows_with_related = 0
min_total = 0
max_total = 0
sum_total = 0
```

Reconciliación del SP versionado, sin imprimir filas de negocio:

- fuente `v_total_rectificaciones`: `662`;
- SP sin filtro: `662`;
- página 1/tamaño 20: `total=662`, `rows=20`;
- página 34/tamaño 20: `total=662`, `rows=2`;
- página 35/tamaño 20: `total=662`, `rows=0`;
- filtro sintético improbable: `total=0`, `rows=0`;
- página extrema `2147483647`/tamaño 100: `total=662`, `rows=0`;
- `STRUCTURAL_MATCH = PASS` contra `sys.sql_modules` después de normalizar sólo
  `CREATE OR ALTER` frente a la definición materializada por SQL Server;
- `RECTIFICATIONS_PAGINATION = PASS`;
- `RECTIFICATIONS_EMPTY_FILTER = PASS`;
- `RECTIFICATIONS_STABLE_ORDERING = PASS`: `pedimento` es la única columna
  proyectada y la vista aplica `DISTINCT`; no se inventa una PK.

Agregados LIVE de unicidad:

```text
RECTIFICATIONS_PEDIMENTO_UNIQUENESS = PASS
total rows = 662
COUNT(DISTINCT pedimento) = 662
duplicated pedimento groups = 0
PEDIMENTO_NULL_ROWS = 0
```

Al no existir pedimentos `NULL` ni grupos duplicados, `ORDER BY pedimento` es
estable para el dataset y contrato actual.

## Semántica del total

`v_total_rectificaciones` calcula `Total` mediante:

```sql
count(distinct pedimento)
from v_operaciones vo2
where vo2.pedimentooriginal = vo1.pedimento
```

Por tanto, `Total` representa el conteo de pedimentos derivados distintos cuyo
`pedimentooriginal` se relaciona con el pedimento resumido. La semántica está
respaldada por la definición LIVE, no sólo por el nombre de la vista:

```text
RECTIFICATIONS_TOTAL_SEMANTICS = CONFIRMED
```

La etiqueta UI `Rectificaciones relacionadas` se mantiene.

## Controles

```text
legacy mutable SP executed = 0
CALE_IMMEX writes = 0
inline Java SQL = 0
RECTIFICATIONS_XLSX = NOT_IMPLEMENTED
```

## Paridad

```text
LEGACY-051 before = UNKNOWN
LEGACY-051 proposed = PARTIAL
```

La V1 cubre sólo consulta/resumen read-only. No modifica la clasificación de
procesos de rectificación ni habilita `INSERTAPEDIMENTO`.
