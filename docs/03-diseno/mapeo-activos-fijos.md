# Activos fijos — ingeniería inversa y contrato V1

## 0. Estado del documento

- **Fecha de auditoría y cierre:** 2026-09-21.
- **Rama:** `feature/backend-fixed-assets`.
- **SHA base:** `dev` en `0ac4b97524ac2a6df703afe696f9febc46cf188e`.
- **Base consultada:** `CALE_IMMEX`, esquema `dbo`.
- **Compatibility level:** `100`.
- **SQL Server:** `16.0.1000.6`, Express Edition 64-bit.
- **Conexión:** disponible con acceso autorizado; no se documentan credenciales ni cadenas de conexión.
- **Modo:** auditoría estrictamente read-only.
- **Datos y objetos:** se creó únicamente `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR`; no se modificaron objetos legacy. Los procedimientos legacy se inspeccionaron sólo por metadata y definición; no se ejecutó ninguno.
- **Estado:** **IMPLEMENTADO V1**; SP APP24 read-only, backend hexagonal y endpoint validados.

### Convención de evidencia

- **CONFIRMADO:** demostrado mediante metadata, definición SQL o consulta diagnóstica read-only.
- **INFERIDO:** conclusión razonable derivada de evidencia técnica, sin prueba funcional completa.
- **DECISIÓN V1:** regla deliberada del contrato nuevo; no se presenta como conducta nativa del legado si no está demostrada.
- **PENDIENTE:** evidencia funcional o decisión de negocio que queda fuera del alcance V1.

## 1. Resumen ejecutivo

### 1.1 Qué representa un Activo Fijo en el snapshot

**CONFIRMADO:** la fuente operativa enlazable a un pedimento para V1 no es
`dbo.ActivoFijo`, sino una **partida de importación** de `dbo.Partidas` cuyo
marcador `EsActivo`, normalizado, es `S`.

```text
IMPORTACIONES (encabezado y pedimento)
    1 ── relación lógica ── N
PARTIDAS (partida de importación; EsActivo = S)
```

`dbo.CARGAPEDIMENTOS` inserta `CargaPedimentosIE.ESACTIVO` directamente en
`PARTIDAS.EsActivo`, junto con `marca`, `modelo` y `serie`. La vista read-only
`dbo.v_g5` une `IMPORTACIONES` con `PARTIDAS`, proyecta `EsActivo` y usa
`IMPORTACIONES.Fecha` como fecha de la operación fiscal.

**CONFIRMADO:** `dbo.ActivoFijo` es una tabla independiente de cuatro filas
sin fecha, pedimento, cantidad, PK, índice ni FK. En el snapshot no enlaza por
`Parte → PARTIDAS.Clave`, `Num_serie → PARTIDAS.serie` ni `Parte →
MATERIAL.clave`. No puede sustentar una consulta que requiera simultáneamente
pedimento y fecha.

**DECISIÓN V1:** una fila API representa **una partida de importación marcada
como activo** (`PARTIDAS`), no un activo individual, ni una serie, ni una fila
de la tabla aislada `ActivoFijo`, ni un saldo/descargo.

### 1.2 Fuente recomendada

**CONFIRMADO:** no existe un SP o view legacy que entregue el contrato V1
completo: filtros parametrizados, total, paginación estable, pedimento,
descripción, identificación histórica y fecha.

**DECISIÓN V1:** la fuente futura será un SP propio estrictamente read-only:

```text
dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR
```

El objeto fue desplegado en esta implementación. Lee sólo:

```text
dbo.Partidas p
INNER JOIN dbo.Importaciones i
  ON i.Ipedimentokey = p.Importacionlink
```

No unirá `dbo.ActivoFijo`, `MATERIAL`, `DESCARGA`, `SALDOS`, `TRAZO` ni tablas
de retorno en V1.

## 2. Alcance y exclusiones V1

### Incluido

- Consultar partidas de importación cuyo indicador activo sea `S`.
- Mostrar pedimento y clave desde el encabezado de importación.
- Mostrar cantidad, unidad, número de parte, descripción y fracción de la
  partida histórica.
- Conservar marca, modelo y serie históricos como campos nullable.
- Filtrar, ordenar y paginar sin mutar información.

### Excluido

- Crear, editar o eliminar activos.
- Inferir un activo individual a partir de una serie.
- Usar `dbo.ActivoFijo` como fuente de datos operativa.
- Generar descargos, PEPS, saldos, G5/G6, retornos o desensambles.
- Mostrar un saldo, una baja o una fecha de retorno.
- Sustituir los datos históricos de la partida por valores vigentes de
  `MATERIAL`.
- Mostrar valores monetarios, proveedor, origen o datos fiscales no requeridos
  para la consulta operacional.

## 3. Tablas investigadas

| Tabla | Filas | Papel observado | Estado |
|---|---:|---|---|
| `dbo.Partidas` | 2 | Fuente de las partidas marcadas como activo y sus datos históricos | CONFIRMADO |
| `dbo.Importaciones` | 2 | Encabezado, pedimento, clave y fecha de importación | CONFIRMADO |
| `dbo.ActivoFijo` | 4 | Tabla independiente de identificación básica, sin relación comprobable | CONFIRMADO; no fuente V1 |
| `dbo.CargaPedimentosIE` | 2 | Staging de importación; origen técnico de `ESACTIVO`, marca, modelo y serie | CONFIRMADO; no fuente V1 |
| `dbo.material` | 2 | Catálogo actual; coincide por clave con las partidas activas, pero no aporta serie | CONFIRMADO; no fuente V1 |
| `dbo.G5` | 0 | Tabla de salida del proceso mutable `SP_G5` | CONFIRMADO; fuera de V1 |
| `dbo.HojaTrabajoG5` | 0 | Tabla creada/poblada por `SP_G5` | CONFIRMADO; fuera de V1 |
| `dbo.G6` | 0 | Salida de procesos G6 mutables | CONFIRMADO; fuera de V1 |

### 3.1 `dbo.Partidas`

**CONFIRMADO:** es la fuente histórica de V1. Campos relevantes:

| Campo | Tipo SQL | Nullable | Uso V1 |
|---|---|---:|---|
| `Partidakey` | `numeric(18,0)` | No | Identidad técnica de la fila |
| `Importacionlink` | `numeric(18,0)` | Sí | Relación lógica hacia importación |
| `Clave` | `varchar(50)` | Sí | Número de parte/código histórico |
| `Descripcion` | `varchar(250)` | Sí | Descripción histórica |
| `Fraccion` | `varchar(15)` | Sí | Fracción histórica |
| `Cantidad` | `numeric(18,4)` | Sí | Cantidad original de la partida |
| `Unidad` | `varchar(10)` | Sí | Unidad de la cantidad |
| `marca` | `varchar(50)` | Sí | Marca histórica nullable |
| `modelo` | `varchar(50)` | Sí | Modelo histórico nullable |
| `serie` | `varchar(50)` | Sí | Serie histórica nullable |
| `EsActivo` | `varchar(2)` | Sí | Marcador técnico de inclusión |
| `PFECHAIMPO` | `datetime` | Sí | Fecha de partida replicada del encabezado |
| `FECHAIMPO` | `datetime` | Sí | No se usa en V1; nula en el snapshot activo |
| `Saldo` | `numeric(18,4)` | Sí | Fuera de V1 |

- PK clustered única: `PK_partidas(Partidakey)`.
- No tiene FKs físicas.
- Índices relevantes:
  - `PK_partidas(Partidakey)`.
  - `INDICE1_G5` incluye `Partidakey`, `Importacionlink` y `EsActivo`, pero
    **no** los tiene como claves de búsqueda.
  - `op11` tiene clave inicial `Clave`; incluye `FECHAIMPO` como quinta clave.
  - `op7` incluye `Clave`; `FECHAIMPO` es cuarta clave.
- **CONFIRMADO:** no hay índice con `EsActivo`, serie, marca o modelo como
  clave; esta auditoría no crea índices.

### 3.2 `dbo.Importaciones`

| Campo | Tipo SQL | Nullable | Uso V1 |
|---|---|---:|---|
| `Ipedimentokey` | `numeric(18,0)` | No | Identidad técnica de importación |
| `Numero_ped` | `varchar(20)` | Sí | Pedimento canónico V1 |
| `Cve_pedimento` | `varchar(5)` | Sí | Clave de pedimento V1 |
| `Fecha` | `datetime` | Sí | Fecha operativa V1 |
| `Fecha_ad` | `datetime` | Sí | Diagnóstico; no fecha V1 |

- PK clustered única: `PK_Importaciones(Ipedimentokey)`.
- No se observaron FKs físicas con `PARTIDAS`.

### 3.3 `dbo.ActivoFijo`

| Campo | Tipo SQL | Nullable |
|---|---|---:|
| `Numero` | `char(35)` | No |
| `Descripcion` | `char(40)` | Sí |
| `Num_serie` | `char(25)` | Sí |
| `Parte` | `char(20)` | Sí |
| `Marca` | `char(30)` | Sí |
| `Modelo` | `char(35)` | Sí |
| `Fraccion` | `char(20)` | Sí |
| `Tipo` | `char(35)` | Sí |

**CONFIRMADO:** es un heap, sin PK, identity, defaults, índices ni FKs.
No contiene fecha, pedimento, cantidad, saldo, retorno o baja.

**DECISIÓN V1:** no se expone su `Numero` como `activoId`; hacerlo inventaría
una relación que no existe en los datos auditados.

## 4. Relaciones y granularidad

### 4.1 Relación de importación

Relación lógica observada:

```text
PARTIDAS.Importacionlink -> IMPORTACIONES.Ipedimentokey
```

- Ambos lados: `numeric(18,0)`.
- FK DDL: ninguna.
- Snapshot de partidas activas: 2 de 2 joins correctos; 0 huérfanos.
- `CARGAPEDIMENTOS` inserta `PARTIDAS.Importacionlink` consultando el
  `Ipedimentokey` correspondiente al `NumeroPedimento` de staging.

### 4.2 Marcador de activo

**CONFIRMADO:** `CARGAPEDIMENTOS` proyecta:

```text
CargaPedimentosIE.ESACTIVO -> PARTIDAS.EsActivo
```

En el snapshot:

| Valor normalizado de `EsActivo` | Filas |
|---|---:|
| `S` | 2 |

**DECISIÓN V1:** predicado de inclusión futuro:

```sql
UPPER(LTRIM(RTRIM(p.EsActivo))) = 'S'
```

El predicado normaliza espacios y mayúsculas; no supone que una categoría de
`MATERIAL` sea un activo fijo.

### 4.3 Granularidad e identidad

**CONFIRMADO:** `Partidakey` es la única identidad técnica físicamente
protegida de la fuente V1. La granularidad es una partida de importación.

**INFERIDO:** una partida puede representar un lote/cantidad y no demuestra por
sí misma un activo individual. No existe una restricción única sobre serie,
marca o modelo que cambie esa conclusión.

**DECISIÓN V1:** usar:

```text
partidaEntradaId = PARTIDAS.Partidakey
importacionId    = IMPORTACIONES.Ipedimentokey
```

Ambos se proyectarán en Java como `BigDecimal`; no se utilizará `Long` para
IDs `numeric(18,0)`.

## 5. Documentos, series y atributos

### 5.1 Pedimento

**CONFIRMADO:** V1 usa el documento canónico:

```text
pedimento      = IMPORTACIONES.Numero_ped  (varchar(20))
clavePedimento = IMPORTACIONES.Cve_pedimento (varchar(5))
```

`PARTIDAS.PEDIMENTO` (`varchar(50)`) y `PNUMERO_PED` (`varchar(20)`) existen,
pero no son la fuente canónica V1: la relación del encabezado está demostrada
y `v_g5` utiliza `IMPORTACIONES.Numero_ped`.

### 5.2 Serie

| Fuente | Filas aplicables | NULL/blank | Distintas no vacías | Duplicados no vacíos |
|---|---:|---:|---:|---:|
| `PARTIDAS.serie` con `EsActivo='S'` | 2 | 2 | 0 | 0 |
| `CargaPedimentosIE.serie` con `ESACTIVO='S'` | 2 | 2 | 0 | N/A |
| `ActivoFijo.Num_serie` | 4 | 0 | 4 | 0 |

**CONFIRMADO:** las series de `ActivoFijo` están completas y son únicas en la
muestra, pero ninguna enlaza con `PARTIDAS.serie` ni tiene relación física.
Las partidas activas no controlan serie en el snapshot.

**DECISIÓN V1:** `numeroSerie` se conserva desde `PARTIDAS.serie` como
`String nullable`; no se rellena desde la tabla aislada ni se usa para
identidad/filtro obligatorio.

### 5.3 Marca y modelo

- `PARTIDAS.marca` y `PARTIDAS.modelo`: `varchar(50)`, ambas NULL/blank en las
  2 partidas activas.
- `MATERIAL.MARCA` y `MATERIAL.MODELO`: ambos no vacíos para esas 2 claves.
- `MATERIAL.numero_serie`: tampoco aporta serie; es NULL/blank en las 2
  claves activas.

**DECISIÓN V1:** `marca` y `modelo` salen de la partida histórica y pueden ser
`NULL`. No se usa fallback al catálogo actual, porque no hay evidencia de que
sea el snapshot histórico correcto y cambiaría si el catálogo se actualiza.

## 6. Fecha V1

Candidatos medidos sobre las 2 partidas activas:

| Campo | NULL | Mínimo | Máximo | Relación observada |
|---|---:|---|---|---|
| `IMPORTACIONES.Fecha` | 0 | 2025-09-23 | 2026-05-28 | Fuente de `v_g5` |
| `IMPORTACIONES.Fecha_ad` | 0 | 2025-09-23 | 2026-05-28 | Igual al campo anterior en snapshot |
| `PARTIDAS.PFECHAIMPO` | 0 | 2025-09-23 | 2026-05-28 | Igual a `IMPORTACIONES.Fecha` en 2/2 |
| `PARTIDAS.FECHAIMPO` | 2 | — | — | No utilizable en snapshot |

**CONFIRMADO:** `dbo.v_g5` usa `IMPORTACIONES.Fecha` para las columnas `Fecha`
y `FDT`.

**DECISIÓN V1:** la fecha operativa será:

```text
fechaImportacion = IMPORTACIONES.Fecha
```

Los filtros de rango se aplicarán sobre ese campo, de forma inclusiva por día.
El rango será opcional, pero si se informa uno de los extremos se requerirá el
otro y `desde <= hasta`.

## 7. Cantidades, saldo, descargo y retorno

### 7.1 Cantidad

**CONFIRMADO:** V1 usa `PARTIDAS.Cantidad numeric(18,4)`, sin conversiones ni
cálculos. En las 2 partidas activas:

- NULL: 0; negativos: 0; ceros: 0.
- Mínimo: `126428.0804`.
- Máximo: `6000000.0000`.

**DECISIÓN V1:** `cantidad` será `BigDecimal` y se proyectará como
`numeric(18,4)`.

### 7.2 Saldo y descargo

**CONFIRMADO:** `PARTIDAS.Saldo numeric(18,4)` existe. En el snapshot activo:
0 NULL, 1 cero, 0 negativos, mínimo `0.0000`, máximo `513541.3950`.

**CONFIRMADO:** las 2 partidas activas aparecen en 3,866 filas de `DESCARGA`;
ninguna conclusión de saldo debe deducirse sólo de ese conteo.

**CONFIRMADO:** `RETORNO_SUBMAQUINA`, `SP_DESENSAMBLE`, `SP_G5`, `SP_G6` y
`SP_G6_F4` contienen DML o ejecución de objetos y son mutables o mixtos.
No se ejecutaron.

**DECISIÓN V1:** no exponer `saldo`, consumido, pendiente, retornado, baja ni
fecha de retorno. No se encontraron columnas con nombres `RETORN*` o `BAJA*`
en el modelo inspeccionado; el significado y momento de actualización de saldo
pertenecen a procesos fuera de esta consulta.

## 8. Objetos SQL relacionados

| Objeto | Tipo / propósito | Efecto | Evidencia y decisión |
|---|---|---|---|
| `dbo.v_g5` | VIEW / QUERY-REPORT | READ ONLY | Une importaciones y partidas, proyecta `EsActivo`, pedimento y fecha. Ejecutada sólo con `SELECT COUNT`: 2 filas, 2 activas. Sin parámetros, total/paginación ni detalle de activo; no usar como API. |
| `dbo.SP_G5` | PROCESS / REPORT | WRITE | Ejecuta `LIGADESPERDICIOS`, elimina/crea `HojaTrabajoG5`, trunca e inserta `G5`; no ejecutar ni usar para GET. |
| `dbo.CARGAPEDIMENTOS` | IMPORT / PROCESS | WRITE | Inserta `PARTIDAS` desde `CargaPedimentosIE`, incluido `ESACTIVO`, marca, modelo y serie; no ejecutar. |
| `dbo.RETORNO_SUBMAQUINA` | PROCESS | WRITE OR MIXED | Definición contiene DML y replica datos de partidas; no ejecutar. |
| `dbo.SP_DESENSAMBLE` | PROCESS | WRITE OR MIXED | Definición contiene DML sobre partidas; no ejecutar. |
| `dbo.V_G6` | VIEW / REPORT-QUERY | READ ONLY | Expone `EsActivo` en contexto de salida/descarga; no es listado de activos. |
| `dbo.V_G6_DESP` | VIEW / REPORT-QUERY | READ ONLY | Variante de desperdicio para salida; no es listado de activos. |
| `dbo.SP_G6` | PROCESS / EXPORT | WRITE OR MIXED | Actualiza/trunca/genera datos G6; no ejecutar. |
| `dbo.SP_G6_F4` | PROCESS / EXPORT | WRITE OR MIXED | Actualiza `DIRIGIDO`; no ejecutar. |
| `dbo.SP_ImportacionesBorradas` | MAINTENANCE | WRITE OR MIXED | Copia información a histórico; no ejecutar. |
| `dbo.SP_PartidasBorradas` | MAINTENANCE | WRITE OR MIXED | Copia información a histórico; no ejecutar. |

**CONFIRMADO:** no se encontró un procedimiento o view con referencia directa
a `dbo.ActivoFijo` que sea una consulta read-only adecuada. La única referencia
directa localizada fue `SP_G5`, objeto mutable.

## 9. Pantalla y reporte legacy

### 9.1 Pantalla

**PENDIENTE:** no hubo URL ni sesión autorizada del sistema web legacy en esta
fase, por lo que no se ejecutó navegación ni POST técnico.

**INFERIDO:** el artefacto histórico de diseño
`docs/03-diseno/prototipos/images/ACTIVO-FIJO.png` muestra título
`OPERACIONES · ACTIVO FIJO`, rango opcional y filtros de descripción, serie,
marca y modelo. Muestra columnas pedimento, descripción, marca, modelo, serie,
fecha de alta y fecha de baja. Es una referencia de diseño, no evidencia de una
consulta runtime ni de las fuentes SQL.

La auditoría técnica no encontró fuente para `fechaAlta`/`fechaBaja` como
campos de activo individual, por lo que no se incorporan a V1.

### 9.2 Reporte

**CONFIRMADO:** `v_g5` y `SP_G5` están relacionados con el indicador
`EsActivo` del Anexo G5, pero no son un reporte operacional de Activo Fijo V1.
`SP_G5` es mutable; `v_g5` es read-only pero no contiene la proyección,
parámetros ni paginación requeridos.

## 10. Contrato V1 cerrado

### 10.1 Fuente SQL futura

| Decisión | Valor |
|---|---|
| Objeto | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` |
| Estado | IMPLEMENTADO V1; desplegado y validado como read-only |
| Tipo | Stored procedure APP24, QUERY, estrictamente READ ONLY |
| Tablas | `dbo.Partidas`, `dbo.Importaciones` |
| Grano | Una partida de importación marcada activa |
| Filtro permanente | `UPPER(LTRIM(RTRIM(p.EsActivo))) = 'S'` |
| Compatibility | `100`; usar posteriormente `ROW_NUMBER()` |
| Paginación | `pagina=1`, `tamano=20`, `1 <= tamano <= 100` |

El SP futuro usará `COUNT_BIG(*)` y el mismo join/filtros que el result set.
Para la fecha máxima seguirá el patrón seguro ya validado en Entradas/Salidas:
no aplicar `DATEADD(DAY, 1, ...)` cuando `hasta = '9999-12-31'`.

### 10.2 Filtros

| Parámetro HTTP / SQL futuro | Fuente | Tipo / máximo | Semántica V1 |
|---|---|---|---|
| `desde` / `@Desde` | `i.Fecha` | `DATE`, opcional en par | Límite inclusivo inicial |
| `hasta` / `@Hasta` | `i.Fecha` | `DATE`, opcional en par | Límite inclusivo final |
| `pedimento` / `@Pedimento` | `i.Numero_ped` | `varchar(20)` | Igualdad exacta tras trim |
| `clavePedimento` / `@ClavePedimento` | `i.Cve_pedimento` | `varchar(5)` | Igualdad exacta tras trim |
| `numeroParte` / `@NumeroParte` | `p.Clave` | `varchar(50)` | Igualdad exacta tras trim |
| `descripcion` / `@Descripcion` | `p.Descripcion` | `varchar(250)` | Igualdad exacta tras trim |
| `serie` / `@Serie` | `p.serie` | `varchar(50)` | Igualdad exacta tras trim; puede no devolver filas en el snapshot |
| `marca` / `@Marca` | `p.marca` | `varchar(50)` | Igualdad exacta tras trim |
| `modelo` / `@Modelo` | `p.modelo` | `varchar(50)` | Igualdad exacta tras trim |
| `pagina` / `@Pagina` | — | `int` | Base 1; default 1 |
| `tamano` / `@Tamano` | — | `int` | Default 20; rango 1..100 |

**DECISIÓN V1:** no hay `LIKE`, búsqueda parcial, wildcard ni filtro global.
Los filtros son acumulativos. El backend validará longitudes después de trim y
no truncará valores.

### 10.3 Response

```text
Pagina<ActivoFijo>
├── items: ActivoFijo[]
├── total: long
├── pagina: int
└── tamano: int
```

Cada item tendrá sólo:

| Campo HTTP | Fuente | Tipo Java propuesto |
|---|---|---|
| `partidaEntradaId` | `p.Partidakey` | `BigDecimal` |
| `importacionId` | `i.Ipedimentokey` | `BigDecimal` |
| `pedimento` | `RTRIM(i.Numero_ped)` | `String` nullable |
| `clavePedimento` | `RTRIM(i.Cve_pedimento)` | `String` nullable |
| `fechaImportacion` | `i.Fecha` | `LocalDateTime` nullable |
| `numeroParte` | `RTRIM(p.Clave)` | `String` nullable |
| `descripcion` | `RTRIM(p.Descripcion)` | `String` nullable |
| `fraccion` | `RTRIM(p.Fraccion)` | `String` nullable |
| `cantidad` | `p.Cantidad` | `BigDecimal` nullable |
| `unidad` | `RTRIM(p.Unidad)` | `String` nullable |
| `numeroSerie` | `RTRIM(p.serie)` | `String` nullable |
| `marca` | `RTRIM(p.marca)` | `String` nullable |
| `modelo` | `RTRIM(p.modelo)` | `String` nullable |

No se exponen: links de descargo, la fila aislada `ActivoFijo.Numero`, saldo,
valor comercial `float`, valor aduanal, retorno, baja, fecha de baja o datos de
catálogo actual.

### 10.4 Orden, endpoint y seguridad

**DECISIÓN V1:** orden determinista:

```sql
i.Fecha DESC,
i.Ipedimentokey DESC,
p.Partidakey DESC
```

Contrato HTTP:

```http
GET /api/v1/operaciones/activos-fijos
```

- Permiso: `OPERACIONES_CONSULTAR`.
- Errores reutilizados: `400`, `401`, `403`, `503`.
- No se crea permiso nuevo; el recurso pertenece a la sección operacional
  visible históricamente.

## 11. Implementación y validación V1

### 11.1 SP y endpoint

**IMPLEMENTADO V1:**

```text
SP:       dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR
Endpoint: GET /api/v1/operaciones/activos-fijos
Permiso:  OPERACIONES_CONSULTAR
```

El SP usa exclusivamente `dbo.Partidas` e `dbo.Importaciones`, aplica el
marcador normalizado `EsActivo = 'S'`, normaliza filtros opcionales, usa
`COUNT_BIG(*)`, `ROW_NUMBER()` y límites de paginación `BIGINT`. No contiene
DML, `EXEC`, transacciones ni referencias a objetos legacy mutables.

### 11.2 Result set implementado

`sys.dm_exec_describe_first_result_set_for_object` confirmó exactamente los
13 aliases y tipos siguientes:

```text
PARTIDA_ENTRADA_ID numeric(18,0)
IMPORTACION_ID      numeric(18,0)
PEDIMENTO           varchar(20)
CLAVE_PEDIMENTO     varchar(5)
FECHA_IMPORTACION   datetime
NUMERO_PARTE        varchar(50)
DESCRIPCION         varchar(250)
FRACCION            varchar(15)
CANTIDAD            numeric(18,4)
UNIDAD              varchar(10)
NUMERO_SERIE        varchar(50)
MARCA               varchar(50)
MODELO              varchar(50)
```

### 11.3 Validación SQL read-only

| Caso | Resultado |
|---|---:|
| Sin filtros / sin rango | Total `2` |
| Rango `2025-09-23` a `2026-05-28` | Total `2` |
| Marcador `EsActivo = 'S'` | 2 filas |
| Pedimento real, parte real y descripción real | Baseline = SP = `1` cada uno |
| Clave de pedimento real | Baseline = SP = `2` |
| Combinación de cuatro filtros de una misma fila | Baseline = SP = `1` |
| Serie, marca y modelo inexistentes | Total `0` |
| Sólo `desde`, sólo `hasta`, rango invertido | Error SQL `50041` |
| Página `2147483647`, tamaño `100` | Total `2`, 0 items, sin overflow |
| Fecha `9999-12-31` a `9999-12-31` | Total `0`, sin overflow |

### 11.4 Backend y pruebas

Implementado bajo `operations/fixedassets/`:

```text
api/FixedAssetController.java
api/dto/ActivoFijoDto.java
application/query/ListarActivosFijosUseCase.java
domain/model/ActivoFijo.java
domain/port/ActivoFijoRepository.java
infrastructure/persistence/ActivoFijoStoredProcedureAdapter.java
```

El adapter usa `@Qualifier("jdbcTemplate")`, `BigDecimal` para IDs y cantidad,
y `Timestamp -> LocalDateTime` nullable. Para el rango ausente envía
`setNull(index, Types.DATE)`; no llama `Date.valueOf(null)` ni usa
`Double`/`Float`.

Pruebas implementadas:

```text
FixedAssetControllerTest
ListarActivosFijosUseCaseTest
ActivoFijoStoredProcedureAdapterTest
```

Cubren rango ausente/en pareja, filtros y longitudes, paginación extrema,
fecha máxima, parámetros JDBC, 13 aliases, nulos, página vacía, errores HTTP
`400/401/403/503` y serialización de la página.

## 12. Pendientes

1. **PENDIENTE FUNCIONAL:** confirmar con usuario de negocio el significado
   formal de `EsActivo='S'`; técnicamente es el único marcador persistido y
   cargado desde staging.
2. **PENDIENTE DE CALIDAD:** acordar si series/marca/modelo faltantes deben
   corregirse en el proceso de carga. V1 los preserva como `NULL`; no hace
   fallback al catálogo.
3. **PENDIENTE DE MODELO:** definir o retirar la tabla independiente
   `dbo.ActivoFijo`; su relación con partidas/importaciones no existe en el
   snapshot.
4. **PENDIENTE FUNCIONAL:** si se requiere saldo, baja o retorno, contratar un
   caso de uso distinto que audite sus procesos mutables y momento de cálculo.
5. **PENDIENTE WEB:** validar contra pantalla legacy autorizada el significado
   de los filtros y las columnas `F. alta`/`F. baja`; no bloquea el contrato
   read-only V1 porque dichas fechas no tienen fuente técnica demostrada.
