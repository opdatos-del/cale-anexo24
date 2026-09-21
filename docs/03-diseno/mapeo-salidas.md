# Salidas / Exportaciones

Auditoría técnica read-only del flujo de Salidas/Exportaciones en `CALE_IMMEX`,
esquema `dbo`.

- **Rama:** `feature/backend-exits`
- **Base:** `dev` en `29cd6e7eca95ba634bfe8f51fd0af312a8e349e5`
- **Estado:** auditoría de ingeniería inversa; no se implementa backend en esta iteración.
- **Estrategia:** STORED PROCEDURE FIRST.
- **Datos:** no modificados.
- **Procesos mutables:** no ejecutados.
- **Reporte read-only ejecutado:** `dbo.PR_INFORME_EXPORTACIONES`, después de confirmar estáticamente que sólo escribe en una tabla variable local.

## 1. Objetivo funcional

Reconstruir el modelo real de Salidas/Exportaciones y separar tres capas que no
deben mezclarse:

1. consulta operacional de salidas y sus líneas;
2. reporte amplio de exportaciones;
3. procesos de carga, descargo, materiales utilizados y actualización de saldos.

La evidencia funcional disponible describe la siguiente pantalla operacional:

| Campo visible | Evidencia |
|---|---|
| Pedimento | Confirmado en la pantalla `OPERACIONES · SALIDAS`. |
| Fecha | Confirmado; corresponde a la fecha de la salida en la consulta legacy. |
| Clave | Confirmado; en SQL corresponde a la clave de línea `PSALIDAS.Clave`. |
| Fracción | Confirmado; en SQL corresponde a `PSALIDAS.Fraccion`. |
| UMC | Confirmado; en SQL corresponde a `PSALIDAS.Unidad`. |
| Cantidad | Confirmado; en SQL corresponde a `PSALIDAS.Cantidad`. |
| N° parte | Confirmado por la pantalla y la documentación funcional, pero la relación física adicional con un catálogo de producto debe conservarse como pendiente de contrato hasta cerrar la semántica de presentación. |

La pantalla exige `Desde` y `Hasta`, ofrece filtros por `Pedimento`, `Fracción`
y `Clave`, y muestra exportación, refresco, limpieza, paginación y conteo de
filas. La evidencia histórica no demuestra que la pantalla consuma directamente
el reporte de 49 columnas.

## 2. Estado de evidencia

| Tema | Estado | Evidencia |
|---|---|---|
| `SALIDAS` es encabezado | **CONFIRMADO** | PK `SalidaKey`; 660 filas; contiene documento, fecha, clave de pedimento y datos generales. |
| `PSALIDAS` es detalle | **CONFIRMADO** | PK `Psalidakey`; 3.392 filas; contiene clave, fracción, cantidad, unidad y factura. |
| Relación encabezado-detalle | **CONFIRMADO** | `PSALIDAS.Salidalink = SALIDAS.SalidaKey`; no existe FK DDL. |
| Producto de una línea | **CONFIRMADO** | `PSALIDAS.Clave = PRODUCTOS.CVE_PRODUCTO` en procesos y muestra actual. |
| Estructura vigente | **CONFIRMADO** | `GETPRODUCTSTRUCT(clave, fecha)` busca la estructura más reciente con `ESTRUCTURAS.INICIO <= fecha`. |
| Materiales/BOM | **INFERIDO** | El descargo usa `PRODUCTOMATERIAL` sólo cuando la estructura seleccionada tiene detalle. La tabla está vacía actualmente. |
| Salida → importación | **CONFIRMADO** para el descargo | `DESCARGA.PSALIDALINK` enlaza con `PSALIDAS.Psalidakey`; `DESCARGA.PENTRADALINK` enlaza con `PARTIDAS.Partidakey`; `PARTIDAS.Importacionlink` enlaza con `IMPORTACIONES.Ipedimentokey`. |
| PEPS | **PENDIENTE DE VALIDAR** | `SALDOS` ordena por `INDICE, PFECHAIMPO`; el significado completo de `INDICE` no está cerrado. |
| Contrato HTTP V1 | **PENDIENTE** | Primero debe separarse la consulta operacional del reporte amplio y decidirse la fuente. |

## 3. Modelo físico observado

```text
SALIDAS (encabezado)
    1 ─────────────── N
PSALIDAS (líneas de salida)
    │
    ├── PSALIDAS.Clave = PRODUCTOS.CVE_PRODUCTO
    │       │
    │       └── GETPRODUCTSTRUCT(clave, SALIDAS.Fecha)
    │               └── ESTRUCTURAS → PRODUCTOMATERIAL → MATERIAL
    │
    └── DESCARGA.PSALIDALINK
            └── DESCARGA.PENTRADALINK = PARTIDAS.Partidakey
                    └── PARTIDAS.Importacionlink = IMPORTACIONES.Ipedimentokey
```

El modelo físico no contiene FK DDL entre estas tablas. Las relaciones son
lógicas y se expresan en views, procedimientos y funciones.

### 3.1 `dbo.SALIDAS`

**Rol:** encabezado de la salida/exportación.

- Filas actuales: **660**.
- PK clustered y única: `PK_salidas(SalidaKey)`, `SalidaKey numeric(18,0) NOT NULL`.
- Índices secundarios observados:
  - `INDICE1_G6`: `SalidaKey`, `Documento`, `Cve_pedimento`, `Fecha`;
  - `op2`: `SalidaKey`, `DESCARGA`, `Fecha`.
- FKs DDL: **ninguna observada**.
- `Documento` no es PK ni constraint unique, aunque no presentó duplicados tras normalizar espacios en la muestra actual.

Columnas principales:

| Grupo | Columnas y tipos | Uso observado |
|---|---|---|
| Identidad/documento | `SalidaKey numeric(18,0) NOT NULL`, `Documento char(60)`, `Cve_pedimento char(5)`, `Tipo_operacion char(25)`, `TipoOperacion int` | Identidad técnica, pedimento/documento y clasificación. |
| Aduana | `Aduana char(10)`, `Agente char(10)`, `AduanaDespacho varchar(50)` | Aduana y patente/agente. |
| Fechas | `Fecha datetime`, `FechaEntrada datetime`, `FechaDocA datetime`, `FechadePresentacion datetime`, `FechaCompl datetime`, `FECHADESCARGA datetime`, `FCreacion datetime`, `FECHAMODIFICACION datetime` | Fecha de salida/pago, presentación, creación y descargo. |
| Cliente/destino | `Cve_cliente char(15)`, `Pais char(10)`, `Origen char(30)`, `Transfiere char(20)`, `DocAduanero char(20)` | Cliente, países y referencias operativas. |
| Valores | `TotalMonto float`, `Tc float`, `dta float`, `FactorME float`, `CNT numeric(18,4)`, `IVA numeric(18,4)`, `IGIE numeric(18,4)`, `MULTAS numeric(18,4)`, `RECARGOS numeric(18,4)` | Valores fiscales/comerciales e incrementables. |
| Estado/descargo | `DESCARGA varchar(2)`, `TipoDescarga varchar(10)`, `bloqueado numeric(18,0)`, `validacion varchar(255)`, `ValidadoVu varchar(10)`, `IDENTIFICADOR varchar(5)`, `CARGA varchar(50)` | Estado de proceso y descargo. |
| Transporte/proyecto | `Tema varchar(50)`, `Proyecto varchar(50)`, `Transportes varchar(50)`, `Placas varchar(50)`, `Licencia varchar(50)`, `PesoTotalBruto float` | Datos operativos adicionales. |
| Técnicas/documentales | `PedimentoOriginal varchar(20)`, `ClavePedimentoOriginal varchar(50)`, `ClavePedimentoRectificada varchar(50)`, `PEDIMENTOS varchar(max)`, `PEDIMENTOSC varchar(max)`, `referencia varchar(50)` | Relaciones/documentos auxiliares. |

Las columnas no marcadas `NOT NULL` por la metadata son nullable.

### 3.2 `dbo.PSALIDAS`

**Rol:** línea o partida de la salida.

- Filas actuales: **3.392**.
- PK clustered y única: `PK_psalidas(Psalidakey)`, `Psalidakey numeric(18,0) NOT NULL`.
- Índices secundarios observados:
  - `IHD2`: `Factura`, `Fecha`, `Cantidad`, `Clave`, `Psalidakey`, `Salidalink`;
  - `op6`: `ORDENDESCARGA`;
  - `opt1`: `Psalidakey`, `Salidalink`, `bloqueado`, `Cantidad`;
  - `opt2`: `Psalidakey`, `Salidalink`.
- FKs DDL: **ninguna observada**.
- `Salidalink` es `float` nullable en la metadata, aunque todas las 3.392 líneas actuales enlazan con una salida.

Columnas principales:

| Grupo | Columnas y tipos | Uso observado |
|---|---|---|
| Identidad/relación | `Psalidakey numeric(18,0) NOT NULL`, `Salidalink float`, `partida float` | Línea técnica, encabezado lógico y secuencia de pedimento. |
| Producto/mercancía | `Clave varchar(50)`, `Descripcion varchar(250)`, `Fraccion char(12)`, `Cantidad numeric(18,4)`, `Unidad char(5)` | Producto, fracción, cantidad y UMC visibles en la consulta. |
| Factura | `Factura char(50)`, `Factura2 char(40)`, `Fecha datetime`, `Folio varchar(50)`, `COVE varchar(100)` | Facturación y referencias documentales. |
| Valores | `Val_pesos numeric(18,4)`, `Val_dolares numeric(18,4)`, `valorcomercial float`, `valoragregado float`, `ValorAduanaCR numeric(18,4)`, `pu numeric(8,4)` | Valores de la línea. |
| Comercio exterior | `paisd varchar(10)`, `paisc varchar(10)`, `CodProveedor varchar(20)`, `cve_cliente varchar(20)`, `INCOTERM varchar(30)`, `NICO varchar(10)` | Países, cliente/proveedor y clasificación. |
| Descargo | `bloqueado int`, `descargaDesp int`, `descargaDirigida varchar(50)`, `FECHADESCARGA datetime`, `ORDENDESCARGA bigint`, `ESTRUCTURAKEY bigint` | Estado de línea y relación con estructura/descargo. |
| Identificación adicional | `No_ParteCli varchar(50)`, `Lote varchar(50)`, `marca varchar(50)`, `modelo varchar(50)`, `serie varchar(50)`, `PERMISO varchar(50)` | Datos comerciales y de trazabilidad. |
| Auditoría/técnicas | `USUARIO varchar(100)`, `FECHAMODIFICACION datetime`, `FCreacion datetime`, `idctm bigint`, `pedimentoexportacionctm varchar(50)` | Control técnico y CTM. |

### 3.3 Tablas de descargo relacionadas

| Tabla | Filas actuales | Rol confirmado |
|---|---:|---|
| `DESCARGA` | 3.866 | Relación cuantificada entre línea de salida y partida de importación; contiene cantidades incorporadas, merma y desperdicio. |
| `TRAZO` | 0 | Trazabilidad del descargo y faltantes; `SALDOS`/descargos insertan registros. |
| `DIRIGIDO` | 0 | Descarga dirigida asociada a `PSALIDAKEY`; no hay filas actuales. |
| `CARGAFACTURA` | 0 | Staging para carga de facturas a líneas de salida. |
| `ERRCARGAFACTURA` | 0 | Errores/resultado de carga de facturas. |
| `PRODUCTOS` | 204 | Catálogo lógico de productos; `CVE_PRODUCTO` coincide con `PSALIDAS.Clave` en la muestra actual. |
| `ESTRUCTURAS` | 0 | Estructuras vigentes; vacía en el ambiente actual. |
| `PRODUCTOMATERIAL` | 0 | Detalle BOM usado por descargo; vacío en el ambiente actual. |
| `MATERIAL` | 2 | Catálogo de materiales usado aguas abajo del descargo. |
| `IMPORTACIONES` | 2 | Encabezados de entrada asociados por `PARTIDAS`. |
| `PARTIDAS` | 2 | Partidas de importación que reciben el descargo. |

## 4. Cardinalidades y calidad de relaciones

Consultas ejecutadas con `COUNT_BIG` y agregados read-only:

| Relación/medición | Resultado |
|---|---:|
| `SALIDAS` | 660 |
| `PSALIDAS` | 3.392 |
| Líneas por salida, mínimo | 1 |
| Líneas por salida, máximo | 19 |
| Líneas por salida, promedio | 5,139393 |
| Salidas sin líneas | 0 |
| Líneas huérfanas (`Salidalink` sin `SalidaKey`) | 0 |
| `Psalidakey` duplicados | 0 grupos |
| `SalidaKey` duplicados | 0 grupos |
| Documento duplicado en `SALIDAS` tras `LTRIM/RTRIM` | 0 grupos |
| Duplicado `(Salidalink, partida)` | 0 grupos |
| Líneas con `Clave` blank/null | 0 |
| Líneas con producto coincidente | 3.392 de 3.392 |
| Claves de producto distintas en líneas | 204 |
| Claves técnicas de estructura distintas en líneas | 1 (`ESTRUCTURAKEY = 1`) |
| Estructuras existentes en `ESTRUCTURAS` | 0 |
| `PRODUCTOMATERIAL` | 0 |
| Filas `DESCARGA` | 3.866 |
| `PENTRADALINK` distintos en `DESCARGA` | 2 |
| `PSALIDALINK` distintos en `DESCARGA` | 3.392 |
| Descargas sin partida de importación | 0 |
| Descargas sin línea de salida | 0 |
| Salidas con `Documento` null/blank | 0 |
| Salidas con `Cve_pedimento` null/blank | 0 |

La muestra actual confirma que una salida tiene N líneas y que el identificador
de línea (`Psalidakey`) debe conservarse separado del identificador de encabezado
(`SalidaKey`). No debe diseñarse un contrato futuro con una única clave plana.

## 5. Relación con PRODUCTOS

La relación confirmada por definiciones de `DESCARGASALIDAPEPS`,
`DESCARGATSALIDA1` y por los datos actuales es:

```text
PSALIDAS.Clave (varchar(50))
        = PRODUCTOS.CVE_PRODUCTO
PRODUCTOS.PRODUCTOKEY (numeric/bigint técnico)
        └── ESTRUCTURAS.PRODUCTOLINK
```

No se observó FK DDL. La evidencia no justifica sustituir la clave funcional
`CVE_PRODUCTO` por `PRODUCTOKEY` en una futura consulta de líneas sin documentar
ambos identificadores.

## 6. Relación con Estructuras/BOM

`dbo.GETPRODUCTSTRUCT(@VAR1 varchar(50), @VAR2 datetime)` es una función escalar
read-only que:

1. busca el producto por `PRODUCTOS.CVE_PRODUCTO = @VAR1`;
2. busca estructuras del producto con `ESTRUCTURAS.INICIO <= @VAR2`;
3. selecciona `TOP 1` ordenando `ESTRUCTURAS.INICIO DESC`;
4. devuelve `ESTRUCTURAKEY` como `float`;
5. devuelve `-1` si no encuentra estructura.

No usa una fecha fin en la selección observada. El fallback `-1` está confirmado
por la definición. La función usa cursor, pero no escribe datos.

`DESCARGASALIDAPEPS` usa esa función con `PSALIDAS.Clave` y `SALIDAS.Fecha`.
Cuando la estructura tiene materiales, une `PRODUCTOS`, `ESTRUCTURAS` y
`PRODUCTOMATERIAL`, filtra cantidades BOM positivas y calcula cantidades
incorporadas, merma y desperdicio multiplicando la cantidad de salida por los
factores del detalle.

Estado actual:

- `ESTRUCTURAS`: 0 filas;
- `PRODUCTOMATERIAL`: 0 filas;
- `PSALIDAS.ESTRUCTURAKEY`: 3.392 filas con valor 1, aunque no existen estructuras actuales;
- `GETPRODUCTSTRUCT` devuelve `-1` para las líneas actuales al no encontrar una estructura.

Por tanto, la asociación salida → estructura → materiales está confirmada
estáticamente, pero no puede validarse con una estructura real en el dataset
actual. El contrato futuro debe tratar el caso sin estructura explícitamente.

## 7. Relación con Importaciones y saldo

La cadena física observada es:

```text
PSALIDAS.Psalidakey
    ↓ DESCARGA.PSALIDALINK
DESCARGA.PENTRADALINK
    ↓
PARTIDAS.Partidakey
    ↓ PARTIDAS.Importacionlink
IMPORTACIONES.Ipedimentokey
```

`DESCARGA` contiene, entre otras, cantidades `CANTUTIL`, `MERMA` y
`DESPERDICIO`. La relación es de consulta para reportes de materiales utilizados,
pero su generación es mutable.

`dbo.SALDOS` recibe el material y cantidades incorporadas/merma/desperdicio junto
con `SALIDAKEY`, `PSALIDAKEY`, fecha y `PRODMATKEY`. Su definición:

- prepara materiales directos/alternativos en una tabla variable local;
- selecciona partidas con `Pdescarga = 'SI'`, saldo mayor a `0.001` y fecha de
  importación/vencimiento compatible con la fecha de exportación;
- ordena por `INDICE` y `PFECHAIMPO`;
- inserta una o más filas en `DESCARGA`;
- actualiza `PARTIDAS.SALDO`;
- inserta trazabilidad en `TRAZO`.

Esto confirma procesamiento de saldo/descargo, no una fuente read-only de líneas
de salida. La etiqueta PEPS queda **PENDIENTE DE VALIDAR** porque la definición
confirma el orden técnico, pero no demuestra que el campo `INDICE` tenga siempre
semántica PEPS.

## 8. Views auditadas

### `dbo.v_Exportaciones`

- 44 columnas.
- Read-only, sin parámetros, sin paginación y sin total.
- Fuentes: `SALIDAS`, `PSALIDAS`, `CLIENTES`, `PROVEEDORES` y `DIRIGIDO`.
- Une `PSALIDAS.Salidalink = SALIDAS.SalidaKey` y hace left join a clientes.
- Incluye pedimento, fecha de pago, cliente, países, producto, descripción,
  cantidad, unidad, factura, fracción, secuencia, claves técnicas, valores,
  descarga y fechas de creación.
- Tiene `TOP (100) PERCENT` y `ORDER BY [Fecha de Pago] DESC`; el orden no es un
  contrato confiable para una API.
- Es la view más cercana al reporte, pero es demasiado amplia y no parametrizada
  para una consulta paginada.

### `dbo.v_descarga`

- 35 columnas.
- Mezcla importación, salida y descargo.
- Fuentes: `DESCARGA`, `IMPORTACIONES`, `PARTIDAS`, `PSALIDAS`, `SALIDAS` y
  `PROVEEDORES`.
- Incluye documento/fecha de importación, material, saldo, cantidades
  incorporadas/merma/desperdicio, documento/fecha de salida, producto, cantidad
  de salida, factura, bloqueo y proveedor.
- Es candidata para materiales utilizados/descargos, no para el listado simple
  de Salidas.

### `dbo.V_INFORMEDESCARGAS`

- 42 columnas.
- Mezcla importación, exportación, producto/material, valores y saldo actual.
- Fuentes: `IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `PSALIDAS`, `DESCARGA` y
  `CATEGORIAS`.
- Incluye ambos documentos, fechas, fracciones, cantidades UMC/UMT, valores,
  destino y `SaldoActual`.
- Es un reporte de conciliación/descargo, no fuente directa de la pantalla
  operacional.

### `dbo.V_STATUS_DESCARGAS`

- 17 columnas.
- Fuentes: `SALIDAS`, `PSALIDAS`, `DESCARGA`, `DIRIGIDO`,
  `PRODUCTOMATERIAL` y `GETPRODUCTSTRUCT`.
- Incluye `salidakey`, `psalidakey`, documento, fecha, clave, producto, cantidad,
  factura, indicadores `DESCARGO`, `DIRIGIDO`, `DESCARGA`, valores y número de
  materiales.
- Útil como estado de descargo; no es contrato de consulta de salidas.

### `dbo.DESCARGA_CTMA` y `dbo.DESCARGA_DESPERDICIO`

- 19 columnas cada una.
- Fuentes: `DESCARGA`, `IMPORTACIONES`, `PARTIDAS`, `SALIDAS` y `PSALIDAS`.
- Proyectan, respectivamente, cantidades `CantUtil` y `Desperdicio`, además de
  documentos, claves, factura, fechas, unidad, origen y valores.
- Son reportes especializados de descarga, no fuente de la pantalla Salidas.

Ninguna de las views auditadas ofrece simultáneamente filtros parametrizados,
paginación y `@Total` para el futuro endpoint.

## 9. Stored procedures y funciones

### 9.1 `dbo.PR_INFORME_EXPORTACIONES`

**Clasificación:** `REPORT / QUERY` — **READ ONLY** sobre tablas persistentes.

**Parámetros:**

| Parámetro | Tipo | Uso |
|---|---|---|
| `@DESDE` | `datetime` | Límite inferior de `SALIDAS.Fecha`. |
| `@HASTA` | `datetime` | Límite superior de `SALIDAS.Fecha`. |
| `@documento` | `varchar(50)` | Filtro exacto de `SALIDAS.Documento`. |

**Comportamiento estático:**

- si `@documento` es null/vacío, conserva las fechas;
- si `@documento` tiene valor, asigna `NULL` a `@DESDE` y `@HASTA`;
- el predicado efectivo es `SALIDAS.FECHA BETWEEN COALESCE(@DESDE,
  SALIDAS.FECHA) AND COALESCE(@HASTA, SALIDAS.FECHA)` y documento exacto;
- inserta el resultado en la tabla variable local `@res` y después la proyecta;
- une `PSALIDAS` con `SALIDAS` por `Salidalink = SalidaKey`;
- hace left join a `CLIENTES`;
- consulta `PROVEEDORES` y `DIRIGIDO` mediante subconsultas;
- no llama otros SP;
- no usa tablas temporales persistentes, cursor, transacción, `TRY/CATCH`,
  `RAISERROR` ni `THROW`;
- no pagina ni devuelve total;
- ordena por fecha de pago, aduana, patente, pedimento y secuencia.

**Result set:** **49 columnas**, todas nullable según metadata de SQL Server:

1. `DIRIGIDO`
2. `Aduana`
3. `Patente`
4. `Pedimento`
5. `Fecha de Pago`
6. `Fecha de presentacion`
7. `Codigo de Cliente`
8. `Nombre Cliente`
9. `RFC / Tax Cliente`
10. `Pais Comprador`
11. `Pais Destino`
12. `TC`
13. `DTA`
14. `Codigo de Producto`
15. `Descripcion`
16. `Cantidad`
17. `Unidad`
18. `Valor Pesos`
19. `Valor Dolares`
20. `Numero de Factura`
21. `Fecha de Factura`
22. `Fraccion`
23. `Secuencia pedimento`
24. `Pedimento Bloqueado`
25. `DESCARGA`
26. `PedimentoOriginal`
27. `Cve_pedimento`
28. `Valor Comercial Maquila`
29. `Valor Agregado Maquila`
30. `TipoDescarga`
31. `descargaDirigida`
32. `CNT`
33. `prev`
34. `COVE`
35. `psalidakey`
36. `salidakey`
37. `Usuario Modifico`
38. `Fecha de Modificacion`
39. `cantidadT`
40. `UnidadT`
41. `observaciones`
42. `ANEXO`
43. `Valor Aduana`
44. `FechaCreacionSalida`
45. `FechaCreacionPartida`
46. `PU`
47. `FME`
48. `NICO`
49. `PedimentoCTM`

Tipos relevantes confirmados: `Pedimento char(100)`, `Fecha de Pago datetime`,
`Codigo de Producto varchar(50)`, `Cantidad numeric(18,4)`, `Unidad char(5)`,
`Fraccion varchar(22)`, `psalidakey numeric(18,0)` y `salidakey numeric(18,0)`.

El informe contiene información de cliente, proveedor, factura, valores,
descarga y claves técnicas; no implementa paginación/total y no debe consumirse
directamente como contrato HTTP V1.

### 9.2 Ejecución controlada del reporte

La ejecución se realizó sólo después de confirmar el comportamiento read-only.
Antes se consultaron `MIN(Fecha)`, `MAX(Fecha)` y documentos existentes.

Datos de control:

- rango actual: `2025-10-31` a `2026-08-18`;
- documento existente usado: `190-1562-5001241`;
- no se imprimieron las 49 columnas ni datos personales innecesarios.

| Caso | Parámetros | Filas | Resultado |
|---|---|---:|---|
| A | Rango completo, documento null | 3.392 | 660 `salidakey` distintos y 3.392 `psalidakey` distintos. |
| B | `2025-10-31` completo, documento null | 2 | Una salida y dos líneas. |
| C | Documento existente + rango `2026-01-01` a `2026-01-02` | 2 | El documento anula el rango; devuelve el documento de `2025-10-31`. |
| D | Documento existente + rango coincidente `2025-10-31` | 2 | Devuelve las mismas dos líneas. |

En el caso A hay 526 grupos con más de una línea por `salidakey`, pero no hay
`psalidakey` duplicados. Esto confirma que el reporte es plano por línea y no
por encabezado. La peculiaridad documento-versus-fechas queda confirmada
empíricamente y es incompatible con copiarlo sin decisión a una nueva API.

### 9.3 `dbo.CARGAFACTURASENPSALIDAS`

**Clasificación:** `IMPORT / PROCESS` — **WRITE**. **No ejecutado.**

Parámetros:

- `@PEDIMENTO varchar(50)`;
- `@FACTURA varchar(50)`.

Flujo observado:

1. borra `ERRCARGAFACTURA`;
2. busca `SALIDAKEY` por `SALIDAS.DOCUMENTO`;
3. valida en `CARGAFACTURA` fracción, producto, unidad, cantidad y valores;
4. registra errores en `ERRCARGAFACTURA`;
5. si no hay errores, genera claves con `MAX(PSALIDAKEY) + 1` y
   `ROW_NUMBER()`;
6. inserta líneas en `PSALIDAS` relacionadas con `SALIDAKEY`;
7. registra mensaje de éxito en `ERRCARGAFACTURA`;
8. borra `GENERADORES`.

Lee `CARGAFACTURA`, `SALIDAS` y `PRODUCTOS`; escribe/borrar en
`ERRCARGAFACTURA`, `PSALIDAS` y `GENERADORES`. No se observaron transacción,
`TRY/CATCH`, `RAISERROR` ni `THROW`. Es un proceso mutable de carga, no una
fuente GET.

### 9.4 `dbo.DESCARGASALIDAPEPS`

**Clasificación:** `PROCESS / CALCULATION` — **WRITE**. **No ejecutado.**

Parámetro:

- `@SALIDAKEY int`.

Flujo estático:

1. elimina el trazo existente de las líneas no bloqueadas de la salida;
2. elimina su descarga previa;
3. recalcula y actualiza `PARTIDAS.SALDO` para todas las partidas;
4. recorre con cursor las líneas de `PSALIDAS` de la salida;
5. obtiene estructura con `GETPRODUCTSTRUCT(PSALIDAS.Clave, SALIDAS.Fecha)`;
6. si hay descarga dirigida, llama `DESCDIRIGIDA`;
7. si hay materiales BOM, explota `PRODUCTOMATERIAL` y llama `SALDOS` por
   material y cantidades calculadas;
8. si no hay estructura/materiales, inserta trazo con observación `SIN ESTRUCTURA`;
9. cierra y libera cursores.

Escribe en `TRAZO`, `DESCARGA` y `PARTIDAS`, y puede escribir indirectamente
por `DESCDIRIGIDA`/`SALDOS`. No se observaron transacción ni `TRY/CATCH`.
No copiar esta regla a Java ni exponerla mediante un GET.

### 9.5 Dependencias de descargo

| Objeto | Clasificación | Evidencia estática |
|---|---|---|
| `DESCDIRIGIDA` | `PROCESS` / WRITE indirecto | Cursor sobre `DIRIGIDO`, llama `SALDOSDIRIGIDOS`. |
| `SALDOS` | `CALCULATION / PROCESS` / WRITE | Lee `MATERIAL`, `PRODUCTOMATERIAL`, `PARTIDAS`, `ALTERNATIVO`; inserta `DESCARGA` y `TRAZO`, actualiza `PARTIDAS`. |
| `DESCARGATSALIDA1` | `PROCESS` / WRITE | Modifica `SETTINGS`, `TRAZO`, `DESCARGA`, `PARTIDAS`; llama `DESCARGATODOSDIRIGIDOS` y usa `SALDOS`. |
| `DESCARGATSALIDAFECHA` | `PROCESS` / WRITE | Mismo flujo que `DESCARGATSALIDA1`, con `@HASTA datetime`. |
| `DESCARGATSALIDA` | `PROCESS` / WRITE indirecto | Llama variantes de descargo. |
| `DESCARGAINICIAL` | `PROCESS` / WRITE | Actualiza `SALIDAS`, borra `DESCARGA` y usa importaciones/partidas/trazo. |
| `DESCARGAS_A31` | `PROCESS` / WRITE | Opera sobre tablas A31, periodo y saldos A31. |
| `RETORNO_SUBMAQUINA` | `COMMAND / PROCESS` / WRITE | Inserta partida, bloquea `PSALIDAS` y actualiza `DESCARGA`. |

Estos objetos se revisaron sólo por definición/metadata. No fueron ejecutados.

## 10. Grafo de dependencias

```text
CARGAFACTURASENPSALIDAS
    ├── CARGAFACTURA
    ├── PRODUCTOS
    ├── SALIDAS
    ├── PSALIDAS
    ├── ERRCARGAFACTURA
    └── GENERADORES

PR_INFORME_EXPORTACIONES / v_Exportaciones
    ├── SALIDAS
    ├── PSALIDAS
    ├── CLIENTES
    ├── PROVEEDORES
    └── DIRIGIDO

DESCARGASALIDAPEPS
    ├── SALIDAS
    ├── PSALIDAS
    ├── GETPRODUCTSTRUCT
    │     ├── PRODUCTOS
    │     └── ESTRUCTURAS
    ├── PRODUCTOMATERIAL
    ├── DIRIGIDO ── DESCDIRIGIDA ── SALDOSDIRIGIDOS
    ├── SALDOS
    │     ├── MATERIAL
    │     ├── PRODUCTOMATERIAL
    │     ├── PARTIDAS
    │     ├── DESCARGA
    │     └── TRAZO
    └── PARTIDAS

DESCARGA / v_descarga / V_INFORMEDESCARGAS
    ├── PSALIDAS
    ├── SALIDAS
    ├── PARTIDAS
    ├── IMPORTACIONES
    └── DESCARGA
```

Las dependencias son referencias SQL observadas; no sustituyen constraints ni
prueban por sí mismas una relación de negocio completa.

## 11. Modelo funcional propuesto para la siguiente decisión

El modelo mínimo observado es plano por línea, manteniendo ambos niveles:

```text
Salida
  ├── salidaId = SALIDAS.SalidaKey
  ├── pedimento = SALIDAS.Documento
  ├── fecha = SALIDAS.Fecha
  ├── clavePedimento = SALIDAS.Cve_pedimento
  └── N líneas
        ├── partidaId = PSALIDAS.Psalidakey
        ├── clave/producto = PSALIDAS.Clave
        ├── fraccion = PSALIDAS.Fraccion
        ├── unidad = PSALIDAS.Unidad
        ├── cantidad = PSALIDAS.Cantidad
        └── factura = PSALIDAS.Factura
```

No se debe mezclar en el primer listado:

- materiales utilizados;
- saldos;
- temporalidad/vencimientos;
- reglas de descargo;
- carga de facturas.

Casos de uso separados:

| Caso de uso | Fuente/responsabilidad | Estado |
|---|---|---|
| Consultar salidas | `PR_INFORME_EXPORTACIONES`/view como referencia; contrato nuevo pendiente | AUDITORÍA CERRADA, implementación pendiente |
| Consultar líneas de una salida | `SALIDAS` + `PSALIDAS` | CONFIRMADO como modelo físico |
| Consultar detalle de una salida | Encabezado + líneas y campos extendidos | PENDIENTE |
| Cargar/generar salida | `CARGAPEDIMENTOS`/otros procesos de carga | Fuera de alcance; mutable |
| Cargar facturas | `CARGAFACTURASENPSALIDAS` | Fuera de alcance; mutable |
| Ejecutar descargo | `DESCARGASALIDAPEPS` y variantes | Fuera de alcance; mutable |
| Consultar materiales utilizados | `v_descarga`/views especializadas | PENDIENTE de contrato propio |
| Consultar saldo asociado | `DESCARGA`, `PARTIDAS`, procesos `SALDOS` | PENDIENTE; no derivar en Java |

## 12. Pantalla operacional vs reporte

### Pantalla Salidas

**CONFIRMADO por prototipo/documentación funcional:**

- rango obligatorio `Desde`/`Hasta`;
- filtros `Pedimento`, `Fracción` y `Clave`;
- columnas: pedimento, fecha, clave, fracción, UMC, cantidad y número de parte;
- botones consultar, limpiar, exportar y refrescar;
- paginación con tamaños de 25/50/100 observada en la maqueta;
- modo sólo lectura;
- sin rango: consulta no disponible y mensaje de definir rango;
- `Desde > Hasta`: error de validación.

**PENDIENTE:** nombres exactos de controles, semántica de `Clave` frente a
`PSALIDAS.Clave` y el contrato de exportación HTTP.

### Reporte de exportaciones

`PR_INFORME_EXPORTACIONES` y `v_Exportaciones` incluyen además cliente,
proveedor, países, factura, valores, descarga, claves técnicas y auditoría. No
se deben considerar equivalentes automáticamente a la pantalla operacional.

La interfaz histórica también distingue reportes y exportación. La auditoría
disponible confirma el patrón general de rango obligatorio y botón de exportar,
pero no demuestra que el reporte amplio sea la fuente directa del grid.

## 13. Mapeo de campos históricos

| Campo histórico de pantalla | Reporte/view legacy | Fuente física confirmada | Estado |
|---|---|---|---|
| Pedimento | `Pedimento` | `SALIDAS.Documento` | CONFIRMADO |
| Fecha | `Fecha de Pago` | `SALIDAS.Fecha` | CONFIRMADO |
| Clave | `Codigo de Producto` | `PSALIDAS.Clave` | CONFIRMADO |
| Fracción | `Fraccion` | `PSALIDAS.Fraccion` | CONFIRMADO |
| UMC | `Unidad` | `PSALIDAS.Unidad` | CONFIRMADO |
| Cantidad | `Cantidad` | `PSALIDAS.Cantidad` | CONFIRMADO |
| N° parte | No existe una segunda columna inequívoca en el result set del reporte | `PSALIDAS.No_ParteCli` existe, pero su uso como columna visible de la pantalla requiere confirmación | PENDIENTE DE VALIDAR |

La pantalla histórica muestra `N° parte`, pero el reporte devuelve `Codigo de
Producto` y la tabla tiene además `No_ParteCli`. No se inventa equivalencia:
se debe cerrar si la UI presenta `PSALIDAS.Clave`, `No_ParteCli` o un dato
transformado antes de definir un response.

## 14. Fuente candidata para futura API

Endpoint candidato, todavía no implementado:

```http
GET /api/v1/operaciones/salidas
```

Posible detalle posterior:

```http
GET /api/v1/operaciones/salidas/{salidaId}
```

Comparación:

| Fuente | Fortalezas | Limitaciones |
|---|---|---|
| `PR_INFORME_EXPORTACIONES` | Reporte probado; une encabezado/detalle; expone `salidakey` y `psalidakey`; orden legacy. | 49 columnas; no pagina ni total; `documento` anula fechas; mezcla cliente, factura, valores y descarga; usa `BETWEEN`; aliases de presentación. |
| `v_Exportaciones` | View read-only; 44 columnas; misma proyección funcional; incluye IDs técnicos. | Sin parámetros, paginación ni total; orden `TOP 100 PERCENT` no contractual; amplia para la pantalla. |
| `SELECT` encapsulado en `APP24_Q_SALIDAS_LISTAR` | Podría fijar filtros acumulativos, paginación, total, aliases y separar consulta de descargo. | Requiere cerrar primero la semántica de `N° parte`, filtros y fecha; no crear todavía. |

**Recomendación:** no consumir directamente el SP ni la view como contrato HTTP.
La evidencia apunta a evaluar un `APP24_Q_SALIDAS_LISTAR` read-only para un
listado plano de líneas, pero sólo después de aprobar el contrato exacto y
confirmar el campo `N° parte`. No se crea el SP en esta auditoría.

## 15. Seguridad

El seed de la aplicación contiene `OPERACIONES_CONSULTAR` (`operaciones`, acción
`CONSULTAR`) como permiso existente para consultas operativas. Es el permiso
candidato para una futura consulta de Salidas. No se creó ningún permiso nuevo.

La exportación de archivos aparece separada funcionalmente como una capacidad
que requeriría permiso de exportación; el permiso candidato para ese caso no se
resuelve en este documento.

## 16. Riesgos

1. `PR_INFORME_EXPORTACIONES` anula fechas cuando se informa documento; copiarlo
   literalmente podría permitir consultas fuera del rango elegido.
2. Ni el SP ni las views ofrecen paginación ni total.
3. El SP reporta 49 columnas y la view 44; ambos mezclan consulta operacional
   con datos de factura, cliente, valores y descargo.
4. `SALIDAS.SalidaKey` y `PSALIDAS.Psalidakey` tienen tipos `numeric(18,0)`,
   mientras `PSALIDAS.Salidalink` es `float`; no deben convertirse a
   `float/double` en el dominio futuro.
5. No hay FKs DDL entre encabezado, líneas, producto, importación, partida o
   descarga.
6. `GETPRODUCTSTRUCT` devuelve una clave de estructura como `float` y usa `-1`
   como fallback.
7. `ESTRUCTURAS` y `PRODUCTOMATERIAL` están vacías; la explosión BOM no puede
   validarse con datos actuales.
8. `PSALIDAS.ESTRUCTURAKEY = 1` en las 3.392 líneas, aunque no existe la
   estructura correspondiente actualmente; no tomar ese valor como evidencia
   de vigencia real.
9. `No_ParteCli` y `Clave` pueden representar conceptos distintos; el campo
   visible `N° parte` no está cerrado.
10. Los procesos mutables no muestran transacciones/`TRY-CATCH` en varias rutas;
    no deben invocarse desde endpoints GET.
11. `SALDOS` modifica saldos de partidas y genera trazabilidad; sus reglas no
    deben copiarse a Java.
12. Valores y cantidades legacy mezclan `numeric` y `float`; el contrato futuro
    deberá cerrar tipos exactos antes de exponerlos.
13. El dataset actual contiene 660 salidas y 3.392 líneas, pero la pantalla
    histórica mostró un conteo de referencia diferente; la diferencia debe
    documentarse y no resolverse creando datos.

## 17. Pendientes

- Confirmar semántica exacta de `N° parte` (`PSALIDAS.No_ParteCli` frente a
  `PSALIDAS.Clave`).
- Confirmar si `Clave` del filtro corresponde a producto, clave de pedimento u
  otro control de la UI.
- Cerrar semántica de fecha y filtros acumulativos para el futuro endpoint.
- Definir si V1 será listado plano de líneas con `salidaId` y `partidaId`, o si
  habrá endpoint de encabezado/detalle separado.
- Confirmar semántica de `INDICE` antes de llamar PEPS a la regla de `SALDOS`.
- Validar estructuras/BOM cuando existan datos autorizados; no crear datos
  sintéticos.
- Definir contrato de materiales utilizados y saldo en un módulo/caso de uso
  separado.
- Revisar exportación de archivos y permiso específico.
- Evaluar, con contrato aprobado, `APP24_Q_SALIDAS_LISTAR`; no creado en esta
  iteración.

## 18. Restricciones cumplidas

- No se ejecutó `CARGAFACTURASENPSALIDAS`.
- No se ejecutó `DESCARGASALIDAPEPS`.
- No se ejecutaron `DESCARGATSALIDA`, `DESCARGATSALIDA1`,
  `DESCARGATSALIDAFECHA`, `DESCARGAINICIAL`, `DESCARGAS_A31`, `SALDOS` ni
  procesos relacionados.
- No se ejecutaron `INSERT`, `UPDATE`, `DELETE`, `MERGE` ni `TRUNCATE` sobre la
  base por esta auditoría.
- La única ejecución de procedimiento fue `PR_INFORME_EXPORTACIONES`, confirmado
  read-only sobre tablas persistentes y con tabla variable local.
- No se modificaron tablas, views, funciones ni procedimientos legacy.
- No se creó `APP24_Q_SALIDAS_LISTAR`.
- No se implementó Java ni frontend.
