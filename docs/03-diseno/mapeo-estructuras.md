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
- `TOP (100) PERCENT ... ORDER BY` dentro de una view no es un contrato de orden
  estable para una API;
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

La definición normaliza `@PRODUCTO` y `@MATERIAL` vacíos a `NULL`. La segunda
lectura directa de `sys.sql_modules` confirmó que `@DESDE` y `@HASTA` aparecen
únicamente en sus declaraciones de parámetros: cada uno tiene una ocurrencia y
ninguno aparece en el SELECT, en el WHERE, en SQL dinámico ni en una llamada a
otro procedimiento. Por tanto:

> **CONFIRMADO:** `@DESDE` y `@HASTA` forman parte del contrato, pero actualmente
> no afectan el result set.

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
- `ESTRUCTURAKEY` es `BIGINT`, pero la función está declarada como `RETURNS
  FLOAT`; el valor se asigna a `@RESULTADO FLOAT` antes de retornarse.
- No escribe tablas.

El futuro dominio/API no debe modelar `estructurakey` como `Float`/`Double`.
La representación técnica deberá conservar la precisión del `BIGINT`, por
 ejemplo `Long` en Java, sujeto a la revisión del contrato final.

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
7. `@DESDE` y `@HASTA` están declarados en `PR_INFORME_ESTRUCTURAS`, pero la
   segunda auditoría confirmó que no afectan el result set.
8. `ESTRUCTURAS1A1` usa un `SELECT *` posicional que desplaza `UMC` y
   `CANTIDADUMC`; la conversión en ejecución no fue probada por la política de
   no ejecutar procesos mutables.
9. `v_Estructuras` y el SP de informe no tienen paginación SQL.
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
- Semántica funcional que el legado esperaba para `@DESDE` y `@HASTA`, aunque su
  uso efectivo en el SP está confirmado como inexistente.
- Orden determinista y paginación compatibles con el legado.
- Significado funcional de `VMax`, `VMin`, `AUXILIAR` y `TIPOM`.
- Ejemplos de datos BOM en un ambiente autorizado no vacío.
- Semántica de estructuras históricas y fecha final.
- Si `v_Estructuras` debe conservar la columna `Ultima Salida` en el nuevo
  contrato.
- Permisos efectivos del usuario técnico para consultar el SP legacy.
- Si se requerirá autorización para un futuro `APP24_Q_ESTRUCTURAS_LISTAR`.

## Segunda pasada de auditoría

La segunda pasada se ejecutó el 2026-09-19 contra `CALE_IMMEX` mediante
consultas de metadata y lectura de `sys.sql_modules`. No se ejecutaron
`CREAESTRUCTURAS` ni `ESTRUCTURAS1A1`, no se insertaron datos de prueba y no se
modificó ningún objeto SQL.

### Contrato real de `PR_INFORME_ESTRUCTURAS`

Los parámetros desplegados son exactamente:

| Parámetro | Tipo SQL | Dirección | Uso observado |
|---|---|---|---|
| `@PRODUCTO` | `varchar(50)` | Entrada | Normalizado a `NULL` si está vacío y usado en el filtro de producto |
| `@MATERIAL` | `varchar(50)` | Entrada | Normalizado a `NULL` si está vacío y usado en el filtro de material |
| `@DESDE` | `datetime` | Entrada | Declarado; no usado en expresiones del procedimiento |
| `@HASTA` | `datetime` | Entrada | Declarado; no usado en expresiones del procedimiento |

La definición desplegada no contiene SQL dinámico ni `EXEC`/`sp_executesql`.
El conteo textual de referencias fue una ocurrencia para cada parámetro, que
corresponde a su declaración. No hay dependencia que consuma esos parámetros.

**CONFIRMADO:** `@DESDE` y `@HASTA` forman parte del contrato legado, pero
actualmente no afectan el result set. La pantalla puede exponer el rango, pero
el comportamiento efectivo del SP no lo respalda como filtro.

### Revisión de `ESTRUCTURAS1A1` y `UMC`/`CANTIDADUMC`

La definición desplegada inserta en ocho columnas destino y utiliza `SELECT *`
sobre una subconsulta con siete expresiones, en este orden:

| Posición | Columna destino | Expresión fuente | Tipo fuente confirmado | Tipo destino confirmado |
|---:|---|---|---|---|
| 1 | `cartadematerialeskey` | `ROW_NUMBER() + @CARTAMATERIALES` | `bigint` | `float` |
| 2 | `codigodeproducto` | `PRODUCTO` (`PARTIDAS.CLAVE`) | `varchar(50)` | `varchar(50)` |
| 3 | `codigodematerial1` | `CLAVE` (`PARTIDAS.CLAVE`) | `varchar(50)` | `varchar(50)` |
| 4 | `iniciovigencia` | `FECHA` (`'2000-01-01'`) | `datetime` implícito | `datetime` |
| 5 | `cantidadumc` | `UMC` (`PARTIDAS.UNIDAD`) | `varchar(10)` | `numeric(18,7)` |
| 6 | `umc` | `CANTIDADUMC` (`1`) | `int` | `varchar(50)` |
| 7 | `desperdicio` | `DESPERDICIO` (`0`) | `int` | `numeric(18,7)` |
| 8 | `merma` | `MERMA` (`0`) | `int` | `numeric(18,7)` |

**CONFIRMADO:** existe una discrepancia posicional entre `cantidadumc`/`umc` y
las expresiones `UMC`/`CANTIDADUMC`. SQL Server debe convertir implícitamente la
unidad `varchar(10)` a `numeric(18,7)` para la quinta posición y el entero `1` a
`varchar(50)` para la sexta. El efecto runtime concreto no se ejecutó por la
prohibición de invocar procesos mutables; por ello no se afirma aquí un resultado
operativo observado. La definición presenta un riesgo de conversión/error si la
unidad contiene texto no numérico.

### Invariante `productolink + inicio`

La metadata actual de `dbo.estructuras` sólo muestra:

- `PK_estructuras`, clustered, primary key y unique sobre `estructurakey`.
- Ningún índice secundario.
- Ninguna unique constraint sobre `(productolink, inicio)`.
- Ninguna FK DDL hacia `productos`.

Por tanto, `(productolink, inicio)` **no es una garantía DDL**. Es una regla de
negocio inferida y asumida por procesos legacy:

- `CREAESTRUCTURAS` busca `ESTRUCTURAKEY` con
  `PRODUCTOLINK = @PRODUCTOKEY AND INICIO = @INICIOVIGENCIA`; si no encuentra
  una fila crea una clave con `MAX(ESTRUCTURAKEY) + 1`.
- `GETPRODUCTSTRUCT` selecciona `TOP 1` por producto con `INICIO <= fecha`,
  ordenando únicamente por `INICIO DESC`; dos filas con la misma fecha no tienen
  desempate determinista.
- `PR_INFORME_ESTRUCTURAS` calcula el fin como el siguiente `inicio` mayor del
  mismo producto.
- Los procesos de descargo consumen `GETPRODUCTSTRUCT`, por lo que dependen de
  la selección de una estructura vigente, pero no imponen unicidad.

**Conclusión:** la unicidad es una regla de negocio inferida, no una garantía
física. No se crea índice ni constraint en esta auditoría.

### `BIGINT` frente a `FLOAT` en `GETPRODUCTSTRUCT`

`dbo.estructuras.ESTRUCTURAKEY` es `BIGINT`. La función desplegada declara
`RETURNS FLOAT`, almacena el valor en `@RESULTADO FLOAT` y devuelve `-1` cuando no
encuentra una estructura. Esto puede introducir una representación numérica
inadecuada para una clave técnica entera, aunque la función legacy lo haya
establecido así.

El futuro dominio/API debe conservar `ESTRUCTURAKEY` como entero de 64 bits
(`Long` en Java o equivalente), nunca como `Float`/`Double`. La adaptación
puede requerir tratar el resultado legacy con cuidado, pero no se modifica la
función en esta fase.

### Caso de uso funcional propuesto

`v_Estructuras` y `PR_INFORME_ESTRUCTURAS` devuelven una fila por componente de
`productomaterial`, acompañada de datos del producto y de la estructura. No
exponen un result set separado de encabezados. Sus joins no constituyen una
consulta de estructuras sin detalle: la forma observada está orientada a
relaciones Producto ↔ Material.

Por compatibilidad con la pantalla legacy y por simplicidad, el primer corte de
consulta debe ser un **listado plano de líneas BOM**:

```text
una fila = producto + estructura vigente/histórica + componente de material
```

No se recomienda comenzar con un modelo header/detail. La necesidad de listar
estructuras sin componentes queda PENDIENTE; la forma actual de ambos objetos no
las entrega como filas independientes.

### Endpoint y filtros candidatos

Propuesta, no implementada:

```http
GET /api/v1/catalogos/estructuras
```

| Filtro | Evidencia | Estado |
|---|---|---|
| `producto` | `@PRODUCTO varchar(50)` filtra `CVE_PRODUCTO` | CONFIRMADO |
| `material` | `@MATERIAL varchar(50)` filtra `CVE_MATERIAL` | CONFIRMADO |
| `desde` | Parámetro legado declarado, pero sin efecto en el SP | PENDIENTE de contrato funcional |
| `hasta` | Parámetro legado declarado, pero sin efecto en el SP | PENDIENTE de contrato funcional |
| `pagina` | No existe en SP ni view; es una necesidad de contrato API | PENDIENTE |
| `tamano` | No existe en SP ni view; es una necesidad de contrato API | PENDIENTE |

Los filtros `producto` y `material` son los únicos respaldados por ejecución
estática del contrato existente. No se implementa paginación por cuenta propia
en esta auditoría.

### Semántica temporal candidata

El sistema ofrece dos evidencias relacionadas:

1. `GETPRODUCTSTRUCT` selecciona la estructura con mayor `inicio` que cumpla
   `inicio <= fecha`, lo que respalda una consulta "vigente a una fecha".
2. `PR_INFORME_ESTRUCTURAS` deriva `fin` como el siguiente `inicio` del mismo
   producto, lo que representa una vigencia por intervalos, aunque no filtra por
   ella.

| Alternativa | Regla | Evidencia | Estado |
|---|---|---|---|
| A | `inicio BETWEEN @Desde AND @Hasta` | No aparece en ningún SP/view auditado | INFERIDA; débilmente respaldada |
| B | `inicio <= @Hasta AND (fin IS NULL OR fin >= @Desde)` | Compatible con `fin` derivado y con la selección as-of de `GETPRODUCTSTRUCT` | INFERIDA; requiere negocio |

La alternativa B tiene mayor respaldo técnico para un rango de vigencia porque
incluye una estructura que ya estaba activa al inicio del rango. Sin embargo,
la semántica oficial de `desde/hasta`, inclusividad de límites y tratamiento de
fechas nulas siguen PENDIENTES de validar con negocio.

### Campos de response para un listado plano

La siguiente propuesta conserva los campos observados sin aprobar todavía el
contrato público:

| Campo propuesto | Fuente legacy | Clasificación | Motivo |
|---|---|---|---|
| `estructuraId` | `PR_INFORME_ESTRUCTURAS.estructurakey` (`bigint`) | TÉCNICO NECESARIO | Identifica la fila de estructura; no lo expone la view |
| `productoId` | `Codigo A24 Interno` (`productos.PRODUCTOKEY`) | TÉCNICO NECESARIO | Identificador técnico del producto |
| `productoClave` | `Codigo de Producto` | CONFIRMADO PARA UI | Código mostrado por el reporte/view |
| `productoDescripcion` | `Descripcion del Producto` | CONFIRMADO PARA UI | Descripción mostrada |
| `productoUnidad` | `Unidad de Medida` | CONFIRMADO PARA UI | Unidad mostrada |
| `fechaInicio` | `Fecha de Inicio Estructura` | CONFIRMADO PARA UI | Fecha presente en reporte/view |
| `fechaFin` | `Fecha de Fin Estructura` del SP | CONFIRMADO PARA UI | Calculada por el reporte; no está en la view |
| `productoMaterialId` | `PRODMATKEY` (`numeric(18,0)`) | TÉCNICO NECESARIO | Identifica la línea BOM |
| `materialClave` | `Codigo de Materia Prima` | CONFIRMADO PARA UI | Código mostrado |
| `materialDescripcion` | `Descripcion de MP` | CONFIRMADO PARA UI | Descripción mostrada |
| `materialUnidad` | `Unidad MP` | CONFIRMADO PARA UI | Unidad mostrada |
| `materialFraccion` | `Fraccion MP` | CONFIRMADO PARA UI | Fracción mostrada |
| `cantidadIncorporada` | `Cantidad Incorporada` | CONFIRMADO PARA UI | Cantidad reportada |
| `cantidadMermada` | `Cantidad Mermada` | CONFIRMADO PARA UI | Merma reportada |
| `cantidadDesperdiciada` | `Cantidad Desperdiciada` | CONFIRMADO PARA UI | Desperdicio reportado |
| `ultimaSalida` | `Ultima Salida` | OPCIONAL | Dato operativo calculado desde `salidas`/`psalidas`; no es núcleo de BOM |

`estructuraId` debe conservarse como entero de 64 bits y `productoMaterialId`
debe respetar la precisión de `numeric(18,0)` hasta aprobar el modelo de
backend. Las etiquetas funcionales y la necesidad de `ultimaSalida` requieren
validación de UI/negocio.

### Comparación de fuentes

| Fuente | Solo lectura | Filtros | Fechas efectivas | Paginación | IDs técnicos | Contrato |
|---|---:|---|---|---:|---|---|
| `dbo.PR_INFORME_ESTRUCTURAS` | Sí | Producto/material | No; parámetros declarados sin efecto | No | Sí: `PRODMATKEY`, `estructurakey`, producto | Reporte de detalle legacy |
| `dbo.v_Estructuras` | Sí | No parametrizados | Sólo `inicio` como columna | No | No: omite `estructurakey` y `PRODMATKEY` | Referencia de detalle |
| `dbo.APP24_Q_ESTRUCTURAS_LISTAR` | No existe | Diseñables | Diseñables | Diseñable | Diseñables | Requeriría autorización y contrato nuevo |

**Recomendación:** para una consulta compatible con la pantalla legacy y sin
requisitos de paginación, `PR_INFORME_ESTRUCTURAS` es la fuente preferida y debe
consumirse mediante un adapter, no mediante SQL directo. Para un endpoint
paginado con fechas efectivas, ninguna fuente existente satisface el contrato:
la view no tiene filtros/IDs y el SP no pagina ni aplica fechas. En ese caso,
`APP24_Q_ESTRUCTURAS_LISTAR` sería la alternativa técnica eventual, pero queda
pendiente de aprobación explícita; no se crea en esta fase.

**Estado de esta auditoría:** consulta plana de detalle respaldada por
`PR_INFORME_ESTRUCTURAS`; contrato paginado y semántica temporal aún PENDIENTES.
