# Productos

Auditoría técnica de solo lectura para definir la integración del catálogo de
Productos con Módulo C bajo la estrategia **STORED PROCEDURE FIRST**. La
revisión se ejecutó sobre `CALE_IMMEX` en la instancia configurada por el
proyecto. No se ejecutó ningún stored procedure operativo y no se modificó
ningún objeto de `CALE_IMMEX` ni de `ANEXO24_DEV`.

**Rama:** `feature/backend-products`.
**Fecha de auditoría:** 2026-09-18.
**Base auditada:** `CALE_IMMEX`, esquema `dbo`.

## Regla de integración aplicada

Para `GET /api/v1/catalogos/productos` se investigó en este orden:

1. Stored procedures existentes y sus definiciones en `sys.sql_modules`,
   parámetros en `sys.parameters`, dependencias y metadata del primer result
   set.
2. Views existentes y sus definiciones.
3. No se implementó SQL productivo directo contra tablas.

Las consultas usadas fueron únicamente de metadata (`sys.procedures`,
`sys.sql_modules`, `sys.parameters`, `sys.objects`, `sys.views`,
`sys.sql_expression_dependencies` y
`sys.dm_exec_describe_first_result_set_for_object`) y consultas diagnósticas
previamente aprobadas. La metadata de algunos procedimientos reportó errores
estáticos por objetos legacy no resolubles o `xp_cmdshell`; eso no implicó
invocarlos.

## Fuente de verdad

`dbo.productos` es la tabla canónica persistente del catálogo en el corte
consultado. Tiene 204 registros exactos en `CALE_IMMEX`; la auditoría funcional
histórica reporta aproximadamente 2,630. La discrepancia sigue pendiente de
reconciliación y no se debe resolver eligiendo otra fuente sin evidencia.

La tabla canónica está confirmada por las cargas y procesos que validan o crean
productos. Sin embargo, bajo la regla nueva **no es todavía una fuente aprobada
para un adapter de consulta directa**.

**Estado de integración del caso de uso:**

> **SIN SP DE CONSULTA — REQUIERE APROBACIÓN PARA SQL DIRECTO**

## Tablas

| Objeto | Tipo | Registros del corte | Uso observado | Estado |
|---|---|---:|---|---|
| `dbo.productos` | Tabla canónica | 204 | Catálogo persistente | CONFIRMADO |
| `dbo.tmpproductos` | Stage | 0 | Entrada de `CARGA_PRODUCTOS` | CONFIRMADO |
| `dbo.ECargaProducto` | Errores de carga | 0 | Errores de validación de productos | CONFIRMADO |
| `dbo.CargaFactura` | Stage/carga | No usado como catálogo | Alimenta cargas de facturas | CONFIRMADO |
| `dbo.estructuras` | BOM | 0 | Versiones de estructura por producto | CONFIRMADO |
| `dbo.productomaterial` | Detalle BOM | 0 | Materiales por estructura/producto | CONFIRMADO |
| `dbo.cartademateriales` | Stage BOM | 0 | Entrada para construir estructuras | CONFIRMADO |
| `dbo.material` | Catálogo de materiales | 2 | Materiales de BOM | CONFIRMADO |

`dbo.v_Estructuras` integra producto, estructura y material; no es fuente de
catálogo porque puede devolver varias filas por producto, una por componente
de BOM.

## Columnas

### `dbo.productos`

| Columna | Tipo SQL | Longitud/precisión | Nullable | Evidencia | Estado |
|---|---|---:|---|---|---|
| `PRODUCTOKEY` | `numeric(18,0)` | 18,0 | No | PK `PK_productos` | CONFIRMADO |
| `CVE_PRODUCTO` | `varchar(50)` | 50 | Sí | Cargas, reportes y relaciones operativas | CONFIRMADO como código |
| `NOMBRE` | `varchar(250)` | 250 | Sí | Cargas y result sets de reportes | CONFIRMADO como descripción |
| `UNIDAD` | `varchar(10)` | 10 | Sí | `CARGA_PRODUCTOS` valida unidad comercial | CONFIRMADO como unidad comercial |
| `fraccion` | `varchar(12)` | 12 | Sí | Cargas y validaciones | CONFIRMADO |
| `CVE_PRODUCTO_CLIENTE` | `varchar(30)` | 30 | Sí | Cargas y exportaciones | CONFIRMADO como clave alternativa |
| `UNIDADT` | `varchar(10)` | 10 | Sí | Procesos de pedimentos/reportes | CONFIRMADO técnicamente |
| `NICO` | `varchar(5)` | 5 | Sí | Procesos de pedimentos | CONFIRMADO técnicamente |
| `TIPO` | `varchar(20)` | 20 | Sí | Definición de tabla | CONFIRMADO técnicamente |
| `ALMACENKEY` | `numeric(18,0)` | 18,0 | Sí | Carga mediante `ENTIDAD(DIVISION)` | CONFIRMADO como auxiliar |
| `AUXILIAR` | `varchar(50)` | 50 | Sí | Carga desde stage | CONFIRMADO como auxiliar |

Calidad del corte actual: 204 `PRODUCTOKEY` no nulos y distintos; 204
`CVE_PRODUCTO` no nulos, no blank y distintos. Es una condición de datos, no
una garantía DDL para `CVE_PRODUCTO`.

### Matriz UI → BD

| Campo UI | Campo BD | Tipo | Evidencia | Estado |
|---|---|---|---|---|
| Número de parte / código | `CVE_PRODUCTO` | `varchar(50)` nullable | Se usa como código en cargas, reportes y relaciones operativas | CONFIRMADO como código; etiqueta exacta INFERIDA |
| Descripción | `NOMBRE` | `varchar(250)` nullable | Carga y reportes lo exponen como descripción | CONFIRMADO |
| Fracción | `fraccion` | `varchar(12)` nullable | Validación de carga y reportes | CONFIRMADO |
| UMC / unidad comercial | `UNIDAD` | `varchar(10)` nullable | `CARGA_PRODUCTOS` aplica validación de unidad | CONFIRMADO como unidad; etiqueta UMC INFERIDA |
| Unidad tarifaria | `UNIDADT` | `varchar(10)` nullable | `PR_INFORME_IMPORTACIONES` la devuelve como unidad de medida tarifa | CONFIRMADO técnicamente; etiqueta UI PENDIENTE DE VALIDAR |
| Clave técnica | `PRODUCTOKEY` | `numeric(18,0)` NOT NULL | PK clustered y unique | CONFIRMADO |
| Clave del cliente | `CVE_PRODUCTO_CLIENTE` | `varchar(30)` nullable | Se usa en exportaciones/CTM | CONFIRMADO como alternativa; filtro PENDIENTE |

## Claves

- `PRODUCTOKEY` es la clave técnica: `NOT NULL`, PK `PK_productos`, clustered y
  unique.
- `CVE_PRODUCTO` es la clave funcional usada por los procesos; el DDL la declara
  nullable y no declara unique constraint.
- No se confirmó una clave funcional alternativa global en
  `CVE_PRODUCTO_CLIENTE`.

## Índices

La metadata confirmó únicamente:

| Índice | Tipo | Unique | Columnas | Estado |
|---|---|---|---|---|
| `PK_productos` | Clustered | Sí | `PRODUCTOKEY` | CONFIRMADO |

No se confirmaron índices secundarios ni unique constraint sobre
`CVE_PRODUCTO`. No se crea ni propone crear DDL como parte de esta auditoría.

## Stored procedures

### Inventario y clasificación de candidatos

La búsqueda amplia encontró 38 procedimientos con nombre o definición
relacionados con productos, catálogos, consultas, reportes, informes, cargas,
estructuras, saldos o descargos. La clasificación es estática a partir de la
definición; no se ejecutaron.

| Stored procedure | Tipo | Lectura/escritura | Uso observado | Estado |
|---|---|---|---|---|
| `BLOQUEA_DOCUMENTO` | COMMAND | WRITE | Bloqueo y cambios operativos | CONFIRMADO |
| `CARGA_ENCABEZADOS` | IMPORT | WRITE | Importación de encabezados | CONFIRMADO |
| `CARGA_ESTRUCTURADESENSAMBLE` | IMPORT | WRITE | Carga de desensamble | CONFIRMADO |
| `CARGA_FACTURAS` | IMPORT | WRITE | Crea productos/clientes y procesa facturas | CONFIRMADO |
| `CARGA_PRODUCTOS` | IMPORT | WRITE | Valida stage e inserta productos | CONFIRMADO |
| `CARGA_SUBMAQUILA` | PROCESS | MIXED | Proceso de submaquila | CONFIRMADO |
| `CARGAACTAS` | PROCESS | MIXED | Carga/proceso de actas; consulta producto como fallback | CONFIRMADO |
| `CARGACONSTANCIAS` | IMPORT/PROCESS | WRITE | Crea productos desde constancias y procesa salidas | CONFIRMADO |
| `CARGAFACTURASENPSALIDAS` | IMPORT/PROCESS | WRITE | Valida productos y crea salidas | CONFIRMADO |
| `CARGAPEDIMENTOS` | IMPORT/PROCESS | WRITE | Valida/crea productos y materiales desde pedimentos | CONFIRMADO |
| `CREAESTRUCTURAS` | PROCESS | WRITE | Construye BOM producto-material | CONFIRMADO |
| `CREAPRODUCTOSCARGAFACTURA` | IMPORT | WRITE | Crea productos faltantes de una factura | CONFIRMADO |
| `DESCARGASALIDAPEPS` | PROCESS | WRITE | Explosión de estructuras y descargo PEPS | CONFIRMADO |
| `DESCARGASCTMF` | PROCESS | WRITE | Genera datos de descarga CTM | CONFIRMADO |
| `DESCARGATSALIDA1` | PROCESS | WRITE | Descarga general y explosión | CONFIRMADO |
| `DESCARGATSALIDAFECHA` | PROCESS | WRITE | Descarga hasta una fecha | CONFIRMADO |
| `DESCARGAXFECHA51` | PROCESS | WRITE | Descarga por fecha y estructuras auxiliares | CONFIRMADO |
| `ESTRUCTURAS1A1` | PROCESS | WRITE | Genera stage y llama a `CREAESTRUCTURAS` | CONFIRMADO |
| `INFORME_CONCENTRADOSALDOS` | REPORT | WRITE | Calcula y llena `CONCENTRADOSALDOS` | CONFIRMADO |
| `INSERTAPEDIMENTO` | COMMAND | WRITE | Inserta/rectifica pedimentos y operaciones | CONFIRMADO |
| `INSTERTAFACTURASFC` | IMPORT | WRITE | Importa/crea facturas | CONFIRMADO |
| `LIGACTMFACTURA` | PROCESS | WRITE | Vincula CTM y facturas | CONFIRMADO |
| `PR_INFORME_ESTRUCTURAS` | REPORT | READ ONLY | Reporte de estructura con producto/material | CONFIRMADO |
| `PR_INFORME_EXPORTACIONES` | REPORT | READ ONLY | Reporte de exportaciones con líneas de producto | CONFIRMADO |
| `PR_INFORME_IMPORTACIONES` | REPORT | READ ONLY | Reporte de importaciones con partida/producto | CONFIRMADO |
| `PR_INFORME_SALDOS` | REPORT | READ ONLY | Reporte de saldos por partida/material | CONFIRMADO |
| `PROC_DESCARGASF4CTMA` | PROCESS | WRITE | Procesa descargas F4 CTMA | CONFIRMADO |
| `PROC_HISTORIADESCARGASALIDA` | REPORT/PROCESS | WRITE | Recalcula y llena historial de descargas | CONFIRMADO |
| `PROC_HISTORIADESCARGASALIDA;1` | REPORT/PROCESS | WRITE | Variante del historial de descargas | CONFIRMADO |
| `PROC_HISTORIADESCARGASALIDAFALTANTES` | REPORT/PROCESS | WRITE | Historial de faltantes | CONFIRMADO |
| `SALDOS` | CALCULATION | WRITE | Calcula y registra descargos/saldos | CONFIRMADO |
| `SALDOS_FAMILIA` | CALCULATION | WRITE | Calcula descargos por familia | CONFIRMADO |
| `SALDOS2` | CALCULATION | WRITE | Variante del cálculo de saldos | CONFIRMADO |
| `SALDOSDIRIGIDOS` | CALCULATION | WRITE | Cálculo de descargos dirigidos | CONFIRMADO |
| `SP_GENERA_TXT_COMPLETO` | EXPORT | MIXED | Normaliza datos, exporta por `bcp` y usa `xp_cmdshell` | CONFIRMADO |
| `SP_MensajesInicio` | MAINTENANCE | WRITE | Regenera mensajes de calidad, incluidos duplicados | CONFIRMADO |
| `Trazo_report` | REPORT | WRITE | Regenera `ANALISIS_MATERIALES` | CONFIRMADO |
| `VALIDA_I_DETALLENP` | VALIDATION | WRITE/MIXED | Valida e inserta errores de pedimentos | CONFIRMADO |

### Parámetros confirmados

Los procedimientos sin parámetros no aparecen en esta tabla. Los tipos son los
reportados por `sys.parameters`; los valores por defecto no están declarados en
los parámetros de estos candidatos.

| Procedimiento | Parámetros de entrada | Parámetros de salida |
|---|---|---|
| `BLOQUEA_DOCUMENTO` | `@PEDIMENTO varchar(50)`, `@BLOQUEADO int`, `@FOLIO int` | Ninguno |
| `CARGAFACTURASENPSALIDAS` | `@PEDIMENTO varchar(50)`, `@FACTURA varchar(50)` | Ninguno |
| `CREAPRODUCTOSCARGAFACTURA` | `@FACTURA varchar(50)` | Ninguno |
| `DESCARGASALIDAPEPS` | `@SALIDAKEY int` | Ninguno |
| `DESCARGATSALIDAFECHA` | `@HASTA datetime` | Ninguno |
| `DESCARGAXFECHA51` | `@hasta datetime` | Ninguno |
| `INFORME_CONCENTRADOSALDOS` | `@DESDE date`, `@HASTA date` | Ninguno |
| `INSERTAPEDIMENTO` | `@ITEM char(30)` | Ninguno |
| `PR_INFORME_ESTRUCTURAS` | `@PRODUCTO varchar(50)`, `@MATERIAL varchar(50)`, `@DESDE datetime`, `@HASTA datetime` | Ninguno |
| `PR_INFORME_EXPORTACIONES` | `@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)` | Ninguno |
| `PR_INFORME_IMPORTACIONES` | `@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)` | Ninguno |
| `PR_INFORME_SALDOS` | `@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)` | Ninguno |
| `PROC_DESCARGASF4CTMA` | `@DESDE datetime`, `@HASTA datetime`, `@DOCUMENTO varchar(20)` | Ninguno |
| `PROC_HISTORIADESCARGASALIDA` | `@DESDE datetime`, `@HASTA datetime`, `@PROD varchar(50)`, `@CLAVE varchar(10)`, `@DOCUMENTO varchar(20)` | Ninguno |
| `PROC_HISTORIADESCARGASALIDA;1` | Igual que `PROC_HISTORIADESCARGASALIDA` | Ninguno |
| `PROC_HISTORIADESCARGASALIDAFALTANTES` | `@DESDE datetime`, `@HASTA datetime`, `@PROD varchar(50)`, `@CLAVE varchar(10)`, `@DOCUMENTO varchar(20)` | Ninguno |
| `SALDOS` | `@ITEM varchar(50)`, `@TOTINCORPORADO numeric(18,6)`, `@TOTDESPERDICIADO numeric(18,6)`, `@TOTMERMADO numeric(18,6)`, `@SALIDALINK bigint`, `@PSALIDALINK bigint`, `@TIPO char(10)`, `@FECHAEXPORT datetime`, `@LINEA bigint`, `@PRODMATKEY bigint` | `@faltodescarga numeric(18,6) OUTPUT` |
| `SALDOS_FAMILIA` | Igual que `SALDOS` | `@faltodescarga numeric(18,6) OUTPUT` |
| `SALDOS2` | `@ITEM varchar(50)`, `@TOTINCORPORADO float`, `@TOTDESPERDICIADO float`, `@TOTMERMADO float`, `@SALIDALINK float`, `@PSALIDALINK float`, `@TIPO char(10)`, `@FECHAEXPORT datetime` | Ninguno |
| `SALDOSDIRIGIDOS` | `@ITEM varchar(50)`, `@TOTINCORPORADO float`, `@TOTDESPERDICIADO float`, `@TOTMERMADO float`, `@SALIDALINK float`, `@PSALIDALINK float`, `@PEDIMENTO varchar(30)`, `@FACTURAI varchar(50)` | Ninguno |
| `Trazo_report` | `@material varchar(100)` | Ninguno |
| `VALIDA_I_DETALLENP` | `@PEDIMENTO varchar(50)`, `@TIPO int` | Ninguno |

### Procedimientos que pueden confundirse con una consulta de Productos

#### `CARGA_PRODUCTOS`

- **Tipo:** `IMPORT`.
- **Lecturas:** `tmpproductos`, `ECargaProducto`, `unidad`, funciones
  `VALIDUNIT` y `ENTIDAD`.
- **Escrituras:** borra/inserta `ECargaProducto` e inserta filas en
  `productos`.
- **Result set:** no hay result set catalogal confirmado.
- **Reglas observadas:** valida unidad, longitud de fracción, longitud de clave,
  longitud de descripción y duplicados; después inserta los registros válidos.
- **Efecto:** escritura destructiva en tabla de errores y altas en catálogo.
- **Conclusión:** no se puede invocar para un GET.

#### `CARGA_FACTURAS` y `CREAPRODUCTOSCARGAFACTURA`

Ambos crean productos faltantes durante una carga de factura. Consultan
`TFACTURA`/`CARGAFACTURA` y `productos`, y escriben en `productos`; además
`CARGA_FACTURAS` modifica clientes, facturas, salidas y sus detalles.
`CREAPRODUCTOSCARGAFACTURA` recibe `@FACTURA` y no devuelve el catálogo.

#### `CARGACONSTANCIAS`, `CARGAPEDIMENTOS` y
`CARGAFACTURASENPSALIDAS`

Validan códigos contra `productos` y, según el proceso, crean productos,
materiales, importaciones, partidas, salidas, errores o dirigidos. Son
`IMPORT/PROCESS` con escritura y sin contrato de lectura paginada.

#### `PR_INFORME_ESTRUCTURAS`

- **Tipo:** `REPORT`, read-only.
- **Parámetros:** producto, material y rango de fechas.
- **Lee:** `productos`, `estructuras`, `productomaterial`, `material`, `salidas`
  y `psalidas`.
- **Result set:** 16 columnas, entre ellas código, descripción, unidad,
  producto interno, material, cantidades, fracciones, fechas y claves de BOM.
- **Orden:** código A24 interno, código de producto y fecha de inicio.
- **Conclusión:** devuelve una fila por relación producto-material/estructura;
  no devuelve el catálogo de productos sin BOM y no ofrece paginación.

#### `PR_INFORME_IMPORTACIONES`

- **Tipo:** `REPORT`, read-only.
- **Parámetros:** rango de fechas o documento.
- **Lee:** `importaciones`, `partidas`, `categorias`, `proveedores` y datos
  operativos relacionados.
- **Result set:** 76 columnas; incluye `Numero de parte`, `Descripcion
  mercancia`, `Fraccion Arancelaria`, `Unidad de medida comercial`, `Cantidad
  en Unidad Tarifa` y `Unidad de medida tarifa`.
- **Conclusión:** es un reporte de partidas de importación; puede repetir un
  producto y no es un catálogo estable ni paginado.

#### `PR_INFORME_EXPORTACIONES` y `PR_INFORME_SALDOS`

Son reportes read-only con parámetros de fecha/documento. Devuelven líneas de
operación o saldos, no filas canónicas de `productos`. `PR_INFORME_EXPORTACIONES`
incluye código, descripción, fracción y unidad provenientes de salidas;
`PR_INFORME_SALDOS` incluye clave, descripción, fracción, unidad y saldo por
partida. Ninguno tiene filtro de catálogo ni paginación.

#### `SP_GENERA_TXT_COMPLETO`

Contiene una extracción `bcp` de `PRODUCTOS` con `PRODUCTOKEY`,
`CVE_PRODUCTO`, `NOMBRE`, `UNIDAD`, `FRACCION` y `UNIDADT`, pero no es una API de
consulta: primero ejecuta varios `UPDATE`, usa `xp_cmdshell`, genera archivos en
una ruta del servidor y exporta otros dominios. Se clasifica `EXPORT/MIXED` y no
debe ser invocado por el backend para un GET.

### Result sets conocidos

| Procedimiento/familia | Result set | Paginación | Efectos |
|---|---|---|---|
| `PR_INFORME_ESTRUCTURAS` | Conocido, 16 columnas de BOM | No | Read-only |
| `PR_INFORME_EXPORTACIONES` | Conocido, 49 columnas operativas | No | Read-only |
| `PR_INFORME_IMPORTACIONES` | Conocido, 76 columnas de partidas | No | Read-only |
| `PR_INFORME_SALDOS` | Conocido, 37 columnas de saldos | No | Read-only |
| Cargas/procesos restantes | No hay contrato de result set catalogal confirmado | No | Escrituras, cursores o procesos |
| `SP_GENERA_TXT_COMPLETO` | Exportación por archivos, no result set API | No | `UPDATE` + `xp_cmdshell` |

La metadata del primer result set no pudo determinar resultado para
`CARGA_ENCABEZADOS` por una referencia a `dbo.NP`, para `INSERTAPEDIMENTO` por
una referencia a `PEDIMENTOS` y para `SP_GENERA_TXT_COMPLETO` por
`xp_cmdshell`. Esto es una limitación de metadata; no se ejecutaron para
averiguarlo.

## Dependencias

### Dependencias comprobadas relevantes

```text
ESTRUCTURAS1A1
    └── CREAESTRUCTURAS
          ├── dbo.productos
          ├── dbo.material
          ├── dbo.estructuras
          ├── dbo.productomaterial
          ├── dbo.cartademateriales
          ├── dbo.VALIDUNIT
          ├── dbo.EXISTEFACTOR
          └── dbo.FACTOR

DESCARGASALIDAPEPS / DESCARGATSALIDA1 / DESCARGATSALIDAFECHA
    ├── GETPRODUCTSTRUCT
    │     ├── dbo.productos
    │     └── dbo.estructuras
    └── SALDOS
          ├── dbo.productomaterial
          ├── dbo.material
          ├── dbo.partidas
          └── dbo.descarga

PR_INFORME_ESTRUCTURAS
    ├── dbo.productos
    ├── dbo.estructuras
    ├── dbo.productomaterial
    ├── dbo.material
    ├── dbo.salidas
    └── dbo.psalidas
```

`CARGA_PRODUCTOS` usa funciones de validación y escribe el catálogo; las cargas
de facturas/constancias/pedimentos dependen de `productos` para validaciones y
altas. Las referencias obtenidas de `sys.sql_expression_dependencies` no
representan FKs ni garantizan que la dependencia dinámica esté completamente
resuelta.

No se confirmó una cadena `SP de catálogo → otro SP de catálogo` que pudiera
adaptarse a `GET /api/v1/catalogos/productos`.

## Views relacionadas

La búsqueda encontró estas views con referencias a productos o a procesos que
los presentan:

| View | Tipo de datos | Por qué no sirve como catálogo |
|---|---|---|
| `dbo.v_Estructuras` | Producto + estructura + material | Multiplica filas por BOM; no es catálogo |
| `dbo.INFORME_CARGA_CARTA` | Stage/error de carta de materiales | Es diagnóstico de carga |
| `dbo.Explosion` | Explosión de descargos | Operación; mezcla producto y material |
| `dbo.v_Exportaciones` | Exportaciones | Filas operativas y sin paginación de catálogo |
| `dbo.v_saldos` | Saldos | Reporte por partida/saldo |
| `dbo.INFORMEDESCARGOS` | Descargos | Reporte operativo |
| `dbo.V_INFORMEDESCARGAS` | Descargas | Reporte operativo |
| `dbo.V_STATUS_DESCARGAS` | Estado de descargas | Estado/BOM, no catálogo |
| `dbo.V_INFORME_F4_CTMAPAA` | CTM | Reporte operativo |
| `dbo.VReporteAplicaciondesperdicios` | Desperdicios | Reporte operativo |
| `dbo.v_descarga` | Descargos | Cálculos de descarga |

No se identificó una view dedicada al catálogo de productos con contrato de
filtros y paginación. En particular, `v_Estructuras` e `INFORME_CARGA_CARTA`
requieren datos/relaciones de BOM o stage que no representan todos los productos.

## Caso de uso y fuente recomendada

**Caso de uso:** Consulta de productos.

**Endpoint:** `GET /api/v1/catalogos/productos`.

**Resultado de la investigación:**

- **SP adecuado:** no existe uno confirmado.
- **View adecuada:** no existe una view canónica confirmada.
- **Fuente canónica de datos:** `dbo.productos`, confirmada como tabla, pero no
  autorizada todavía para SQL productivo directo.
- **Decisión:** detener implementación y solicitar aprobación explícita antes de
  crear `ProductoJdbcAdapter` con SQL directo.

La opción de consulta directa sería técnicamente una consulta parametrizada sobre
`dbo.productos` con `COUNT(*)`, filtros y `ORDER BY CVE_PRODUCTO, PRODUCTOKEY`,
pero queda como propuesta, no como implementación aprobada.

## Filtros

Filtros funcionales respaldados por la auditoría previa:

| Filtro propuesto | Columna | Estado |
|---|---|---|
| Número de parte/código | `CVE_PRODUCTO` | INFERIDO como etiqueta; columna confirmada |
| Descripción | `NOMBRE` | CONFIRMADO |
| Fracción | `fraccion` | CONFIRMADO |

No se propone todavía filtrar por `CVE_PRODUCTO_CLIENTE`, `UNIDADT`, `NICO`,
`TIPO` o `ALMACENKEY`. Los SP auditados no ofrecen estos filtros como contrato
de catálogo.

## Paginación

Si se aprueba SQL directo, la API conservaría el contrato de Materiales:

- `pagina >= 1`.
- `1 <= tamano <= 100`.
- `pagina` por defecto 1 y `tamano` por defecto 20.
- Respuesta:

```json
{
  "items": [],
  "total": 0,
  "pagina": 1,
  "tamano": 20
}
```

Los procedimientos y views auditados no exponen paginación SQL para este caso.
No se debe envolver ni modificar un SP existente automáticamente.

Si se autoriza consulta directa, el orden determinista propuesto es:

```sql
ORDER BY CVE_PRODUCTO, PRODUCTOKEY
```

`CVE_PRODUCTO` refleja el orden funcional y `PRODUCTOKEY` es el desempate
estable. El orden no se ha implementado.

## Seguridad

El permiso `PRODUCTOS_CONSULTAR` existe en `ANEXO24_DEV` / esquema `app24`,
tiene dos perfiles asignados y no se agrega ningún permiso nuevo. El futuro
endpoint deberá usar ese permiso.

## Endpoint propuesto

```http
GET /api/v1/catalogos/productos
```

El flujo backend queda bloqueado hasta decidir la fuente:

```text
Caso de uso
    ↓
Endpoint
    ↓
Use Case
    ↓
Repository Port
    ↓
StoredProcedureAdapter o ViewAdapter
    ↓
SP/View aprobado
```

La alternativa directa queda deliberadamente detenida:

```text
No hay SP adecuado
    ↓
No hay View adecuada
    ↓
STOP
    ↓
Aprobación explícita para SQL directo
```

## DTO propuesto

Solo como propuesta de contrato, no implementado:

```text
productokey          numeric(18,0)
cveProducto          varchar(50)
nombre               varchar(250)
unidad                varchar(10)
fraccion             varchar(12)
cveProductoCliente   varchar(30)  // sujeto a validación
unidadt              varchar(10)  // sujeto a validación
```

`NICO`, `TIPO`, `ALMACENKEY` y `AUXILIAR` quedan fuera hasta contar con una
necesidad funcional confirmada.

## Riesgos

1. **No existe contrato de consulta:** implementar SELECT directo sin aprobación
   rompería la estrategia `STORED PROCEDURE FIRST`.
2. **Diferencia de volumen:** 204 filas actuales frente a aproximadamente 2,630
   históricas.
3. **Reportes no son catálogos:** los SP de informes pueden duplicar productos,
   limitarse a operaciones o excluir productos sin movimientos/BOM.
4. **Efectos secundarios:** cargas, descargos, saldos y exportaciones escriben o
   invocan procesos mutables.
5. **`SP_GENERA_TXT_COMPLETO`:** contiene `xp_cmdshell`, `bcp`, updates y rutas
   del servidor; no es reutilizable como endpoint de lectura.
6. **Sin FKs:** Producto/BOM/Material depende de convenciones y procesos, no de
   integridad referencial declarada.
7. **Clave funcional nullable:** `CVE_PRODUCTO` no tiene `NOT NULL` ni unique DDL.
8. **Índices:** solo se confirmó la PK clustered; filtros directos podrían
   requerir scans.
9. **Unidades:** falta confirmar la etiqueta UI exacta de `UNIDAD` y `UNIDADT`.
10. **Transacciones legacy:** los procesos existentes controlan sus propias
    escrituras; el backend no debe envolverlos con `@Transactional` sin análisis.

## Confirmado

- `CALE_IMMEX.dbo.productos` es la tabla canónica observada.
- `PRODUCTOKEY` es PK clustered, unique y `numeric(18,0) NOT NULL`.
- `CVE_PRODUCTO`, `NOMBRE`, `UNIDAD` y `fraccion` existen con los tipos indicados.
- Se revisaron 38 SP candidatos por metadata y definición.
- Los SP que crean/validan productos son de carga o proceso y escriben datos.
- Los SP de informe que contienen campos de producto no son catálogos paginados.
- `SP_GENERA_TXT_COMPLETO` es un exportador con escritura y `xp_cmdshell`.
- No existe un SP de consulta de catálogo confirmado.
- No existe una view dedicada y adecuada para el catálogo.
- `PRODUCTOS_CONSULTAR` existe en `ANEXO24_DEV`.
- No se ejecutó ningún SP mutable.

## Inferido

- `CVE_PRODUCTO` es el número de parte visible en la pantalla histórica.
- `UNIDAD` corresponde a UMC.
- `ORDER BY CVE_PRODUCTO, PRODUCTOKEY` sería determinista si se aprueba SQL
  directo.
- Las relaciones Producto → Estructura → Material se realizan mediante
  `PRODUCTOKEY`, `PRODUCTOLINK`, `ESTRUCTURAKEY`, `ESTRUCTURALINK`,
  `CVE_MATERIAL` y `CLAVE`, pero no son FKs.

## Pendiente de validar

- Reconciliar 204 contra aproximadamente 2,630 productos.
- Confirmar con negocio/UI las etiquetas de `UNIDAD` y `UNIDADT`.
- Confirmar si número de parte filtra `CVE_PRODUCTO`, `CVE_PRODUCTO_CLIENTE` o
  ambos.
- Confirmar si el dueño de Módulo C autoriza consulta directa ante la ausencia de
  SP/View.
- Confirmar permisos efectivos del usuario técnico en la tabla, si se aprueba
  SQL directo.
- Medir planes y volumen antes de decidir índices; no modificar DDL aquí.
- Confirmar DTO final y exposición de `CVE_PRODUCTO_CLIENTE`/`UNIDADT`.
- Revisar contratos de otros SP únicamente cuando se implemente su caso de uso.

## Plan exacto después de aprobación

1. Obtener aprobación explícita para SQL directo o una instrucción del dueño de
   Módulo C que defina un SP/View de consulta.
2. Si aparece un SP adecuado, documentar su contrato completo y diseñar un
   `ProductoStoredProcedureAdapter`; no copiar lógica SQL a Java.
3. Si aparece una view adecuada, documentar columnas, filtros y cardinalidad y
   diseñar un `ProductoViewAdapter`.
4. Solo si se aprueba SQL directo, crear `Producto`, `ProductoRepository`,
   `ListarProductosUseCase` y un adapter JDBC parametrizado contra
   `dbo.productos`.
5. Mantener validación `pagina >= 1`, `1 <= tamano <= 100`, el orden aprobado y
   el permiso `PRODUCTOS_CONSULTAR`.
6. Agregar cobertura 200/400/401/403/503 y pruebas del contrato de fuente.
7. Ejecutar `clean test` y `build` después de implementar.

No se implementa todavía `ProductController`, `ProductoRepository`,
`ProductoJdbcAdapter`, `ListarProductosUseCase`, DTO, mapper, frontend ni tests
funcionales de Productos. Tampoco se implementan Estructuras, Entradas, Salidas,
Materiales utilizados, Descargos, Saldos, Reportes o Facturación.
