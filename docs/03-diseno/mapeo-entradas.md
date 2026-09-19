# Entradas / Importaciones / Pedimentos

Auditoría técnica de solo lectura del flujo de Entradas e Importaciones en
`CALE_IMMEX`, esquema `dbo`.

- **Rama:** `feature/backend-entries`
- **Base auditada:** `CALE_IMMEX`
- **Estado:** consulta V1 read-only implementada mediante `APP24_Q_ENTRADAS_LISTAR`; procesos de carga permanecen fuera de alcance.
- **Estrategia:** STORED PROCEDURE FIRST.
- **Procedimientos mutables:** no ejecutados.
- **Datos:** no modificados.

## Objetivo funcional

Reconstruir la relación real entre la carga/staging de pedimentos, el encabezado
de importación, sus partidas y los catálogos de producto/material:

```text
CARGAPEDIMENTOSIE (staging)
          ↓ CARGAPEDIMENTOS
IMPORTACIONES (encabezado)
          ↓ PARTIDAS.IMPORTACIONLINK
PARTIDAS (detalle)
       ↙          ↘
MATERIAL / PRODUCTOS   PROVEEDORES
          ↓
   saldo y descargo posterior
```

El procedimiento `CARGAPEDIMENTOS` también procesa registros con
`TIPOOPERACION = 2` como exportación directa y crea `SALIDAS`/`PSALIDAS`. Por
ello no debe confundirse la carga general de pedimentos con una consulta de
Entradas exclusivamente.

## Casos de uso separados

| Caso de uso | Estado de auditoría |
|---|---|
| Consultar entradas/importaciones | Implementado como listado plano paginado mediante `APP24_Q_ENTRADAS_LISTAR` |
| Consultar detalle de una entrada/pedimento | Pendiente; no se implementa endpoint por identificador |
| Importar/cargar pedimentos | `CARGAPEDIMENTOS`, mutable |
| Validar pedimento | `VALIDAPEDIMENTO`, mutable/mixto |
| Consultar partidas | Incluido como líneas en `APP24_Q_ENTRADAS_LISTAR` |
| Relacionar partida con material/producto | Relación lógica usada por procesos; sin FK DDL |
| Consultar saldos derivados | Fuera de este módulo; intervienen `SALDOS` y procesos de descargo |

No se mezclan GETs con carga, validación o modificación de inventario.

## Tablas

### `dbo.CARGAPEDIMENTOSIE`

Tabla de staging consumida por `CARGAPEDIMENTOS`.

- Filas actuales: **2**.
- PK clustered: `PK_CargaPedimentosIE(CargaKey)`, `CargaKey BIGINT NOT NULL`.
- No se observaron FKs DDL.
- No se observaron índices secundarios.

Columnas funcionales confirmadas:

| Grupo | Columnas y tipos |
|---|---|
| Identidad del documento | `Aduana varchar(50)`, `Patente varchar(50)`, `NumeroPedimento varchar(50)`, `ClavePedimento varchar(50)`, `CargaKey bigint NOT NULL` |
| Operación | `TipoOperacion int`, `TipoPedimento int`, `FechaPago datetime`, `FECHAENTRADA datetime`, `Descarga varchar(10)`, `APARTADO varchar(2)` |
| Partida | `Sec int`, `Clave varchar(50)`, `Descripcion varchar(250)`, `Fraccion varchar(50)`, `Categoria varchar(10)` |
| Unidades y cantidades | `CantidadComercial float`, `UnidadComercial varchar(50)`, `CantidadTarifa float`, `UnidadTarifa varchar(50)` |
| Valores fiscales | `IGIE float`, `IVA float`, `DTA float`, `PREV float`, `IVA_PRE numeric(18,4)`, `MULTAS numeric(18,4)`, `RECARGOS numeric(18,4)`, `CNT numeric(18,4)` |
| Comercio exterior | `PaisOD varchar(50)`, `PaisCV varchar(50)`, `ValorDolares float`, `ValorComercial float`, `ValorAduanal float`, `ValorME float`, `Factura varchar(50)`, `FechaFactura datetime`, `PedimentoOriginal varchar(50)`, `INCOTERM varchar(5)`, `NICO varchar(5)` |
| Identificación comercial | `ClaveCP varchar(50)`, `NombreCP varchar(150)`, `marca varchar(50)`, `modelo varchar(50)`, `serie varchar(50)`, `COVE varchar(100)`, `FACTORINC numeric(18,4)`, `ESACTIVO varchar(5)` |

Las columnas no indicadas como `NOT NULL` en la metadata son nullable.

### `dbo.ERRORCARGA`

Errores generados durante la validación del staging.

- Filas actuales: **0**.
- PK clustered: `PK_errorcarga(errorkey)`, `errorkey BIGINT NOT NULL`.
- No se observaron FKs ni índices secundarios.

| Columna | Tipo | Nullable | Rol |
|---|---|---:|---|
| `errorkey` | `bigint` | No | Clave técnica |
| `error` | `varchar(250)` | Sí | Mensaje de validación |
| `cargakey` | `bigint` | Sí | Referencia lógica al staging |

### `dbo.IMPORTACIONES`

Encabezado persistente de importación.

- Filas actuales: **2**.
- PK clustered: `PK_Importaciones(Ipedimentokey)`, `Ipedimentokey NUMERIC(18,0) NOT NULL`.
- No se observaron FKs DDL ni índices secundarios.

Columnas funcionales principales:

| Grupo | Columnas y tipos |
|---|---|
| Identidad | `Ipedimentokey numeric(18,0)`, `Numero_ped varchar(20)`, `Aduana varchar(10)`, `Patente varchar(10)`, `Cve_pedimento varchar(5)` |
| Fechas | `Fecha datetime`, `Fecha_ad datetime`, `FECHAORIGINAL datetime`, `FCreacion datetime`, `FECHAMODIFICACION datetime` |
| Operación | `TipoOper varchar(25)`, `TipoPed int`, `TipoOperacion varchar(10)`, `TOper int`, `DESCARGA varchar(2)`, `PedimentoOriginal varchar(20)` |
| Proveedor | `CveProveedor varchar(15)`, `NombreProveedor varchar(250)`, `TaxID varchar(50)` |
| Fiscal/valor | `tc numeric(18,4)`, `iva numeric(18,4)`, `dta numeric(18,4)`, `advalorem numeric(18,4)`, `prevalidacion numeric(18,4)`, `MULTAS numeric(18,4)`, `RECARGOS numeric(18,4)`, `IVA_PRE numeric(18,4)`, `CNT numeric(18,4)` |
| Incrementables | `FactorInc numeric(18,4)`, `FactorME numeric(18,4)`, `Fletes numeric(18,4)`, `Seguros numeric(18,4)`, `Embalajes numeric(18,4)`, `Otrosincrementables numeric(18,4)` |

### `dbo.PARTIDAS`

Detalle de mercancía de una importación.

- Filas actuales: **2**.
- PK clustered: `PK_partidas(Partidakey)`, `Partidakey NUMERIC(18,0) NOT NULL`.
- Índices secundarios observados: `INDICE1_G5`, `op7`, `op10`, `op11`.
- No se observaron FKs DDL.
- Las relaciones con `IMPORTACIONES`, `MATERIAL` y `PRODUCTOS` son lógicas.

Columnas funcionales principales:

| Grupo | Columnas y tipos |
|---|---|
| Identidad y relación | `Partidakey numeric(18,0)`, `Importacionlink numeric(18,0)`, `partida int`, `Clave varchar(50)` |
| Mercancía | `Descripcion varchar(250)`, `Fraccion varchar(15)`, `Cantidad numeric(18,4)`, `Unidad varchar(10)`, `CantidadT float`, `UnidadT char(5)`, `Categoria char(5)` |
| Valores | `Val_aduanal numeric(18,4)`, `Val_dolares numeric(18,4)`, `Val_comercial float`, `ValorME numeric(18,4)`, `Saldo numeric(18,4)` |
| Proveedor/origen | `Proveedor char(15)`, `Origen varchar(50)`, `paisvendedor varchar(50)`, `taxid varchar(50)` |
| Fiscal | `Arancel float`, `Montoigi float`, `Fpigi float`, `Montoiva float`, `Fpiva float`, `Tasaiva float`, `TasaCCompen float`, `MontoCCompen float`, `FpCCompen float`, `fpIEPS numeric(18,0)`, `montoIEPS numeric(18,4)` |
| Factura | `Factura char(30)`, `FechaFactura datetime`, `COVE varchar(100)`, `INCOTERM varchar(5)` |
| Control operativo | `PDESCARGA varchar(10)`, `ORDENDESCARGA bigint`, `FECHAIMPO datetime`, `FECHAVENCE datetime`, `DESCARGAP varchar(10)`, `APLICAA int`, `FCreacion datetime`, `USUARIO varchar(100)`, `FECHAMODIFICACION datetime` |
| Identificación adicional | `Lote varchar(50)`, `marca varchar(50)`, `modelo varchar(50)`, `serie varchar(50)`, `NICO varchar(10)`, `EsActivo varchar(2)` |

La tabla contiene más columnas técnicas y operativas; la metadata completa fue
consultada mediante `sys.columns`. No se asumió que todas formen parte del
primer contrato API.

### Catálogos relacionados

| Tabla | Filas | PK/índice principal | Relación observada |
|---|---:|---|---|
| `dbo.MATERIAL` | 2 | `PK_material(materialkey)` clustered | `PARTIDAS.Clave = MATERIAL.Clave` en reglas y funciones |
| `dbo.PRODUCTOS` | 204 | `PK_productos(PRODUCTOKEY)` clustered | `PARTIDAS.Clave = PRODUCTOS.CVE_PRODUCTO` en validaciones |
| `dbo.PROVEEDORES` | 5 | HEAP, sin PK observada | `PARTIDAS.Proveedor = PROVEEDORES.Clave` en el informe |
| `dbo.CLIENTES` | 5 | HEAP, sin PK observada | Consumida por procesos auxiliares/exportación |
| `dbo.CATEGORIAS` | 3 | HEAP, sin PK observada | `Categoria` determina meses de temporalidad |

No se observaron FKs DDL entre estas tablas y las tablas de Entradas.

### Tablas operativas relacionadas

`CARGAPEDIMENTOS` también toca, según su definición:

```text
SALIDAS
PSALIDAS
DIRIGIDO
GENERADORES
```

Cardinalidades actuales:

| Tabla | Filas |
|---|---:|
| `SALIDAS` | 660 |
| `PSALIDAS` | 3,392 |
| `DIRIGIDO` | 0 |
| `GENERADORES` | 5 |

Estas tablas no son parte del contrato de consulta de Entradas; aparecen por el
procesamiento de operaciones con `TIPOOPERACION = 2`.

## Views auditadas

| View | Result set / dependencia | Aptitud para Entradas |
|---|---|---|
| `dbo.v_Importaciones` | 70 columnas; `Importaciones`, `partidas`, `Proveedores`, `BUSCATIPOM` | Read-only, refleja el reporte, sin parámetros ni paginación |
| `dbo.v_Importaciones_EXTENDIDO` | 59 columnas; agrega `ESTA_BOOM` y descripción de tipo de operación | Read-only, útil para referencia extendida, sin contrato paginado |
| `dbo.V_ImportacionesFraccionSensible` | 10 columnas; importaciones, partidas, categorías y fracciones sensibles | Reporte especializado, no catálogo general |
| `dbo.V_I_PEDIMENTOS` | 31 columnas; `I_PEDIMENTO`, `I_FACTURA`, `I_DETALLENP`, clientes | Flujo alterno/auxiliar, no fuente canónica confirmada |
| `dbo.v_operaciones` | 15 columnas; importaciones, partidas, salidas y psalidas | Mezcla entradas y salidas |
| `dbo.v_glosa_pedimentos` | 4 columnas; glosa y `v_operaciones` | Conciliación, no consulta de entradas |
| `dbo.V_partidas` | Una columna `dummy`; depende de `v_partidas` | No sirve como fuente |

Las views no ofrecen filtros parametrizados ni paginación. `v_Importaciones` es
la referencia visual más cercana, pero el SP reporta un contrato explícito de
fechas/documento y agrega estados calculados.

## Stored procedures

### `dbo.CARGAPEDIMENTOS`

**Tipo:** `IMPORT / PROCESS`.

**Comportamiento:** `WRITE`.

**Parámetros:** ninguno.

**Tablas de entrada/staging:**

```text
CARGAPEDIMENTOSIE
```

**Tablas que modifica:**

```text
ERRORCARGA
CARGAPEDIMENTOSIE
MATERIAL
PRODUCTOS
IMPORTACIONES
PARTIDAS
SALIDAS
PSALIDAS
DIRIGIDO
GENERADORES
```

**Secuencia observada:**

1. elimina el contenido previo de `ERRORCARGA`;
2. valida longitud y presencia de aduana, patente, pedimento, clave,
   descripción, fracción, cliente/proveedor y claves de operación;
3. convierte ciertas claves (`F4`, `F5`, `A3`, `DE`) a `TIPOOPERACION = 2`;
4. usa un cursor sobre pedimentos distintos para detectar inconsistencias de
   aduana, patente, clave, tipo, tipo de cambio, fechas y pedimento original;
5. crea materiales faltantes para operaciones de importación mediante
   `MAX(MATERIALKEY) + 1` y `ROW_NUMBER()`;
6. crea productos faltantes para determinadas operaciones mediante
   `MAX(PRODUCTOKEY) + 1` y `ROW_NUMBER()`;
7. registra errores cuando no encuentra producto/material o no existe factor de
   unidades;
8. genera encabezados en `IMPORTACIONES` para registros sin error;
9. genera `PARTIDAS` relacionadas mediante `Importacionlink`;
10. genera `SALIDAS` y `PSALIDAS` para la rama de operación 2;
11. actualiza clasificación de salidas/importaciones;
12. crea registros `DIRIGIDO` para descargas dirigidas;
13. elimina registros de `GENERADORES` al terminar.

**Funciones utilizadas:**

```text
VALIDUNIT
ISVALIDUNIT
EXISTEFACTOR
FACTOR
```

**Control de flujo y errores:**

- cursor de validación confirmado;
- no se observó `BEGIN TRANSACTION`, `COMMIT` ni `ROLLBACK`;
- no se observó bloque `TRY/CATCH`;
- no se observó `RAISERROR` ni `THROW`;
- no tiene `EXEC` a otros procedimientos;
- no devuelve un result set de consulta como contrato;
- usa variables escalares y cursor, no tablas temporales persistentes.

**Conclusión:** proceso mutable de importación/procesamiento. No puede ser
invocado por un endpoint GET.

### `dbo.PR_INFORME_IMPORTACIONES`

**Tipo:** `REPORT / QUERY`.

**Comportamiento:** `READ ONLY` sobre tablas persistentes; modifica únicamente
la tabla variable local `@res`.

**Parámetros:**

| Parámetro | Tipo | Uso |
|---|---|---|
| `@DESDE` | `datetime` | Límite inferior por `Importaciones.Fecha` |
| `@HASTA` | `datetime` | Límite superior por `Importaciones.Fecha` |
| `@documento` | `varchar(50)` | Filtro por `Importaciones.Numero_ped` |

Semántica observada:

- si `@documento` está vacío o `NULL`, conserva el rango de fechas;
- si `@documento` tiene valor, establece `@DESDE` y `@HASTA` en `NULL` y filtra
  por documento;
- el predicado efectivo es:

```sql
Importaciones.Fecha BETWEEN COALESCE(@DESDE, Importaciones.Fecha)
                         AND COALESCE(@HASTA, Importaciones.Fecha)
AND Importaciones.Numero_ped = COALESCE(@documento, Importaciones.Numero_ped)
```

- une `Importaciones` con `partidas` por
  `Importaciones.Ipedimentokey = partidas.Importacionlink`;
- une opcionalmente `Proveedores` por `partidas.Proveedor = Proveedores.Clave`;
- calcula temporalidad, vencimiento, días restantes y estado sobre `@res`;
- utiliza `BUSCATIPOM(partidas.clave)`;
- ordena por fecha de pago, documento y secuencia de partida;
- no tiene paginación SQL;
- no llama otros procedimientos;
- no usa cursor, transacción ni tablas temporales persistentes;
- no modifica tablas persistentes.

**Estado:** referencia legacy read-only para consulta de entradas/detalle; no
satisface por sí solo el contrato paginado V1.

## Result set de `PR_INFORME_IMPORTACIONES`

Metadata confirmada mediante `sys.dm_exec_describe_first_result_set_for_object`:
76 columnas, todas nullable en el contrato descrito por SQL Server.

| # | Columna | Tipo SQL |
|---:|---|---|
| 1 | `Aduana` | `char(10)` |
| 2 | `Patente` | `char(10)` |
| 3 | `Documento` | `char(20)` |
| 4 | `Fecha de pago` | `datetime` |
| 5 | `Tipo de operacion` | `char(25)` |
| 6 | `Codigo de proveedor` | `char(150)` |
| 7 | `Clave de Pedimento` | `char(5)` |
| 8 | `Nombre Proveedor` | `varchar(250)` |
| 9 | `RFC/TAX ID` | `char(20)` |
| 10 | `tc` | `float` |
| 11 | `IVA` | `float` |
| 12 | `DTA` | `float` |
| 13 | `Prevalidacion` | `float` |
| 14 | `Numero de parte` | `varchar(50)` |
| 15 | `Descripcion mercancia` | `varchar(250)` |
| 16 | `Fraccion Arancelaria` | `varchar(20)` |
| 17 | `Cantidad importada comercial` | `numeric(18,4)` |
| 18 | `Unidad de medida comercial` | `varchar(10)` |
| 19 | `Valor en aduana` | `numeric(18,4)` |
| 20 | `Valor en dolares` | `numeric(18,4)` |
| 21 | `Pais Origen` | `varchar(50)` |
| 22 | `Tasa IGI` | `float` |
| 23 | `Saldo en Unidad Comercial` | `numeric(18,4)` |
| 24 | `Categoria` | `char(5)` |
| 25 | `Cantidad en Unidad Tarifa` | `float` |
| 26 | `Unidad de medida tarifa` | `char(5)` |
| 27 | `Valor comercial` | `float` |
| 28 | `Numero de Factura` | `char(50)` |
| 29 | `Secuencia del pedimento` | `int` |
| 30 | `Monto IGI` | `float` |
| 31 | `Forma de pago IGI` | `float` |
| 32 | `Monto IVA` | `float` |
| 33 | `Forma de pago IVA` | `float` |
| 34 | `Tasa Cuota Compensatoria` | `float` |
| 35 | `Monto Cuota Compensatoria` | `float` |
| 36 | `Forma de pago Cuota Compensatoria` | `float` |
| 37 | `Tasa IVA` | `float` |
| 38 | `Fecha de Factura` | `datetime` |
| 39 | `Pais Vendedor` | `varchar(50)` |
| 40 | `DESCARGA` | `varchar(2)` |
| 41 | `PedimentoOriginal` | `varchar(19)` |
| 42 | `Lote` | `varchar(50)` |
| 43 | `Complemento 1` | `varchar(50)` |
| 44 | `Complemento 2` | `varchar(50)` |
| 45 | `Complemento 3` | `varchar(50)` |
| 46 | `CNT` | `numeric(18,4)` |
| 47 | `pu` | `float` |
| 48 | `Saldo en pesos valorcomercial` | `float` |
| 49 | `Saldo en pesos de IVA` | `float` |
| 50 | `COVE` | `varchar(100)` |
| 51 | `partidakey` | `numeric(18,0)` |
| 52 | `ipedimentokey` | `numeric(18,0)` |
| 53 | `Usuario Modifico` | `varchar(100)` |
| 54 | `Fecha Modificacion` | `datetime` |
| 55 | `Aplica a 303` | `varchar(2)` |
| 56 | `observaciones` | `char(150)` |
| 57 | `Tipo Material` | `varchar(150)` |
| 58 | `SEGUROS` | `numeric(1,1)` |
| 59 | `FLETES` | `numeric(1,1)` |
| 60 | `EMBALAJES` | `numeric(1,1)` |
| 61 | `OTROS` | `numeric(1,1)` |
| 62 | `INCOTERM` | `varchar(1)` |
| 63 | `FACTORINCREMENTABLES` | `float` |
| 64 | `IDENTIFICADORPARTIDA` | `varchar(1)` |
| 65 | `FechaCreacionImportacion` | `datetime` |
| 66 | `FechaCreacionPartida` | `datetime` |
| 67 | `Fecha de entrada` | `datetime` |
| 68 | `NICO` | `varchar(10)` |
| 69 | `marca` | `varchar(150)` |
| 70 | `modelo` | `varchar(150)` |
| 71 | `serie` | `varchar(150)` |
| 72 | `FactorME` | `float` |
| 73 | `ESTADO` | `varchar(50)` |
| 74 | `DIAS RESTANTES` | `int` |
| 75 | `TEMPORALIDAD EN MESES` | `int` |
| 76 | `FECHA VENCIMIENTO` | `datetime` |

## Funciones

### `dbo.BUSCATIPOM`

Read-only. Recibe una clave de material y devuelve el primer `TIPOMATERIAL`
encontrado en `MATERIAL`, o cadena vacía si no existe.

### `dbo.VALIDUNIT`

Read-only. Busca una unidad por `CVE_UNIDAD` o `ALIAS` y devuelve la unidad
normalizada; devuelve `SD` si no encuentra equivalencia.

### `dbo.ISVALIDUNIT`

Read-only. Cuenta coincidencias de una unidad en `dbo.UNIDAD` por clave o alias;
devuelve cero si no encuentra.

### `dbo.EXISTEFACTOR`

Read-only. Devuelve si existe un factor no cero en `FACTORESMP` para material y
par de unidades; devuelve uno si las unidades ya coinciden.

### `dbo.FACTOR`

Read-only. Normaliza la unidad destino y obtiene el factor de `FACTORESMP`; usa
la unidad del material si la unidad origen está vacía y devuelve cero si no
encuentra factor.

## Grafo de dependencias

### Consulta

```text
PR_INFORME_IMPORTACIONES
    ├── Importaciones
    ├── partidas
    ├── Proveedores
    ├── categorias
    └── BUSCATIPOM
          └── material
```

### Carga

```text
CARGAPEDIMENTOSIE
    ↓
CARGAPEDIMENTOS
    ├── ERRORCARGA
    ├── MATERIAL
    │     ├── VALIDUNIT
    │     ├── ISVALIDUNIT
    │     ├── FACTOR
    │     └── EXISTEFACTOR
    ├── PRODUCTOS
    ├── IMPORTACIONES
    ├── PARTIDAS
    ├── SALIDAS
    ├── PSALIDAS
    ├── DIRIGIDO
    └── GENERADORES
```

### Descargo posterior

```text
IMPORTACIONES
    ↓ Importacionlink
PARTIDAS
    ↓ clave lógica
MATERIAL / PRODUCTOS
    ↓
SALDOS y procesos de descargo
```

Las relaciones son confirmadas por joins y definiciones SQL, pero no por FKs DDL.

## Modelo Pedimento → Partidas → Material

Relaciones confirmadas:

```text
IMPORTACIONES.Ipedimentokey = PARTIDAS.Importacionlink
PARTIDAS.Clave = MATERIAL.Clave       (uso lógico en procesos)
PARTIDAS.Clave = PRODUCTOS.CVE_PRODUCTO (validación según operación)
PARTIDAS.Proveedor = PROVEEDORES.Clave
```

Cardinalidad observada en el ambiente actual:

- `2` staging rows en `CARGAPEDIMENTOSIE`;
- `2` encabezados en `IMPORTACIONES`;
- `2` partidas en `PARTIDAS`;
- `2` `Importacionlink` distintos en `PARTIDAS`;
- por lo tanto, el corte actual muestra una partida por cada encabezado, pero
  esto no prueba una regla general de 1:1;
- el proceso y el modelo permiten `1 pedimento → N partidas`;
- no se confirmó una relación física obligatoria de una partida con un único
  material o producto.

## Reglas Anexo 24 identificadas

| Dato/regla | Evidencia | Estado |
|---|---|---|
| Aduana | `CARGAPEDIMENTOSIE.Aduana`; valida longitud 3 | CONFIRMADO |
| Patente | `CARGAPEDIMENTOSIE.Patente`; valida longitud 4 | CONFIRMADO |
| Número de pedimento | `NumeroPedimento` / `Importaciones.Numero_ped` | CONFIRMADO |
| Clave de pedimento | `ClavePedimento` / `Cve_pedimento` | CONFIRMADO |
| Tipo de operación | `TipoOperacion`; 1 importación, 2 rama de exportación/cambio | CONFIRMADO en proceso |
| Fecha de pago | `FechaPago` / `Importaciones.Fecha` | CONFIRMADO |
| Fecha de entrada | `FECHAENTRADA` / `Importaciones.Fecha_ad` | CONFIRMADO |
| Fracción arancelaria | staging y `PARTIDAS.Fraccion` | CONFIRMADO |
| Cantidad comercial | `CantidadComercial` / `PARTIDAS.Cantidad` | CONFIRMADO |
| Unidad comercial | `UnidadComercial` / `PARTIDAS.Unidad` | CONFIRMADO |
| Cantidad/unidad tarifa | `CantidadTarifa`, `UnidadTarifa` / `CantidadT`, `UnidadT` | CONFIRMADO |
| Valor y contribuciones | IGIE, IVA, DTA, PREV, valores y tasas | CONFIRMADO técnicamente |
| Proveedor | `ClaveCP`, `PARTIDAS.Proveedor`, `PROVEEDORES` | CONFIRMADO como relación lógica |
| Saldo | `PARTIDAS.Saldo` y cálculos de reportes | CONFIRMADO técnicamente; regla de saldo PENDIENTE |
| Vigencia | `categorias.meses`, fecha de vencimiento calculada en el informe | CONFIRMADO en reporte |
| Relación producto/material | Rama de operación y validaciones de `CARGAPEDIMENTOS` | INFERIDO según operación |

No se reinterpretaron reglas fiscales ni se copiaron cálculos a Java.

## Procesos auxiliares relacionados

La búsqueda por nombre/definición detectó, entre otros:

| Procedimiento | Clasificación | Comportamiento observado | Estado |
|---|---|---|---|
| `CARGA_ENCABEZADOS` | IMPORT/PROCESS | Escribe `IMPORTACIONES` y `SALIDAS` desde tablas auxiliares | CONFIRMADO por definición/flags |
| `CARGA_FACTURAS` | IMPORT/PROCESS | Escribe facturas, salidas y tablas de error; usa cursor | CONFIRMADO por definición/flags |
| `CARGA_MATERIALES` | IMPORT | Escribe materiales/factores | CONFIRMADO por definición/flags |
| `CARGA_PRODUCTOS` | IMPORT | Escribe productos y errores | CONFIRMADO por definición/flags |
| `CARGAPROVEEDORES` | IMPORT | Escribe proveedores desde staging | CONFIRMADO por definición/flags |
| `CREAPRODUCTOSCARGAFACTURA` | IMPORT | Crea productos desde carga de factura | CONFIRMADO por definición/flags |
| `INSERTAPEDIMENTO` | COMMAND/PROCESS | Escribe pedimentos, importaciones, partidas, salidas y psalidas; llama validación | CONFIRMADO por definición/flags |
| `VALIDAPEDIMENTO` | VALIDATION/PROCESS | Escribe errores/inventario y usa `VALIDUNIT` | CONFIRMADO por definición/flags |
| `SP_ImportacionesBorradas` | MAINTENANCE | Escribe respaldo/borrado de importaciones | CONFIRMADO por dependencias |
| `SP_PartidasBorradas` | MAINTENANCE | Escribe respaldo/borrado de partidas | CONFIRMADO por dependencias |

Estos procedimientos quedan fuera de una futura consulta GET.

## Validación funcional controlada

La ejecución de `dbo.PR_INFORME_IMPORTACIONES` se realizó únicamente contra
`CALE_IMMEX` y no produjo escrituras persistentes.

### Dataset actual

| Evidencia | Resultado |
|---|---:|
| Importaciones | 2 |
| Partidas | 2 |
| `MIN(Importaciones.Fecha)` | `2025-09-23 00:00:00` |
| `MAX(Importaciones.Fecha)` | `2026-05-28 00:00:00` |
| Documentos existentes | `5003971`, `6001720` |
| Partidas por importación | 1 y 1 en el dataset actual |

### Casos ejecutados

| Caso | Parámetros | Filas | Resultado |
|---|---|---:|---|
| A | rango `2025-09-23` a `2026-05-28`, `@documento = NULL` | 2 | devuelve ambas partidas |
| B | documento `5003971`, fechas `NULL` | 1 | devuelve la partida del documento |
| C | documento `5003971`, rango `1925-09-23` a `1926-09-23` | 1 | el documento prevalece y desactiva el rango |

El caso C confirma empíricamente el comportamiento de la definición: cuando
`@documento` tiene valor, el procedimiento pone `@DESDE` y `@HASTA` en `NULL`.
El procedimiento devuelve una fila plana por partida e incluye
`partidakey` e `ipedimentokey`.

### Identidad de encabezado y partida

Para las filas actuales se confirmó:

| `Ipedimentokey` | `Partidakey` | `Importacionlink` |
|---:|---:|---:|
| 1001 | 2001 | 1001 |
| 1002 | 2002 | 1002 |

`Ipedimentokey` es único en `IMPORTACIONES`, `Partidakey` es único en
`PARTIDAS` y `Importacionlink` relaciona el detalle con su encabezado. No se
observaron duplicados de esas claves ni duplicados de filas en los tres casos.
El ambiente actual muestra una partida por importación, pero el proceso y el
modelo siguen permitiendo `1 pedimento → N partidas`; no se debe modelar la
entrada con una sola clave plana.

### Mapeo de los ocho campos históricos

La definición desplegada y la ejecución confirman el siguiente mapeo:

| Pantalla/Excel | `PR_INFORME_IMPORTACIONES` | Tabla fuente | Estado |
|---|---|---|---|
| pedimento | `Documento` | `Importaciones.Numero_ped` | CONFIRMADO |
| clave pedimento | `Clave de Pedimento` | `Importaciones.Cve_pedimento` | CONFIRMADO |
| fecha de entrada | `Fecha de entrada` | `Importaciones.Fecha_ad` | CONFIRMADO |
| fracción | `Fraccion Arancelaria` | `Partidas.Fraccion` | CONFIRMADO |
| UMC | `Unidad de medida comercial` | `Partidas.Unidad` | CONFIRMADO |
| cantidad | `Cantidad importada comercial` | `Partidas.Cantidad` | CONFIRMADO |
| número de parte | `Numero de parte` | `Partidas.Clave` | CONFIRMADO |
| fecha de pago | `Fecha de pago` | `Importaciones.Fecha` | CONFIRMADO |

Las cantidades se exponen como `numeric(18,4)` en el contrato del reporte; el
futuro contrato Java debe usar `BigDecimal`, no `float` ni `double`. Los IDs
`partidakey` e `ipedimentokey` son `numeric(18,0)` y deben conservarse como
`BigDecimal` o un tipo entero equivalente, nunca como flotante.

### Comparación con `v_Importaciones`

`dbo.v_Importaciones` reproduce los ocho campos con los mismos orígenes
funcionales y también expone `partidakey` e `ipedimentokey`. Sin embargo,
publica 70 columnas, no recibe parámetros, no pagina y no devuelve total.
Además, conserva cálculos/reportes como saldos, valores derivados y
`BUSCATIPOM`. Es una referencia visual legacy, no un contrato API parametrizado.

### Campos calculados dependientes de `GETDATE()`

`PR_INFORME_IMPORTACIONES` calcula `TEMPORALIDAD EN MESES`, `FECHA VENCIMIENTO`,
`DIAS RESTANTES` y `ESTADO`. Los días restantes y el estado asociado a la
vigencia cambian con `GETDATE()`, por lo que no son datos históricos estables.
La evidencia de la pantalla básica de Entradas sólo requiere los ocho campos
anteriores. Estos cálculos pertenecen al reporte de importaciones/vigencias o a
un futuro módulo de vencimientos, no al contrato V1 de consulta de Entradas.
No se copian a Java ni se reinterpretan en esta fase.

## Contrato funcional V1 implementado

El primer corte debe ser un listado plano paginado de líneas de entrada:

```http
GET /api/v1/operaciones/entradas
```

Cada fila conserva ambos niveles de identidad:

```text
importacionId
partidaId
pedimento
clavePedimento
fechaEntrada
fraccion
unidadComercial
cantidadComercial
numeroParte
fechaPago
```

Tipos implementados:

| Campo | Tipo candidato |
|---|---|
| `importacionId` | `BigDecimal` para `numeric(18,0)` |
| `partidaId` | `BigDecimal` para `numeric(18,0)` |
| `pedimento` | `String` |
| `clavePedimento` | `String` |
| `fechaEntrada` | `LocalDateTime` |
| `fraccion` | `String` |
| `unidadComercial` | `String` |
| `cantidadComercial` | `BigDecimal` para `numeric(18,4)` |
| `numeroParte` | `String` |
| `fechaPago` | `LocalDateTime` |

`GET /api/v1/operaciones/entradas/{importacionId}` puede quedar para una
segunda iteración de detalle. No se justifica introducir header/detail en V1
cuando la pantalla histórica trabaja con líneas y el listado puede preservar
los dos IDs técnicos.

### Filtros V1

| Filtro | Estado y evidencia |
|---|---|
| `desde` | Obligatorio; filtra `Importaciones.Fecha` desde el inicio del día |
| `hasta` | Obligatorio; filtra `Importaciones.Fecha` hasta el final del día |
| `pedimento` | Opcional; filtro acumulativo sobre `Importaciones.Numero_ped` |
| `clavePedimento` | Opcional; filtro acumulativo sobre `Importaciones.Cve_pedimento` |
| `fraccion` | Opcional; filtro acumulativo sobre `Partidas.Fraccion` |
| `numeroParte` | Opcional; filtro acumulativo sobre `Partidas.Clave` |
| `pagina`, `tamano` | Implementados; página base 1 y tamaño entre 1 y 100 |

La obligatoriedad de `desde`/`hasta` proviene de la UI histórica y quedó
implementada. Todos los filtros son acumulativos: `pedimento` no desactiva ni
modifica el rango de fechas. El rango se aplica a `Importaciones.Fecha`, que
representa `fechaPago`; `fechaEntrada` se devuelve desde `Importaciones.Fecha_ad`
pero no es el eje temporal de V1.

## Decisión de fuente

| Fuente | Ventajas | Limitaciones para V1 |
|---|---|---|
| `PR_INFORME_IMPORTACIONES` | Read-only, IDs técnicos, ocho campos confirmados, rango de fechas y documento | 76 columnas, sin paginación/total, filtros incompletos y cálculos dinámicos ajenos |
| `v_Importaciones` | Read-only, proyección visual cercana y ocho campos disponibles | 70 columnas, sin parámetros, sin paginación/total, no contrato estable |
| `APP24_Q_ENTRADAS_LISTAR` | Fija ocho campos, IDs, filtros acumulativos, fechas obligatorias, orden, paginación y `@Total` | No incluye campos de reporte ajenos al contrato V1 |

**Decisión implementada:** `dbo.APP24_Q_ENTRADAS_LISTAR` es la fuente del
contrato HTTP V1. `PR_INFORME_IMPORTACIONES` y `v_Importaciones` permanecen como
referencias legacy, no como contratos públicos. El SP propio es estrictamente
read-only y no copia cálculos dinámicos de temporalidad, saldo o vencimiento.

## Implementación read-only

### `dbo.APP24_Q_ENTRADAS_LISTAR`

Fuente implementada para `GET /api/v1/operaciones/entradas`.

- Tipo: `QUERY`.
- Escrituras: ninguna; no ejecuta SP legacy ni funciones mutables.
- Tablas leídas: `dbo.Importaciones` y `dbo.Partidas`.
- Relación: `Partidas.Importacionlink = Importaciones.Ipedimentokey`.
- Parámetros: `@Desde DATE`, `@Hasta DATE`, `@Pedimento VARCHAR(20)`,
  `@ClavePedimento VARCHAR(5)`, `@Fraccion VARCHAR(15)`,
  `@NumeroParte VARCHAR(50)`, `@Pagina INT`, `@Tamano INT` y `@Total BIGINT OUTPUT`.
- Rango: `Importaciones.Fecha >= @Desde` y `< DATEADD(DAY, 1, @Hasta)`.
- Filtros de texto: acumulativos, parametrizados y con normalización de blank a
  `NULL`; no se utiliza `LIKE` ni SQL dinámico.
- Orden: fecha, documento, ID de importación, secuencia y ID de partida.
- Paginación: `OFFSET/FETCH` con operandos `BIGINT` para evitar overflow.

El result set tiene sólo estos diez campos:

```text
IMPORTACION_ID       numeric(18,0)
PARTIDA_ID           numeric(18,0)
PEDIMENTO            varchar(20)
CLAVE_PEDIMENTO     varchar(5)
FECHA_ENTRADA        datetime
FRACCION            varchar(15)
UNIDAD_COMERCIAL    varchar(10)
CANTIDAD_COMERCIAL  numeric(18,4)
NUMERO_PARTE        varchar(50)
FECHA_PAGO          datetime
```

No se incluyen saldo, estado, días restantes, temporalidad, vencimiento,
valores fiscales, proveedor, factura, `GETDATE()` ni `BUSCATIPOM`.

### Validación SQL del SP implementado

El procedimiento fue desplegado únicamente en `CALE_IMMEX`. No se crearon datos
ni se ejecutaron procesos mutables.

| Caso | Resultado `@Total` | Resultado |
|---|---:|---|
| A. Rango completo actual | 2 | dos líneas |
| B. Fecha de la primera importación | 1 | `Ipedimentokey=1001`, `Partidakey=2001` |
| C. Fecha de la segunda importación | 1 | `Ipedimentokey=1002`, `Partidakey=2002` |
| D. Pedimento existente dentro del rango | 1 | una línea |
| E. Mismo pedimento fuera del rango | 0 | confirma que el pedimento no desactiva fechas |
| F. Clave de pedimento existente | 2 | dos líneas |
| G. Fracción existente | 2 | dos líneas |
| H. Número de parte existente | 1 | una línea |
| I. Combinación de filtros válidos | 1 | una línea |
| J. Página `2147483647`, tamaño 100 | 2 | result set vacío, sin overflow |

La metadata obtenida con `sys.dm_exec_describe_first_result_set_for_object`
coincide exactamente con el RowMapper del adapter: aliases, tipos y
nullability fueron comprobados para los diez campos.

## Seguridad

El seed versionado contiene:

```text
OPERACIONES_CONSULTAR
```

No existe evidencia de un permiso específico adicional para Entradas. No se
agregan permisos en esta fase.

## Riesgos

1. `CARGAPEDIMENTOS` es mutable y no muestra transacción explícita ni
   `TRY/CATCH`; no debe llamarse desde una consulta.
2. `CARGAPEDIMENTOS` usa `MAX + 1` y `ROW_NUMBER()` para claves técnicas.
3. Las relaciones entre importaciones, partidas, materiales, productos y
   proveedores carecen de FKs DDL.
4. `PR_INFORME_IMPORTACIONES` mezcla consulta con cálculos de saldo/temporalidad
   y depende de `categorias` y `BUSCATIPOM`.
5. El result set legacy usa nombres con espacios y mezcla tipos `float`,
   `numeric`, `char` y `varchar`.
6. El SP no pagina y no devuelve total.
7. Los cálculos de vigencia dependen de `GETDATE()` y no son parte del listado
   histórico básico.
8. El ambiente actual sólo contiene dos importaciones y dos partidas; no permite
   validar cardinalidades amplias.
9. La rama de `TIPOOPERACION = 2` procesa salidas dentro de `CARGAPEDIMENTOS`,
   lo que puede contaminar una interpretación de Entrada.
10. Las reglas de saldo, descargo y temporalidad no deben copiarse a Java sin
    auditar primero sus procesos especializados.

## Confirmado

- `CARGAPEDIMENTOS` existe, no recibe parámetros y es mutable.
- `PR_INFORME_IMPORTACIONES` existe, recibe fechas/documento y es read-only sobre
  tablas persistentes.
- La ejecución controlada devolvió 2 filas por rango, 1 por documento con fechas
  nulas y 1 por documento con rango no coincidente.
- El documento existente desactiva efectivamente el rango de fechas.
- El result set de `PR_INFORME_IMPORTACIONES` tiene 76 columnas confirmadas por
  metadata.
- Los ocho campos históricos de Entradas tienen mapeo confirmado en el SP y sus
  tablas fuente.
- `Importaciones.Ipedimentokey = Partidas.Importacionlink` es la relación usada.
- Las tablas auditadas no tienen FKs DDL entre sí.
- `CARGAPEDIMENTOSIE` es staging y actualmente tiene 2 filas.
- `IMPORTACIONES` tiene 2 filas y `PARTIDAS` tiene 2 filas.
- No se ejecutó ningún proceso mutable.
- `APP24_Q_ENTRADAS_LISTAR` fue desplegado y validado en solo lectura.
- No se modificaron datos, tablas, views, funciones ni SP legacy.

## Inferido

- Un pedimento/importación puede tener N partidas.
- `PARTIDAS.Clave` representa el número de parte/código que se valida contra
  material o producto según el tipo de operación.
- `PR_INFORME_IMPORTACIONES` representa una fila plana por partida.
- `v_Importaciones` es la proyección visual más cercana al informe.
- El listado V1 debe ser plano y conservar `importacionId` y `partidaId`; el detalle
  header/detail queda para una iteración posterior.

## Pendiente de validar

- `planta` y `periodo`, observados en flujos ampliados pero sin mapeo confirmado
  para este contrato V1.
- Endpoint de detalle por `importacionId`.
- Semántica pública de saldo y temporalidad.
- Correspondencia funcional definitiva entre tipo de operación, producto y
  material.
- Relación con pedimentos alternos, rectificaciones y retornos.
- Datos reales suficientes para validar 1:N y casos con múltiples partidas.

## Estado

```text
CONSULTA V1 IMPLEMENTADA — APP24 QUERY READ-ONLY

FUENTE HTTP:
APP24_Q_ENTRADAS_LISTAR

REFERENCIAS LEGACY:
PR_INFORME_IMPORTACIONES y v_Importaciones — NO SON CONTRATO HTTP

CARGAPEDIMENTOS:
PROCESO MUTABLE — NO USAR EN GET
```
