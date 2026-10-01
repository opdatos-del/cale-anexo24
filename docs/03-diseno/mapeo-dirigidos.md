# Dirigidos Read V1

## Frontera funcional

Esta V1 consulta únicamente líneas de salida marcadas como dirigidas por una
fuente read-only. No genera ni ejecuta descargos.

```text
DIRECTED_READ_CONTRACT = PARTIAL
DIRECTED_READ_V1 = IMPLEMENTED_STATUS_SUBSET
LEGACY-049 = PARTIAL
LEGACY-030 = BLOCKED_BUSINESS (sin cambios)
```

La separación es explícita:

- `LEGACY-049`: consultar/reportar operaciones dirigidas;
- `LEGACY-030`: generar o ejecutar descargo dirigido, aplicar saldos y ejecutar
  el motor operativo.

La implementación de `LEGACY-049` no desbloquea `LEGACY-030`.

## Inventario LIVE

La auditoría usó `sys.objects`, `sys.columns`, `sys.indexes`, dependencias,
definiciones y conteos agregados. No se imprimieron filas de negocio.

| Objeto | Tipo | Clasificación | Filas/resultado | Decisión |
|---|---|---|---:|---|
| `dbo.DIRIGIDO` | TABLE | READ SOURCE / MUTABLE PROCESS SOURCE | 0 | Fuente de la marca; no escribir |
| `dbo.V_STATUS_DESCARGAS` | VIEW | READ | 3392 | Fuente read-only V1 |
| `dbo.V_INFORMEDESCARGAS` | VIEW | READ | 3866 | No contiene flag ni dependencia directa de `DIRIGIDO` |
| `dbo.TRAZO` | TABLE | MUTABLE PROCESS RESULT | 0 | No exponer como Dirigidos |
| `dbo.DESCDIRIGIDA` | PROCEDURE | WRITE | No ejecutado | Descarga dirigida; excluido |
| `dbo.SALDOSDIRIGIDOS` | PROCEDURE | WRITE | No ejecutado | Calcula saldos y escribe `DESCARGA`/`TRAZO` |
| `dbo.Trazo_report` | PROCEDURE | WRITE/MIXED | No ejecutado | Trunca/escribe `ANALISIS_MATERIALES` y actualiza acumulados |

Otros procesos relacionados (`DESCARGATODOSDIRIGIDOS`, `DESCARGASALIDAPEPS`,
`DESCARGATSALIDA*`, `INSERTADIRIGIDOS`, `CONVIERTEDIRIGIDO*`) fueron clasificados
por definición como procesos mutables o generadores. Ninguno fue ejecutado.

## Contrato read-only parcial

`V_STATUS_DESCARGAS` deriva una fila por pareja de salida/partida de salida y
calcula `DIRIGIDO` mediante existencia de una fila de `dbo.DIRIGIDO` con la
misma `salidakey` y `psalidakey`. También expone documento, fecha, clave de
pedimento, secuencia, producto, cantidad, factura, estado de descargo, valores
de descarga y presencia de estructura.

Mediciones LIVE:

```text
DIRIGIDO rows = 0
V_STATUS_DESCARGAS total = 3392
V_STATUS_DESCARGAS DIRIGIDO = SI = 0
V_STATUS_DESCARGAS DIRIGIDO = NO = 3392
V_STATUS_DESCARGAS DIRIGIDO NULL = 0
V_INFORMEDESCARGAS rows = 3866
```

Gate de orden estable sobre `V_STATUS_DESCARGAS`:

```text
STATUS_DESCARGAS_LINE_KEY_UNIQUE = PASS
total rows = 3392
distinct (SALIDAKEY, PSALIDAKEY) groups = 3392
duplicate (SALIDAKEY, PSALIDAKEY) groups = 0
NULL SALIDAKEY = 0
NULL PSALIDAKEY = 0
DIRECTED_STABLE_ORDERING = PASS
```

La identidad observable de línea queda demostrada por la pareja
`SALIDAKEY + PSALIDAKEY`. El `ORDER BY` de la consulta conserva esa pareja y
las columnas adicionales `SECUENCIA`, `DOCUMENTO`, `PRODUCTO` y `FACTURA` como
tie-breakers defensivos; no se inventa una PK ni se concatenan claves para medir
unicidad. La verificación positiva de unicidad proviene del `GROUP BY` agregado
sobre el dataset LIVE.

El contrato es `PARTIAL`, no globalmente confirmado, porque el snapshot no
contiene ninguna línea `DIRIGIDO = SI` y no se capturó una pantalla legacy
específica de consulta. La definición sí demuestra la semántica técnica de la
marca y la relación entrada/salida suficiente para una lectura acotada.

`V_INFORMEDESCARGAS` es un informe de descargas con importación, exportación,
cantidad descargada y saldo actual. No tiene una columna `DIRIGIDO` ni una
dependencia directa con `dbo.DIRIGIDO`; por eso no se utiliza como fuente de
Dirigidos.

## Nueva consulta

- SP: `dbo.APP24_Q_DIRIGIDOS_LISTAR`.
- Fuente: `dbo.V_STATUS_DESCARGAS`.
- Filtro estructural: `DIRIGIDO = 'SI'`.
- API: `GET /api/v1/reportes/dirigidos`.
- Permiso: `REPORTES_GENERAR`.
- Filtro textual: documento, clave de pedimento, producto o factura.
- Paginación: página 1-based, tamaño entre 1 y 100.
- Total: después del filtro y antes de `OFFSET/FETCH`.
- Orden: `SALIDA_KEY`, `PSALIDA_KEY`, `SECUENCIA`, `DOCUMENTO`, `PRODUCTO`,
  `FACTURA`.
- `OFFSET`: aritmética `BIGINT`.
- XLSX: `NOT_IMPLEMENTED`.

La API sólo devuelve líneas que la fuente read-only marca como dirigidas. No
incluye botones ni comandos de procesar, generar, aplicar o recalcular. Cuando
el resultado es vacío, la UI informa: `No hay líneas marcadas como dirigidas para
los filtros seleccionados.`

## Reconciliación LIVE del SP

```text
source total DIRIGIDO = 0
SP page 1 total = 0
SP extreme page total = 0
SP synthetic empty filter total = 0
STRUCTURAL_MATCH = PASS
mutable SP executed = 0
CALE_IMMEX writes = 0
inline Java SQL = 0
```

El SP fue aplicado como procedimiento versionado propio en `CALE_IMMEX`.
La definición LIVE coincide estructuralmente con
`infra/sql/procedures/queries/APP24_Q_DIRIGIDOS_LISTAR.sql`, normalizando el
encabezado generado por SQL Server.

## Fallback de análisis de descarga

No se activó el fallback `LEGACY-032` porque `DIRECTED_READ_CONTRACT` resultó
`PARTIAL`, no `UNKNOWN`. Durante la misma auditoría se revisó
`V_INFORMEDESCARGAS` y `TRAZO`, pero no se implementó un reporte adicional de
descargas:

```text
DISCHARGE_ANALYSIS_CONTRACT = NOT_ENTERED_FALLBACK
DISCHARGE_ANALYSIS_OVERLAP = NOT_DECIDED
```

`TRAZO` permanece fuera de la API porque es resultado mutable del motor de
descargo y está vacío. `Trazo_report` tampoco es una fuente read-only: trunca
`ANALISIS_MATERIALES`, inserta resultados y actualiza acumulados.

La información de descargas ya existente en
`/operaciones/materiales-utilizados` y
`APP24_Q_MATERIALES_UTILIZADOS_LISTAR` no se duplicó.

## Exclusiones

No se ejecutaron ni se replicaron:

- `DESCDIRIGIDA`;
- `SALDOSDIRIGIDOS`;
- `DESCARGATSALIDA*`;
- `DESCARGASALIDAPEPS`;
- `SALDOS*`;
- selección PEPS;
- cálculo de saldo;
- aplicación o rollback;
- generación de trazo.

No se modificaron `DIRIGIDO`, `DESCARGA`, `PARTIDAS`, `TRAZO`, saldos,
inventario ni tablas históricas.

## Paridad

```text
LEGACY-049 before = UNKNOWN
LEGACY-049 proposed = PARTIAL
LEGACY-030 = BLOCKED_BUSINESS (sin cambios)
LEGACY-032 = UNKNOWN (sin cambios)
LEGACY-033 = PARTIAL (sin cambios)
LEGACY-034 = PARTIAL (sin cambios)

TOTAL = 79
IMPLEMENTED_EQUIVALENT = 5
IMPLEMENTED_REDESIGNED = 20
PARTIAL = 13
MISSING = 8
BLOCKED_BUSINESS = 5
CONSOLIDATE = 6
UNKNOWN = 22
```

Validación:

```text
5 + 20 + 13 + 8 + 5 + 6 + 22 = 79
```
