# Materiales utilizados — ingeniería inversa y propuesta de contrato V1

## 0. Estado del documento

- **Fecha de auditoría:** 2026-09-21.
- **Rama:** `feature/backend-used-materials`.
- **Base:** `dev` en `a9a1c76647711048239820b1b619a4a8d20cd302`.
- **Base consultada:** `CALE_IMMEX`, esquema `dbo`.
- **Compatibility level:** `100`.
- **SQL Server:** `16.0.1000.6`, Express Edition 64-bit, RTM.
- **Conexión:** disponible con acceso autorizado; no se documentan credenciales ni cadenas de conexión.
- **Modo:** auditoría estrictamente read-only.
- **Datos:** no modificados.
- **Objetos mutables:** sólo inspeccionados mediante metadata/definición; ninguno ejecutado.
- **Implementación:** no se creó Java, endpoint, frontend ni `APP24_Q_MATERIALES_UTILIZADOS_LISTAR`.

### Convención de evidencia

- **CONFIRMADO:** demostrado por metadata, definición SQL o conteo/consulta ejecutada.
- **INFERIDO:** conclusión razonable derivada de varias evidencias, pero no demostrada funcionalmente de forma completa.
- **PENDIENTE DE VALIDAR:** falta evidencia funcional o una decisión de negocio.

## 1. Resumen ejecutivo

### 1.1 ¿Qué es Materiales Utilizados?

**CONFIRMADO:** la fuente histórica del consumo es `dbo.DESCARGA`. Cada registro
persistido enlaza una partida de entrada con una línea de salida mediante links
lógicos y conserva cantidades de incorporación, merma y desperdicio. Las views y
procedimientos históricos presentan ese resultado para consultas de descargo,
historial y conciliación.

**CONFIRMADO:** una fila no es:

- una estructura/BOM vigente;
- un saldo actual;
- una operación de PEPS ejecutable;
- una orden de generación de descargo;
- una fila agregada por periodo.

**INFERIDO:** el caso de uso correcto para la nueva aplicación es una consulta
read-only del histórico persistido de asignaciones de `DESCARGA`. La generación
o recalculo de esas filas pertenece a los procesos de descargo/saldos y queda
fuera de Materiales Utilizados V1.

### 1.2 Fuente principal recomendada

**CONFIRMADO:** no existe un objeto legacy read-only dedicado que entregue
simultáneamente filtros parametrizados, paginación, total y una proyección
estable de Materiales Utilizados.

**RECOMENDADO:** crear en una fase posterior un objeto propio:

```text
dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR
```

No se crea en esta auditoría. El objeto deberá leer `DESCARGA` y resolver sus
relaciones con `PARTIDAS`, `IMPORTACIONES`, `PSALIDAS`, `SALIDAS`, `MATERIAL` y,
si se requiere descripción, `PRODUCTOS`.

### 1.3 Granularidad recomendada

**CONFIRMADO:** la granularidad mínima es una fila física de `DESCARGA`, es
decir, una asignación histórica entre:

```text
partida de entrada + línea de salida + material utilizado
```

No debe aplicarse `DISTINCT` para ocultar duplicados. En el ambiente auditado:

- `DESCARGA`: 3,866 filas.
- Combinaciones distintas `(Pentradalink, Psalidalink, Clave)`: 3,643.
- Grupos duplicados de esa combinación: 223.
- Máximo de filas por grupo: 2.
- Líneas de salida con una fila `DESCARGA`: 2,918.
- Líneas de salida con dos filas `DESCARGA`: 474.
- Líneas de salida con un material distinto: 3,141.
- Líneas de salida con dos materiales distintos: 251.
- Grupos `(Psalidalink, Clave)` con dos filas: 223.

Los duplicados observados no son copias idénticas: separan cantidades, por
principio una fila de incorporación y otra con merma en la muestra. La
identidad física segura es `DESCARGA.Descargakey`.

## 2. Alcance y exclusiones

### Dentro de Materiales Utilizados V1

- Consultar el resultado persistido de descargas.
- Relacionar material de entrada con producto/línea de salida.
- Filtrar por rango de fecha de salida y filtros operacionales aprobados.
- Mostrar cantidades y unidades sin recalcular saldos.
- Mantener IDs técnicos para trazabilidad futura.
- Paginar de forma estable.

### Fuera de Materiales Utilizados V1

- Generar o recalcular `DESCARGA`.
- Ejecutar `SALDOS`, `DESCARGASALIDAPEPS` o variantes.
- Aplicar PEPS/FIFO.
- Actualizar `PARTIDAS.SALDO`.
- Insertar `TRAZO`.
- Construir o seleccionar estructuras/BOM vigentes.
- Consultar saldo inicial/final.
- Regularizar desperdicio.
- Generar descargos.
- Exportar archivos.
- Editar, eliminar o crear registros.

## 3. Fuentes auditadas

### 3.1 Fuentes de datos

| Fuente | Filas | Uso en la auditoría | Estado |
|---|---:|---|---|
| `dbo.DESCARGA` | 3,866 | Histórico de asignaciones y cantidades | CONFIRMADO |
| `dbo.PARTIDAS` | 2 | Partidas de entrada enlazadas | CONFIRMADO |
| `dbo.IMPORTACIONES` | 2 | Encabezado/documento de entrada | CONFIRMADO |
| `dbo.PSALIDAS` | 3,392 | Líneas de salida/producto | CONFIRMADO |
| `dbo.SALIDAS` | 660 | Encabezado/documento y fecha de salida | CONFIRMADO |
| `dbo.MATERIAL` | 2 | Catálogo de material | CONFIRMADO |
| `dbo.PRODUCTOS` | 204 | Catálogo de producto terminado | CONFIRMADO |
| `dbo.ESTRUCTURAS` | 0 | Estructuras/BOM vigentes | CONFIRMADO |
| `dbo.PRODUCTOMATERIAL` | 0 | Detalle de estructura/BOM | CONFIRMADO |
| `dbo.TRAZO` | 0 | Trazabilidad/faltantes del proceso | CONFIRMADO |
| `dbo.DIRIGIDO` | 0 | Descarga dirigida | CONFIRMADO |
| `dbo.HistoriaDescarga` | 2 | Histórico auxiliar del proceso legacy | CONFIRMADO |
| `dbo.HistoriaDescargaSalida` | 0 | Tabla auxiliar usada por historial de salida | CONFIRMADO |

La búsqueda por nombre y definición también encontró tablas auxiliares como
`DESCARGA_CTMA`, `DESCARGA_DESPERDICIO`, `DescargaDesp`, `DescDirDesperdicios`,
`DESCARGACTMF`, `CONCENTRADOSALDOS`, `A31_DESCARGAS` y variantes históricas.
No se consideran fuente canónica de V1 sin una decisión específica de negocio.

### 3.2 Objetos SQL consultados por metadata

Se consultaron `sys.objects`, `sys.sql_modules`, `sys.parameters`,
`sys.columns`, `sys.indexes`, `sys.foreign_keys`,
`sys.sql_expression_dependencies` y
`sys.dm_exec_describe_first_result_set_for_object` cuando el objeto era un
procedimiento o trigger.

El descriptor de result set no acepta views directamente y devuelve error 11531
para ellas; sus definiciones se analizaron con `sys.sql_modules`.

## 4. Modelo físico

```text
IMPORTACIONES (encabezado de entrada)
    1 ─────────────── N
PARTIDAS (partida de entrada)
    1 ─────────────── N
DESCARGA (asignación histórica)
    N ─────────────── 1
PSALIDAS (línea de salida)
    N ─────────────── 1
SALIDAS (encabezado de salida)
```

Relaciones lógicas observadas:

```text
DESCARGA.PENTRADALINK  -> PARTIDAS.Partidakey
PARTIDAS.Importacionlink -> IMPORTACIONES.Ipedimentokey
DESCARGA.PSALIDALINK   -> PSALIDAS.Psalidakey
DESCARGA.SALIDALINK    -> SALIDAS.SalidaKey
DESCARGA.Clave         -> PARTIDAS.Clave / MATERIAL.clave
DESCARGA.PT            -> PSALIDAS.Clave / PRODUCTOS.CVE_PRODUCTO
```

**CONFIRMADO:** no se observaron foreign keys DDL entre las tablas principales.
Los joins son relaciones lógicas legacy. Los links de `DESCARGA`,
`PSALIDAS.Salidalink` y varios IDs históricos son `float`; no deben exponerse
como `double` ni usarse sin validación de integralidad.

### 4.1 `dbo.DESCARGA`

- Filas: **3,866**.
- PK clustered y única: `PK_descarga(Descargakey)`.
- `Descargakey`: `bigint NOT NULL IDENTITY`.
- FKs: ninguna.
- Índices:
  - `IHD1(Entradalink, Pentradalink)`.
  - `INDICE2_G5(Pentradalink)`.
  - `op4(Psalidalink)` con `Descargakey` incluido.
- Defaults: ninguno observado.

Inventario completo de columnas (`?` = nullable, `*` = identity):

```text
Descargakey bigint* NOT NULL,
Entradalink float?, Pentradalink float?, Salidalink float?, Psalidalink float?,
CantUtil float?, Unidad char(5)?, Valor_pesos float?, Valor_Dolares float?,
Origen char(5)?, Impuestos_causados float?, Desperdicio float?,
Val_desperdicio float?, Merma float?, Val_merma float?, Tasa float?,
CantUtilT float?, UnidadT char(5)?, MermaT float?, DesperdicioT float?,
desperdiciolink float?, SaldoDesperdicio float?, linea bigint?, orden int?,
Importacion varchar(50)?, PT varchar(50)?, Salida varchar(50)?,
Clave varchar(50)?, SALDOCTMA numeric(18,4)?, PROMKEY bigint?,
OriginalItem varchar(50)?, tipoItem varchar(2)?, SUBMAQUILA numeric(18,7)?,
PUA numeric(18,4)?, VAD numeric(18,4)?, IVAD numeric(18,4)?
```

### 4.2 `dbo.PARTIDAS`

- Filas: **2**.
- PK clustered y única: `PK_partidas(Partidakey)`.
- `Partidakey`: `numeric(18,0) NOT NULL`.
- FKs: ninguna.
- Índices relevantes:
  - `PK_partidas(Partidakey)`.
  - `op7(PDESCARGA, Saldo, FECHAVENCE, FECHAIMPO)` con `Clave` y
    `ORDENDESCARGA` incluidos.
  - `op11(Clave, PDESCARGA, Saldo, FECHAVENCE, FECHAIMPO)` con
    `ORDENDESCARGA` incluido.
  - `op10(ORDENDESCARGA)`.
  - `INDICE1_G5(Cantidad)` con columnas de cobertura.

Columnas completas:

```text
Partidakey numeric(18,0) NOT NULL,
Clave varchar(50)?, Descripcion varchar(250)?, Fraccion varchar(15)?,
Cantidad numeric(18,4)?, Unidad varchar(10)?, Val_aduanal numeric(18,4)?,
Val_dolares numeric(18,4)?, Origen varchar(50)?, Arancel float?,
Importacionlink numeric(18,0)?, Saldo numeric(18,4)?, Categoria char(5)?,
Proveedor char(15)?, CantidadT float?, UnidadT char(5)?, TipoMat char(25)?,
Val_comercial float?, Lote varchar(50)?, Factura char(30)?, Cantotala float?,
Cantotalb float?, Cantotalc float?, FacInc float?, Observaciones char(78)?,
pident char(4)?, isprosec bit?, ValorME numeric(18,4)?, partida int?,
Montoigi float?, Fpigi float?, Montoiva float?, Fpiva float?,
TasaCCompen float?, MontoCCompen float?, FpCCompen float?, Tasaiva float?,
ALMACENKEY numeric(18,0)?, FechaFactura datetime?, TipoTasaIGIE char(25)?,
llave varchar(60)?, pesobruto float?, paisvendedor varchar(50)?, taxid varchar(50)?,
complemento1 varchar(50)?, complemento3 varchar(50)?, complemento2 varchar(50)?,
permiso varchar(50)?, marca varchar(50)?, modelo varchar(50)?, serie varchar(50)?,
PNUMERO_PED varchar(20)?, PTC numeric(18,4)?, PDESCARGA varchar(10)?,
PFECHAIMPO datetime?, PFECHAVENCE datetime?, ORDENDESCARGA bigint?,
EsActivo varchar(2)?, FECHAIMPO datetime?, FECHAVENCE datetime?, TL varchar(20)?,
PX varchar(20)?, PEDIMENTO varchar(50)?, observacion varchar(50)?,
tc numeric(18,5)?, fme numeric(18,9)?, CLAVEVIEJA varchar(50)?,
CantidadCOriginal numeric(18,4)?, MermaD numeric(18,4)?, DesperdicioD numeric(18,4)?,
carga varchar(50)?, familia varchar(50)?, DESCARGAP varchar(10)?,
tcpar numeric(18,6)?, SALDO_UEPS numeric(18,4)?, recargos numeric(18,4)?,
COVE varchar(100)?, USUARIO varchar(100)?, FECHAMODIFICACION datetime?,
APLICAA int?, unidadPedimento varchar(10)?, CantidadPedimento numeric(18,4)?,
INCOTERM varchar(5)?, FRACCIONPERMISO varchar(50)?, FCreacion datetime DEFAULT getdate()?,
SBM bigint?, PKORIGINAL bigint?, CLAVESUBMAQUILA varchar(50)?, NICO varchar(10)?,
fpIEPS numeric(18,0)?, montoIEPS numeric(18,4)?
```

### 4.3 `dbo.IMPORTACIONES`

- Filas: **2**.
- PK clustered y única: `PK_Importaciones(Ipedimentokey)`.
- `Ipedimentokey`: `numeric(18,0) NOT NULL`.
- FKs e índices secundarios: ninguno observado.

```text
Ipedimentokey numeric(18,0) NOT NULL, Aduana varchar(10)?, Patente varchar(10)?,
Numero_ped varchar(20)?, Fecha datetime?, TipoOper varchar(25)?,
CveProveedor varchar(15)?, Cve_pedimento varchar(5)?, Recibe varchar(20)?,
Tratado varchar(35)?, ident varchar(4)?, programa varchar(25)?, igipagado bit?,
FactorInc numeric(18,4)?, Tracking varchar(25)?, Cia varchar(25)?,
Fecha_ad datetime?, tc numeric(18,4)?, iva numeric(18,4)?, dta numeric(18,4)?,
advalorem numeric(18,4)?, porcentaje numeric(18,4)?, Observaciones varchar(78)?,
FactorME numeric(18,4)?, NoPermiso varchar(15)?, FactorIva numeric(18,4)?,
Lote char(50)?, almacenkey numeric(18,4)?, prevalidacion numeric(18,4)?,
validacion varchar(250)?, referencia varchar(20)?, AduanaES varchar(10)?,
PesoTotalBruto numeric(18,4)?, TipoPed int?, TipoOperacion varchar(10)?,
TOper int?, FechaPresentacion datetime?, ValorComercialtotalDlls numeric(18,4)?,
Otrosincrementables numeric(18,4)?, Fletes numeric(18,4)?, Seguros numeric(18,4)?,
Embalajes numeric(18,4)?, VComTotalPrecioPagado numeric(18,4)?,
VAduanatotal numeric(18,4)?, DESCARGA varchar(2)?, PedimentoOriginal varchar(20)?,
CNT numeric(18,4)?, FECHAORIGINAL datetime?, NombreProveedor varchar(250)?,
TaxID varchar(50)?, marca varchar(50)?, USUARIO varchar(100)?,
FECHAMODIFICACION datetime?, ValidadoVu varchar(10)?, MULTAS numeric(18,4)?,
RECARGOS numeric(18,4)?, IVA_PRE numeric(18,4)?, FCreacion datetime DEFAULT getdate()?
```

### 4.4 `dbo.PSALIDAS`

- Filas: **3,392**.
- PK clustered y única: `PK_psalidas(Psalidakey)`.
- `Psalidakey`: `numeric(18,0) NOT NULL`.
- `Salidalink`: `float NULL`.
- FKs: ninguna.
- Índices relevantes:
  - `IHD2(Psalidakey, Salidalink)` con columnas de factura/fecha/cantidad/clave.
  - `opt2(Salidalink)` con `Psalidakey` incluido.
  - `opt1(Cantidad)` con claves de relación incluidas.
  - `op6(ORDENDESCARGA)`.

```text
Psalidakey numeric(18,0) NOT NULL, Factura char(50)?, Fecha datetime?,
Fraccion char(12)?, Descripcion varchar(250)?, Cantidad numeric(18,4)?,
Unidad char(5)?, Val_pesos numeric(18,4)?, Val_dolares numeric(18,4)?,
Salidalink float?, ArancelTLCAN float?, Clave varchar(50)?, Factura2 char(40)?,
corte varchar(50)?, ValManoObra numeric(18,4)?, ValMatPrima numeric(18,4)?,
ValorMOU numeric(18,4)?, ObDesc char(200)?, Anexo char(3)?, montoigi float?,
fpigi float?, montoiva float?, fpiva float?, tasacc float?, montocc float?,
fpcc float?, ValorME float?, Fraccioneucan char(15)?, Tasaeucan float?,
Araneucan float?, Importeeucan float?, DescripcionIngles char(30)?,
Observaciones char(78)?, Almacenkey float?, valorcomercial float?,
valoragregado float?, partida float?, paisd varchar(10)?, paisc varchar(10)?,
llaveglosa varchar(50)?, pesobruto float?, unidadt varchar(10)?, cantidadt float?,
glosakey float?, validacion varchar(5)?, CodProveedor varchar(20)?,
bloqueado int?, descargaDesp int?, descargaDirigida varchar(50)?,
ValorAduanaCR numeric(18,4)?, TipoContenedor varchar(50)?, Contenedor varchar(50)?,
Documento2 varchar(50)?, Folio varchar(50)?, Lote varchar(50)?, pu numeric(8,4)?,
marca varchar(50)?, modelo varchar(50)?, serie varchar(50)?, FECHADESCARGA datetime?,
ORDENDESCARGA bigint?, ESTRUCTURAKEY bigint?, cve_cliente varchar(20)?,
M2 numeric(18,4)?, carga varchar(50)?, multa numeric(18,4)?, COVE varchar(100)?,
USUARIO varchar(100)?, FECHAMODIFICACION datetime?, No_ParteCli varchar(50)?,
unidadPedimento varchar(10)?, CantidadPedimento numeric(18,4)?,
pedimentoexportacionctm varchar(50)?, idctm bigint?, IGIE numeric(18,4)?,
SEGUROS numeric(18,4)?, FLETES numeric(18,4)?, EMBALAJES numeric(18,4)?,
OTROS numeric(18,4)?, INCOTERM varchar(30)?, FCreacion datetime DEFAULT getdate()?,
NICO varchar(10)?, PERMISO varchar(50)?
```

### 4.5 `dbo.SALIDAS`

- Filas: **660**.
- PK clustered y única: `PK_salidas(SalidaKey)`.
- `SalidaKey`: `numeric(18,0) NOT NULL`.
- FKs: ninguna.
- Índices relevantes:
  - `INDICE1_G6(Fecha)` con `SalidaKey`, `Documento` y `Cve_pedimento` incluidos.
  - `op2(Fecha)` con `SalidaKey` y `DESCARGA` incluidos.

```text
SalidaKey numeric(18,0) NOT NULL, Tipo_operacion char(25)?, Aduana char(10)?,
Agente char(10)?, Documento char(60)?, Fecha datetime?, Cve_cliente char(15)?,
Cve_pedimento char(5)?, Transfiere char(20)?, Pais char(10)?, Origen char(30)?,
DocAduanero char(20)?, FechaDocA datetime?, FechaEntrada datetime?,
PruebaSuf char(35)?, TotalMonto float?, TCDocAduanero float?,
PedComplementario char(25)?, FechaCompl datetime?, Tc float?, dta float?,
Observaciones char(150)?, FactorME float?, Almacenkey float?,
bloqueado numeric(18,0)?, validacion varchar(255)?, referencia varchar(50)?,
AduanaDespacho varchar(50)?, PesoTotalBruto float?,
ClavePedimentoOriginal varchar(50)?, ClavePedimentoRectificada varchar(50)?,
FechadePresentacion datetime?, ValorComercialTotalDlls float?,
VComTotalPrecioPagado float?, VAduanatotal float?, Otrosincrementables float?,
Fletes float?, Seguros float?, Embalajes float?, DESCARGA varchar(2)?,
PedimentoOriginal varchar(20)?, prev float?, TipoPedimento int?, Tema varchar(50)?,
Proyecto varchar(50)?, Transportes varchar(50)?, Placas varchar(50)?,
Licencia varchar(50)?, TipoDescarga varchar(10)?, TipoOperacion int?,
CNT numeric(18,4)?, TIPO_F4 varchar(20)?, corrida datetime?, FECHADESCARGA datetime?,
cve_a31 varchar(5)?, USUARIO varchar(100)?, FECHAMODIFICACION datetime?,
ValidadoVu varchar(10)?, IDENTIFICADOR varchar(5)?, CARGA varchar(50)?,
IVA numeric(18,4)?, IVA_PRE numeric(18,4)?, IGIE numeric(18,4)?, MULTAS numeric(18,4)?,
RECARGOS numeric(18,4)?, FCreacion datetime DEFAULT getdate()?,
PEDIMENTOS varchar(max)?, PEDIMENTOSC varchar(max)?
```

### 4.6 Catálogos y tablas auxiliares

| Tabla | Filas | PK | Columnas funcionales relevantes | Índices/FKs |
|---|---:|---|---|---|
| `MATERIAL` | 2 | `PK_material(materialkey)` | `materialkey numeric(18,0)`, `clave varchar(50)?`, `descripcion varchar(250)?`, `fraccion char(10)?`, `unidad char(5)?`, `unidadt char(5)?`, `tipomaterial char(100)?`, más atributos de proveedor/familia/serie | Sólo PK; sin FK |
| `PRODUCTOS` | 204 | `PK_productos(PRODUCTOKEY)` | `PRODUCTOKEY numeric(18,0)`, `CVE_PRODUCTO varchar(50)?`, `NOMBRE varchar(250)?`, `UNIDAD varchar(10)?`, `fraccion varchar(12)?`, `UNIDADT varchar(10)?`, más cliente/almacén/tipo/NICO | Sólo PK; sin FK |
| `ESTRUCTURAS` | 0 | `PK_estructuras(estructurakey)` | `estructurakey bigint`, `inicio datetime?`, `productolink bigint?` | Sólo PK; sin FK |
| `PRODUCTOMATERIAL` | 0 | `PK_productomaterial(PRODMATKEY)` | `PRODMATKEY numeric(18,0)`, `CVE_MATERIAL varchar(50)?`, `CANT_UTILIZADA numeric(18,7)?`, `CANT_MERMADA numeric(18,7)?`, `CANT_DESPERDICIADA numeric(18,7)?`, `PRODUCTOLINK numeric(18,0)?`, `DESCRIPCION varchar(250)?`, `UNIDAD char(10)?`, `VMax/VMin float?`, `estructuralink bigint?`, auxiliares | Sólo PK; sin FK |
| `TRAZO` | 0 | `PK_trazo(trazokey)` | `trazokey bigint identity`, `psalidakey bigint?`, `producto varchar(50)?`, `cantidad float?`, `linea bigint?`, `descargo float?`, `falto float?`, `padre varchar(50)?`, `salidakey bigint?`, `observacion varchar(100)?`, `PRODMATKEY bigint?`, `PRODMATKEYT bigint?`, `CLAVEORIGINAL varchar(50)?` | `op3(psalidakey)`; sin FK |
| `DIRIGIDO` | 0 | `PK_dirigido(dirigidokey)` | `dirigidokey bigint`, `documento varchar(35)?`, `clave varchar(50)?`, `incorporado/desperdicio/merma float?`, `salidakey int?`, `psalidakey int?`, `valorcomercial numeric(14,2)?`, `PU numeric(18,8)?`, `Fraccion varchar(10)?`, `saldo numeric(18,4)?`, `Factura varchar(50)?` | `indopt1(psalidakey)`; sin FK |

Defaults relevantes confirmados: `FCreacion` usa `getdate()` en las tablas que
lo declaran. No se observaron defaults que cambien las cantidades de
`DESCARGA`.

## 5. Relaciones y calidad de datos

### 5.1 Relaciones `DESCARGA` → entrada

| Relación | Total | Nulos | No enteros | Match | Fallos | Links distintos |
|---|---:|---:|---:|---:|---:|---:|
| `Pentradalink` → `PARTIDAS.Partidakey` | 3,866 | 0 | 0 | 3,866 | 0 | 2 |
| `Entradalink` → `IMPORTACIONES.Ipedimentokey` | 3,866 | 0 | 0 | 3,866 | 0 | 2 |
| `PARTIDAS.Importacionlink` → `IMPORTACIONES.Ipedimentokey` | 2 | 0 | — | 2 | 0 | 2 |

**CONFIRMADO:** los joins funcionan para el snapshot actual. La relación física
sigue siendo lógica y usa coerción `float`/`numeric`.

### 5.2 Relaciones `DESCARGA` → salida

| Relación | Total | Nulos | No enteros | Match | Fallos | Links distintos |
|---|---:|---:|---:|---:|---:|---:|
| `Psalidalink` → `PSALIDAS.Psalidakey` | 3,866 | 0 | 0 | 3,866 | 0 | 3,392 |
| `Salidalink` → `SALIDAS.SalidaKey` | 3,866 | 0 | 0 | 3,866 | 0 | 660 |

**CONFIRMADO:** no existen líneas de salida o encabezados de salida sin una
fila `DESCARGA` en este snapshot. Esto no es una garantía histórica para todos
los ambientes.

### 5.3 Relaciones de material y producto

| Comparación | Total | Match | Fallo | Valores distintos | Estado |
|---|---:|---:|---:|---:|---|
| `DESCARGA.Clave` ↔ `MATERIAL.clave` | 3,866 | 3,866 | 0 | 2 | CONFIRMADO actual |
| `DESCARGA.Clave` ↔ `PARTIDAS.Clave` | 3,866 | 3,866 | 0 | 2 | CONFIRMADO actual |
| `DESCARGA.PT` ↔ `PRODUCTOS.CVE_PRODUCTO` | 3,866 | 3,866 | 0 | 204 | CONFIRMADO actual |
| `DESCARGA.PT` ↔ `PSALIDAS.Clave` | 3,866 | 3,866 | 0 | 204 | CONFIRMADO actual |
| `DESCARGA.Clave` ↔ `PRODUCTOS.CVE_PRODUCTO` | 3,866 | 0 | 3,866 | 2 | CONFIRMADO que no es producto |

**CONFIRMADO:** `DESCARGA.Clave` identifica material en la muestra actual;
`DESCARGA.PT` identifica producto terminado. Para V1 se recomienda resolver
ambos desde las tablas relacionadas, no confiar en los duplicados textuales de
`DESCARGA`.

`DESCARGA.Importacion` no es una clave canónica: sus dos valores actuales son
documentos completos con formato de aduana/patente/folio, mientras que
`IMPORTACIONES.Numero_ped` contiene sólo el folio. El match textual fue 0 de
3,866. La consulta debe usar `Entradalink` → `IMPORTACIONES.Ipedimentokey`.
`DESCARGA.Salida` sí coincide con `SALIDAS.Documento` tras recortar espacios.

### 5.4 Huérfanos e inversas

| Medición | Total | Sin relación |
|---|---:|---:|
| `PARTIDAS` sin `DESCARGA` | 2 | 0 |
| `IMPORTACIONES` sin `DESCARGA` | 2 | 0 |
| `PSALIDAS` sin `DESCARGA` | 3,392 | 0 |
| `SALIDAS` sin `DESCARGA` | 660 | 0 |
| `DESCARGA` sin `TRAZO` | 3,866 | 3,866 |

`TRAZO` vacío no convierte las descargas en inválidas: los procedimientos lo
usan para faltantes y trazabilidad del proceso, no como vínculo obligatorio para
consultar el material utilizado.

### 5.5 Identidad y duplicados

- `Descargakey`: valores 1 a 3,866, 3,866 distintos.
- `linea`: 3,866 valores no nulos, pero sólo 1 valor distinto (`1`).
- `orden`: 3,866 valores no nulos, pero sólo 1 valor distinto (`10`).
- `PROMKEY`: nulo en las 3,866 filas.
- `(Pentradalink, Psalidalink, Clave)`: 223 grupos repetidos, máximo 2 filas.
- `(Psalidalink, Clave, linea)`: misma duplicidad; `linea` no desambigua.

**CONFIRMADO:** `linea`, `orden` y `PROMKEY` no pueden imponerse como identidad
funcional en el ambiente actual.

## 6. Cantidades y unidades

### 6.1 Evidencia de la definición de `SALDOS`

`dbo.SALDOS` recibe:

```text
@TOTINCORPORADO
@TOTDESPERDICIADO
@TOTMERMADO
@FECHAEXPORT
@PRODMATKEY
```

La definición confirma:

```text
@TOTAL = @TOTINCORPORADO + @TOTDESPERDICIADO + @TOTMERMADO
@PINCORPORADO = @TOTINCORPORADO * 100 / @TOTAL
@PMERMA       = @TOTMERMADO * 100 / @TOTAL
@PDESPERDICIO = @TOTDESPERDICIADO * 100 / @TOTAL
```

Al insertar en `DESCARGA`, el procedimiento conserva por separado `CANTUTIL`,
`MERMA`, `DESPERDICIO`, `CANTUTILT`, `MERMAT` y `DESPERDICIOT`, además de las
unidades. También actualiza `PARTIDAS.SALDO` e inserta `TRAZO`.

### 6.2 Interpretación

| Campo | Significado | Estado |
|---|---|---|
| `DESCARGA.CantUtil` | Porción incorporada/consumida del material en unidad comercial de la partida | CONFIRMADO por definición de `SALDOS` |
| `DESCARGA.Merma` | Porción de merma separada del total solicitado | CONFIRMADO por definición y reportes |
| `DESCARGA.Desperdicio` | Porción de desperdicio separada del total solicitado | CONFIRMADO por definición; snapshot actual nulo |
| `DESCARGA.CantUtil + Merma + Desperdicio` | Total descargado usado por el historial legacy | CONFIRMADO por `PROC_HISTORIADESCARGASALIDA` y `HISTORIADESCARGASF` |
| `DESCARGA.CantUtilT` | Cantidad incorporada convertida a unidad tarifaria | CONFIRMADO por definición |
| `DESCARGA.MermaT` / `DesperdicioT` | Conversiones tarifarias separadas | CONFIRMADO por columnas; valor actual pendiente por estar nulo |
| `DESCARGA.Unidad` | Unidad de `PARTIDAS.Unidad` elegida al guardar el descargo | CONFIRMADO por `SALDOS` |
| `DESCARGA.UnidadT` | Unidad de `PARTIDAS.UnidadT` elegida al guardar el descargo | CONFIRMADO por `SALDOS` |

### 6.3 Calidad numérica actual

- `CantUtil`: no nulo en 3,866 filas; mínimo `0.4234`; máximo `14664.0892`;
  cero negativos; cero ceros; suma diagnóstica aproximada `5612886.6854`.
- `CantUtilT`: no nulo en 3,866 filas; coincide exactamente con `CantUtil` en
  3,866 filas actuales.
- `Merma`: no nulo en 3,866 filas; mínimo `0`; máximo `320.632`; suma
  diagnóstica aproximada `38700.1869`; sin negativos.
- `Desperdicio`: nulo en 3,866 filas actuales.
- `MermaT`: nulo en 3,866 filas actuales.
- `DesperdicioT`: nulo en 3,866 filas actuales.
- `Unidad` y `UnidadT`: no nulos, iguales en 3,866 filas; el valor observado es
  `KG`.
- Total histórico diagnóstico `CantUtil + COALESCE(Merma,0) +
  COALESCE(Desperdicio,0)`: mínimo `0.4272`, máximo `14751.2027`, suma
  aproximada `5651586.8723`.

**RECOMENDACIÓN V1:** no colapsar silenciosamente `CantUtil`, `Merma` y
`Desperdicio` en un solo significado. Si la pantalla conserva una columna
`Cantidad consumida`, debe definirse explícitamente como
`CantUtil + COALESCE(Merma,0) + COALESCE(Desperdicio,0)` y devolver, de ser
posible, los tres componentes por separado.

## 7. Estructuras/BOM frente a histórico

### Evidencia actual

- `ESTRUCTURAS`: 0 filas.
- `PRODUCTOMATERIAL`: 0 filas.
- `PSALIDAS.ESTRUCTURAKEY`: 3,392 filas con valor `1`, aunque no existe la
  estructura actual `1`.
- `PROMKEY` en `DESCARGA`: nulo en 3,866 filas.
- `DESCARGA`: 3,866 filas históricas todavía enlazables a entradas/salidas.

### Definiciones

`DESCARGASALIDAPEPS`, `DESCARGATSALIDA1` y `DESCARGATSALIDAFECHA` consultan
`GETPRODUCTSTRUCT(PSALIDAS.Clave, SALIDAS.Fecha)`, explotan
`PRODUCTOMATERIAL` y llaman a `SALDOS`. `SALDOS` persiste el resultado en
`DESCARGA`, actualiza `PARTIDAS.SALDO` e inserta `TRAZO`.

**CONFIRMADO:** la estructura/BOM participa en la generación del descargo.

**INFERIDO:** `DESCARGA` conserva el resultado histórico y Materiales
Utilizados debe leer ese resultado, no recalcular la estructura vigente al
momento de la consulta. La inferencia está respaldada por la existencia de
`DESCARGA` aun con las tablas de estructura vacías, pero no se ejecutó un
proceso mutable para demostrar el antes/después.

**PENDIENTE DE VALIDAR:** política funcional cuando una descarga histórica no
puede reconstruirse desde la estructura vigente. V1 debe leer la fila
persistida y no omitirla por ausencia de BOM actual.

## 8. Descarga, descargo, saldos y trazo

### 8.1 Descarga versus descargo

**CONFIRMADO:** “descargo” es el proceso que calcula/asigna materiales. Sus
procedimientos borran y recrean filas, actualizan saldos y escriben trazabilidad.

**CONFIRMADO:** “Materiales Utilizados” puede consultar el resultado persistido
de `DESCARGA` sin ejecutar ese proceso.

### 8.2 Descarga versus saldos

`SALDOS` selecciona partidas con `PDESCARGA = 'SI'`, saldo mayor a `0.001` y
vigencia compatible con `@FECHAEXPORT`. Ordena por `INDICE` y `PFECHAIMPO`,
calcula la cantidad asignada, inserta `DESCARGA` y actualiza `PARTIDAS.SALDO`.

**CONFIRMADO:** saldo es estado/cálculo mutable y queda fuera de Materiales
Utilizados V1.

### 8.3 Descarga versus trazo

`TRAZO` está vacío. Sus definiciones consumidoras aparecen en procesos de
descargo y representan descargado/faltante/relación de estructura.

**INFERIDO:** `TRAZO` pertenece a trazabilidad del proceso y no es necesario
para el listado V1, porque `DESCARGA` ya tiene links completos en el snapshot.

## 9. Fechas

### 9.1 Candidatos auditados

| Campo | No nulos | Rango actual | Uso observado | Estado |
|---|---:|---|---|---|
| `SALIDAS.Fecha` | 660/660 | 2025-10-31 a 2026-08-18 | Fecha de salida/pago operacional; filtro de historial | CONFIRMADO |
| `PSALIDAS.Fecha` | 3,392/3,392 | 2025-10-31 a 2026-08-18 | Fecha de factura/línea | CONFIRMADO |
| `SALIDAS.FechaEntrada` | 0/660 | — | No utilizable actual | CONFIRMADO |
| `SALIDAS.FechaDocA` | 0/660 | — | No utilizable actual | CONFIRMADO |
| `SALIDAS.FechadePresentacion` | 0/660 | — | No utilizable actual | CONFIRMADO |
| `SALIDAS.FECHADESCARGA` | 0/660 | — | No utilizable actual | CONFIRMADO |
| `IMPORTACIONES.Fecha` | 2/2 | 2025-09-23 a 2026-05-28 | Fecha de pago/entrada; usada por reportes de importación/saldos | CONFIRMADO |
| `IMPORTACIONES.Fecha_ad` | 2/2 | 2025-09-23 a 2026-05-28 | Fecha de entrada equivalente en snapshot | CONFIRMADO |
| `PARTIDAS.PFECHAIMPO` | 2/2 | 2025-09-23 a 2026-05-28 | Vigencia de partida para saldo | CONFIRMADO |
| `PARTIDAS.FECHAIMPO` | 0/2 | — | No utilizable actual | CONFIRMADO |

### 9.2 Evidencia específica de Materiales Utilizados/Historial

`PROC_HISTORIADESCARGASALIDA` recibe `@DESDE` y `@HASTA` y filtra:

```sql
SALIDAS.FECHA >= @DESDE
AND SALIDAS.FECHA <= @HASTA
```

El mismo procedimiento proyecta `FECHASALIDA`, relaciona producto y salida, y
luego persiste por cada `DESCARGA` `CantUtil`, `Desperdicio`, `Merma` y su suma.

`HISTORIADESCARGASF` es otra variante histórica: filtra
`IMPORTACIONES.FECHA` para recorrer una entrada y después muestra las salidas
asociadas ordenadas por `SALIDAS.Fecha`. Las dos variantes tienen propósitos
históricos distintos.

### 9.3 Decisión propuesta para V1

**RECOMENDADO / INFERIDO:** el rango de Materiales Utilizados V1 debe aplicarse
a `SALIDAS.Fecha`, porque:

1. el caso “material usado por salida” se ancla a la salida/exportación;
2. `PROC_HISTORIADESCARGASALIDA` es el objeto histórico más cercano a la
   pantalla y filtra explícitamente `SALIDAS.Fecha`;
3. `v_descarga` expone `SALIDAS.Fecha` como `Fecha de Salida`;
4. `V_INFORMEDESCARGAS` separa fecha de pago de entrada y `FechaPagoExp`.

**PENDIENTE DE VALIDAR:** aprobación funcional de que la pantalla operacional
use fecha de salida y no fecha de importación. La evidencia técnica es fuerte,
pero el acceso funcional autenticado al sistema legacy no estuvo disponible en
esta sesión.

### 9.4 Regla de rango futura

El contrato debe ser inclusivo por fecha, pero el SQL debe evitar perder horas
del último día. Propuesta conceptual:

```text
desde <= fecha < hasta_exclusivo
```

`hasta_exclusivo` será el día siguiente salvo `9999-12-31`. Para ese máximo no
se debe ejecutar ciegamente `DATEADD(day, 1, @Hasta)`, porque desborda el rango
de fecha de SQL Server; debe usarse una rama max-safe con el último instante
representable de `datetime`.

No se implementó este SQL durante la auditoría.

## 10. Pantalla y reporte legacy

### 10.1 Evidencia accesible

Está disponible en el repositorio el prototipo histórico
`docs/03-diseno/prototipos/images/MATERIALES-UTILIZADOS.png`.

El prototipo muestra:

- título `OPERACIONES · MATERIALES UTILIZADOS`;
- `Desde` obligatorio;
- `Hasta` obligatorio;
- filtro `Material`;
- filtro `Producto`;
- botones `Consultar` y `Limpiar`;
- botones `Exportar` y `Refrescar`;
- columnas `Importación`, `Exportación`, `Material`, `Producto`,
  `Cant. consumida`, `Unidad`, `Fecha`;
- paginación de 25/50/100;
- estado visual de sin resultados/error/correlationId/403/sesión expirada.

**Estado:** evidencia de prototipo, no prueba de contrato implementado.

### 10.2 Acceso web legacy

**PENDIENTE DE VALIDAR:** no se realizó login ni consulta contra la pantalla
Web Forms legacy en esta auditoría. No había una sesión autorizada disponible
para ejecutar el caso de uso. No se ejecutaron POST técnicos ni acciones que
pudieran generar o recalcular descargas.

### 10.3 Reporte legacy

Los objetos `v_descarga`, `V_INFORMEDESCARGAS`, `DESCARGA_CTMA`,
`DESCARGA_DESPERDICIO` e `INFORMEDESCARGOS` son reportes o proyecciones
read-only relacionadas. No tienen la misma proyección ni la misma finalidad:

| Objeto | Columnas aproximadas | Fecha principal | Uso | Aptitud V1 |
|---|---:|---|---|---|
| `v_descarga` | 35 | Importación y salida | Descarga amplia | Referencia, no contrato |
| `V_INFORMEDESCARGAS` | 42 | Importación y exportación | Conciliación/descargo | Referencia, no contrato |
| `DESCARGA_CTMA` | 19 | `SALIDAS.Fecha` | CTMA | No |
| `DESCARGA_DESPERDICIO` | 19 | `SALIDAS.Fecha` | Desperdicio | No |
| `INFORMEDESCARGOS` | agregado | `SALIDAS.Fecha` | Informe fiscal/descargos | No |
| `V_STATUS_DESCARGAS` | 17 | `SALIDAS.Fecha` | Estado de descargo | No |

**CONFIRMADO:** ningún reporte debe reutilizarse directamente como contrato
HTTP V1 sin fijar campos, joins, filtros, orden y paginación.

## 11. Objetos SQL relacionados y clasificación

### 11.1 Candidatos read-only

| Objeto | Tipo | Clasificación | Efecto | Observación |
|---|---|---|---|---|
| `dbo.v_descarga` | VIEW | QUERY/REPORT | READ ONLY | 35 columnas; mezcla entrada, salida, material, cantidades y saldo |
| `dbo.V_INFORMEDESCARGAS` | VIEW | REPORT/QUERY | READ ONLY | 42 columnas; incluye saldo actual, valores y fechas |
| `dbo.DESCARGA_CTMA` | VIEW | REPORT | READ ONLY | Especializada CTMA |
| `dbo.DESCARGA_DESPERDICIO` | VIEW | REPORT | READ ONLY | Especializada desperdicio |
| `dbo.INFORMEDESCARGOS` | VIEW | REPORT | READ ONLY | Agrega valor descargado por salida/fracción |
| `dbo.V_STATUS_DESCARGAS` | VIEW | QUERY/REPORT | READ ONLY | Estado de descargo y estructura |
| `dbo.v_saldos` | VIEW | REPORT | READ ONLY | Saldo actual, no material utilizado |
| `dbo.v_saldosdesp` | VIEW | REPORT | READ ONLY | Saldos/desperdicios |
| `dbo.PR_INFORME_SALDOS` | PROCEDURE | REPORT/QUERY | READ ONLY | Tabla variable local; 37 columnas, fecha de importación |
| `dbo.PR_INFORME_IMPORTACIONES` | PROCEDURE | REPORT/QUERY | READ ONLY | Referencia de entradas; no material utilizado |
| `dbo.PR_INFORME_EXPORTACIONES` | PROCEDURE | REPORT/QUERY | READ ONLY | 49 columnas; no material utilizado dedicado |
| `dbo.GETPRODUCTSTRUCT` | FUNCTION | QUERY/CALCULATION | READ ONLY | Resuelve BOM vigente; no debe recalcular V1 |

`sys.dm_exec_describe_first_result_set_for_object` confirmó los 37 campos de
`PR_INFORME_SALDOS`; para views se confirmó la proyección leyendo su definición.
Ninguno de los objetos anteriores ofrece el contrato V1 completo.

### 11.2 Procesos mutables no ejecutados

| Objeto | Clasificación | Efecto | Evidencia de escritura/side effect |
|---|---|---|---|
| `DESCARGASALIDAPEPS` | PROCESS/CALCULATION | WRITE | Borra `TRAZO`/`DESCARGA`, actualiza `PARTIDAS`, llama `SALDOS`, inserta `TRAZO` |
| `DESCARGATSALIDA1` | PROCESS | WRITE | Modifica `SETTINGS`, borra/recrea descargas, actualiza saldos, ejecuta procesos |
| `DESCARGATSALIDAFECHA` | PROCESS | WRITE | Igual que anterior y filtra `SALIDAS.Fecha <= @HASTA` |
| `DESCARGAXFECHA51` | PROCESS/CALCULATION | WRITE | Variante por fecha y tablas auxiliares |
| `SALDOS` | CALCULATION | WRITE | Inserta `DESCARGA`, actualiza `PARTIDAS.SALDO`, inserta `TRAZO` |
| `SALDOS_FAMILIA` | CALCULATION | WRITE | Variante de saldo/familia |
| `SALDOS2` | CALCULATION | WRITE | Variante de descarga/saldo |
| `SALDOSDIRIGIDOS` | CALCULATION | WRITE | Descarga dirigida |
| `SALDOSCTM` | CALCULATION | WRITE | CTM y trazabilidad CTM |
| `DESCARGATODOSDIRIGIDOS` | PROCESS | WRITE | Descargas dirigidas |
| `DESCARGAINICIAL` | PROCESS | WRITE | Inicialización de descargas |
| `HISTORIADESCARGAS` | REPORT/PROCESS | WRITE | Borra/inserta `HISTORIADESCARGA` |
| `HISTORIADESCARGASF` | REPORT/PROCESS | WRITE | Borra/inserta historia; filtra `IMPORTACIONES.Fecha` |
| `HISTORIADESCARGASP` | REPORT/PROCESS | WRITE | Borra/inserta historia por documento de importación |
| `PROC_HISTORIADESCARGASALIDA` | REPORT/PROCESS | WRITE | `TRUNCATE` e `INSERT` en `HISTORIADESCARGASALIDA`; filtra `SALIDAS.Fecha` |
| `PROC_HISTORIADESCARGASALIDA;1` | REPORT/PROCESS | WRITE | Variante histórica del anterior |
| `PROC_HISTORIADESCARGASALIDAFALTANTES` | REPORT/PROCESS | WRITE | Historia de faltantes y `TRAZO` |
| `GUARDADESCARGACTMA` | COMMAND | WRITE | Actualiza saldos e historial CTMA |
| `LIGACTMA` | PROCESS | WRITE | Vincula datos CTMA |
| `LIGADESPERDICIOS` | PROCESS | WRITE | Vincula/regulariza desperdicios |
| `INFORME_CONCENTRADOSALDOS` | REPORT/PROCESS | WRITE | Llena tabla de concentración |
| `CARGA_MATERIALES` | IMPORT | WRITE | Carga catálogo de materiales |
| `CARGAPEDIMENTOS` | IMPORT/PROCESS | WRITE | Carga entradas y puede generar salidas |
| `CREAESTRUCTURAS` | PROCESS | WRITE | Genera estructura/BOM |
| `ESTRUCTURAS1A1` | PROCESS | WRITE | Prepara estructuras y llama creación |
| `RETORNO_SUBMAQUINA` | PROCESS | WRITE | Actualiza partidas/retornos |
| `SP_DESENSAMBLE` | PROCESS | WRITE | Modifica partidas/desensamble |
| `SP_GENERA_TXT_COMPLETO` | EXPORT | WRITE/MIXED | Exportación y uso de comandos externos |

También se detectaron familias A31, compulsa, CTM, carga y borrado. Se
consideran fuera del contrato V1 y no se ejecutaron.

### 11.3 Objetos no ejecutados

No se ejecutó ningún procedimiento de descarga, saldo, historial, estructura,
trazo, regularización, exportación o mantenimiento. En particular:

```text
DESCARGASALIDAPEPS
DESCARGATSALIDA1
DESCARGATSALIDAFECHA
DESCARGAXFECHA51
SALDOS
SALDOS_FAMILIA
SALDOS2
SALDOSDIRIGIDOS
PROC_HISTORIADESCARGASALIDA
PROC_HISTORIADESCARGASALIDAFALTANTES
HISTORIADESCARGAS
HISTORIADESCARGASF
HISTORIADESCARGASP
GUARDADESCARGACTMA
LIGACTMA
LIGADESPERDICIOS
INFORME_CONCENTRADOSALDOS
```

La razón es que sus definiciones contienen `INSERT`, `UPDATE`, `DELETE`,
`TRUNCATE`, `EXEC` a otros procesos o efectos persistentes desconocidos.

### 11.4 Triggers

No se observaron triggers asociados a `DESCARGA`, `PARTIDAS`, `IMPORTACIONES`,
`PSALIDAS`, `SALIDAS`, `MATERIAL`, `PRODUCTOS`, `ESTRUCTURAS`,
`PRODUCTOMATERIAL`, `TRAZO` o `DIRIGIDO`.

## 12. Trazabilidad de una fila real

Se siguió la fila `DESCARGA.Descargakey = 390`. Los valores se documentan sólo
como evidencia técnica de la muestra actual:

```text
IMPORTACIONES.Ipedimentokey = 1001
    documento/folio = 5003971
    fecha de importación = 2025-09-23
        |
PARTIDAS.Partidakey = 2001
    Clave = 500017
    material = AZUCAR ESTANDAR
    unidad = KG
        |
DESCARGA.Descargakey = 390
    Pentradalink = 2001.0
    Entradalink = 1001.0
    Psalidalink = 4124.0
    Salidalink = 3024.0
    CantUtil = 426.747
    Merma = 0
    Desperdicio = NULL
    Unidad = KG
    UnidadT = KG
        |
PSALIDAS.Psalidakey = 4124
    Clave/producto = 300861
    descripción = CHERRY SLICES
    unidad = CAJA
    cantidad de salida = 224
        |
SALIDAS.SalidaKey = 3024
    documento = 190-1562-5001284
    clave de pedimento = F4
    Fecha = 2025-12-01
```

**CONFIRMADO:** la fila es una asignación material-entrada a una línea de
salida. `DESCARGA.Importacion` no se usó como vínculo porque es un texto
legacy denormalizado distinto a `IMPORTACIONES.Numero_ped`.

## 13. Campos candidatos para V1

### 13.1 Campos visibles mínimos

| Campo API propuesto | Fuente | Estado | Nota |
|---|---|---|---|
| `importacion` | `IMPORTACIONES.Numero_ped` vía `DESCARGA.Entradalink` | CONFIRMADO | No usar `DESCARGA.Importacion` como canonical |
| `exportacion` | `SALIDAS.Documento` vía `DESCARGA.Salidalink` | CONFIRMADO | `char(60)` con espacios físicos |
| `materialCode` | `PARTIDAS.Clave` y validación `MATERIAL.clave` | CONFIRMADO actual | `DESCARGA.Clave` coincide en snapshot |
| `materialDescription` | `MATERIAL.descripcion` | CONFIRMADO actual | Resolver por código |
| `productCode` | `PSALIDAS.Clave`/`PRODUCTOS.CVE_PRODUCTO` | CONFIRMADO actual | `DESCARGA.PT` coincide en snapshot |
| `productDescription` | `PRODUCTOS.NOMBRE` o `PSALIDAS.Descripcion` | INFERIDO | Elegir una fuente canónica en contrato |
| `cantidadIncorporada` | `DESCARGA.CantUtil` | CONFIRMADO | No equivale automáticamente al total descargado |
| `cantidadMerma` | `DESCARGA.Merma` | CONFIRMADO | Separar de incorporación |
| `cantidadDesperdicio` | `DESCARGA.Desperdicio` | CONFIRMADO | Puede ser `NULL` en legacy |
| `cantidadTotalDescargada` | `COALESCE(CantUtil,0)+COALESCE(Merma,0)+COALESCE(Desperdicio,0)` | INFERIDO con evidencia histórica | La fórmula aparece en historiales |
| `unidad` | `DESCARGA.Unidad` / `PARTIDAS.Unidad` | CONFIRMADO | UMC del material de entrada |
| `cantidadTarifaria` | `DESCARGA.CantUtilT` | CONFIRMADO por columna | Datos actuales no demuestran conversión distinta |
| `unidadTarifaria` | `DESCARGA.UnidadT` / `PARTIDAS.UnidadT` | CONFIRMADO | UMT del material de entrada |
| `fecha` | `SALIDAS.Fecha` | RECOMENDADO; técnico confirmado | Pendiente aprobación funcional final |

### 13.2 IDs internos

No se deben exponer los `float` legacy como identificadores numéricos de
precisión doble. El modelo de backend debe conservar, preferentemente como
`BigDecimal` cuando provienen de `numeric(18,0)`:

```text
descargaId       <- DESCARGA.Descargakey (bigint)
partidaEntradaId <- PARTIDAS.Partidakey (numeric(18,0))
entradaId        <- IMPORTACIONES.Ipedimentokey (numeric(18,0))
partidaSalidaId  <- PSALIDAS.Psalidakey (numeric(18,0))
salidaId         <- SALIDAS.SalidaKey (numeric(18,0))
```

`Pentradalink`, `Entradalink`, `Psalidalink` y `Salidalink` sólo deben usarse
como links legacy validados contra esos IDs. `PROMKEY` no se propone: está nulo
en todas las filas actuales.

## 14. Filtros candidatos

### 14.1 Evidencia

| Filtro | Evidencia | Semántica observada |
|---|---|---|
| `desde` / `hasta` | Prototipo y todos los historiales | Rango obligatorio; el ancla depende de la variante |
| `material` | Prototipo; `PROC_HISTORIADESCARGASALIDA.@PROD` realmente filtra producto; `HISTORIADESCARGASF.@CODIGO` filtra entrada | Legacy usa `LIKE '%texto%'` en variantes |
| `producto` | Prototipo; `PROC_HISTORIADESCARGASALIDA.@PROD` filtra `PSALIDAS.Clave` | Legacy usa contiene |
| `pedimentoSalida` | `PROC_HISTORIADESCARGASALIDA.@DOCUMENTO` sobre `SALIDAS.Documento` | Legacy usa contiene |
| `clavePedimentoSalida` | `PROC_HISTORIADESCARGASALIDA.@CLAVE` sobre `SALIDAS.Cve_pedimento` | Legacy usa contiene |
| `pedimentoEntrada` | Campo y join existen; `HISTORIADESCARGASP` filtra `IMPORTACIONES.Numero_ped` | No aparece en el prototipo de Materiales Utilizados |
| `fraccion` | Existe en `PARTIDAS`/`PSALIDAS` | No evidenciado en pantalla de este caso |
| `numeroParte` | Producto/claves funcionales existen | Debe resolverse si será sinónimo de producto |

### 14.2 Propuesta V1

**Propuesta mínima respaldada:**

- `desde` obligatorio.
- `hasta` obligatorio.
- `material` opcional, igualdad exacta sobre `PARTIDAS.Clave`.
- `producto` opcional, igualdad exacta sobre `PSALIDAS.Clave`.
- `pedimentoSalida` opcional, igualdad exacta sobre `SALIDAS.Documento`.
- `clavePedimentoSalida` opcional, igualdad exacta sobre `SALIDAS.Cve_pedimento`.

`pedimentoEntrada`, `fraccion` y `numeroParte` quedan como filtros futuros o
requieren aprobación funcional explícita. Sus columnas existen, pero la pantalla
histórica no basta para incorporarlos al primer contrato.

### 14.3 Semántica de búsqueda

El legacy usa `LIKE '%texto%'` en `PROC_HISTORIADESCARGASALIDA` y en variantes
de historia. Para V1 se recomienda **igualdad exacta** para códigos completos:

- evita ocultar la semántica detrás de `LIKE '%...%'`;
- permite índices y planes previsibles;
- coincide con los contratos V1 de Entradas/Salidas ya implementados;
- evita devolver materiales/productos parcialmente parecidos sin intención.

Si negocio requiere búsqueda parcial, debe aprobarse por filtro y documentarse
por separado; no debe introducirse de forma global.

## 15. Propuesta de contrato HTTP V1

### 15.1 Endpoint y permiso

```http
GET /api/v1/operaciones/materiales-utilizados
```

Permiso propuesto:

```text
OPERACIONES_CONSULTAR
```

**Estado:** INFERIDO por consistencia con Entradas/Salidas y por tratarse de una
consulta operacional. Requiere aprobación del contrato; no se creó permiso ni
endpoint.

### 15.2 Parámetros propuestos

```text
desde                 date obligatorio
hasta                 date obligatorio
material              string opcional
producto              string opcional
pedimentoSalida       string opcional
clavePedimentoSalida  string opcional
pagina                int default 1
tamano                int default 20, rango 1..100
```


### 15.3 Response mínimo propuesto

```json
{
  "items": [
    {
      "descargaId": 390,
      "entradaId": "1001",
      "partidaEntradaId": "2001",
      "salidaId": "3024",
      "partidaSalidaId": "4124",
      "importacion": "5003971",
      "exportacion": "190-1562-5001284",
      "materialCode": "500017",
      "materialDescription": "AZUCAR ESTANDAR",
      "productCode": "300861",
      "productDescription": "CHERRY SLICES",
      "cantidadIncorporada": "426.7470",
      "cantidadMerma": "0.0000",
      "cantidadDesperdicio": null,
      "cantidadTotalDescargada": "426.7470",
      "unidad": "KG",
      "cantidadTarifaria": "426.7470",
      "unidadTarifaria": "KG",
      "fecha": "2025-12-01"
    }
  ],
  "total": 1,
  "pagina": 1,
  "tamano": 20
}
```

El JSON es propuesta, no contrato aprobado. Las cantidades deben mantenerse
como `BigDecimal` en Java y serializarse sin convertir a `double` ni perder
escala. Los IDs `numeric(18,0)` deben mantenerse como `BigDecimal` o una
representación decimal segura.

### 15.4 Orden y paginación

Orden recomendado y determinista:

```text
SALIDAS.Fecha DESC,
SALIDAS.SalidaKey DESC,
PSALIDAS.Psalidakey DESC,
DESCARGA.Descargakey DESC
```

SQL Server está en compatibility level 100. Si se crea el SP propio, usar
`ROW_NUMBER()` para paginar; no asumir `OFFSET/FETCH`. No agregar por material o
salida salvo que la aprobación cambie explícitamente la granularidad.

### 15.5 Joins propuestos

```text
DESCARGA d
JOIN PARTIDAS pe
  ON d.Pentradalink = representación legacy de pe.Partidakey
JOIN IMPORTACIONES i
  ON pe.Importacionlink = i.Ipedimentokey
JOIN PSALIDAS ps
  ON d.Psalidalink = representación legacy de ps.Psalidakey
JOIN SALIDAS s
  ON d.Salidalink = s.SalidaKey
LEFT JOIN MATERIAL m
  ON m.clave = pe.Clave
LEFT JOIN PRODUCTOS pr
  ON pr.CVE_PRODUCTO = ps.Clave
```

La implementación debe documentar y probar la conversión de links `float`; no
debe devolver esos links como `float`.

## 16. Decisión de fuente

| Alternativa | Ventajas | Limitaciones | Decisión |
|---|---|---|---|
| `PROC_HISTORIADESCARGASALIDA` | Fecha `SALIDAS.Fecha`, cantidades y filtros históricos | `TRUNCATE`, `INSERT`, tabla de historia, `LIKE`, side effects | No usar en GET |
| `HISTORIADESCARGASF/P` | Historia por importación, suma de cantidades | Borran/recrean historia, mezclan regularización/desperdicio | No usar en GET |
| `v_descarga` | Read-only, joins y columnas cercanas | 35 columnas, sin parámetros, sin paginación/total, mezcla saldos/reportes | Referencia, no contrato |
| `V_INFORMEDESCARGAS` | Ambos documentos, cantidades, unidades y fechas | 42 columnas, saldo actual y valores de conciliación | Referencia, no contrato |
| `DESCARGA_CTMA` / `DESCARGA_DESPERDICIO` | Proyecciones read-only especializadas | Casos CTMA/desperdicio, no general | No |
| `PR_INFORME_SALDOS` | Read-only y parametrizado | Fecha de importación, saldo actual, 37 columnas, no descarga por línea | No |
| `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | Contrato estable, paginación, total, aliases y filtros controlados | Aún no existe | **Recomendado en fase posterior** |

**Decisión:** no consumir directamente un reporte por el hecho de existir. Crear
un SP propio read-only sólo después de aprobar fecha, granularidad, filtros y
campos.

## 17. Queries diagnósticas utilizadas

Las consultas siguientes son representativas y no contienen credenciales. Se
ejecutaron contra `CALE_IMMEX` en solo lectura.

### 17.1 Base y compatibility

```sql
SELECT DB_NAME(), compatibility_level
FROM sys.databases
WHERE name = DB_NAME();

SELECT SERVERPROPERTY('ProductVersion'),
       SERVERPROPERTY('ProductLevel'),
       SERVERPROPERTY('Edition');
```

### 17.2 Inventario y conteos

```sql
SELECT SCHEMA_NAME(t.schema_id), t.name,
       SUM(CONVERT(bigint, ps.row_count)) AS row_count
FROM sys.tables t
JOIN sys.dm_db_partition_stats ps
  ON ps.object_id = t.object_id
 AND ps.index_id IN (0, 1)
WHERE t.name IN (...)
GROUP BY t.schema_id, t.name;
```

### 17.3 Columnas, PK, identity y defaults

```sql
SELECT c.column_id, c.name, ty.name, c.max_length,
       c.precision, c.scale, c.is_nullable, c.is_identity,
       dc.definition
FROM sys.columns c
JOIN sys.tables t ON t.object_id = c.object_id
JOIN sys.types ty ON ty.user_type_id = c.user_type_id
LEFT JOIN sys.default_constraints dc
  ON dc.parent_object_id = c.object_id
 AND dc.parent_column_id = c.column_id
WHERE t.name IN (...)
ORDER BY t.name, c.column_id;
```

### 17.4 Índices y foreign keys

```sql
SELECT t.name, i.name, i.type_desc, i.is_unique,
       i.is_primary_key, ic.key_ordinal, ic.is_included_column,
       c.name
FROM sys.tables t
JOIN sys.indexes i ON i.object_id = t.object_id
JOIN sys.index_columns ic
  ON ic.object_id = i.object_id
 AND ic.index_id = i.index_id
JOIN sys.columns c
  ON c.object_id = ic.object_id
 AND c.column_id = ic.column_id;

SELECT fk.name, OBJECT_NAME(fk.parent_object_id),
       OBJECT_NAME(fk.referenced_object_id)
FROM sys.foreign_keys fk;
```

### 17.5 Integridad lógica

```sql
SELECT COUNT_BIG(*) AS total,
       SUM(CASE WHEN d.Pentradalink IS NULL THEN 1 ELSE 0 END) AS nulls,
       SUM(CASE WHEN p.Partidakey IS NOT NULL THEN 1 ELSE 0 END) AS matched
FROM dbo.DESCARGA d
LEFT JOIN dbo.PARTIDAS p
  ON CONVERT(float, p.Partidakey) = d.Pentradalink;
```

Se repitió el patrón para `Entradalink`, `Psalidalink`, `Salidalink` y
`PARTIDAS.Importacionlink`.

### 17.6 Duplicados funcionales

```sql
SELECT Pentradalink, Psalidalink, Clave, COUNT(*) AS rows_count
FROM dbo.DESCARGA
GROUP BY Pentradalink, Psalidalink, Clave
HAVING COUNT(*) > 1;
```

### 17.7 Cantidades y fechas

```sql
SELECT COUNT_BIG(*), MIN(CantUtil), MAX(CantUtil),
       SUM(CASE WHEN CantUtil < 0 THEN 1 ELSE 0 END),
       MIN(Merma), MAX(Merma),
       MIN(Desperdicio), MAX(Desperdicio)
FROM dbo.DESCARGA;

SELECT MIN(Fecha), MAX(Fecha), COUNT(*)
FROM dbo.SALIDAS;
```

### 17.8 Objetos por nombre y definición

```sql
SELECT o.type_desc, o.name, LEN(m.definition)
FROM sys.objects o
JOIN sys.sql_modules m ON m.object_id = o.object_id
WHERE UPPER(o.name) LIKE '%DESCARG%'
   OR UPPER(m.definition) LIKE '%PENTRADALINK%'
   OR UPPER(m.definition) LIKE '%CANTUTIL%';
```

### 17.9 Parámetros y result set

```sql
SELECT o.name, p.parameter_id, p.name, ty.name,
       p.max_length, p.precision, p.scale, p.is_output
FROM sys.objects o
LEFT JOIN sys.parameters p ON p.object_id = o.object_id
LEFT JOIN sys.types ty ON ty.user_type_id = p.user_type_id
WHERE o.name IN (...);

SELECT name, system_type_name, is_nullable
FROM sys.dm_exec_describe_first_result_set_for_object(
       OBJECT_ID('dbo.PR_INFORME_SALDOS'), NULL);
```

No se guardaron archivos SQL persistentes ni se creó ningún objeto auxiliar.

## 18. Pendientes reales para aprobar contrato

1. Confirmar funcionalmente que la fecha V1 es `SALIDAS.Fecha` y no
   `IMPORTACIONES.Fecha`.
2. Confirmar si “Cantidad consumida” significa `CantUtil` o el total
   `CantUtil + Merma + Desperdicio`.
3. Confirmar si merma y desperdicio se muestran como columnas separadas.
4. Confirmar semántica exacta de filtros: igualdad, prefijo o contiene.
5. Confirmar si `pedimentoEntrada`, fracción y número de parte entran en V1 o
   quedan para una iteración posterior.
6. Confirmar descripción canónica del producto: `PRODUCTOS.NOMBRE` o
   `PSALIDAS.Descripcion`.
7. Validar links `float` con datos de mayor magnitud antes de fijar joins
   definitivos.
8. Confirmar comportamiento ante `DESCARGA.Desperdicio NULL`.
9. Auditar la pantalla Web Forms autenticada y el reporte exportable si se
   obtiene acceso seguro de solo lectura.
10. Acordar alias HTTP, límites de longitud y contrato de error/paginación.
11. Revalidar conteos contra un ambiente con estructuras/productomaterial reales;
    este snapshot tiene ambos catálogos vacíos.

## 19. Conclusión de auditoría

**CONFIRMADO:** Materiales Utilizados es una consulta histórica de filas de
`DESCARGA` que relaciona entrada, material y salida. La generación de esas
filas pertenece a descargos/saldos y es mutable.

**CONFIRMADO:** la fuente física principal es `dbo.DESCARGA`, con la cadena:

```text
DESCARGA
  -> PARTIDAS -> IMPORTACIONES
  -> PSALIDAS -> SALIDAS
  -> MATERIAL / PRODUCTOS
```

**RECOMENDADO:** contrato V1 paginado mediante un nuevo SP propio read-only,
`dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR`, después de cerrar fecha,
cantidad/fórmula, filtros y aliases. El objeto no se crea ahora.

**PENDIENTE:** aprobación funcional final de fecha y semántica de “consumida”,
además de acceso a la pantalla/reporte legacy para confirmar que el prototipo
representa la operación vigente.
