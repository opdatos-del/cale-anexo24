# Mapeo de importación autoritativa de pedimentos

Discovery V1 (solo lectura) del pipeline legacy posterior al staging/preview.
Reconstruye cómo la aplicación legada convierte filas de pedimento en tablas
operativas (`IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `PSALIDAS`).

```text
PEDIMENT_STAGING_V1 = IMPLEMENTED (upload + validación estructural + preview)
LEGACY_PEDIMENT_AUTHORITATIVE_CONFIRMATION = NOT_IMPLEMENTED
CALE_IMMEX_WRITES = 0
MUTABLE_SP_EXECUTED = 0
INLINE_JAVA_SQL = 0
```

La evidencia se obtuvo únicamente por metadata (`sys.objects`, `sys.columns`,
`sys.indexes`, `sys.sql_expression_dependencies`), `OBJECT_DEFINITION` y conteos
agregados. No se ejecutó ningún procedimiento legacy mutable ni se imprimieron
filas de negocio.

## 1. Frontera con la V1 existente

La V1 de `/operaciones/pedimentos` implementa `LEGACY-016` (carga a staging) y
parcialmente `LEGACY-017` (validación read-only PED-001..004). No implementa la
confirmación operativa, que corresponde a `LEGACY-057`.

| Capacidad | Superficie | Estado |
|---|---|---|
| `LEGACY-016` | `POST/GET /api/v1/operaciones/pedimentos/cargas` | PARTIAL |
| `LEGACY-017` | validación batch durante staging | PARTIAL |
| `LEGACY-019` | misma staging, `TipoOperacion = 2` | PARTIAL |
| `LEGACY-057` | confirmación hacia tablas operativas | MISSING |

`LEGACY-057` no es un duplicado de `LEGACY-016`: comparten el staging de entrada,
pero 057 es el procesamiento autoritativo y 016 sólo el preview.

## 2. Pipeline legacy (tres entradas, cuatro procesos)

La auditoría confirma que "Importar pedimentos" no es un único procedimiento, sino
**tres flujos de entrada distintos** que escriben las mismas tablas operativas:

```text
(A) Batch staging legacy  (wired)
    CARGAPEDIMENTOSIE ─► CARGAPEDIMENTOS ─► IMPORTACIONES / PARTIDAS
                                          └► SALIDAS / PSALIDAS / DIRIGIDO

(B) Per-pedimento         (dangling: PEDIMENTOS ausente)
    PEDIMENTOS ─► INSERTAPEDIMENTO @ITEM
                    ├─ EXEC VALIDAPEDIMENTO @ITEM ─► ERRORVALIDACION (INSERTERROR)
                    └─ IMPORTACIONES / PARTIDAS / SALIDAS / PSALIDAS / DESCARGA
                       / RECTIFICACIONIMPORT / RECTIFICACIONEXPORT

(C) Flujo NP / I_PEDIMENTO (dangling: NP e INVENTARIO ausentes)
    NP ─────────► CARGA_ENCABEZADOS ─► IMPORTACIONES / SALIDAS / I_DETALLENP
    I_PEDIMENTO ─► VALIDA_I_DETALLENP @PEDIMENTO,@TIPO
                    └─ I_ERROR / PARTIDAS / PSALIDAS

(D) INDICAOPERACION ─► UPDATE A31_ENTRADAS.OPERACION
```

## 3. Objetos y disponibilidad LIVE

| Objeto | Tipo | Filas | Estado LIVE |
|---|---|---:|---|
| `dbo.CARGAPEDIMENTOSIE` | staging batch | 2 | existe; 62 columnas; PK `PK_CargaPedimentosIE(CargaKey)` |
| `dbo.ERRORCARGA` | errores batch | 0 | existe; 3 columnas; PK `PK_errorcarga`; global, sin usuario/lote/archivo |
| `dbo.PEDIMENTOS` | staging per-pedimento | — | **NO EXISTE** |
| `dbo.INVENTARIO` | inventario validación | — | **NO EXISTE** |
| `dbo.NP` | staging NP | — | **NO EXISTE** |
| `dbo.I_PEDIMENTO` | staging NP encabezado | 0 | existe; 38 columnas; PK `PK_I_PEDIMENTO` |
| `dbo.I_DETALLENP` | staging NP detalle | 0 | existe; 15 columnas; PK `PK_I_DETALLENP` |
| `dbo.I_ERROR` | errores NP | 0 | existe; 4 columnas; PK identity |
| `dbo.I_FACTURA` | facturas NP | 0 | existe |
| `dbo.ERRORVALIDACION` | errores per-pedimento | 0 | existe; 6 columnas; **sin PK/unique** (heap) |
| `dbo.GENERADORES` | consecutivos | 5 | existe; `(tabla, consecutivo)`; **sin PK**; scratch global |
| `dbo.A31_ENTRADAS` | entradas A31 | 0 | existe; PK `PK_A31_ENTRADAS`; `OPERACION` escrito por `INDICAOPERACION` |

Hallazgo relevante: los flujos **(B)** y **(C)** referencian tablas que ya no
existen en `CALE_IMMEX` (`PEDIMENTOS`, `INVENTARIO`, `NP`). Sólo el flujo **(A)**
(`CARGAPEDIMENTOS`) apunta a un conjunto completo de tablas presentes. Esto
delimita la fuente candidata real a `CARGAPEDIMENTOSIE → CARGAPEDIMENTOS`.

Clasificación de estas dependencias ausentes (cierre en Parte B):

| Objeto | OBJECT_STATUS | Sinónimo | Linked server | Referencia cross-db |
|---|---|---|---|---|
| `PEDIMENTOS` | NOT_PRESENT_LIVE | no | no | no |
| `NP` | NOT_PRESENT_LIVE | no | no | no |
| `INVENTARIO` | NOT_PRESENT_LIVE | no | no | no |

No existen `sys.synonyms`, no hay linked servers (`sys.servers.is_linked = 0`) y
`sys.sql_expression_dependencies` no registra referencias cross-database para
estos objetos; por tanto no son dependencias externas sino objetos no presentes
en la instancia actual. El estado por flujo se registra por separado
(`FLOW_STATUS = BROKEN_IN_CURRENT_SCHEMA`, ver sección 15).

## 4. Call graph (SP → SP)

```text
CARGAPEDIMENTOS        (sin parámetros)   ── no ejecuta otros SP
CARGA_ENCABEZADOS      (sin parámetros)   ── no ejecuta otros SP
VALIDAPEDIMENTO        (@PEDIMENTO)       ── EXEC INSERTERROR (N veces)
INSERTAPEDIMENTO       (@ITEM)            ── EXEC VALIDAPEDIMENTO @ITEM
VALIDA_I_DETALLENP     (@PEDIMENTO,@TIPO) ── no ejecuta otros SP
INDICAOPERACION        (sin parámetros)   ── no ejecuta otros SP
INSERTERROR            (@ERROR,@PEDIMENTO,@TIPO,@PARTIDA) ── INSERT ERRORVALIDACION
```

```text
PEDIMENT_LEGACY_CALL_GRAPH = COMPLETE
aristas SP→SP = 2 (INSERTAPEDIMENTO→VALIDAPEDIMENTO, VALIDAPEDIMENTO→INSERTERROR)
```

No se observó `EXEC` con nombre dinámico, `sp_executesql` ni SQL dinámico en
ninguno de los cuerpos auditados.

## 5. Escrituras por procedimiento

### `dbo.CARGAPEDIMENTOS` (flujo A)

| Tabla | Acción | Condición observada |
|---|---|---|
| `ERRORCARGA` | `DELETE` | borra todo al inicio (no aislado) |
| `ERRORCARGA` | `INSERT` | longitudes aduana/patente/pedimento/clave/descripción/fracción; clave `R1` inválida |
| `CARGAPEDIMENTOSIE` | `UPDATE` | `CLAVEPEDIMENTO IN ('F4','F5','A3','DE')` → `TIPOOPERACION = 2` |
| `MATERIAL` | `INSERT` | crea materiales faltantes (`MAX(MATERIALKEY)+1` + `ROW_NUMBER`) |
| `PRODUCTOS` | `INSERT` | crea productos faltantes (`MAX(PRODUCTOKEY)+1` + `ROW_NUMBER`) |
| `IMPORTACIONES` | `INSERT` | rama `TIPOOPERACION = 1`, sin error y no duplicado |
| `PARTIDAS` | `INSERT` | `CARGAKEY` sin error y `TIPOOPERACION = 1`; `Importacionlink` al nuevo encabezado |
| `SALIDAS` | `INSERT` | rama `TIPOOPERACION = 2`, sin error y no duplicado en `SALIDAS.DOCUMENTO` |
| `PSALIDAS` | `INSERT` | detalle de la rama 2, ligado por `Salidalink` |
| `SALIDAS` | `UPDATE` | tipo de operación por clave (`F4/F5/A3/CT/DE`) y `DESCARGA`/`IVA` |
| `IMPORTACIONES` | `UPDATE` | `CVE_PEDIMENTO IN ('AF','A6')` → `TIPOOPER = 'Importacion AF'` |
| `DIRIGIDO` | `INSERT` | `PSALIDAS.DESCARGADIRIGIDA <> ''` y `PSALIDAKEY` no presente en `DIRIGIDO` |
| `GENERADORES` | `DELETE` | borra toda la tabla al final (scratch global) |

### `dbo.INSERTAPEDIMENTO` (flujo B, por pedimento)

| Tabla | Acción | Condición |
|---|---|---|
| `ERRORVALIDACION` | vía `VALIDAPEDIMENTO`/`INSERTERROR` | borra por pedimento y re-inserta errores |
| `SALIDAS` / `PSALIDAS` / `DESCARGA` | `DELETE` | `@TOPER = 2`; borra por `DOCUMENTO`/`SALIDALINK` antes de re-insertar |
| `RECTIFICACIONEXPORT` | `INSERT`/`UPDATE` | `@TIPOPED = 2`; mueve rectificaciones anteriores al nuevo `SALIDAKEY` |
| `RECTIFICACIONIMPORT` | `INSERT`/`UPDATE` | `@TIPOPED = 2`; mueve rectificaciones anteriores al nuevo `IPEDIMENTOKEY` |
| `SALIDAS` / `PSALIDAS` | `INSERT` | encabezado/detalle de salida |
| `IMPORTACIONES` / `PARTIDAS` | `INSERT` | encabezado/detalle de importación |
| `GENERADORES` | `UPDATE` | `CONSECUTIVO = key + 1` para `SALIDAS`, `PARTIDAS`, `IMPORTACIONES` |

### `dbo.CARGA_ENCABEZADOS` (flujo C)

| Tabla | Acción | Condición |
|---|---|---|
| `IMPORTACIONES` | `INSERT` | `I_PEDIMENTO.TIPO_OPERACION = 1` y `CLAVE_PEDIMENTO NOT IN ('F4','F5','A3')` y `DOCUMENTO NOT IN IMPORTACIONES` |
| `SALIDAS` | `INSERT` | `I_PEDIMENTO.TIPO_OPERACION = 2` y `DOCUMENTO NOT IN SALIDAS` |
| `SALIDAS` | `INSERT` | `CLAVE_PEDIMENTO IN ('F4','F5','A3')` (cambio de régimen/regularización) |
| `I_DETALLENP` | `INSERT` | desde `NP`, si `ADUANA-PATENTE-PEDIMENTO` no está en `I_DETALLENP` |
| `I_DETALLENP` | `UPDATE` | normaliza `UNIDAD_MEDIDA` por alias en `UNIDAD` |

### `dbo.VALIDA_I_DETALLENP` (flujo C)

| Tabla | Acción | Condición |
|---|---|---|
| `I_ERROR` | `DELETE` / `INSERT` | errores de cantidad, unidad, catálogo, valor ME por pedimento/partida |
| `I_DETALLENP` | `UPDATE` | remapea `CODIGO` por `claveProveedor`, calcula `FME` y `VALOR_FACTURA_DLLS` |
| `GENERADORES` | `DELETE` | borra toda la tabla al inicio (scratch global) |
| `PARTIDAS` | `DELETE`/`INSERT` | `@TIPO = 1`; borra por `IMPORTACIONLINK` y re-inserta desde `V_I_PEDIMENTOS` |
| `PSALIDAS` | `DELETE`/`INSERT` | `@TIPO = 2`; borra por `SALIDALINK` y re-inserta desde `V_I_PEDIMENTOS` |

### `dbo.INDICAOPERACION` (flujo D)

| Tabla | Acción | Condición |
|---|---|---|
| `A31_ENTRADAS` | `UPDATE` | pone `OPERACION = 0` y renumera por cursor; hereda operación del pedimento original si es rectificación |

```text
IMPORTACIONES:  INSERT/UPDATE
PARTIDAS:       INSERT/DELETE
SALIDAS:        INSERT/UPDATE/DELETE
PSALIDAS:       INSERT/DELETE
DIRIGIDO:       INSERT
DESCARGA:       DELETE
MATERIAL:       INSERT   (auto-creación de catálogo)
PRODUCTOS:      INSERT   (auto-creación de catálogo)
GENERADORES:    UPDATE/DELETE
RECTIFICACIONIMPORT / RECTIFICACIONEXPORT: INSERT/UPDATE
ERRORCARGA / ERRORVALIDACION / I_ERROR:    DELETE/INSERT
```

## 6. Transacción y atomicidad

Ninguno de los procedimientos auditados contiene `BEGIN TRAN`, `COMMIT`,
`ROLLBACK`, `SET XACT_ABORT ON`, `BEGIN TRY` ni `BEGIN CATCH`. El control de flujo
se apoya sólo en cursores, `IF` y `PRINT`.

```text
LEGACY_PEDIMENT_TRANSACTION = NONE
PARTIAL_WRITE_RISK = YES
```

Consecuencia: si el encabezado se inserta y una partida falla, o si una salida se
crea y otra línea falla, no existe rollback. El pipeline legacy puede dejar estado
parcial. Una confirmación moderna no puede delegar la atomicidad en este legado.

## 7. Idempotencia y duplicados

No hay índices únicos ni claves compuestas que impidan reprocesar el mismo
pedimento. La protección es lógica y **silenciosa** (omite filas, no genera error).

Regla de duplicado confirmada (`PED-007`):

| Rama | Condición exacta | Efecto |
|---|---|---|
| `TIPOOPERACION = 1` | `NUMEROPEDIMENTO NOT IN (SELECT NUMERO_PED FROM IMPORTACIONES WHERE NUMERO_PED IS NOT NULL)` | la fila no se inserta; sin error |
| `TIPOOPERACION = 2` | `NUMEROPEDIMENTO NOT IN (SELECT DOCUMENTO FROM SALIDAS WHERE DOCUMENTO IS NOT NULL)` | la fila no se inserta; sin error |

Otras protecciones:

- `INSERTAPEDIMENTO` y `VALIDA_I_DETALLENP` aplican patrón borrar-y-reinsertar por
  `DOCUMENTO` (idempotencia por reemplazo mientras corre una sola instancia).
- `CARGAPEDIMENTOS` limpia `ERRORCARGA` completo al inicio; no está aislado por
  usuario, archivo ni lote.

```text
LEGACY_PEDIMENT_IDEMPOTENCY = PARTIAL
PED_007_LEGACY_RULE = CONFIRMED
PED_007_MODERN_IDEMPOTENCY_KEY = UNKNOWN
```

La regla legacy (`NUMEROPEDIMENTO` vs `IMPORTACIONES.NUMERO_PED` para operación 1,
y vs `SALIDAS.DOCUMENTO` para operación 2) es evidencia de duplicado legacy. **No**
se declara todavía que esa combinación sea suficiente como clave de idempotencia
moderna; `PED_007_MODERN_IDEMPOTENCY_KEY` queda `UNKNOWN` hasta cerrar la identidad
operacional (Parte D).

## 8. Generación de claves y concurrencia

Todas las claves se calculan con `MAX(clave) + 1` (a veces `+ ROW_NUMBER()` para
inserciones de conjunto) y se sincronizan parcialmente contra `GENERADORES`.

```text
IMPORTACIONKEY = MAX(IPEDIMENTOKEY)+1  (+ ROW_NUMBER)  / GENERADORES='IMPORTACIONES'
PARTIDAKEY     = MAX(PARTIDAKEY)+1     (+ ROW_NUMBER)  / GENERADORES='PARTIDAS'
SALIDAKEY      = MAX(SALIDAKEY)+1      (+ ROW_NUMBER)  / GENERADORES='SALIDAS'
PSALIDAKEY     = MAX(PSALIDAKEY)+1     (+ ROW_NUMBER)
DIRIGIDOKEY    = MAX(DIRIGIDOKEY)+1    (+ ROW_NUMBER)
```

`GENERADORES` es una tabla scratch compartida (sin PK) que varios procedimientos
borran (`CARGAPEDIMENTOS`, `VALIDA_I_DETALLENP`) o actualizan; su `CONSECUTIVO` es
un espejo del `MAX+1`, no un `IDENTITY`/`SEQUENCE`.

```text
KEY_GENERATION_STRATEGY = MAX_PLUS_ONE_WITH_ROW_NUMBER (GENERADORES como espejo)
CONCURRENCY_SAFE = NO
```

Riesgo: dos confirmaciones concurrentes obtienen el mismo `MAX+1` y colisionan
(duplicado de PK o corrupción silenciosa). Una implementación moderna debe usar
`IDENTITY`/`SEQUENCE` o bloqueo explícito dentro de una transacción.

## 9. Validaciones (evidencia exacta)

`VALIDAPEDIMENTO @PEDIMENTO` (cursor por partida sobre `PEDIMENTOS`):

| Regla | Condición exacta | Mensaje |
|---|---|---|
| `PED-001` | `ISVALIDUNIT(@UMC) = 0` | `UNIDAD DE MEDIDA COMERCIAL INVALIDA: ...` |
| `PED-002` | `ISVALIDUNIT(@UMT) = 0` | `UNIDAD DE MEDIDA TARIFARIA INVALIDA: ...` |
| `PED-005` | `ABS(SUM(INVENTARIO.CANTUNIDADMEDIDACOMPRAVENTA) - @CANTCOMERCIAL) > 1` | `DIFERENCIA DE CANTIDAD` (tolerancia 1) |
| `PED-006` | `ABS(SUM(INVENTARIO.VALORENUSD) - @VALOR) > 3` | `DIFERENCIA DE VALOR DLLS` (tolerancia 3) |
| inventario | `ISVALIDUNIT(UNIDADMEDIDACOMPRAVENTA) = 0` | `UNIDAD DE MEDIDA COMPRA VENTA INVALIDA` |

`VALIDA_I_DETALLENP @PEDIMENTO,@TIPO`:

| Regla | Condición exacta | Mensaje |
|---|---|---|
| `PED-003` | `@TIPO = 1` y `CODIGO NOT IN (SELECT CLAVE FROM MATERIAL)` | `HAY NUMEROS DE PARTE QUE NO EXISTEN EN EL CATALOGO` |
| `PED-004` | `@TIPO = 2` y `CODIGO NOT IN (SELECT CVE_PRODUCTO FROM PRODUCTOS)` | `HAY PRODUCTOS QUE NO EXISTEN EN EL CATALOGO` |
| cantidad | `SUM(I_DETALLENP.CANTIDAD) <> I_PEDIMENTO.CANTIDAD` | `CANTIDAD ERRONEA` |
| unidad | `VALIDUNIT(UNIDAD_MEDIDA) <> @UNIDAD` | `UNIDAD ERRONEA` |
| valor ME | `ROUND(SUM(VALOR_FACTURA*ISNULL(FME,1)),-1) <> ROUND(@VALORDLLS,-1)` | `ERROR EN VALOR MONEDA EXTRANJERA` |

`CARGAPEDIMENTOS` (validación de longitud/dominio sobre `CARGAPEDIMENTOSIE`):
`LEN(ADUANA)=3`, `LEN(PATENTE)=4`, `LEN(NUMEROPEDIMENTO)>=15`,
`LEN(CLAVEPEDIMENTO)=2`, `LEN(CLAVE)>1`, `LEN(DESCRIPCION)>1`, `LEN(FRACCION)>=7`,
`CLAVECP`/`NOMBRECP` no vacíos, `TIPOOPERACION IN (1,2)`, `TIPOPEDIMENTO IN (1,2)`,
`CLAVEPEDIMENTO <> 'R1'`, y consistencia de encabezado dentro del mismo
`NUMEROPEDIMENTO` (aduanas, patentes, claves, tipos, `TC`, fechas, pedimento
original únicos).

Estado de reglas: `PED-001..PED-004` implementadas en `APP24_Q_PEDIMENTO_VALIDAR_REGLAS`;
`PED-005` y `PED-006` quedan `PARTIAL` porque requieren una tabla `INVENTARIO`
normalizada que el staging V1 no posee; `PED-007` es regla confirmada pero no
implementada.

## 10. Rectificaciones

`INSERTAPEDIMENTO` entra a `RECTIFICACIONIMPORT`/`RECTIFICACIONEXPORT` únicamente
cuando `PEDIMENTOS.TIPOPED = 2` (pedimento rectificado) y con
`PEDIMENTOORIGINAL` informado:

- inserta una fila con la nueva clave (`@IPEDIMENTOKEY`/`@SALIDAKEY`) y el documento;
- antes borra el encabezado original (`PEDIMENTOORIGINAL`) y luego reasigna las
  rectificaciones previas (`UPDATE ... SET LINK = @nuevo WHERE LINK = @anterior`).

No se implementan reglas nuevas de rectificación en esta fase.

## 11. Contratos (resultado del discovery)

| Contrato | Resultado | Razón |
|---|---|---|
| `IMPORT_CONFIRMATION_CONTRACT` | PARTIAL | Encabezado+partidas reconstruibles desde `CARGAPEDIMENTOS`; faltan transacción, claves seguras y decisión sobre auto-creación de `MATERIAL`/`PRODUCTOS` |
| `EXPORT_CONFIRMATION_CONTRACT` | PARTIAL | Salidas/psalidas/dirigido reconstruibles; misma carencia transaccional y de claves |
| `PEDIMENT_CONFIRM_PERMISSION` | UNKNOWN | No hay evidencia de permiso de confirmación en el modelo legacy; sin RBAC legacy capturado |

Recomendación de permiso (no implementada): reutilizar `PEDIMENTOS_CARGAR` como
mínimo privilegio mientras no exista evidencia de separación.

## 12. Decisión SP-FIRST

Todos los procedimientos candidatos quedan **no reutilizables** para construir una
confirmación segura, por efectos mutables, ausencia de transacción y scratch global:

| SP | Clasificación | Reutilizable | Motivo |
|---|---|---|---|
| `CARGAPEDIMENTOS` | MIXED | No | Escribe catálogos y operativas; `MAX+1`; borra `GENERADORES`/`ERRORCARGA` |
| `INSERTAPEDIMENTO` | MIXED | No | Borra-y-reinserta por documento; `EXEC` de validation; sin transacción |
| `CARGA_ENCABEZADOS` | MIXED | No | Escribe `IMPORTACIONES`/`SALIDAS`; staging `NP` ausente |
| `VALIDAPEDIMENTO` | MIXED | No | Inserta `ERRORVALIDACION`; lee `INVENTARIO` ausente |
| `VALIDA_I_DETALLENP` | MIXED | No | Escribe `PARTIDAS`/`PSALIDAS`; borra `GENERADORES`; staging `NP` ausente |
| `INDICAOPERACION` | WRITE | No | Actualiza `A31_ENTRADAS.OPERACION` |
| `INSERTERROR` | WRITE | No | Inserta `ERRORVALIDACION` |

Se conservan como **fuente de reglas** (no de ejecución): las validaciones de
`VALIDAPEDIMENTO`/`VALIDA_I_DETALLENP` y la regla de duplicado `PED-007`.

```text
SAFE_IMPLEMENTATION_STRATEGY = PROVISIONAL_NEW_AUTHORITATIVE_COMMAND
LEGACY_REUSABLE_FOR_CONFIRMATION = 0
APP24_COMMANDS_PROPOSED = APP24_C_PEDIMENTO_CONFIRMAR (nombre conceptual, no confirmado)
AUTHORITATIVE_COMMAND_DATABASE = UNKNOWN
```

`APP24_C_PEDIMENTO_CONFIRMAR` es únicamente un **nombre conceptual/tentativo**. No
se confirma todavía que deba vivir en `app24`, `ANEXO24_DEV` ni
`CALE_IMMEX.dbo`; la frontera transaccional (Parte F) aún no está cerrada.

`AUTHORITATIVE_COMMAND_DATABASE` queda explícitamente **sin decidir**, con valores
posibles `CALE_IMMEX`, `ANEXO24_DEV_CROSS_DATABASE`, `SPLIT_COORDINATION` o
`UNKNOWN`.

Justificación explícita de no reutilizar legacy: sin transacción (`NONE`), claves
`MAX+1` no seguras, `GENERADORES` como scratch global borrado por varios SP,
auto-creación de catálogos `MATERIAL`/`PRODUCTOS` no deseada en una confirmación
controlada, y procedimientos (B)/(C) apoyados en tablas `PEDIMENTOS`/`INVENTARIO`/
`NP` ausentes en LIVE (clasificación en Parte B).

## 13. Diseño futuro (no implementado en esta fase)

### 13.1 Estado de carga

`app24.CargaPedimento.estado` admite hoy sólo `PREVISUALIZADA` y `CON_ERRORES`
(`CK_CargaPedimento_estado`). Una confirmación requeriría, como mínimo, un estado
`CONFIRMADA` y un estado de fallo. Propuesta (no aplicada):

```text
PREVISUALIZADA -> CONFIRMADA
CON_ERRORES    -> (corregida) -> PREVISUALIZADA -> CONFIRMADA
CONFIRMADA     -> CONFIRMADA_FALLIDA (si el command falla)
```

### 13.2 Idempotencia operativa

El hash SHA-256 protege sólo la subida de archivo
(`HASH_GUARD != OPERATIONAL_CONFIRMATION_IDEMPOTENCY`). La clave operativa
propuesta debe derivarse de pedimento + operación (+ lote), respaldada por índice
único en `app24`, **no** por `NOT IN` en `IMPORTACIONES`.

### 13.3 API, UI y bitácora futuras

- `POST /api/v1/operaciones/pedimentos/cargas/{id}/confirmacion`.
- UI: acción "Confirmar" sólo con carga válida, sin errores, no confirmada y con
  permiso; confirmación explícita del usuario.
- Bitácora: `PEDIMENTO_CONFIRMADO` / `PEDIMENTO_CONFIRMACION_FALLIDA` (no creadas).
- La confirmación debe ejecutarse en una transacción atómica propia; no acepta
  escrituras parciales.

### 13.4 Matriz de pruebas futura

success import · success export · duplicate · invalid status · already confirmed ·
partial SQL failure · rollback · concurrent confirmation · unauthorized · forbidden.

## 14. Incógnitas conocidas

- No existen filas de negocio suficientes en las tablas operativas (2
  importaciones, 2 partidas) para validar cardinalidades 1:N amplias.
- `PED-005`/`PED-006` dependen de `INVENTARIO`, ausente en LIVE: su contrato de
  inventario queda sin fuente.
- El permiso de confirmación y la semántica de estados definitivos requieren
  decisión de negocio.
- No se confirmó si la confirmación moderna debe reproducir la auto-creación de
  materiales/productos del legacy.

```text
AUTHORITATIVE_PEDIMENT_CONTRACT = PARTIAL
READY_FOR_IMPLEMENTATION = NO (discovery)
```

---

## 15. Cierre V2 — ruta legacy activa

Dependencias de runtime verificadas por objeto (existen/ausentes en LIVE):

| SP | Dependencias faltantes | Estado |
|---|---|---|
| `CARGAPEDIMENTOS` | ninguna (CARGAPEDIMENTOSIE, ERRORCARGA, MATERIAL, PRODUCTOS, IMPORTACIONES, PARTIDAS, SALIDAS, PSALIDAS, DIRIGIDO, GENERADORES, CLIENTES, PROVEEDORES, VALIDUNIT, ISVALIDUNIT, FACTOR, EXISTEFACTOR) | `CARGAPEDIMENTOS_RUNTIME_DEPENDENCIES = COMPLETE` |
| `CARGA_ENCABEZADOS` | `NP` | `CARGA_ENCABEZADOS_RUNTIME_DEPENDENCIES = BROKEN` |
| `INSERTAPEDIMENTO` | `PEDIMENTOS`, `INVENTARIO` | `INSERTAPEDIMENTO_RUNTIME_DEPENDENCIES = BROKEN` |
| `VALIDAPEDIMENTO` | `PEDIMENTOS`, `INVENTARIO` | `VALIDAPEDIMENTO_RUNTIME_DEPENDENCIES = BROKEN` |
| `VALIDA_I_DETALLENP` | `NP` (a través de `V_I_PEDIMENTOS`) | BROKEN |
| `INDICAOPERACION` | ninguna (`A31_ENTRADAS` existe) | COMPLETE |

| Flow | Source | Missing deps | Target | FLOW_STATUS |
|---|---|---|---|---|
| A `CARGAPEDIMENTOS` | `CARGAPEDIMENTOSIE` | — | `IMPORTACIONES`/`PARTIDAS`/`SALIDAS`/`PSALIDAS`/`DIRIGIDO` | **ACTIVE** |
| B `INSERTAPEDIMENTO` | `PEDIMENTOS` | `PEDIMENTOS`, `INVENTARIO` | `IMPORTACIONES`/`PARTIDAS`/`SALIDAS`/`PSALIDAS`/`DESCARGA` | **BROKEN_IN_CURRENT_SCHEMA** |
| C `CARGA_ENCABEZADOS`/`VALIDA_I_DETALLENP` | `NP`/`I_PEDIMENTO` | `NP`, `INVENTARIO` | `IMPORTACIONES`/`SALIDAS`/`I_DETALLENP` | **BROKEN_IN_CURRENT_SCHEMA** |
| D `INDICAOPERACION` | `A31_ENTRADAS` | — | `A31_ENTRADAS.OPERACION` | **ACTIVE** (sólo alcanzable desde `INSERTAFALTANTESA31`, que no tiene llamadores) |

Cada objeto ausente se registra con `OBJECT_STATUS` y cada flujo con `FLOW_STATUS`
(son conceptos distintos; no se usa la doble etiqueta
`MISSING_DEPLOYMENT_OBJECT / DEAD_LEGACY_BRANCH`):

| Objeto | OBJECT_STATUS | Evidencia |
|---|---|---|
| `PEDIMENTOS` | `NOT_PRESENT_LIVE` | ausente en todos los esquemas; sin synonym; sin linked server; sin ref cross-db |
| `NP` | `NOT_PRESENT_LIVE` | idem |
| `INVENTARIO` | `NOT_PRESENT_LIVE` | idem |

Call graph de llamadores (metadata):

```text
INSERTAPEDIMENTO   <- NONE
CARGA_ENCABEZADOS  <- NONE
VALIDA_I_DETALLENP <- NONE
VALIDAPEDIMENTO    <- INSERTAPEDIMENTO
INSERTERROR        <- VALIDAPEDIMENTO
INDICAOPERACION    <- INSERTAFALTANTESA31 <- NONE
CARGAPEDIMENTOS    <- NONE
```

Ningún adapter Java, endpoint o SQL versionado invoca estas ramas. Aun así, la
taxonomía se mantiene conservadora: se registra `BROKEN_IN_CURRENT_SCHEMA` y **no**
`DEAD_CONFIRMED`, porque la ausencia del objeto por sí sola no basta y no se
revisó la totalidad de entrypoints legacy externos no versionados.

```text
PEDIMENTOS_DEPENDENCY_LOCATION = NOT_PRESENT_LIVE (no synonym / no linked / no cross-db)
NP_DEPENDENCY_LOCATION = NOT_PRESENT_LIVE (no synonym / no linked / no cross-db)
INVENTARIO_DEPENDENCY_LOCATION = NOT_PRESENT_LIVE (no synonym / no linked / no cross-db)
ACTIVE_PEDIMENT_PIPELINE = (A) CARGAPEDIMENTOSIE -> CARGAPEDIMENTOS -> tablas operativas
```

## 16. Cierre V2 — cobertura staging moderno → operación

El staging moderno guarda cada fila como JSON (`app24.CargaPedimentoFila.datos_json`)
con los 56 campos de `ExcelPedimentoParser.CAMPOS_CONFIRMADOS`. El legacy dispone
de 62 columnas en `dbo.CargaPedimentosIE`. Campos legacy sin equivalente moderno:
`IGIE`, `IVA`, `DTA`, `PREV`, `TIPOTASAIGIE` (y `CargaKey`, clave técnica).

`IMPORTACIONES` (19 columnas no-clave):

| Columnas cubiertas desde staging | Derivadas | Faltantes |
|---|---|---|
| `ADUANA,PATENTE,NUMERO_PED,FECHA,CVE_PEDIMENTO,TC,PEDIMENTOORIGINAL,CVEPROVEEDOR,CNT,MULTAS,RECARGOS,FECHA_AD,IVA_PRE` (13) + `DESCARGA` (según `TipoPedimento`) | `TIPOOPER`='IMPORTACION' | `IVA(SUM),DTA,PREVALIDACION,ADVALOREM(SUM IGIE)` |

`PARTIDAS` (42 columnas no-clave): cubiertas 38; derivadas `IMPORTACIONLINK`,
`UNIDAD`/`UNIDADT` (`VALIDUNIT`), `CANTIDAD` (`*FACTOR`); default `MONTOCCOMPEN=0`;
faltantes `MONTOIGI(IGIE)`, `MONTOIVA(IVA)`, `TIPOTASAIGIE`.

`SALIDAS` (17 columnas no-clave): cubiertas 14; derivada `TIPO_OPERACION`;
faltantes `DTA`, `PREV`.

`PSALIDAS` (28 columnas no-clave): cubiertas 24; derivadas `SALIDALINK`; default
`BLOQUEADO=0`, `CORTE(Lote)`; faltante `montoiva(IVA)`.

`IMPORT_FIELD_COVERAGE` (base = columnas de destino no-clave de los INSERT legacy):

| Categoría | IMPORTACIONES | PARTIDAS | Total |
|---|---:|---:|---:|
| STORED | 13 | 34 | 47 |
| DERIVED | 2 | 3 | 5 |
| CATALOG_LOOKUP | 0 | 1 | 1 |
| DEFAULT_CONFIRMED | 0 | 1 | 1 |
| MISSING_FROM_STAGING | 4 | 3 | 7 |
| NOT_APPLICABLE | 0 | 0 | 0 |
| UNKNOWN | 0 | 0 | 0 |
| **TOTAL** | **19** | **42** | **61** |

`EXPORT_FIELD_COVERAGE`:

| Categoría | SALIDAS | PSALIDAS | Total |
|---|---:|---:|---:|
| STORED | 13 | 22 | 35 |
| DERIVED | 2 | 4 | 6 |
| CATALOG_LOOKUP | 0 | 0 | 0 |
| DEFAULT_CONFIRMED | 0 | 1 | 1 |
| MISSING_FROM_STAGING | 2 | 1 | 3 |
| NOT_APPLICABLE | 0 | 0 | 0 |
| UNKNOWN | 0 | 0 | 0 |
| **TOTAL** | **17** | **28** | **45** |

Definición de "covered" (= STORED + DERIVED + DEFAULT_CONFIRMED +
CATALOG_LOOKUP + NOT_APPLICABLE):

```text
FIELD_COVERAGE_ACCOUNTING = PASS
IMPORT_FIELD_COVERAGE = PARTIAL (54 / 61)
EXPORT_FIELD_COVERAGE = PARTIAL (42 / 45)
```

Campos faltantes por flujo (todos `MISSING_FROM_STAGING`, ninguno derivable):

```text
IMPORT_MISSING_FIELDS = [IVA, DTA, PREVALIDACION(<-PREV), ADVALOREM(<-IGIE), MONTOIGI(<-IGIE), MONTOIVA(<-IVA), TIPOTASAIGIE]
EXPORT_MISSING_FIELDS = [DTA, PREV, montoiva(<-IVA)]
```

Columnas legacy sin equivalente moderno y su uso/target (metadata
`dbo.CargaPedimentosIE`, sin imprimir valores): todas `float(8) NULL` salvo
`TIPOTASAIGIE varchar(50) NULL`:

| Campo legacy | Tipo | Target |
|---|---|---|
| `IGIE` | float NULL | `IMPORTACIONES.ADVALOREM` (SUM), `PARTIDAS.MONTOIGI` |
| `IVA` | float NULL | `IMPORTACIONES.IVA` (SUM), `PARTIDAS.MONTOIVA`, `PSALIDAS.montoiva` |
| `DTA` | float NULL | `IMPORTACIONES.DTA`, `SALIDAS.DTA` (MAX) |
| `PREV` | float NULL | `IMPORTACIONES.PREVALIDACION`, `SALIDAS.PREV` (MAX) |
| `TIPOTASAIGIE` | varchar(50) NULL | `PARTIDAS.tipotasaigie` |

## 17. Cierre V2 — identidad operacional

Agregados LIVE (sin imprimir valores):

| Tabla | rows | distinct ident | NULLs | duplicate groups | DDL únicos |
|---|---:|---:|---:|---:|---|
| `IMPORTACIONES.NUMERO_PED` | 2 | 2 | 0 | 0 | ninguno (sólo PK) |
| `SALIDAS.DOCUMENTO` | 660 | 660 | 0 | 0 | ninguno (sólo PK) |

```text
IMPORT_OPERATIONAL_IDENTITY = {NumeroPedimento}
EXPORT_OPERATIONAL_IDENTITY = {NumeroPedimento}
LEGACY_DUPLICATE_IDENTITY = SINGLE_DOCUMENT_NUMBER
LEGACY_DUPLICATE_IDENTITY_CONTRACT = CONFIRMED
MODERN_OPERATIONAL_IDENTITY_CONTRACT = PARTIAL
```

La guardia legacy compara directamente el número de documento
(`NUMEROPEDIMENTO` vs `IMPORTACIONES.NUMERO_PED` / `SALIDAS.DOCUMENTO`), lo que
confirma `LEGACY_DUPLICATE_IDENTITY = SINGLE_DOCUMENT_NUMBER`. Los datos actuales
no muestran duplicados ni nulos. Sin embargo, esto **no** demuestra una restricción
universal fiscal ni una garantía transaccional moderna; por eso
`MODERN_OPERATIONAL_IDENTITY_CONTRACT = PARTIAL` hasta crear una protección
transaccional propia (índice único en `app24` + revalidación dentro de la
transacción).

## 18. Cierre V2 — PED-007 implementado

Implementado dentro del SP read-only existente `dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS`
(extensión, sin SP nuevo), con dos ramas `NOT EXISTS` NULL-safe que emiten error
explícito `PED-007` en lugar de omitir la fila en silencio:

- `TipoOperacion = 1`: `EXISTS (SELECT 1 FROM dbo.IMPORTACIONES WHERE NUMERO_PED = @numero)`.
- `TipoOperacion = 2`: `EXISTS (SELECT 1 FROM dbo.SALIDAS WHERE DOCUMENTO = @numero)`.

Mensajes sin datos sensibles. `PedimentoReglasJdbcAdapter` añade `NumeroPedimento`
al XML lote. Ver [[mapeo-validacion-pedimentos]].

```text
PED_007_IMPLEMENTED = YES (validación preventiva read-only)
PED_007_SCOPE = compatibilidad/duplicado operativo observado
PED_007_IS_NOT = garantía de idempotencia de confirmación
PED_007_SP_REUSE = PASS (extensión de APP24_Q_PEDIMENTO_VALIDAR_REGLAS; sin SP nuevo)
PED_007_SP = dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS
PED_007_LIVE_SMOKE = PASS (import existente, export existente, no existente, blank)
```

`PED-007` es validación preventiva de duplicado observado; reutiliza el SP
read-only existente y no crea uno redundante. No sustituye la idempotencia
transaccional de la futura confirmación (ver sección 17).

## 19. Cierre V2 — frontera transaccional

```text
DATABASES_SAME_INSTANCE = YES (ANEXO24_DEV y CALE_IMMEX en SERVERSAPBO\SERVERSAPBO_DEV)
CROSS_DB_ATOMIC_COMMAND_FEASIBLE = YES (misma instancia; transacción local con nombres de 3 partes, sin MSDTC)
RUNTIME_IDENTITY_CONTRACT = CONFIRMED
CURRENT_RUNTIME_LOGIN = opdatos
MODULE_C_LOGIN = opdatos
MODULE_C_DB_USER = dbo
APP_DB_LOGIN = opdatos
APP_DB_USER = dbo
CURRENT_RUNTIME_PERMISSION = SUFFICIENT
TARGET_LEAST_PRIVILEGE_LOGIN = anexo24_app (objetivo futuro)
TARGET_LEAST_PRIVILEGE_MIGRATION = PENDING
AUTHORITATIVE_COMMAND_DATABASE = UNKNOWN (sin decidir)
```

Identidad efectiva verificada con `SUSER_SNAME()` / `USER_NAME()` en las dos
conexiones reales: `opdatos` / `dbo` en `CALE_IMMEX` y en `ANEXO24_DEV`. El
principal real tiene `SELECT/INSERT/UPDATE/DELETE` sobre `IMPORTACIONES`,
`PARTIDAS`, `SALIDAS`, `PSALIDAS`, `DIRIGIDO` y sobre
`app24.CargaPedimento`/`CargaPedimentoFila`/`ErrorCargaPedimento`, por lo que
`CURRENT_RUNTIME_PERMISSION = SUFFICIENT` **para el runtime actual**. Esto **no**
valida a `anexo24_app` (EXECUTE-only), que permanece documentado como objetivo
futuro de hardening (`TARGET_LEAST_PRIVILEGE_MIGRATION = PENDING`); no se mezclan
ambos conceptos.

Criterios de ubicación pendientes: atomicidad (misma instancia la permite),
ownership/versionado (los objetos `APP24_*` viven hoy en `CALE_IMMEX.dbo` y los
staging en `ANEXO24_DEV.app24`), least privilege y acoplamiento. No se decide
`CALE_IMMEX` vs `ANEXO24_DEV_CROSS_DATABASE` vs `SPLIT_COORDINATION`.

Regla dura: **no** diseñar dos commits de base separados (CALE_IMMEX y app24 por
separado). Si no se puede garantizar atomicidad local, `READY_FOR_IMPLEMENTATION = NO`.

## 20. Cierre V2 — generación de claves

```text
legacy = MAX(key)+1 / ROW_NUMBER() / GENERADORES (espejo, sin PK)
KEY_ALLOCATION_STRATEGY_PROPOSED = sp_getapplock + transacción (o SERIALIZABLE/UPDLOCK-HOLDLOCK sobre GENERADORES)
KEY_ALLOCATION_COMPATIBLE_WITH_LEGACY = UNKNOWN
```

No se asume que se pueda introducir `IDENTITY`/`SEQUENCE` en tablas legacy sin
romper compatibilidad; se propone serializar la asignación de claves mediante
bloqueo de aplicación o hints de bloqueo dentro de la transacción, sin implementar.

## 21. Cierre V2 — auto-creación de catálogo

`CARGAPEDIMENTOS` puede **auto-crear** `MATERIAL` y `PRODUCTOS`; el staging moderno
en cambio exige que existan (`PED-003`/`PED-004`). Son estrategias contradictorias.

| Aspecto | Evidencia |
|---|---|
| legacy auto-create | `INSERT INTO MATERIAL`/`PRODUCTOS` con `MAX+1` + `ROW_NUMBER` |
| modern validation | `PED-003`/`PED-004` rechazan clave ausente |

```text
UNKNOWN_CATALOG_ITEM_POLICY = REJECT (DECISION_PROPOSED)
```

No se implementa auto-creación. Dado que existe carga de catálogos por separado,
se propone `REJECT` como decisión, marcada como propuesta hasta cerrar contrato.

## 22. Cierre V2 — permiso

```text
PEDIMENT_CONFIRM_PERMISSION = PEDIMENTOS_CONFIRMAR (DECISION_PROPOSED)
```

La confirmación realizará escrituras autoritativas, por lo que es más privilegiada
que `PEDIMENTOS_CARGAR`. Se propone separación por least privilege
(`PEDIMENTOS_CONFIRMAR`), sin implementar todavía mientras el contrato siga
`PARTIAL`. No hay evidencia de un permiso equivalente en el legacy (sin RBAC
capturado).

---

## 23. Cierre V3 — decisiones V1 formalizadas

```text
UNKNOWN_CATALOG_ITEM_POLICY = REJECT
PEDIMENT_CONFIRM_PERMISSION = PEDIMENTOS_CONFIRMAR
CONFIRMATION_FAILURE_MODEL = ROLLBACK_TO_PREVIOUS_STATE
CONFIRMED_STATE_TERMINAL = YES
LEGACY_FISCAL_FIELD_POLICY = PRESERVE_INPUT
```

- `REJECT`: `PED-003` exige `MATERIAL` y `PED-004` exige `PRODUCTO`; existen flujos
  separados de importación de catálogos. No se copia la auto-creación implícita del
  legacy durante la confirmación.
- `CONFIRMADA` es terminal V1: no hay desconfirmar/revertir/eliminar. Si la
  transacción falla: rollback total, la carga conserva su estado previo, la API
  devuelve error y la bitácora registra el intento fallido. No se crea un estado
  `FALLIDA` por una excepción.
- `PRESERVE_INPUT`: `IGIE/IVA/DTA/PREV/TIPOTASAIGIE` se capturan, validan
  tipo/formato, preservan y **no** se recalculan ni transforman fiscalmente.

## 24. Cierre V3 — staging V2

`version_plantilla` pasa de `LEGACY-STAGE-DERIVED-V1` a `LEGACY-STAGE-DERIVED-V2`.

Campos añadidos a `CAMPOS_CONFIRMADOS` (nombres físicos legacy preservados para
mantener el mapping exacto; el staging guarda `datos_json`, por lo que **no**
requiere `ALTER TABLE`):

| Campo | Tipo moderno | Regla |
|---|---|---|
| `IGIE` | `BigDecimal` | decimal nullable; blank/celda vacía → `null` |
| `IVA` | `BigDecimal` | decimal nullable; blank/celda vacía → `null` |
| `DTA` | `BigDecimal` | decimal nullable; blank/celda vacía → `null` |
| `PREV` | `BigDecimal` | decimal nullable; blank/celda vacía → `null` |
| `TIPOTASAIGIE` | texto | trim; blank/celda vacía → `null` |

Contrato de valor nulo: celda vacía, propiedad ausente en el JSON y `null`
explícito se interpretan todos como `null`; `"0"` se conserva como
`BigDecimal.ZERO`. Nunca `blank → ""` ni `blank → 0`.

```text
FISCAL_BLANK_VALUE_CONTRACT = NULL
```

```text
STAGING_V1_BACKWARD_COMPATIBILITY = PASS (JSON sin columnas físicas; cargas V1 siguen legibles)
FISCAL_FIELD_MAPPING = CONFIRMED
```

Mapping exacto (línea por línea de `CARGAPEDIMENTOS`):

| Origen | Destino | Acción legacy |
|---|---|---|
| `IGIE` | `IMPORTACIONES.ADVALOREM` | `SUM(IGIE)` por pedimento |
| `IGIE` | `PARTIDAS.MONTOIGI` | valor directo por partida |
| `IVA` | `IMPORTACIONES.IVA` | `SUM(IVA)` por pedimento |
| `IVA` | `PARTIDAS.MONTOIVA` | valor directo por partida |
| `IVA` | `PSALIDAS.montoiva` | valor directo por partida de salida |
| `PREV` | `IMPORTACIONES.PREVALIDACION` | `MAX(PREV)` por pedimento |
| `PREV` | `SALIDAS.PREV` | `MAX(PREV)` por pedimento |
| `DTA` | `IMPORTACIONES.DTA` | `MAX(DTA)` por pedimento |
| `DTA` | `SALIDAS.DTA` | `MAX(DTA)` por pedimento |
| `TIPOTASAIGIE` | `PARTIDAS.tipotasaigie` | valor directo |

Tipos destino: `IMPORTACIONES.IVA/DTA/PREVALIDACION/ADVALOREM` `float`;
`PARTIDAS.MONTOIGI/MONTOIVA` `float`; `PARTIDAS.tipotasaigie` heredado; se conserva
el valor sin recomputar (no se define escala que trunque).

```text
IMPORT_FIELD_COVERAGE = COMPLETE (61 / 61)
EXPORT_FIELD_COVERAGE = COMPLETE (45 / 45)
```

Los 7 faltantes de import y 3 de export pasan a `STORED`/`DERIVED`: no queda ningún
campo `MISSING_FROM_STAGING` ni `UNKNOWN`.

## 25. Cierre V3 — identidad operacional

```text
LEGACY_DUPLICATE_IDENTITY_CONTRACT = CONFIRMED
MODERN_OPERATIONAL_IDENTITY = IMPORT: NumeroPedimento / EXPORT: NumeroPedimento
MODERN_OPERATIONAL_IDENTITY_CONTRACT = PARTIAL
IDEMPOTENCY_TWO_PHASE_GUARD = DESIGNED
```

Es identidad operacional del sistema V1; **no** se afirma unicidad fiscal universal.
Guardia de dos fases: PRECHECK en validación (`PED-007`) + TRANSACTIONAL_RECHECK
(`EXISTS ... WITH (UPDLOCK, HOLDLOCK)`) dentro de la transacción, antes de insertar.

## 26. Cierre V3 — generación de claves (con prueba)

El legacy usa `MAX(key)+1` + `ROW_NUMBER()` + `GENERADORES` (espejo sin PK).
`sp_getapplock` no protege frente al legacy, que no solicita el mismo applock.

```text
legacy strategy = MAX_PLUS_ONE_WITH_ROW_NUMBER
new strategy = TABLE_LOCKED_MAX_PLUS_ONE (TABLOCKX + HOLDLOCK)
KEY_ALLOCATION_STRATEGY_V1 = TABLE_LOCKED_MAX_PLUS_ONE
KEY_ALLOCATION_COMPATIBLE_WITH_LEGACY = CONFIRMED_FOR_V1
```

Conjuntos de bloqueo (tablas con clave `MAX+1`):

```text
IMPORT_LOCK_SET = IMPORTACIONES, PARTIDAS
EXPORT_LOCK_SET = SALIDAS, PSALIDAS, DIRIGIDO
LOCK_ORDER = IMPORTACIONES, PARTIDAS, SALIDAS, PSALIDAS, DIRIGIDO
```

Prueba aislada real (tabla temporal global en `tempdb`, sin tocar tablas de
negocio; dos conexiones concurrentes):

```text
SIN_LOCK_COLISION = true            (MAX+1 concurrente colisiona)
CON_LOCK_BLOQUEO_LEGACY = true      (sesión legacy bloqueada durante el lock)
CON_LOCK_CLAVES_DISTINTAS = true    (sin duplicación)
CON_LOCK_FILAS = 2
```

Trade-off: menor concurrencia a cambio de compatibilidad con el esquema legacy sin
cambiar PK; aceptable porque confirmar un archivo no es operación de alta
frecuencia. Prueba reproducible en CI vía Testcontainers SQL Server
(`PedimentKeyAllocationConcurrencyTest`).

## 27. Cierre V3 — ubicación del command

```text
AUTHORITATIVE_COMMAND_DATABASE = CALE_IMMEX.dbo.APP24_C_PEDIMENTO_CONFIRMAR
COMMAND_CREATED = YES (implementado, probado en CI 15/15 y desplegado LIVE)
COMMAND_REPO_LIVE_MATCH = PASS
```

Razón: los datos autoritativos viven en `CALE_IMMEX`; el command genera claves y
bloquea tablas de `CALE_IMMEX`; y actualiza `ANEXO24_DEV.app24` por nombre de 3
partes. Ambas bases están en la misma instancia → una sola transacción local, sin DTC.

## 28. Cierre V3 — transacción, fallo y seguridad runtime

```text
DATABASES_SAME_INSTANCE = YES
CROSS_DB_ATOMIC_COMMAND_FEASIBLE = YES
failure model = ROLLBACK_TO_PREVIOUS_STATE
CURRENT_RUNTIME_LOGIN = opdatos
CURRENT_RUNTIME_PERMISSION = SUFFICIENT
TARGET_LEAST_PRIVILEGE_LOGIN = anexo24_app
TARGET_RUNTIME_SECURITY_MODEL = MODULE_EXECUTE_LEAST_PRIVILEGE
```

Patrón obligatorio: `SET XACT_ABORT ON` + `BEGIN TRAN` + `TRY/CATCH` con
`ROLLBACK`; cero partial writes. No se usa `TRUSTWORTHY`. El hardening futuro de
`anexo24_app` (EXECUTE-only por módulo/firma) es objetivo, no requisito de esta fase.
