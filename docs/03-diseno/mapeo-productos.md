# Productos

Auditoría técnica de solo lectura para definir la integración del catálogo de
Productos con Módulo C. La auditoría se ejecutó sobre `CALE_IMMEX` en la
instancia remota configurada por el proyecto y sobre `ANEXO24_DEV` únicamente
para confirmar el permiso de aplicación. No se modificaron objetos de ninguna
base de datos.

**Base de trabajo:** `dev` actualizado con `feature/backend-materials-hardening`.
**Rama de auditoría:** `feature/backend-products`.
**Fecha de consulta:** 2026-09-18.

## Fuente de verdad

La tabla principal confirmada para el catálogo es `dbo.productos` de
`CALE_IMMEX`.

Evidencia:

- `dbo.productos` existe como tabla de usuario.
- Tiene 204 registros en la base consultada en esta auditoría.
- Los procedimientos de carga `CARGA_PRODUCTOS`, `CARGA_FACTURAS`,
  `CREAPRODUCTOSCARGAFACTURA`, `CARGACONSTANCIAS` y `CARGAPEDIMENTOS` insertan o
  validan datos contra `dbo.productos`.
- Las vistas y funciones relacionadas con estructuras, descargos y saldos
  consumen `dbo.productos`, pero no presentan un contrato de listado paginado
  de productos.
- Los procedimientos encontrados son procesos de carga, validación u
  operación; no se identificó un stored procedure existente que sea una fuente
  de consulta paginada del catálogo.

La auditoría funcional documenta aproximadamente 2,630 productos. La consulta
actual de `CALE_IMMEX` devolvió 204 registros. La diferencia es un riesgo y queda
pendiente de reconciliar: puede corresponder a ambiente, fecha de extracción,
permisos, datos depurados o alcance distinto de la consulta funcional. No debe
ocultarse mediante una consulta alternativa sin validar el origen.

## Tablas

| Objeto | Tipo | Registros aproximados/actuales | Uso observado | Estado |
|---|---|---:|---|---|
| `dbo.productos` | Tabla principal | 204 exactos | Catálogo persistente de productos | **CONFIRMADO** |
| `dbo.tmpproductos` | Tabla stage | 0 | Entrada temporal para `CARGA_PRODUCTOS` | **CONFIRMADO** |
| `dbo.ECargaProducto` | Tabla de errores de carga | 0 | Errores asociados a la carga de productos | **CONFIRMADO** |
| `dbo.CargaFactura` | Tabla de carga | No se tomó como fuente de catálogo | Alimenta procesos de facturas y altas derivadas | **CONFIRMADO** |
| `dbo.estructuras` | Tabla de estructuras/BOM | 0 | Versiones de estructura asociadas a producto | **CONFIRMADO** |
| `dbo.productomaterial` | Tabla detalle de BOM | 0 | Materiales y cantidades por estructura/producto | **CONFIRMADO** |
| `dbo.cartademateriales` | Stage/entrada de BOM | 0 | Entrada de relaciones producto-material | **CONFIRMADO** |
| `dbo.material` | Catálogo de materiales | 2 en la consulta actual | Materiales referenciados por BOM | **CONFIRMADO** |
| `dbo.estructurasML` | Auxiliar de estructuras | 0 | Auxiliar generado por procesos de descarga | **CONFIRMADO** |

`dbo.v_Estructuras` es una vista de integración de producto, estructura y
material. No es la fuente recomendada para el catálogo de Productos porque
puede multiplicar filas por cada componente de una estructura.

## Columnas

### `dbo.productos`

| Columna | Tipo SQL | Longitud/precisión | Nullable | Evidencia | Estado |
|---|---|---:|---|---|---|
| `PRODUCTOKEY` | `numeric(18,0)` | 18,0 | No | Definición de tabla y PK | **CONFIRMADO** |
| `CVE_PRODUCTO` | `varchar(50)` | 50 | Sí | Cargas, vistas y joins operativos | **CONFIRMADO** como código de producto |
| `NOMBRE` | `varchar(250)` | 250 | Sí | Cargas y vistas de estructuras | **CONFIRMADO** como nombre/descripción |
| `UNIDAD` | `varchar(10)` | 10 | Sí | `CARGA_PRODUCTOS` aplica `VALIDUNIT(UNIDAD)` | **CONFIRMADO** como unidad comercial candidata a UMC |
| `fraccion` | `varchar(12)` | 12 | Sí | Cargas, validaciones y vistas | **CONFIRMADO** |
| `CVE_PRODUCTO_CLIENTE` | `varchar(30)` | 30 | Sí | `CARGA_PRODUCTOS`, `v_Exportaciones` | **CONFIRMADO** como clave alternativa del cliente |
| `ALMACENKEY` | `numeric(18,0)` | 18,0 | Sí | Carga mediante `ENTIDAD(DIVISION)` | **CONFIRMADO** como dato auxiliar; significado funcional pendiente |
| `AUXILIAR` | `varchar(50)` | 50 | Sí | Carga desde `tmpproductos` | **CONFIRMADO** como dato auxiliar |
| `TIPO` | `varchar(20)` | 20 | Sí | Columna existente | **CONFIRMADO** técnicamente; significado funcional pendiente |
| `UNIDADT` | `varchar(10)` | 10 | Sí | Columna existente y referencias operativas | **CONFIRMADO** como unidad tarifaria; etiqueta UI pendiente |
| `NICO` | `varchar(5)` | 5 | Sí | Columna existente y procesos de pedimentos | **CONFIRMADO** técnicamente; fuera del mínimo UI observado |

La tabla no tiene defaults relevantes para las columnas del catálogo. La
consulta de calidad devolvió, para el corte actual:

- 204 filas.
- 204 `PRODUCTOKEY` distintos y no nulos.
- 204 `CVE_PRODUCTO` distintos y no nulos/no blank.
- Ningún `NOMBRE`, `UNIDAD` o `fraccion` nulo o blank en la muestra actual.

Esas condiciones de datos actuales no sustituyen las restricciones de esquema:
las columnas funcionales siguen declaradas nullable.

### Matriz de mapeo UI → BD

| Campo UI | Campo BD | Tipo | Evidencia | Estado |
|---|---|---|---|---|
| Número de parte / código de producto | `CVE_PRODUCTO` | `varchar(50)` nullable | El sistema y los SP lo usan como código, clave de producto y criterio de existencia | **CONFIRMADO** como código; **INFERIDO** como etiqueta exacta “número de parte” |
| Descripción | `NOMBRE` | `varchar(250)` nullable | `CARGA_PRODUCTOS` carga `NOMBRE`; vistas lo presentan como descripción | **CONFIRMADO** |
| Fracción | `fraccion` | `varchar(12)` nullable | `CARGA_PRODUCTOS` valida longitud; procesos y vistas la consultan | **CONFIRMADO** |
| UMC / unidad comercial | `UNIDAD` | `varchar(10)` nullable | `CARGA_PRODUCTOS` la valida como unidad comercial; procedimientos la usan para operaciones | **CONFIRMADO** como unidad comercial; etiqueta UMC **INFERIDA** |
| Unidad / unidad tarifaria | `UNIDADT` | `varchar(10)` nullable | Columna explícita y referencias a unidad tarifa en procesos de pedimentos | **CONFIRMADO** técnicamente; etiqueta UI exacta **PENDIENTE DE VALIDAR** |
| Clave del cliente | `CVE_PRODUCTO_CLIENTE` | `varchar(30)` nullable | Campo alternativo validado por `CARGA_PRODUCTOS` | **CONFIRMADO** como dato auxiliar; no incluir como filtro sin validar UX |
| Clave técnica | `PRODUCTOKEY` | `numeric(18,0)` NOT NULL | PK clustered única | **CONFIRMADO** |

## Claves

- **Clave técnica:** `PRODUCTOKEY`.
  - `NOT NULL`.
  - `PRIMARY KEY` `PK_productos`.
  - Índice clustered y unique por ser PK.
- **Clave funcional:** `CVE_PRODUCTO`, usada por los SP, vistas y relaciones
  operativas como código/número de producto.
  - Actualmente todos los 204 valores son distintos y no nulos.
  - El esquema la declara nullable y no existe una unique constraint.
  - Por tanto, la unicidad funcional actual es un hecho de datos, no una
    garantía formal de DDL.
- `CVE_PRODUCTO_CLIENTE` es una clave alternativa de cliente, no se confirmó
  como clave funcional global.

## Índices

Para `dbo.productos` se confirmó:

| Índice | Tipo | Unique | Columnas | Estado |
|---|---|---|---|---|
| `PK_productos` | `CLUSTERED` | Sí | `PRODUCTOKEY` | **CONFIRMADO** |

No se observaron índices secundarios ni unique constraints sobre
`CVE_PRODUCTO` en la metadata consultada. La única entrada de índice reportada
para `dbo.productos` fue su PK clustered.

Implicación: los filtros por `CVE_PRODUCTO`, `NOMBRE` y `fraccion` pueden
requerir scans en el estado actual. No se debe crear un índice desde este
mapeo; cualquier cambio de DDL requiere una decisión separada del propietario
de Módulo C.

## Stored procedures

La revisión de `docs/03-diseno/procedimientos-almacenados.md` se contrastó con
las definiciones actuales de `sys.sql_modules`. Se encontraron 36
procedimientos con referencia textual a `PRODUCT`/`PRODUCTOS`; 52 aparecieron
en la búsqueda amplia que también incluyó familias de carga, facturas y
estructuras. La referencia textual no significa que todos sean fuentes del
catálogo.

### Procedimientos de carga y mantenimiento del catálogo

| Procedimiento | Parámetros | Consulta/lee | Modifica | Resultado/objetivo aparente | Estado |
|---|---|---|---|---|---|
| `CARGA_PRODUCTOS` | Ninguno | `tmpproductos`, `ECargaProducto`, `unidad`, funciones `VALIDUNIT` y `ENTIDAD` | `ECargaProducto`, `productos` | Valida unidad, fracción, claves, descripción y duplicados; inserta productos válidos desde stage | **CONFIRMADO**, carga |
| `CARGA_FACTURAS` | Ninguno | `TFACTURA`, `productos`, `TERRORFACTURA`, `FACTURA` | `productos`, `TERRORFACTURA`, `clientes`, `FACTURA`, `SALIDAS`, `PSALIDAS`, `GENERADORES` | Crea productos faltantes desde facturas y continúa el proceso de facturación | **CONFIRMADO**, carga/proceso |
| `CREAPRODUCTOSCARGAFACTURA` | `@FACTURA varchar(50)` | `CARGAFACTURA`, `productos`, funciones de validación | `productos` | Crea productos faltantes de una factura si unidad, fracción y longitud cumplen | **CONFIRMADO**, carga |
| `CARGACONSTANCIAS` | Ninguno | `CONSTANCIATRANSF`, `productos`, `settings` | `ERRORCARGA`, `productos`, `IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `PSALIDAS`, `DIRIGIDO` | Opcionalmente crea productos desde constancias y procesa operaciones | **CONFIRMADO**, carga/proceso |
| `CARGAPEDIMENTOS` | Ninguno | `CARGAPEDIMENTOSIE`, `productos`, `material`, funciones de unidad/factor | `ERRORCARGA`, `productos`, `material`, `IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `PSALIDAS`, `DIRIGIDO` | Valida y crea productos/materiales a partir de pedimentos; carga operaciones | **CONFIRMADO**, carga/proceso |

Ninguno de estos procedimientos expone un contrato estable de consulta
paginada para `GET /api/v1/catalogos/productos`. Sus definiciones contienen
`INSERT`, `UPDATE`, `DELETE`, tablas de stage o procesos operativos. No deben
invocarse para resolver un endpoint de lectura.

### Procedimientos de estructuras y relaciones

| Procedimiento | Parámetros | Consulta/lee | Modifica | Objetivo aparente | Estado |
|---|---|---|---|---|---|
| `CREAESTRUCTURAS` | Ninguno | `cartademateriales`, `productos`, `material`, `estructuras`, funciones de unidad/factor | `estructuras`, `productomaterial`, `alternativo`, `errorCartaMateriales`, `generadores` | Construye estructuras y sus componentes a partir de una carta de materiales | **CONFIRMADO**, proceso de BOM |
| `ESTRUCTURAS1A1` | Ninguno | `PARTIDAS`, `productomaterial` | `cartademateriales`; invoca `CREAESTRUCTURAS` | Genera una carta auxiliar y dispara la creación de estructuras | **CONFIRMADO**, proceso |

### Procedimientos operativos que consumen Productos

| Procedimientos | Parámetros confirmados | Uso observado | Clasificación |
|---|---|---|---|
| `CARGAFACTURASENPSALIDAS` | `@PEDIMENTO varchar(50)`, `@FACTURA varchar(50)` | Valida código, fracción y unidad contra `productos`; inserta `PSALIDAS` y errores | **CONFIRMADO**, proceso |
| `CARGA_SUBMAQUILA` | Ninguno | Consulta `productos` para completar fracción de `PSALIDAS`; modifica salidas | **CONFIRMADO**, proceso |
| `CARGAACTAS` | Ninguno | Usa `productos` como fallback cuando no encuentra datos en `material` | **CONFIRMADO**, proceso |
| `DESCARGASALIDAPEPS` | No confirmado en esta extracción | Usa producto, estructura y `productomaterial` para explosión de descargos | **CONFIRMADO** por definición; parámetros/resultados exactos **PENDIENTES** |
| `DESCARGASCTMF` | Ninguno | Genera salida de CTM y consulta datos asociados a producto | **CONFIRMADO**, proceso |
| `DESCARGATSALIDA1`, `DESCARGATSALIDAFECHA`, `DESCARGAXFECHA51` | Ninguno / fecha en `DESCARGATSALIDAFECHA` | Procesan descargos, estructuras y auxiliares; no son catálogo | **CONFIRMADO**, proceso |
| `LIGACTMFACTURA` | Ninguno | Consulta `PRODUCTOS.CVE_PRODUCTO_CLIENTE` para reportar CTM | **CONFIRMADO**, proceso |

### Inventario completo de referencias textuales

Los siguientes procedimientos también contienen referencias textuales a
Productos, pero no fueron considerados fuente de consulta del catálogo:

`BLOQUEA_DOCUMENTO`, `CARGA_ENCABEZADOS`, `CARGA_ESTRUCTURADESENSAMBLE`,
`CARGA_FACTURAS`, `CARGA_PRODUCTOS`, `CARGA_SUBMAQUILA`, `CARGAACTAS`,
`CARGACONSTANCIAS`, `CARGAFACTURASENPSALIDAS`, `CARGAPEDIMENTOS`,
`CREAESTRUCTURAS`, `CREAPRODUCTOSCARGAFACTURA`, `DESCARGASALIDAPEPS`,
`DESCARGASCTMF`, `DESCARGATSALIDA1`, `DESCARGATSALIDAFECHA`,
`DESCARGAXFECHA51`, `ESTRUCTURAS1A1`, `INSERTAPEDIMENTO`, `INSTERTAFACTURASFC`,
`LIGACTMFACTURA`, `PR_INFORME_ESTRUCTURAS`, `PR_INFORME_EXPORTACIONES`,
`PR_INFORME_SALDOS`, `PROC_DESCARGASF4CTMA`, `PROC_HISTORIADESCARGASALIDA`,
`PROC_HISTORIADESCARGASALIDA;1`, `PROC_HISTORIADESCARGASALIDAFALTANTES`,
`SALDOS`, `SALDOS_FAMILIA`, `SALDOS2`, `SALDOSDIRIGIDOS`,
`SP_GENERA_TXT_COMPLETO`, `SP_MensajesInicio`, `Trazo_report` y
`VALIDA_I_DETALLENP`.

Para los procedimientos operativos secundarios la metadata comprobó la
referencia, pero esta auditoría no los propone para el endpoint ni reimplementa
sus reglas. Sus parámetros, result sets y tablas exactas deben revisarse en la
fase del módulo que los necesite. Estado: **INFERIDO/PENDIENTE DE VALIDAR**
según el caso.

## Relaciones

### Relaciones comprobables por DDL

La consulta a `sys.foreign_keys` no devolvió foreign keys que involucren
`productos`, `estructuras`, `productomaterial` o `material`. Por lo tanto, no
existe una relación referencial declarada que pueda reportarse como FK real.

### Relaciones inferidas por columnas y SQL existente

```text
productos.PRODUCTOKEY
        │  (referenciado por nombre PRODUCTOLINK)
        ▼
estructuras.PRODUCTOLINK
        │  (referenciado por nombre ESTRUCTURALINK)
        ▼
productomaterial.ESTRUCTURALINK
        │
        └── productomaterial.PRODUCTOLINK ──► productos.PRODUCTOKEY

productomaterial.CVE_MATERIAL ──► material.CLAVE

cartademateriales.CODIGODEPRODUCTO ──(por CREAESTRUCTURAS)──► productos.CVE_PRODUCTO
cartademateriales.CODIGODEMATERIAL1 ──(por CREAESTRUCTURAS)──► material.CLAVE
```

- `productos → estructuras` está inferido por `ESTRUCTURAS.PRODUCTOLINK` y
  por `CREAESTRUCTURAS`.
- `estructuras → productomaterial` está inferido por
  `PRODUCTOMATERIAL.ESTRUCTURALINK` y por las vistas/procedimientos.
- `productomaterial → material` está inferido por `CVE_MATERIAL` frente a
  `material.CLAVE`; no es FK DDL.
- La relación de `cartademateriales` es una relación de stage por códigos y
  reglas del procedimiento, no una FK.

No se implementará Estructuras en esta fase.

## Mapeo UI → BD

El sistema legado documentó para Productos: fracción, descripción, unidad, UMC
y número de parte. El mapeo técnico queda así:

- `CVE_PRODUCTO` es el código funcional usado por el sistema para identificar
  el producto; se propone como número de parte de consulta, pero la equivalencia
  exacta del texto de la pantalla queda **INFERIDA**.
- `NOMBRE` corresponde a descripción.
- `fraccion` corresponde a fracción arancelaria.
- `UNIDAD` es la unidad comercial validada por `CARGA_PRODUCTOS` y es la mejor
  correspondencia confirmada para UMC.
- `UNIDADT` es la unidad tarifaria; no se confirmó qué etiqueta visible del
  legado la mostraba como “unidad”.
- `CVE_PRODUCTO_CLIENTE` es una clave de cliente y no debe confundirse con el
  número de parte global sin una validación de UX/negocio.

## Filtros

Filtros propuestos únicamente con evidencia suficiente:

| Filtro API propuesto | Columna | Estado | Observación |
|---|---|---|---|
| `filtro` para número de parte/código | `CVE_PRODUCTO` | **INFERIDO**, respaldado por uso operativo | Buscar por coincidencia parcial si se conserva el patrón de Materiales |
| `filtro` para descripción | `NOMBRE` | **CONFIRMADO** por mapeo de columna | La pantalla histórica documenta descripción |
| `filtro` para fracción | `fraccion` | **CONFIRMADO** | La pantalla histórica documenta fracción |

No se propone aún un filtro separado por `CVE_PRODUCTO_CLIENTE`, `UNIDADT`,
`NICO`, `TIPO` o `ALMACENKEY` porque no están confirmados como filtros del
catálogo legado.

## Paginación

El contrato propuesto conserva el patrón aprobado para Materiales:

- `pagina >= 1`.
- `1 <= tamano <= 100`.
- Sin normalización silenciosa de valores inválidos.
- Respuesta:

```json
{
  "items": [],
  "total": 0,
  "pagina": 1,
  "tamano": 20
}
```

Orden determinista propuesto para la consulta directa:

```sql
ORDER BY CVE_PRODUCTO, PRODUCTOKEY
```

Justificación:

- `CVE_PRODUCTO` es la clave funcional y el orden visible esperado.
- `PRODUCTOKEY` es `NOT NULL`, PK, unique y clustered; desempata códigos
  funcionales repetidos o futuros datos sin unique constraint.
- `CVE_PRODUCTO` está declarado nullable, por lo que el código debe tolerar
  nulos aunque el corte actual no contiene ninguno.
- No se propone ordenar por `NOMBRE`, `fraccion` o `CVE_PRODUCTO_CLIENTE` porque
  no son claves únicas y pueden cambiar o repetirse.

## Seguridad

El permiso `PRODUCTOS_CONSULTAR` existe en el seed de `app24` y fue confirmado
contra `ANEXO24_DEV`:

- clave: `PRODUCTOS_CONSULTAR`;
- recurso: `productos`;
- acción: `CONSULTAR`;
- perfiles asignados en la consulta: 2.

No se agrega ningún permiso nuevo.

## Endpoint propuesto

```text
GET /api/v1/catalogos/productos
```

Parámetros propuestos:

- `filtro` opcional, aplicado a `CVE_PRODUCTO`, `NOMBRE` y `fraccion` según el
  contrato final aprobado;
- `pagina`, base 1, default 1;
- `tamano`, default 20 y máximo 100.

La autoridad requerida será `PRODUCTOS_CONSULTAR`.

Flujo previsto, sujeto a aprobación:

```text
Caso de uso
    ↓
GET /api/v1/catalogos/productos
    ↓
Use Case
    ↓
Repository Port
    ↓
JDBC Adapter
    ↓
dbo.productos en CALE_IMMEX
```

El endpoint no debe invocar `CARGA_PRODUCTOS`, `CARGA_FACTURAS`,
`CREAESTRUCTURAS` ni otro SP de proceso.

## DTO propuesto

El DTO público inicial propuesto es:

```text
productokey          numeric(18,0)
cveProducto          varchar(50)
nombre               varchar(250)
unidad                varchar(10)
fraccion             varchar(12)
cveProductoCliente   varchar(30)
unidadt               varchar(10)
```

`productokey`, `cveProducto`, `nombre`, `unidad` y `fraccion` cubren la consulta
mínima. `cveProductoCliente` y `unidadt` se proponen como campos técnicos
útiles, pero su exposición final depende de validar la pantalla y el contrato
funcional. `NICO`, `TIPO`, `ALMACENKEY` y `AUXILIAR` quedan fuera del DTO inicial
salvo nueva evidencia.

## Riesgos

1. **Diferencia de volumen:** la auditoría funcional reporta aproximadamente
   2,630 productos y la base consultada contiene 204. No implementar filtros o
   paginación asumiendo que ambos cortes son equivalentes.
2. **Sin FK reales:** las relaciones producto-estructura-material dependen de
   convenciones de nombres, procesos y datos, no de integridad referencial DDL.
3. **Clave funcional nullable:** `CVE_PRODUCTO` no tiene `NOT NULL` ni unique
   constraint, aunque todos los registros actuales cumplen ambas condiciones.
4. **Índices:** solo se confirmó la PK clustered; los filtros de catálogo pueden
   requerir scans.
5. **Reglas dispersas:** las validaciones de carga viven en SP y funciones
   antiguas; una consulta de catálogo no debe reimplementar reglas de carga.
6. **Unidades:** la diferencia entre unidad comercial (`UNIDAD`) y unidad
   tarifaria (`UNIDADT`) debe confirmarse contra la UI antes de fijar nombres
   públicos.
7. **Stage vacío:** `tmpproductos` y `ECargaProducto` están vacíos en el corte,
   por lo que no se pudo observar una corrida de carga activa.
8. **Permisos cruzados:** el datasource de Módulo C y `ANEXO24_DEV` son bases
   distintas; el backend debe conservar la separación de datasources ya usada
   por Materiales.

## Confirmado

- Base de consulta: `CALE_IMMEX`, esquema `dbo`.
- Tabla principal: `dbo.productos`.
- PK: `PK_productos` sobre `PRODUCTOKEY`, clustered y unique.
- `PRODUCTOKEY` es `numeric(18,0) NOT NULL`.
- Columnas del catálogo: `CVE_PRODUCTO`, `NOMBRE`, `UNIDAD`, `fraccion`.
- Columnas auxiliares existentes: `CVE_PRODUCTO_CLIENTE`, `UNIDADT`, `NICO`,
  `TIPO`, `ALMACENKEY`, `AUXILIAR`.
- No hay FKs declaradas hacia o desde los objetos producto/BOM revisados.
- `PRODUCTOS_CONSULTAR` existe en `ANEXO24_DEV`.
- Los SP de carga modifican datos y no son fuentes de lectura paginada.

## Inferido

- `CVE_PRODUCTO` es el número de parte visible en el legado.
- `UNIDAD` es la UMC mostrada por el legado.
- `CVE_PRODUCTO_CLIENTE` es una clave alternativa, no el identificador global.
- La relación producto-estructura-material se realiza mediante
  `PRODUCTOKEY`, `PRODUCTOLINK`, `ESTRUCTURAKEY`, `ESTRUCTURALINK`,
  `CVE_MATERIAL` y `CLAVE`.
- La consulta directa a `dbo.productos` es la alternativa de menor riesgo para
  el catálogo porque no duplica lógica de carga ni mezcla filas de BOM.
- `ORDER BY CVE_PRODUCTO, PRODUCTOKEY` es el orden determinista apropiado.

## Pendiente de validar

- Reconciliar los 204 registros actuales contra los aproximadamente 2,630 de la
  auditoría funcional.
- Confirmar con negocio/UI las etiquetas exactas de `UNIDAD` y `UNIDADT`.
- Confirmar si el número de parte debe buscar `CVE_PRODUCTO`,
  `CVE_PRODUCTO_CLIENTE` o ambos.
- Confirmar si `CVE_PRODUCTO` debe convertirse en `NOT NULL`/unique en Módulo C;
  no modificar DDL como parte de esta iniciativa.
- Confirmar necesidad de índices secundarios con el propietario de la base y
  mediciones reales.
- Validar permisos efectivos del usuario técnico del backend en `dbo.productos`
  y en los metadatos requeridos.
- Revisar los result sets exactos de los procedimientos secundarios si otro
  módulo necesita consumirlos.
- Confirmar si `NICO` debe ser visible en una futura versión del DTO.

## Plan exacto para implementar después de aprobación

1. Aprobar este mapeo y resolver la discrepancia de volumen.
2. Confirmar las etiquetas de unidad y el filtro de número de parte con negocio.
3. Crear únicamente el modelo de dominio `Producto` con los campos aprobados,
   sin dependencias de Spring/JDBC.
4. Crear el puerto `ProductoRepository` para consulta paginada.
5. Crear `ListarProductosUseCase` con normalización de filtro y validación
   `pagina >= 1`, `1 <= tamano <= 100`.
6. Crear `ProductoJdbcAdapter` con `@Qualifier("jdbcTemplate")`, consulta
   parametrizada directa a `dbo.productos`, `COUNT(*)`, filtros aprobados y
   `ORDER BY CVE_PRODUCTO, PRODUCTOKEY`.
7. Crear DTO/mapper y `ProductController` con
   `GET /api/v1/catalogos/productos`.
8. Aplicar `@PreAuthorize` para `PRODUCTOS_CONSULTAR` y conservar los contratos
   400/401/403/503 de Materiales.
9. Crear tests unitarios del caso de uso y adapter, y tests MockMvc del
   controlador/security.
10. Ejecutar `clean test` y `build`, revisar el plan SQL en un ambiente
    controlado y comparar conteos/muestras contra el legado.

No se implementan en esta etapa Productos, Estructuras, Entradas, Salidas,
Materiales utilizados, Saldos, Reportes ni frontend.
