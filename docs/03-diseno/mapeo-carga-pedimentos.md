# Mapeo de carga de pedimentos V1

## 1. Alcance y fuente

Esta V1 cubre únicamente:

```text
archivo Excel → parser → validación → staging aislado → errores → preview durable
```

No confirma importaciones, partidas, salidas, descargos ni inventario. No ejecuta
`CARGAPEDIMENTOS`, `CARGA_ENCABEZADOS`, `INSERTAPEDIMENTO`, `VALIDAPEDIMENTO` ni
`VALIDA_I_DETALLENP`.

La auditoría read-only de `CALE_IMMEX` confirmó `dbo.CargaPedimentosIE` con 62
columnas, 2 filas observadas, PK clustered `PK_CargaPedimentosIE(CargaKey)`, sin
foreign keys ni índices secundarios observados. La tabla no se usa como staging
nuevo porque no demuestra aislamiento por usuario, archivo o lote.

No se encontró un layout oficial descargable o versionado en las fuentes
consultadas. Por ello:

- `PEDIMENT_LAYOUT_SOURCE = LEGACY_STAGE_DERIVED`;
- `PEDIMENT_LAYOUT_CONTRACT = PARTIAL`;
- los nombres físicos siguientes provienen de la metadata LIVE;
- las reglas fiscales, el nombre oficial de la hoja y la obligatoriedad de negocio
  siguen pendientes de confirmación.

La ausencia de `IGIE`, `IVA`, `DTA` y `PREV` en la metadata LIVE actual es
intencional: no se agregan al contrato nuevo sólo porque aparecieran en
 documentación histórica.

## 2. Tabla de mapping

`CONFIRMED` significa que la columna existe en la tabla legacy y que el parser V1
la reconoce. `PARTIAL` significa que falta la regla de negocio, formato oficial o
semántica completa. `UNKNOWN` significa que no hay evidencia suficiente para
incorporarla como contrato funcional.

La columna `Obligatorio` distingue la validación técnica V1 de una regla fiscal:
`Sí (V1)` no equivale a obligatoriedad legal definitiva.

| Campo funcional | Columna layout | Stage legacy | Tipo SQL LIVE | Obligatorio | Regla V1 | Evidencia | Estado |
|---|---|---|---|---|---|---|---|
| Aduana | `Aduana` | `Aduana` | `varchar(50)` nullable | Sí (V1) | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | CONFIRMED |
| Patente | `Patente` | `Patente` | `varchar(50)` nullable | Sí (V1) | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | CONFIRMED |
| Número/pedimento | `NumeroPedimento` | `NumeroPedimento` | `varchar(50)` nullable | Sí (V1) | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | CONFIRMED |
| Clave de pedimento | `ClavePedimento` | `ClavePedimento` | `varchar(50)` nullable | Sí (V1) | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | CONFIRMED |
| Tipo de operación | `TipoOperacion` | `TipoOperacion` | `int` nullable | Sí (V1) | entero; se aceptan valores documentados 1 y 2 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Tipo de pedimento | `TipoPedimento` | `TipoPedimento` | `int` nullable | No | entero cuando se informa | SQL_METADATA, AUDIT_UI, CODE | CONFIRMED |
| Tipo de cambio | `tc` | `tc` | `float` nullable | No | decimal canónico con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Fecha de pago | `FechaPago` | `FechaPago` | `datetime` nullable | Sí (V1) | fecha ISO o formato controlado | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Clave de contribuyente | `ClaveCP` | `ClaveCP` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Nombre de contribuyente | `NombreCP` | `NombreCP` | `varchar(150)` nullable | No | texto hasta 150 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Secuencia | `Sec` | `Sec` | `int` nullable | Sí (V1) | entero | SQL_METADATA, AUDIT_UI, CODE | CONFIRMED |
| Clave de artículo | `Clave` | `Clave` | `varchar(50)` nullable | Sí (V1) | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Descripción | `Descripcion` | `Descripcion` | `varchar(250)` nullable | Sí (V1) | texto hasta 250 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Fracción arancelaria | `Fraccion` | `Fraccion` | `varchar(50)` nullable | Sí (V1) | texto, nunca entero | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Cantidad comercial | `CantidadComercial` | `CantidadComercial` | `float` nullable | Sí (V1) | decimal positivo con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Unidad comercial | `UnidadComercial` | `UnidadComercial` | `varchar(50)` nullable | Sí (V1) | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Cantidad tarifa | `CantidadTarifa` | `CantidadTarifa` | `float` nullable | No | decimal positivo con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Unidad tarifa | `UnidadTarifa` | `UnidadTarifa` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| País de origen/destino | `PaisOD` | `PaisOD` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| País de compra/venta | `PaisCV` | `PaisCV` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Valor dólares | `ValorDolares` | `ValorDolares` | `float` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Valor comercial | `ValorComercial` | `ValorComercial` | `float` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Valor aduanal | `ValorAduanal` | `ValorAduanal` | `float` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Valor en moneda extranjera | `ValorME` | `ValorME` | `float` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Factura | `Factura` | `Factura` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Fecha de factura | `FechaFactura` | `FechaFactura` | `datetime` nullable | No | fecha cuando se informa | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Pedimento original | `PedimentoOriginal` | `PedimentoOriginal` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Descarga dirigida | `DescargaDirigida` | `DescargaDirigida` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Tasa IGIE | `TASAIGIE` | `TASAIGIE` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Forma de pago IGIE | `FPIGIE` | `FPIGIE` | `int` nullable | No | entero | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Tipo de tasa IGIE | `TIPOTASAIGIE` | `TIPOTASAIGIE` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Tasa IVA | `TASAIVA` | `TASAIVA` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Forma de pago IVA | `FPIVA` | `FPIVA` | `int` nullable | No | entero | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| CNT | `CNT` | `CNT` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| COVE | `COVE` | `COVE` | `varchar(100)` nullable | No | texto hasta 100 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Factor Incoterm | `FACTORINC` | `FACTORINC` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| FME | `FME` | `FME` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Activo | `ESACTIVO` | `ESACTIVO` | `varchar(5)` nullable | No | texto hasta 5 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Peso bruto | `PESOBRUTO` | `PESOBRUTO` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Apartado | `APARTADO` | `APARTADO` | `varchar(2)` nullable | No | texto hasta 2 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Descarga | `Descarga` | `Descarga` | `varchar(10)` nullable | No | texto hasta 10 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| IVA previo | `IVA_PRE` | `IVA_PRE` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Fecha de entrada | `FECHAENTRADA` | `FECHAENTRADA` | `datetime` nullable | No | fecha cuando se informa | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| NICO | `NICO` | `NICO` | `varchar(5)` nullable | No | texto hasta 5 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Lote | `lote` | `lote` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Complemento 3 | `complemento3` | `complemento3` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Complemento 2 | `complemento2` | `complemento2` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| División | `DIVISION` | `DIVISION` | `varchar(50)` nullable | No | texto hasta 50; no valida catálogo independiente | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Complemento 1 | `complemento1` | `complemento1` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Categoría | `CATEGORIA` | `CATEGORIA` | `varchar(10)` nullable | No | texto hasta 10; no valida relación sin evidencia | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Marca | `marca` | `marca` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Modelo | `modelo` | `modelo` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Serie | `serie` | `serie` | `varchar(50)` nullable | No | texto hasta 50 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| IVA previo retenido | `IVA_PRV` | `IVA_PRV` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Multas | `MULTAS` | `MULTAS` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Recargos | `RECARGOS` | `RECARGOS` | `numeric(18,4)` nullable | No | decimal con `BigDecimal` | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Incoterm | `INCOTERM` | `INCOTERM` | `varchar(5)` nullable | No | texto hasta 5 | SQL_METADATA, AUDIT_UI, CODE | PARTIAL |
| Clave de carga legacy | no es layout V1 | `CargaKey` | `bigint` NOT NULL | Generada | identidad de stage legacy; no se recibe del archivo | SQL_METADATA | PARTIAL |

## 3. Reglas técnicas V1

- Formatos admitidos: `.xls` y `.xlsx`, sólo si la firma binaria coincide con la
  extensión.
- Una sola hoja; si hay más, se reporta `HOJAS_NO_SOPORTADAS`.
- Máximo 10 MiB, 2.000 filas, 80 columnas y 100.000 celdas.
- Los encabezados se comparan normalizados; encabezados desconocidos y duplicados
  generan errores.
- Fechas se normalizan a ISO `uuuu-MM-dd`.
- Enteros se validan como `int`.
- Cantidades, tasas, pesos y valores se validan con `BigDecimal`; no se usa
  `float` en el modelo Java ni en el JSON nuevo.
- Fracción, pedimento, claves y números de negocio permanecen como texto.
- Los errores conservan fila, columna, código y mensaje, pero el valor se guarda
  como `no almacenado`.
- Las filas se guardan como JSON canónico sólo después del parseo y validación
  estructural.

## 4. Legacy observado y frontera de seguridad

| Objeto | Clasificación | Evidencia | Decisión V1 |
|---|---|---|---|
| `dbo.CARGAPEDIMENTOS` | MIXED | SELECT/INSERT/UPDATE/DELETE, cursor y referencias a `IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `DIRIGIDO`, `GENERADORES` | No ejecutar |
| `dbo.CARGA_ENCABEZADOS` | MIXED | SELECT/INSERT/UPDATE y referencias operativas | No ejecutar |
| `dbo.INSERTAPEDIMENTO` | MIXED | SELECT/INSERT/UPDATE/DELETE/EXEC/cursor | No ejecutar |
| `dbo.VALIDAPEDIMENTO` | MIXED | SELECT/DELETE/EXEC/cursor | No ejecutar |
| `dbo.VALIDA_I_DETALLENP` | MIXED | SELECT/INSERT/UPDATE/DELETE/cursor | No ejecutar |
| `dbo.CargaPedimentosIE` | staging legacy global no demostrado como aislado | 2 filas, 62 columnas, sin FKs ni índices secundarios | No escribir |
| `dbo.ERRORCARGA` | error legacy global no aislado | 3 columnas, PK `errorkey`, sin usuario/lote/archivo | No escribir |

La documentación legacy observada indica que `CARGAPEDIMENTOS` limpia
`ERRORCARGA` y puede crear o modificar materiales, productos, importaciones,
partidas, salidas, dirigidos y generadores. Por eso el pipeline mutable se deja
fuera de la V1.

## 5. Staging nuevo

La migración `infra/sql/migrations/09-pedimentos-staging-v1.sql` crea en
`ANEXO24_DEV/app24`:

- `app24.CargaPedimento`: archivo, SHA-256 único, usuario, fecha, estado,
  totales, versión de plantilla y correlación;
- `app24.CargaPedimentoFila`: hoja, número de fila y JSON validado, aislado por
  `carga_id`;
- `app24.ErrorCargaPedimento`: fila, columna, código, mensaje y valor
  enmascarado.

El command versionado es `APP24_C_PEDIMENTO_CARGA_CREAR`. Los queries versionados
son `APP24_Q_PEDIMENTO_CARGA_POR_HASH`, `APP24_Q_PEDIMENTO_CARGA_OBTENER` y
`APP24_Q_PEDIMENTO_CARGA_ERRORES`. La escritura nueva ocurre únicamente en
`ANEXO24_DEV`; no hay SQL de negocio inline en Java.

Estados V1: `PREVISUALIZADA` y `CON_ERRORES`. El procesamiento es síncrono:
no se persisten estados artificiales `RECIBIDA` o `VALIDANDO`. No existe
`CONFIRMADA` porque la confirmación operativa no es parte de esta entrega.

## 6. Reconciliación LIVE y seguridad transaccional

La estructura LIVE de `ANEXO24_DEV` coincide con el contrato versionado:

| SP | Parámetros LIVE | Referencias externas | Resultado |
|---|---:|---|---|
| `APP24_C_PEDIMENTO_CARGA_CREAR` | 11 | `CALE_IMMEX = 0`, SP legacy mutable = 0 | `STRUCTURAL_MATCH = PASS` |
| `APP24_Q_PEDIMENTO_CARGA_POR_HASH` | 2 | ninguna | `PASS` |
| `APP24_Q_PEDIMENTO_CARGA_OBTENER` | 3 | ninguna | `PASS` |
| `APP24_Q_PEDIMENTO_CARGA_ERRORES` | 3 | ninguna | `PASS` |

El command abre una transacción SQL con `XACT_ABORT`, inserta únicamente en
`app24` y registra la bitácora en la misma transacción. Su `COMMIT` es un commit
anidado de SQL Server: una transacción exterior permanece activa y pudo revertir
la carga, filas, errores y bitácora con `ROLLBACK`.

El smoke LIVE con payload 100% sintético verificó:

- command `CON_ERRORES` con 2 filas y 1 error;
- query por hash con `exists = 1`;
- detalle y errores coherentes mediante los SP versionados;
- bitácora `OPERACIONES / CARGA_PEDIMENTO_CON_ERRORES / FALLO`;
- después del rollback: carga, filas, errores y bitácora = 0.

El smoke adicional de duplicado produjo conflicto por SHA-256 repetido y dejó 0
filas y 0 eventos después del rollback: `DUPLICATE_HASH_BEHAVIOR = PASS`.

## 7. Compatibilidad de bitácora

`BitacoraAccion` incluye las acciones controladas:

- `CARGA_PEDIMENTO_VALIDADA`;
- `CARGA_PEDIMENTO_CON_ERRORES`.

`BitacoraConsultaJdbcAdapter` las convierte mediante `BitacoraAccion.valueOf` y
los tests verifican ambos mapeos. No hay whitelist separada en la API o reportes:
DTO, reportes y exportación reutilizan el enum/registro de dominio.

## 8. Paridad

La capacidad concreta de carga desde staging se actualiza en la matriz como
`LEGACY-016: MISSING → PARTIAL`: upload, validación estructural, errores, preview,
hash, RBAC y staging durable están implementados. `LEGACY-017` permanece
`MISSING` porque las reglas autoritativas de validación legacy antes del proceso
operativo aún no están cerradas. La confirmación hacia `IMPORTACIONES` y
`PARTIDAS` no se implementa.
