# Mapeo de Vencimientos Read V1

## Alcance y clasificación

La V1 implementa únicamente la consulta read-only de vencimientos calculados por
la vista `dbo.vDESPERDICIOS`. El alcance es el subconjunto de vencimientos de
desperdicio que el legacy proyecta explícitamente; no se implementan saldos,
descargos, aplicación, confirmación ni cálculo de estados operativos.

```text
WASTE_EXPIRATIONS_ONLY = YES
GLOBAL_EXPIRATIONS = NOT_IMPLEMENTED
```

```text
EXPIRATIONS_CONTRACT = PARTIAL
EXPIRATIONS_READ_V1 = IMPLEMENTED_WASTE_SUBSET
SCRAP_FALLBACK = NOT_REQUIRED
```

`LEGACY-046` puede pasar de `UNKNOWN` a `PARTIAL` porque la fuente, la fórmula,
el grano y los campos del subconjunto están demostrados. El contrato global de
Vencimientos no se declara equivalente completo mientras existan otras
proyecciones legacy con alcances distintos.

## Discovery

| Objeto | Tipo/clasificación | Filas LIVE | Decisión |
|---|---|---:|---|
| `dbo.vDESPERDICIOS` | VIEW / READ SOURCE | 0 | Fuente canónica V1 |
| `dbo.VReporteAplicaciondesperdicios` | VIEW / READ REPORT | 0 | Auditada; no es fuente canónica V1 |
| `dbo.vDesperdiciosDetalleAplicacion` | VIEW / READ DETAIL | 0 | Auditada para aplicación; no se expone |
| `dbo.v_saldosdesp` | VIEW / READ REPORT | 0 | Fuera de V1; mezcla saldos y desperdicio |
| `dbo.V_INFORMEDESCARGAS` | VIEW / READ REPORT | 3866 | Fuente amplia de descargas; no se usa como vencimientos canónicos |
| `dbo.DESCARGA_DESPERDICIO` | VIEW / READ REPORT | 0 | Descarga/desperdicio, no fuente de fecha canónica |
| `dbo.PED_DESPERDICIOS` | VIEW / READ AGGREGATE | 0 | Pendientes de desperdicio; no proyecta vencimiento |
| `dbo.PFECHAVENCE` | No localizado en `sys.objects`/`sys.sql_modules` LIVE | N/A | No ejecutar ni asumir contrato |

También se auditaron las dependencias de las views. No se ejecutaron
`LIGADESPERDICIOS`, `SALDOS*`, `DESCARGA*` ni otros procesos mutables.

## Semántica demostrada

`vDESPERDICIOS` construye una fila agrupada desde `Importaciones`, `partidas`,
`descarga`, `salidas` y `psalidas`, conservando sólo grupos con
`sum(d.desperdicio) > 0` y excluyendo ciertas claves de pedimento de salida.

La fecha se calcula exactamente como:

```sql
DATEADD(
    month,
    (SELECT categorias.meses
     FROM categorias
     WHERE categorias.categoria = p.categoria),
    im.fecha
) AS VENCIMIENTO
```

Por tanto:

```text
qué vence       = el desperdicio agrupado de una importación/partida
fecha base      = Importaciones.Fecha
plazo           = categorias.meses según partidas.Categoria
fecha final     = DATEADD(month, categorias.meses, Importaciones.Fecha)
```

La auditoría LIVE observó 3 categorías con `meses` no nulo, en el rango
agregado de 3 a 12 meses. La V1 no replica esa configuración en Java.

```text
EXPIRATION_DATE_RULE = DATEADD(month, categorias.meses, Importaciones.Fecha)
EXPIRATION_DATE_RULE_STATE = CONFIRMED_FROM_VIEW_DEFINITION
```

No se derivan `VIGENTE`, `POR_VENCER` ni `VENCIDO`, porque no existe una regla
legacy observada para rangos relativos a la fecha actual.

## Grano y campos

El grano de `vDESPERDICIOS` está definido por el `GROUP BY` legacy:

- `Importaciones.numero_ped`;
- `Importaciones.fecha`;
- `Importaciones.cve_pedimento`;
- `partidas.clave`;
- `partidas.unidad` y `partidas.unidadt`;
- `partidas.Factura`;
- `partidas.Categoria`.

`Categoria` participa en el cálculo, aunque no se proyecta como columna. La V1
expone únicamente campos que la view proyecta y no inventa una clave técnica.

## Nueva consulta

- SP: `dbo.APP24_Q_VENCIMIENTOS_LISTAR`.
- Fuente: `dbo.vDESPERDICIOS`.
- API: `GET /api/v1/reportes/vencimientos`.
- Contrato del endpoint: `WASTE_EXPIRATIONS_ONLY`.
- Permiso: `REPORTES_GENERAR`.
- Filtro: texto sobre pedimento, clave de pedimento, clave y factura.
- Paginación: página 1-based, tamaño entre 1 y 100.
- Total: después del filtro y antes de `OFFSET/FETCH`.
- Orden: `numero_ped`, `fecha`, `cve_pedimento`, `clave`, `unidad`, `unidadt`,
  `FACTURA`, `VENCIMIENTO`, `Desperdicio`, `DesperdicioT`, `VALORDESPERDICIO`,
  `Aplicado` y `linea`; usa todas las columnas proyectadas relevantes y `linea`
  como último desempate estructural. Se usa aritmética `BIGINT` en `OFFSET` y no
  se inventa una PK.
- XLSX: `NOT_IMPLEMENTED`.

Campos API:

```text
pedimento
fechaBase
clavePedimento
clave
unidad
unidadT
desperdicio
desperdicioT
valorDesperdicio
aplicado
factura
vencimiento
```

## UI

Se agregó `Vencimientos de desperdicio` dentro de `/reportes`, sin ruta ni item
de sidebar nuevo. La pantalla reutiliza loading, empty, error, retry, filtro y
paginación. La ayuda visible es `Consulta de fechas de vencimiento asociadas a
desperdicios de importación.` El estado vacío identifica explícitamente que no
hay vencimientos de desperdicio para los filtros seleccionados.

No se muestran estados fiscales derivados ni se agrega exportación XLSX.

## Reconciliación LIVE

```text
source = dbo.vDESPERDICIOS
source total = 0
SP total = 0
pagina 1/tamano 20 = total 0, rows 0
ultima pagina/tamano 100 = total 0, rows 0
pagina fuera de rango = total 0, rows 0
filtro sintético = total 0, rows 0
pagina extrema 2147483647/tamano 100 = total 0, rows 0
STRUCTURAL_MATCH = PASS
```

El dataset vacío no invalida la definición ni la fórmula, pero se conserva como
limitación de evidencia de datos activos.

## Orden estable y calidad del dataset

El criterio de orden del SP se deriva de columnas reales de `vDESPERDICIOS` y
cubre el grano proyectado de la vista; `linea` se conserva como desempate final
sin presentarla como PK. La verificación agregada LIVE, sin imprimir valores,
produjo:

```text
total rows = 0
distinct ordering tuples = 0
duplicate ordering groups = 0
NULL rows in ordering columns = 0
VENCIMIENTOS_STABLE_ORDERING = PASS
```

Estos ceros demuestran que el snapshot actual no contiene duplicados ni NULL,
pero no permiten demostrar unicidad positiva sobre filas activas. La decisión
`PASS` se basa principalmente en la definición y el `GROUP BY` de la vista, y
no en una observación de datos no existentes.

## Controles

```text
legacy mutable SP executed = 0
CALE_IMMEX writes = 0
inline Java SQL = 0
```

No se modificaron saldos, descargos, inventario, `Importaciones`, `partidas`,
`descarga` ni tablas de desperdicio.

## Paridad

```text
LEGACY-046 before = UNKNOWN
LEGACY-046 proposed = PARTIAL
LEGACY-048 = UNKNOWN (sin cambios)

TOTAL = 79
IMPLEMENTED_EQUIVALENT = 5
IMPLEMENTED_REDESIGNED = 20
PARTIAL = 12
MISSING = 8
BLOCKED_BUSINESS = 5
CONSOLIDATE = 6
UNKNOWN = 23
```
