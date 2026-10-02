# Mapeo de Scrap / Desperdicios Read V1

## Decisión de alcance

La auditoría no demuestra que `Scrap` sea un nombre alternativo para todas las
proyecciones de desperdicio. El legacy separa al menos el desperdicio agregado,
la aplicación de desperdicio, el detalle de destino, el descargo y el pendiente
de desperdicio/merma.

```text
SCRAP_WASTE_RELATION = PARTIAL_OVERLAP
SCRAP_CONTRACT = PARTIAL
SCRAP_READ_V1 = NOT_IMPLEMENTED
```

`LEGACY-048` permanece `UNKNOWN` en esta primera pasada. La clasificación
`PARTIAL` del contrato describe el subconjunto técnico de desperdicio demostrado,
no una implementación completa de la capacidad legacy Scrap.

```text
WASTE_EXPIRATION_SUBSET_ALREADY_EXPOSED = YES
SCRAP_FULL_CONTRACT_COVERED = NO
```

## Fuentes auditadas LIVE

Todas las consultas fueron metadata, `OBJECT_DEFINITION` o conteos agregados.
No se imprimieron filas de negocio.

| Objeto | Tipo | Filas LIVE | Significado demostrado | Decisión |
|---|---|---:|---|---|
| `dbo.vDESPERDICIOS` | VIEW | 0 | Desperdicio agregado por importación/partida y clave; incluye aplicado, valor, factura y vencimiento | Fuente read-only ya reutilizada por Vencimientos |
| `dbo.VReporteAplicaciondesperdicios` | VIEW | 0 | Reporte combinado de desperdicio y aplicación; calcula `Diferencia` y hace `FULL OUTER JOIN` con el detalle | No reutilizar como equivalente sin contrato de detalle |
| `dbo.vDesperdiciosDetalleAplicacion` | VIEW | 0 | Aplicación agregada por documento/clave y destino de salida | No exponer como Scrap V1 |
| `dbo.DESCARGA_DESPERDICIO` | VIEW | 0 | Proyección de descargas con enlaces de entrada/salida, cantidad, factura y valor de desperdicio | Fuera de V1; mezcla descarga con desperdicio |
| `dbo.PED_DESPERDICIOS` | VIEW | 0 | Pendientes agrupados por `PEDIMENTOI`, con desperdicio y merma | No equivale al agregado de `vDESPERDICIOS` |
| `dbo.DescargaDesp` | TABLE | 0 | Relación física de descarga de desperdicio; tiene `DescargaDespKey` como columna no nullable | No modificar ni usar como fuente canónica sin contrato |

También existen tablas auxiliares `Desperdicios`, `DesperdiciosPendientes`,
`InformeDesperdicio`, `pdesperdicios` y `DescDirDesperdicios`. No se declararon
fuentes funcionales porque su ciclo de vida y relación con la pantalla Scrap no
están demostrados.

## Significado de las fuentes

### `vDESPERDICIOS`

La view agrupa por `numero_ped`, `fecha`, `cve_pedimento`, `clave`, `unidad`,
`unidadt`, `Factura` y `Categoria`, conservando grupos con
`sum(d.desperdicio) > 0`. Sus fuentes son `Importaciones`, `partidas`,
`descarga`, `salidas` y `psalidas`.

Los campos demostrados son:

- desperdicio y desperdicio convertido (`Desperdicio`, `DesperdicioT`);
- valor calculado (`VALORDESPERDICIO`);
- aplicación calculada (`Aplicado`);
- factura y fecha de vencimiento.

`Aplicado` no se presenta como saldo. La definición lo calcula a partir de
`dirigido`, `salidas` y `psalidas` para descargas de tipo `DESP`, excluyendo
vínculos ya presentes en `descarga`.

### Aplicación y detalle

`vDesperdiciosDetalleAplicacion` agrupa `dirigido` con `salidas` y `psalidas`
por documento, clave y atributos del destino. `VReporteAplicaciondesperdicios`
combina ese resultado con `vDESPERDICIOS` mediante un `FULL OUTER JOIN` por
`linea`, importación y clave, y expone `Diferencia` como
`Desperdicio - Aplicado`.

Ese campo no se llama `saldo`: el contrato de saldos sigue fuera de alcance y
no se demostró que `Diferencia` sea un saldo fiscal o inventario.

### Descargo y pendientes

`DESCARGA_DESPERDICIO` lee relaciones de `descarga`, `Importaciones`,
`partidas`, `salidas` y `psalidas`, filtrando `descarga.Desperdicio > 0.001`.
Representa una proyección de descargo, no la misma granularidad que
`vDESPERDICIOS`.

`PED_DESPERDICIOS` agrupa `DESPERDICIOSPENDIENTES` por `PEDIMENTOI` y separa
`DESPERDICIO` de `MERMA`. Es un conjunto de pendientes y no una fuente
canónica demostrada para el reporte Scrap.

## Reconciliación de capacidades

```text
LEGACY_046_SCOPE = consulta de vencimientos del subconjunto de desperdicio
LEGACY_048_SCOPE = consulta de Scrap/desperdicio; semántica exacta de pantalla y proyección no capturada
OVERLAP = PARTIAL
```

La API existente `GET /api/v1/reportes/vencimientos` expone el agregado de
`vDESPERDICIOS`, incluyendo campos de desperdicio, aplicado, valor, factura y
vencimiento. Esto cubre parte del contrato técnico de desperdicio, pero no
sustituye una consulta de aplicación/detalle/descargo ni demuestra que sea la
pantalla legacy Scrap.

Por ello:

- no se creó `APP24_Q_DESPERDICIOS_LISTAR` duplicado;
- no se creó otra ruta, permiso, tab ni item de sidebar;
- no se cambió `LEGACY-048` en la matriz;
- se mantiene `LEGACY-046 = PARTIAL` por el subconjunto de vencimientos de desperdicio.

## Auditoría de procesos

`dbo.LIGADESPERDICIOS` es un procedimiento mutable: su definición contiene
`TRUNCATE` sobre `DescDirDesperdicios` y actualizaciones de saldos antes de
reconstruir relaciones. No se ejecutó.

Tampoco se ejecutaron `SALDOS*`, `DESCARGA*`, `DESCDIRIGIDA` ni procesos de
aplicación. No se modificaron `DESCARGA`, `PARTIDAS`, `Importaciones`,
`dirigido`, tablas de saldos ni tablas de desperdicio.

## Auditoría UI legacy

La auditoría disponible del repositorio no conserva una captura o ruta exacta
de una pantalla denominada `Scrap`. Sí conserva los objetos de reporte y
operación descritos arriba. Sin nombre de pantalla, filtros, columnas, acción
esperada y caso de aceptación, no se puede declarar equivalencia funcional con
`vDESPERDICIOS`.

```text
SCRAP_UI_CONTRACT = UNKNOWN
SCRAP_LAYOUT = UNKNOWN
SCRAP_ACCEPTANCE_CASE = UNKNOWN
```

## Reapertura / siguiente paso

Para convertir `LEGACY-048` en una capacidad implementable se necesita:

- nombre o ruta exacta de la pantalla legacy;
- columnas y filtros visibles;
- definición de si Scrap significa desperdicio agregado, aplicado, descargo,
  pendiente o saldo;
- grano esperado y caso de aceptación anonimizado;
- confirmación de si `Diferencia` es sólo una diferencia de reporte o una
  cantidad funcional autorizada;
- permiso legacy y comportamiento esperado de exportación, si aplica.

Hasta obtener esa evidencia, la implementación correcta es conservar la
reutilización de Vencimientos y no duplicar código. Si el contrato futuro
aprueba únicamente el agregado de `vDESPERDICIOS`, podrá consolidarse sobre la
API existente sin crear otro SP.

## Evidencia necesaria para cerrar el contrato Scrap

Para reabrir `LEGACY-048` se necesita evidencia específica y no sólo la
existencia de una view relacionada:

- pantalla o ruta legacy inequívoca;
- columnas visibles;
- filtros;
- significado de `Scrap`;
- relación entre Scrap y desperdicio;
- relación con `Aplicado`;
- relación con `DescargaDesp`;
- relación con `PED_DESPERDICIOS`;
- grano;
- exportación o layout, si existe;
- caso de aceptación.

Hasta cerrar esos puntos, no se debe declarar `SCRAP = DESPERDICIO` ni mover
`LEGACY-048` a `PARTIAL` por la sola exposición de `vDESPERDICIOS` mediante
Vencimientos.

## Controles

```text
source totals:
  vDESPERDICIOS = 0
  VReporteAplicaciondesperdicios = 0
  vDesperdiciosDetalleAplicacion = 0
  DESCARGA_DESPERDICIO = 0
  PED_DESPERDICIOS = 0
  DescargaDesp = 0

new SP = 0
legacy mutable SP executed = 0
CALE_IMMEX writes = 0
inline Java SQL = 0
persistent synthetic data = 0
```

No hubo Java, Angular, migración, endpoint, permiso ni cambio de paridad en
esa primera pasada.

## Segunda pasada — reauditoría exhaustiva (sin implementación)

Reauditoría read-only sobre `CALE_IMMEX` para decidir si existe contrato
suficiente para una consulta Scrap V1. Sólo metadata, definiciones, conteos,
dependencias y parámetros; ningún objeto mutable fue ejecutado.

```text
SCRAP_OBJECT_NAMED = NONE
SCRAP_UI_EVIDENCE  = NONE
READ_ONLY_WASTE_SP = NONE
NEW_SP_REQUIRED    = NOT_PROVEN
SCRAP_CONTRACT     = PARTIAL (sin cambio)
LEGACY-048         = UNKNOWN (sin cambio)
```

### Cobertura del barrido

- nombres (`%DESPERD%`, `%MERMA%`, `%SCRAP%`, `%DESP`, `DESCARGA%`) sobre
  `sys.objects`;
- definiciones de módulos (`sys.sql_modules`) que mencionan `desperd`, `merma`
  o `scrap`;
- columnas de 14 candidatos (`sys.columns`);
- conteos de 18 tablas/views;
- dependencias (`sys.sql_expression_dependencies`) y parámetros (`sys.parameters`).

### Clasificación SP-FIRST

| Candidato | Tipo | Efecto | Clasificación | Decisión |
|---|---|---|---|---|
| `dbo.vDESPERDICIOS` | VIEW | READ ONLY | `SP_EXISTING_REUSABLE` (fuente) | Ya consumida por `APP24_Q_VENCIMIENTOS_LISTAR` (LEGACY-046); no es contrato Scrap |
| `dbo.VReporteAplicaciondesperdicios` | VIEW | READ ONLY | `SP_EXISTING_NOT_REUSABLE` | `FULL OUTER JOIN` + `Diferencia` sin caso de aceptación |
| `dbo.vDesperdiciosDetalleAplicacion` | VIEW | READ ONLY | `SP_EXISTING_NOT_REUSABLE` | Aplicación por destino; grano documento/clave no aprobado |
| `dbo.DESCARGA_DESPERDICIO` | VIEW | READ ONLY | `SP_EXISTING_NOT_REUSABLE` | Proyección por línea de descarga; mezcla descargo con desperdicio |
| `dbo.PED_DESPERDICIOS` | VIEW | READ ONLY | `SP_EXISTING_NOT_REUSABLE` | Pendientes por pedimento; separa desperdicio y merma |
| `dbo.V_F4DESP`, `dbo.V_G6_DESP`, `dbo.v_saldosdesp` | VIEW | READ ONLY | `SP_EXISTING_NOT_REUSABLE` | Subtipos F4/G6/saldos con contrato especializado |
| `dbo.DescargaDesp`, `Desperdicios`, `DesperdiciosPendientes`, `InformeDesperdicio`, `pdesperdicios`, `DescDirDesperdicios` | TABLES | físicas | `SP_EXISTING_NOT_REUSABLE` | Sin contrato; `InformeDesperdicio`, `Desperdicios` y `pdesperdicios` sin referencias en dependencias; `DescDirDesperdicios` sólo tocada por procesos mutables |
| `dbo.LIGADESPERDICIOS`, `SALDOS*`, `HISTORIADESCARGAS*`, `SP_G5` | SP | WRITE/MIXED | `SP_EXISTING_NOT_REUSABLE` | Mutables; no GET |
| `dbo.DESCARGADO_G5` | FUNCTION | READ ONLY | `FALSE_POSITIVE` | Cálculo G5; no es desperdicio |
| Nuevo SP | — | — | `NEW_SP_REQUIRED` = NOT_PROVEN | Sin contrato de pantalla/columnas/filtros no se define proyección |

### Conteos re-verificados (LIVE)

```text
descarga                       = 3866 filas (Desperdicio: 3866 NULL)
DESCARGA_DESPERDICIO           = 0
DescargaDesp                   = 0
DescDirDesperdicios            = 0
Desperdicios                   = 0
DesperdiciosPendientes         = 0
InformeDesperdicio             = 0
pdesperdicios                  = 0
PED_DESPERDICIOS               = 0
V_F4DESP                       = 0
V_G6_DESP                      = 0
v_saldosdesp                   = 0
vDESPERDICIOS                  = 0
vDesperdiciosDetalleAplicacion = 0
VReporteAplicaciondesperdicios = 0
```

### Dependencias relevantes

- `APP24_Q_VENCIMIENTOS_LISTAR → vDESPERDICIOS`: única reutilización read-only existente;
- `VReporteAplicaciondesperdicios → vDESPERDICIOS + vDesperdiciosDetalleAplicacion`;
- `LIGADESPERDICIOS → descarga/DESCARGA_DESPERDICIO/DescDirDesperdicios/dirigido/salidas/psalidas/V_F4DESP` (mutable; invocada por `SP_G5`);
- `v_saldosdesp → descarga/DescargaDesp/partidas/...` (saldos; fuera de alcance).

### Evidencia faltante (sin cambio)

- pantalla o ruta legacy inequívoca de Scrap;
- columnas visibles, filtros y orden;
- significado de Scrap y relación con `Aplicado`/`DescargaDesp`/`PED_DESPERDICIOS`;
- grano y caso de aceptación anonimizado;
- comportamiento de exportación.

### Controles de la segunda pasada

```text
LIVE reads = SELECT/metadata
LIVE writes = 0
legacy mutable SP executed = 0
new SP = 0
new endpoint = 0
Java/Angular changes = 0
```
