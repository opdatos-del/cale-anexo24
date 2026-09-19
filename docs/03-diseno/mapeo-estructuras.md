# Estructuras / BOM

Auditoría técnica de solo lectura del módulo Estructuras/BOM en `CALE_IMMEX`.

- **Rama:** `feature/backend-structures`
- **Fecha de auditoría:** 2026-09-19
- **Base:** `CALE_IMMEX`, esquema `dbo`
- **Estado:** auditoría de ingeniería inversa; no se implementa backend en esta iteración.
- **Estrategia:** STORED PROCEDURE FIRST.

No se ejecutaron procedimientos operativos. No se modificaron tablas, views,
funciones ni stored procedures legacy.

## Objetivo funcional

El sistema legado representa una estructura/BOM como una relación entre un
producto, una fecha de inicio de vigencia y uno o más materiales con cantidades,
unidades, merma y desperdicio.

Se separan estos casos de uso:

1. Consulta de estructuras/BOM.
2. Consulta del detalle de una estructura.
3. Construcción o generación de estructura.
4. Relación Producto → Estructura.
5. Relación Estructura → Materiales.
6. Vigencia e inicio de estructura.
7. Cantidades, factores y unidades.
8. Errores de carga o validación.
9. Uso de estructura durante procesos de descargo.

La consulta y la construcción son contratos diferentes. `CREAESTRUCTURAS` y
`ESTRUCTURAS1A1` no son fuentes válidas para un endpoint GET porque tienen
efectos mutables.

## Modelo observado

```text
Producto
  PRODUCTOS.PRODUCTOKEY
       │
       │  ESTRUCTURAS.PRODUCTOLINK = PRODUCTOS.PRODUCTOKEY
       ▼
Estructura
  ESTRUCTURAS.ESTRUCTURAKEY
  ESTRUCTURAS.INICIO
       │
       │  PRODUCTOMATERIAL.ESTRUCTURALINK = ESTRUCTURAS.ESTRUCTURAKEY
       ▼
Detalle BOM
  PRODUCTOMATERIAL.PRODMATKEY
  PRODUCTOMATERIAL.CVE_MATERIAL
  cantidades / merma / desperdicio / unidad
       │
       │  MATERIAL.CLAVE = PRODUCTOMATERIAL.CVE_MATERIAL
       ▼
Material
  MATERIAL.MATERIALKEY
  MATERIAL.CLAVE
```

Las relaciones anteriores son joins y convenciones observadas en SQL. La
metadata confirmó **cero foreign keys DDL** entre estas cuatro tablas.

## Tablas

### `dbo.estructuras`

| Columna | Tipo | Nullable | Rol observado | Estado |
|---|---|---:|---|---|
| `estructurakey` | `bigint` | No | Identificador técnico de estructura | CONFIRMADO |
| `inicio` | `datetime` | Sí | Inicio de vigencia | CONFIRMADO |
| `productolink` | `bigint` | Sí | Referencia lógica a `productos.PRODUCTOKEY` | CONFIRMADO por joins; sin FK |

Cardinalidad del corte: **0 filas**.

Índice confirmado:

| Índice | Tipo | Único | Columna |
|---|---|---:|---|
| `PK_estructuras` | CLUSTERED | Sí | `estructurakey` |

No se observaron índices secundarios.

### `dbo.productomaterial`

| Columna | Tipo | Nullable | Rol observado | Estado |
|---|---|---:|---|---|
| `PRODMATKEY` | `numeric(18,0)` | No | Identificador técnico del detalle | CONFIRMADO |
| `CVE_MATERIAL` | `varchar(50)` | Sí | Código lógico de material | CONFIRMADO por join |
| `CANT_UTILIZADA` | `numeric(18,7)` | Sí | Cantidad incorporada | CONFIRMADO |
| `CANT_MERMADA` | `numeric(18,7)` | Sí | Cantidad de merma | CONFIRMADO |
| `CANT_DESPERDICIADA` | `numeric(18,7)` | Sí | Cantidad de desperdicio | CONFIRMADO |
| `PRODUCTOLINK` | `numeric(18,0)` | Sí | Referencia lógica al producto | CONFIRMADO por procesos |
| `DESCRIPCION` | `varchar(250)` | Sí | Descripción duplicada del material | CONFIRMADO |
| `UNIDAD` | `char(10)` | Sí | Unidad usada en el detalle | CONFIRMADO |
| `VMax` | `float` | Sí | Límite auxiliar | CONFIRMADO técnicamente; semántica pendiente |
| `VMin` | `float` | Sí | Límite auxiliar | CONFIRMADO técnicamente; semántica pendiente |
| `AUXILIAR` | `char(50)` | Sí | Dato auxiliar | CONFIRMADO técnicamente |
| `estructuralink` | `bigint` | Sí | Referencia lógica a la estructura | CONFIRMADO por joins |
| `TIPOM` | `varchar(10)` | Sí | Tipo de material | CONFIRMADO técnicamente; regla funcional pendiente |

Cardinalidad del corte: **0 filas**.

Índice confirmado:

| Índice | Tipo | Único | Columna |
|---|---|---:|---|
| `PK_productomaterial` | CLUSTERED | Sí | `PRODMATKEY` |

No se observaron índices secundarios.

### `dbo.productos`

| Columna | Tipo | Nullable | Rol en BOM | Estado |
|---|---|---:|---|---|
| `PRODUCTOKEY` | `numeric(18,0)` | No | Clave técnica enlazada desde `productolink` | CONFIRMADO |
| `CVE_PRODUCTO` | `varchar(50)` | Sí | Código funcional mostrado en reportes | CONFIRMADO |
| `NOMBRE` | `varchar(250)` | Sí | Descripción | CONFIRMADO |
| `UNIDAD` | `varchar(10)` | Sí | Unidad del producto | CONFIRMADO |
| `fraccion` | `varchar(12)` | Sí | Fracción | CONFIRMADO técnicamente |
| `CVE_PRODUCTO_CLIENTE` | `varchar(30)` | Sí | Clave alternativa | CONFIRMADO técnicamente |
| `ALMACENKEY` | `numeric(18,0)` | Sí | Dato auxiliar | CONFIRMADO técnicamente |
| `AUXILIAR` | `varchar(50)` | Sí | Dato auxiliar | CONFIRMADO técnicamente |
| `TIPO` | `varchar(20)` | Sí | Tipo de producto | CONFIRMADO técnicamente |
| `UNIDADT` | `varchar(10)` | Sí | Unidad tarifaria | CONFIRMADO técnicamente |
| `NICO` | `varchar(5)` | Sí | Dato arancelario | CONFIRMADO técnicamente |

Cardinalidad del corte: **204 filas**. No se encontraron filas relacionadas
con `estructuras` o `productomaterial` en la muestra por ausencia de datos BOM.

Índice confirmado:

| Índice | Tipo | Único | Columna |
|---|---|---:|---|
| `PK_productos` | CLUSTERED | Sí | `PRODUCTOKEY` |

### `dbo.material`

| Columna | Tipo | Nullable | Rol en BOM | Estado |
|---|---|---:|---|---|
| `materialkey` | `numeric(18,0)` | No | Clave técnica | CONFIRMADO |
| `clave` | `varchar(50)` | Sí | Código lógico enlazado desde `CVE_MATERIAL` | CONFIRMADO por join |
| `descripcion` | `varchar(250)` | Sí | Descripción de material | CONFIRMADO |
| `fraccion` | `char(10)` | Sí | Fracción | CONFIRMADO |
| `unidad` | `char(5)` | Sí | Unidad base | CONFIRMADO |
| `unidadt` | `char(5)` | Sí | Unidad tarifaria | CONFIRMADO |
| `tipomaterial` | `char(100)` | Sí | Tipo descriptivo | CONFIRMADO técnicamente |
| `tipo` | `char(5)` | Sí | Tipo | CONFIRMADO técnicamente |
| `FactorUM` | `float` | Sí | Factor auxiliar | CONFIRMADO técnicamente |
| `IGIE` | `bigint` | Sí | Dato arancelario | CONFIRMADO técnicamente |
| `CertNAFTA` | `varchar(50)` | Sí | Certificación | CONFIRMADO técnicamente |
| `claveProveedor` | `varchar(40)` | Sí | Proveedor | CONFIRMADO técnicamente |
| `ALMACENKEY` | `bigint` | Sí | Almacén | CONFIRMADO técnicamente |
| `TIPOM` | `varchar(10)` | Sí | Tipo de material usado por procesos | CONFIRMADO |
| `FAMILIA` | `varchar(20)` | Sí | Familia | CONFIRMADO técnicamente |
| `numero_serie` | `varchar(50)` | Sí | Activo/serie | CONFIRMADO técnicamente |
| `MARCA` | `varchar(50)` | Sí | Marca | CONFIRMADO técnicamente |
| `MODELO` | `varchar(50)` | Sí | Modelo | CONFIRMADO técnicamente |
| `NICO` | `varchar(5)` | Sí | Dato arancelario | CONFIRMADO técnicamente |

Cardinalidad del corte: **2 filas**. No se encontraron filas relacionadas
con `productomaterial` en la muestra actual.

Índice confirmado:

| Índice | Tipo | Único | Columna |
|---|---|---:|---|
| `PK_material` | CLUSTERED | Sí | `materialkey` |

## Claves, índices y FKs

Las cuatro tablas tienen únicamente una PK clustered observada:

- `estructuras.estructurakey`.
- `productomaterial.PRODMATKEY`.
- `productos.PRODUCTOKEY`.
- `material.materialkey`.

No se observaron índices secundarios ni unique constraints adicionales en las
tablas auditadas. La consulta de `sys.foreign_keys` no devolvió filas para FKs
que involucren estas cuatro tablas.

Las claves funcionales son convencionales y no están protegidas por FK:

- Producto → Estructura: `estructuras.productolink = productos.PRODUCTOKEY`.
- Estructura → Detalle: `productomaterial.estructuralink = estructuras.estructurakey`.
- Detalle → Material: `productomaterial.CVE_MATERIAL = material.clave`.

## Views

### `dbo.v_Estructuras`

La view es **read-only** y devuelve una fila por detalle de BOM mediante joins de
producto, estructura, `productomaterial` y material. También calcula `Ultima
Salida` con `MAX(salidas.Fecha)` unido a `psalidas` por producto.

Result set confirmado: 13 columnas.

| Columna |
|---|
| `Codigo de Producto` |
| `Descripcion del Producto` |
| `Unidad de Medida` |
| `Fecha de Inicio Estructura` |
| `Codigo de Materia Prima` |
| `Cantidad Incorporada` |
| `Cantidad Mermada` |
| `Cantidad Desperdiciada` |
| `Codigo A24 Interno` |
| `Descripcion de MP` |
| `Unidad MP` |
| `Fraccion MP` |
| `Ultima Salida` |

Limitaciones para un futuro endpoint:

- no acepta filtros como parámetros;
- no implementa paginación;
- no expone `estructurakey` ni `PRODMATKEY`;
- incluye una subconsulta operacional de salidas;
- puede multiplicar filas por detalle de estructura.

**Estado:** fuente read-only relacionada, pero no confirmada como contrato
suficiente para un catálogo paginado.

Otras views observadas (`Explosion`, `v_saldos`, `V_STATUS_DESCARGAS`) mezclan
BOM con descargos, saldos o estado operativo. No son fuentes canónicas de
consulta de estructuras.

## Stored procedures

### `dbo.PR_INFORME_ESTRUCTURAS`

**Tipo:** REPORT / QUERY de lectura.

**Lectura/escritura:** READ ONLY respecto a tablas persistentes. Usa tablas
variables locales `@res` y `@temp`, que no son tablas persistentes.

**Parámetros confirmados:**

| Parámetro | Tipo | Dirección |
|---|---|---|
| `@PRODUCTO` | `varchar(50)` | Entrada |
| `@MATERIAL` | `varchar(50)` | Entrada |
| `@DESDE` | `datetime` | Entrada |
| `@HASTA` | `datetime` | Entrada |

La definición normaliza `@PRODUCTO` y `@MATERIAL` vacíos a `NULL`. En la
revisión estática de la definición no se observó uso de `@DESDE` ni `@HASTA` en
el SELECT principal; esto requiere validación funcional adicional antes de
tratar el rango de fechas como filtro efectivo.

**Tablas leídas:**

- `dbo.productos`.
- `dbo.estructuras`.
- `dbo.productomaterial`.
- `dbo.material`.
- `dbo.salidas`.
- `dbo.psalidas`.

**Result set confirmado:** 16 columnas:

| Columna | Tipo |
|---|---|
| `Codigo de Producto` | `varchar(80)` |
| `Descripcion del Producto` | `varchar(250)` |
| `Unidad de Medida` | `char(10)` |
| `Fecha de Inicio Estructura` | `datetime` |
| `Fecha de Fin Estructura` | `datetime` |
| `Codigo de Materia Prima` | `varchar(50)` |
| `Cantidad Incorporada` | `numeric(18,7)` |
| `Cantidad Mermada` | `numeric(18,7)` |
| `Cantidad Desperdiciada` | `numeric(18,7)` |
| `Codigo A24 Interno` | `numeric(18,0)` |
| `Descripcion de MP` | `varchar(250)` |
| `Unidad MP` | `char(5)` |
| `Fraccion MP` | `char(10)` |
| `Ultima Salida` | `datetime` |
| `PRODMATKEY` | `numeric(18,0)` |
| `estructurakey` | `bigint` |

El procedimiento calcula la fecha final buscando el siguiente `inicio` mayor
para el mismo producto. No tiene `OFFSET/FETCH` ni contrato de paginación.
No se ejecutó.

**Estado:** CONFIRMADO como reporte read-only de detalle; PENDIENTE como fuente
final de un endpoint paginado.

### `dbo.CREAESTRUCTURAS`

**Tipo:** PROCESS.

**Lectura/escritura:** WRITE.

**Parámetros:** ninguno.

**Efectos observados en la definición:**

- actualiza `cartademateriales.tipodematerial`;
- elimina estructuras que coinciden con producto y fecha de la carta nueva;
- elimina detalles `productomaterial` sin estructura vigente;
- limpia `alternativo`, `generadores` y `errorcartamateriales` según sus reglas;
- recorre `cartademateriales` con cursor `FAST_FORWARD`;
- busca el producto por `CVE_PRODUCTO`;
- crea `estructuras` con `MAX(ESTRUCTURAKEY) + 1` cuando no existe producto/fecha;
- valida unidades con `VALIDUNIT`, `EXISTEFACTOR` y `FACTOR`;
- inserta filas en `productomaterial`;
- inserta materiales alternativos en `alternativo`;
- registra errores en `errorcartamateriales`.

No se observaron `BEGIN TRANSACTION`, `COMMIT`, `ROLLBACK`, `TRY` ni `CATCH` en
la definición revisada. No devuelve un result set de consulta como contrato.

**Estado:** CONFIRMADO como proceso mutable. No debe ser invocado por un GET.

### `dbo.ESTRUCTURAS1A1`

**Tipo:** PROCESS.

**Lectura/escritura:** WRITE.

**Parámetros:** ninguno.

**Efectos observados:**

1. ejecuta `TRUNCATE TABLE cartademateriales`;
2. genera filas de `cartademateriales` desde claves distintas de `partidas`,
   usando fecha `2000-01-01`, unidad de la primera partida y cantidad 1;
3. excluye materiales que ya aparecen en `productomaterial`;
4. ejecuta `CREAESTRUCTURAS` sólo si la carta tiene filas.

No se ejecutó y no se deben implementar sus efectos en esta auditoría.

**Estado:** CONFIRMADO como preparación mutable que delega en
`CREAESTRUCTURAS`.

### Otros SP relacionados

La búsqueda por nombre y definición encontró los siguientes candidatos. Se
leyeron sus metadatos, dependencias y tokens de comportamiento; no se
invocaron:

| Procedimiento | Tipo | Comportamiento | Relación observada | Estado |
|---|---|---|---|---|
| `DESCARGASALIDAPEPS` | PROCESS / CALCULATION | WRITE | Usa `GETPRODUCTSTRUCT`, explota `productomaterial` y llama `SALDOS` | CONFIRMADO |
| `DESCARGATSALIDA1` | PROCESS | WRITE | Calcula estructuras para salidas, usa `GETPRODUCTSTRUCT` y `SALDOS` | CONFIRMADO |
| `DESCARGATSALIDAFECHA` | PROCESS | WRITE | Variante por fecha; usa `GETPRODUCTSTRUCT` y `SALDOS` | CONFIRMADO |
| `DESCARGAXFECHA51` | PROCESS | WRITE | Variante por fecha; usa `estructuras`, `productomaterial`, `SALDOS` y tablas operativas | CONFIRMADO |
| `CARGAPEDIMENTOS` | IMPORT / PROCESS | WRITE / MIXED | Valida productos/materiales y factores; modifica operación de pedimentos | CONFIRMADO |
| `CARGA_MATERIALES` | IMPORT | WRITE | Carga materiales y `FACTORESMP`; usa `VALIDUNIT` | CONFIRMADO |
| `CARGA_ENCABEZADOS` | IMPORT | WRITE / MIXED | Carga encabezados; no es consulta de BOM | CONFIRMADO |
| `HISTORIADESCARGASF` | REPORT / PROCESS | WRITE / MIXED | Historial de descargos; usa descargas y unidades | CONFIRMADO |
| `HISTORIADESCARGASP` | REPORT / PROCESS | WRITE / MIXED | Historial de descargos; usa descargas y unidades | CONFIRMADO |
| `PR_COMPULSACANTIDADES` | REPORT / CALCULATION | WRITE / MIXED | Compulsa cantidades y usa factores/unidades | CONFIRMADO |
| `PR_CompulsaDSA24` | REPORT / CALCULATION | WRITE / MIXED | Compulsa de operaciones; usa factores/unidades | CONFIRMADO |
| `VALIDAPEDIMENTO` | VALIDATION | WRITE / MIXED | Validación de pedimentos y errores; no es catálogo BOM | CONFIRMADO |

Los procesos de descargo no son fuentes de consulta: borran/recalculan
`descarga`, actualizan `partidas`, escriben `trazo` y llaman procesos de saldo.

## Funciones

### `dbo.GETPRODUCTSTRUCT`

- Tipo: scalar function.
- Parámetros: `@VAR1 varchar(50)` y `@VAR2 datetime`.
- Lee `estructuras` y `productos`.
- Busca la estructura del producto cuyo `inicio <= @VAR2`, ordenada por
  `inicio DESC`, y retorna la primera `ESTRUCTURAKEY`.
- Retorna `-1` si no encuentra estructura.
- Usa cursor local para obtener el resultado.
- No escribe tablas.

**Estado:** CONFIRMADO; cálculo read-only de estructura vigente para una fecha.

### `dbo.VALIDUNIT`

- Tipo: scalar function.
- Parámetro: `@VAR1 varchar(15)`.
- Lee `dbo.UNIDAD` por `CVE_UNIDAD` o `ALIAS`.
- Retorna `CVE_UNIDAD` o `'SD'` si no encuentra equivalencia.

**Estado:** CONFIRMADO; validación read-only.

### `dbo.EXISTEFACTOR`

- Tipo: scalar function.
- Parámetros: unidad origen, unidad destino y clave de material.
- Si las unidades son iguales retorna 1.
- En otro caso cuenta filas de `FACTORESMP` con factor no cero.
- No escribe tablas.

**Estado:** CONFIRMADO; validación read-only.

### `dbo.FACTOR`

- Tipo: scalar function.
- Parámetros: unidad origen, unidad destino y clave de material.
- Normaliza la unidad destino con `VALIDUNIT`.
- Si la unidad origen está vacía, obtiene la unidad del material.
- Retorna 1 para unidades iguales o el factor de `FACTORESMP`; retorna 0 si no
  encuentra un factor válido.
- No escribe tablas.

**Estado:** CONFIRMADO; cálculo read-only.

### Funciones adicionales observadas

- `dbo.FACTORPT`: variante que consulta `FACTORESPROD` y toma la unidad del
  producto cuando la unidad origen está vacía.
- `dbo.TIENE_BOOM`: retorna `SI`/`NO` según exista un `CVE_MATERIAL` en
  `productomaterial`.

## Result sets y reglas

| Objeto | Result set | Paginación | Escritura persistente |
|---|---|---:|---:|
| `dbo.v_Estructuras` | 13 columnas de detalle | No | No |
| `dbo.PR_INFORME_ESTRUCTURAS` | 16 columnas de detalle | No | No |
| `dbo.CREAESTRUCTURAS` | No hay result set de catálogo | No | Sí |
| `dbo.ESTRUCTURAS1A1` | No hay contrato de consulta | No | Sí |

Reglas confirmadas:

- La estructura vigente se determina por el mayor `inicio` que no exceda la
  fecha consultada en `GETPRODUCTSTRUCT`.
- El reporte calcula la fecha final como el siguiente `inicio` del mismo
  producto.
- `CREAESTRUCTURAS` reutiliza la estructura cuando coincide producto y fecha;
  de lo contrario genera una nueva clave por `MAX + 1`.
- Las unidades se comparan directamente o mediante factores de `FACTORESMP`.
- `CANT_DESPERDICIADA` y `CANT_MERMADA` se multiplican por el factor calculado
  antes de insertar el detalle.
- Los procesos de descargo explotan el detalle BOM y delegan el cálculo de
  saldos en `SALDOS`.

## Grafo de dependencias

### Modelo persistente

```text
PRODUCTOS.PRODUCTOKEY
    ↓ productolink
ESTRUCTURAS.ESTRUCTURAKEY + INICIO
    ↓ estructuralink
PRODUCTOMATERIAL.PRODMATKEY
    ↓ CVE_MATERIAL = CLAVE
MATERIAL
```

**CONFIRMADO:** joins y columnas usados por `v_Estructuras`,
`PR_INFORME_ESTRUCTURAS` y `CREAESTRUCTURAS`.

**PENDIENTE:** integridad referencial DDL; no hay FKs declaradas.

### Construcción

```text
ESTRUCTURAS1A1
    ↓ TRUNCATE/INSERT cartademateriales
    ↓ EXEC
CREAESTRUCTURAS
    ├── productos
    ├── estructuras
    ├── productomaterial
    ├── material
    ├── cartademateriales
    ├── errorcartamateriales
    ├── alternativo
    ├── generadores
    ├── VALIDUNIT
    ├── EXISTEFACTOR
    └── FACTOR
```

**CONFIRMADO por definición y dependencias estáticas.**

### Descargo

```text
DESCARGASALIDAPEPS / DESCARGATSALIDA1 / DESCARGATSALIDAFECHA
    ├── GETPRODUCTSTRUCT
    │     ├── productos
    │     └── estructuras
    ├── productomaterial
    └── SALDOS
          ├── descarga
          ├── partidas
          └── trazo
```

**CONFIRMADO por definición y metadata de dependencias.** Estos procesos son
mutables y quedan fuera de la implementación de consulta.

## Fuente candidata para consulta

### Prioridad 1: SP legacy

`dbo.PR_INFORME_ESTRUCTURAS` es la mejor fuente legacy candidata para una
consulta de detalle porque:

- es read-only respecto a tablas persistentes;
- ya une producto, estructura, material, salidas y `psalidas`;
- recibe filtros de producto y material;
- retorna `PRODMATKEY` y `estructurakey`;
- expone cantidades, unidades y fechas.

Sin embargo, no es todavía un contrato confirmado para un catálogo paginado:

- no tiene `OFFSET/FETCH`;
- no se observó uso efectivo de `@DESDE` y `@HASTA`;
- su salida es un reporte de detalle, no una página API estable;
- no fue ejecutado durante esta auditoría.

### Prioridad 2: View legacy

`dbo.v_Estructuras` es read-only y presenta el detalle funcional, pero es menos
adecuada que el SP para un endpoint porque no recibe filtros, no pagina y no
expone las claves técnicas de estructura y detalle.

### Recomendación provisional

Para una futura consulta de detalle, investigar primero la adaptación de
`dbo.PR_INFORME_ESTRUCTURAS` mediante un adapter de infraestructura. Para un
endpoint paginado de catálogo, la fuente queda **PENDIENTE DE VALIDAR** hasta
confirmar el contrato de fechas, volumen y estrategia de paginación.

No se crea todavía `APP24_Q_ESTRUCTURAS_LISTAR`. Si el SP legacy no puede
satisfacer el contrato después de esa validación, se documentará la autorización
necesaria antes de crear un SP propio read-only.

## Filtros y paginación candidatos

Filtros respaldados por contratos existentes:

- producto: `@PRODUCTO varchar(50)`;
- material: `@MATERIAL varchar(50)`;
- rango de fechas: parámetros declarados `@DESDE` y `@HASTA`, pero su aplicación
  efectiva está PENDIENTE DE VALIDAR.

La vista no ofrece filtros formales.

No se define todavía `ORDER BY`, página ni tamaño de página para API porque el
SP legacy y la view no ofrecen paginación. La clave determinista candidata sería
`Codigo A24 Interno`, `Fecha de Inicio Estructura`, `PRODMATKEY`, pero queda
PENDIENTE DE VALIDAR contra el contrato funcional y el orden legado.

## Seguridad

El permiso previsto por el seed del proyecto es:

```text
ESTRUCTURAS_CONSULTAR
```

Está confirmado en el seed de seguridad versionado; la autorización efectiva de
un usuario de ambiente no se probó en esta auditoría.

## Endpoint futuro propuesto

```http
GET /api/v1/catalogos/estructuras
```

Contrato provisional pendiente de aprobación:

```text
Caso de uso
    ↓
Endpoint
    ↓
Use Case
    ↓
Repository Port
    ↓
Adapter de consulta
    ↓
PR_INFORME_ESTRUCTURAS o fuente aprobada
```

No se crean controller, use case, port, adapter, DTO ni tests funcionales en
esta iteración.

## Riesgos

1. Las tablas `estructuras` y `productomaterial` están vacías en el corte actual;
   no fue posible comparar ejemplos reales de BOM.
2. La auditoría funcional histórica reporta aproximadamente 4,981 registros de
   consulta amplia, pero el ambiente actual no contiene filas de BOM.
3. No existen FKs DDL; las relaciones dependen de códigos y procesos legacy.
4. `MAX + 1` para claves técnicas puede generar colisiones bajo concurrencia.
5. `CREAESTRUCTURAS` borra y reconstruye datos; no debe confundirse con consulta.
6. No se observaron transacciones explícitas en los procesos clave.
7. `@DESDE` y `@HASTA` están declarados en `PR_INFORME_ESTRUCTURAS`, pero su uso
   efectivo no aparece en la definición revisada.
8. `v_Estructuras` y el SP de informe no tienen paginación SQL.
9. Los procesos de descargo pueden recalcular saldos y escribir historial/trazo.
10. Las reglas de factores, unidades, merma y desperdicio están repartidas entre
    funciones y procesos; no deben copiarse a Java.

## Confirmado

- Las cuatro tablas existen en `dbo`.
- Cardinalidad actual: `estructuras=0`, `productomaterial=0`, `productos=204`,
  `material=2`.
- Cada tabla auditada tiene una PK clustered y no se observaron índices
  secundarios.
- No hay FKs DDL entre Producto, Estructura, Productomaterial y Material.
- `v_Estructuras` existe y devuelve 13 columnas de detalle.
- `PR_INFORME_ESTRUCTURAS` existe, es read-only respecto a tablas persistentes y
  devuelve 16 columnas.
- `CREAESTRUCTURAS` y `ESTRUCTURAS1A1` son procesos mutables.
- `GETPRODUCTSTRUCT`, `VALIDUNIT`, `EXISTEFACTOR` y `FACTOR` son funciones
  read-only observadas.
- `ESTRUCTURAS1A1` llama a `CREAESTRUCTURAS`.
- Los procesos principales de descargo llaman `GETPRODUCTSTRUCT` y `SALDOS`.
- No se ejecutó ningún SP mutable ni se modificó ningún objeto SQL.

## Inferido

- `estructuras.inicio` representa el inicio de vigencia de una estructura.
- La fecha de fin se deriva como el siguiente inicio para el mismo producto.
- `productolink` y `estructuralink` son claves técnicas lógicas aunque no tengan
  FK declarada.
- `PR_INFORME_ESTRUCTURAS` es la fuente legacy más cercana a un detalle de BOM.

## Pendiente de validar

- Contrato funcional exacto de consulta y si debe incluir estructuras sin
  `productomaterial`.
- Uso efectivo de `@DESDE` y `@HASTA` en el reporte.
- Orden determinista y paginación compatibles con el legado.
- Significado funcional de `VMax`, `VMin`, `AUXILIAR` y `TIPOM`.
- Ejemplos de datos BOM en un ambiente autorizado no vacío.
- Semántica de estructuras históricas y fecha final.
- Si `v_Estructuras` debe conservar la columna `Ultima Salida` en el nuevo
  contrato.
- Permisos efectivos del usuario técnico para consultar el SP legacy.
- Si se requerirá autorización para un futuro `APP24_Q_ESTRUCTURAS_LISTAR`.
