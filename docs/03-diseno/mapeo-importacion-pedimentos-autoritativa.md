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

Clasificación pendiente de estas dependencias ausentes (no se concluye "legacy
roto"):

| Objeto | Estado LIVE | Clasificación pendiente |
|---|---|---|
| `dbo.PEDIMENTOS` | ABSENT | ACTIVE_DEPENDENCY / DEAD_LEGACY_BRANCH / EXTERNAL_DATABASE_DEPENDENCY / MISSING_DEPLOYMENT_OBJECT / UNKNOWN |
| `dbo.NP` | ABSENT | idem |
| `dbo.INVENTARIO` | ABSENT | idem |

La clasificación se cierra en la Parte B de esta fase (auditoría de synonyms,
referencias de 3/4 partes y linked servers).

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
