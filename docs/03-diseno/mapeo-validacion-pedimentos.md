# Mapeo de validación de pedimentos V1

## Alcance

La validación se ejecuta automáticamente durante el `POST /cargas` existente,
después de la validación estructural del Excel y antes de persistir el staging.
No se crea otra carga ni una pantalla separada.

```text
Excel → parser estructural → validación batch read-only → staging/error
```

La validación V1 no confirma `IMPORTACIONES`, `PARTIDAS`, `SALIDAS` ni inventario.
No ejecuta procedimientos legacy y no escribe datos en `CALE_IMMEX`.

## Inventario legacy

| Objeto | Clasificación | Evidencia | Decisión |
|---|---|---|---|
| `VALIDAPEDIMENTO` | MIXED | limpia `ERRORVALIDACION`, recorre pedimentos y llama `INSERTERROR` | No ejecutar; separar comprobaciones read-only |
| `VALIDA_I_DETALLENP` | MIXED | limpia `I_ERROR`, actualiza FME/valores y puede insertar `PARTIDAS`/`PSALIDAS` | No ejecutar |
| `CARGAPEDIMENTOS` | MIXED | inserta/actualiza materiales, productos, importaciones, partidas, salidas y dirigidos | No ejecutar |
| `INDICAOPERACION` | WRITE | actualiza `A31_ENTRADAS.OPERACION` | No ejecutar |
| `ERRORVALIDACION`, `I_ERROR` | tablas legacy globales | no están aisladas por usuario/lote | No escribir |

## Reglas

`CONFIRMED` significa que la condición aparece explícitamente en una definición
legacy y puede evaluarse sin el efecto mutable asociado. Los mensajes nuevos son
estables y no contienen valores completos de negocio.

| Rule ID | Campo | Condición | Fuente legacy | Nueva validación | Estado |
|---|---|---|---|---|---|
| PED-001 | `UnidadComercial` | `ISVALIDUNIT(unidad) = 0` | `VALIDAPEDIMENTO`, `CARGAPEDIMENTOS` | SP batch read-only | CONFIRMED |
| PED-002 | `UnidadTarifa` | `ISVALIDUNIT(unidad) = 0` | `VALIDAPEDIMENTO`, `CARGAPEDIMENTOS` | SP batch read-only | CONFIRMED |
| PED-003 | `Clave` | operación 1 requiere existencia en `MATERIAL` | `VALIDA_I_DETALLENP`, `CARGAPEDIMENTOS` | SP batch read-only | CONFIRMED |
| PED-004 | `Clave` | operación 2 requiere existencia en `PRODUCTOS` | `VALIDA_I_DETALLENP`, `CARGAPEDIMENTOS` | SP batch read-only | CONFIRMED |
| PED-005 | cantidades | suma de inventario vs pedimento con tolerancia 1 | `VALIDAPEDIMENTO` | No implementada: staging V1 no contiene inventario normalizado | PARTIAL |
| PED-006 | valor dólares | suma de inventario vs pedimento con tolerancia 3 | `VALIDAPEDIMENTO` | No implementada: staging V1 no contiene inventario normalizado | PARTIAL |
| PED-007 | duplicado operativo | pedimento ya existente en la operación destino | `CARGAPEDIMENTOS` | SP batch read-only (extensión de reglas) | IMPLEMENTED (read-only) |

Regla exacta de `PED-007` confirmada en `CARGAPEDIMENTOS` (ver
`mapeo-importacion-pedimentos-autoritativa.md`): la fila no se inserta y **no se
reporta error** cuando el documento ya existe.

- `TIPOOPERACION = 1`:
  `NUMEROPEDIMENTO NOT IN (SELECT NUMERO_PED FROM IMPORTACIONES WHERE NUMERO_PED IS NOT NULL)`.
- `TIPOOPERACION = 2`:
  `NUMEROPEDIMENTO NOT IN (SELECT DOCUMENTO FROM SALIDAS WHERE DOCUMENTO IS NOT NULL)`.

`PED_007_LEGACY_RULE = CONFIRMED` y ahora también `PED_007_IMPLEMENTED = YES`
dentro de `dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS` (lectura), emitiendo error
explícito en lugar de omitir la fila. La identidad de una sola columna replica la
regla legacy; `PED_007_MODERN_IDEMPOTENCY_KEY` sigue `UNKNOWN` para el diseño de la
futura confirmación (ver `mapeo-importacion-pedimentos-autoritativa.md`).

- `TipoOperacion = 1`: error si `EXISTS (SELECT 1 FROM dbo.IMPORTACIONES WHERE NUMERO_PED = @numero)`.
- `TipoOperacion = 2`: error si `EXISTS (SELECT 1 FROM dbo.SALIDAS WHERE DOCUMENTO = @numero)`.

Sin mutación; `NOT EXISTS`/`EXISTS` NULL-safe (no se copia el `NOT IN` legacy).
`PED-007` es validación **preventiva** de duplicado observado; **no** es garantía
de idempotencia transaccional de la futura confirmación.

## Implementación

- SP: `dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS` (incluye `PED-007` desde la fase de cierre V2).
- Entrada batch: XML temporal, no persistido.
- Fuente: `MATERIAL`, `PRODUCTOS` y `ISVALIDUNIT` en `CALE_IMMEX`.
- Transporte: XML porque el servidor LIVE no tiene `OPENJSON` disponible.
- Serialización: DOM/Transformer seguro; no se concatenan valores de negocio para
  formar XML y los caracteres especiales se escapan por el serializer.
- Java: `PedimentoReglasRepository` + `PedimentoReglasJdbcAdapter`.
- Integración: `ValidarPedimentoUseCase` dentro de `CargarPedimentosUseCase`.
- Errores: modelo existente `PedimentoError`, con fila, columna, código, mensaje y
  `valorEnmascarado = no almacenado`.
- Batch: una consulta para todas las filas; no existe una consulta por celda/fila.
- `TipoOperacion = 1` evalúa `PED-003`; `TipoOperacion = 2` evalúa `PED-004`.
  Otros valores no generan reglas de catálogo en este SP; el parser conserva su
  validación de valores permitidos.

La combinación de errores estructurales y reglas de negocio se deduplica por
hoja, fila, columna y código dentro del resultado de una carga. Esto no equivale
a una revalidación persistida: `REVALIDATION = NOT_IMPLEMENTED`.
El control de carga repetida es independiente y corresponde al hash SHA-256:
`DUPLICATE_UPLOAD_HASH_GUARD = PASS`.

## Contrato de `ISVALIDUNIT`

La validación consume `dbo.ISVALIDUNIT` únicamente como función de consulta. El
contrato observado es una entrada textual de unidad y un resultado entero/bit
interpretable como válido (`1`) o inválido (`0`); los valores nulos o vacíos no
se convierten en una regla de unidad porque el filtro del SP los excluye. La
función no se ejecuta con efectos DML ni modifica tablas:

```text
ISVALIDUNIT_READ_CONTRACT = PASS
PEDIMENT_XML_ESCAPING = PASS
BATCH_EMPTY = PASS
VALIDATION_ERROR_DEDUPLICATION = PASS
REVALIDATION = NOT_IMPLEMENTED
```

## Seguridad y efectos

```text
legacy mutable SP executed = 0
CALE_IMMEX data writes = 0
app24 staging writes = sólo mediante el flujo existente de carga
inline Java SQL = 0
```

La creación/reemplazo del SP versionado es DDL de despliegue, no una escritura de
datos de negocio.
