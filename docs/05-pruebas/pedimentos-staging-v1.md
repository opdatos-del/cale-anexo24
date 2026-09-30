# Pruebas de staging de pedimentos V1

## 1. Decisión de alcance

La entrega implementa una superficie segura de:

```text
archivo → hash → parser → validación → staging durable → errores → preview
```

La confirmación operativa permanece bloqueada/no implementada. No se ejecutan
procedimientos legacy mutables ni se escriben `CALE_IMMEX`,
`CARGAPEDIMENTOSIE`, `ERRORCARGA`, `IMPORTACIONES`, `PARTIDAS` o inventario.

Estados de decisión:

- `PEDIMENT_LEGACY_CONTRACT = PARTIAL`;
- `PEDIMENT_LAYOUT_CONTRACT = PARTIAL`;
- `PEDIMENT_STAGING_V1 = PASS` después de compilar, probar y aplicar la
  migración de aplicación;
- `PEDIMENT_CONFIRMATION = NOT_IMPLEMENTED`.

## 2. Contrato legacy observado

La auditoría read-only en `CALE_IMMEX` obtuvo:

| Objeto | Resultado |
|---|---|
| `dbo.CargaPedimentosIE` | 2 filas observadas, 62 columnas, PK `CargaKey`, sin FKs ni índices secundarios observados |
| `dbo.ERRORCARGA` | 0 filas, 3 columnas, PK `errorkey`, sin usuario/lote/archivo observado |
| `CARGAPEDIMENTOS` | `MIXED`: SELECT/INSERT/UPDATE/DELETE y cursor |
| `CARGA_ENCABEZADOS` | `MIXED`: SELECT/INSERT/UPDATE |
| `INSERTAPEDIMENTO` | `MIXED`: SELECT/INSERT/UPDATE/DELETE/EXEC y cursor |
| `VALIDAPEDIMENTO` | `MIXED`: SELECT/DELETE/EXEC y cursor |
| `VALIDA_I_DETALLENP` | `MIXED`: SELECT/INSERT/UPDATE/DELETE y cursor |

No se ejecutó ninguno de esos procedimientos. La documentación de
`CARGAPEDIMENTOS` confirma efectos sobre `IMPORTACIONES`, `PARTIDAS`, `SALIDAS`,
`PSALIDAS`, `DIRIGIDO`, `GENERADORES`, materiales y productos; por eso no se
reutilizó como paso de preview.

No se encontró un layout oficial descargable o versionado. El contrato de
entrada se deriva de `CargaPedimentosIE`:

```text
PEDIMENT_LAYOUT_SOURCE = LEGACY_STAGE_DERIVED
PEDIMENT_LAYOUT_CONTRACT = PARTIAL
```

El mapping completo, incluyendo tipos LIVE, campos técnicos, obligatoriedad V1 y
campos todavía parciales, está en
[`docs/03-diseno/mapeo-carga-pedimentos.md`](../03-diseno/mapeo-carga-pedimentos.md).

## 3. Staging nuevo

La migración aplicada es:

```text
infra/sql/migrations/09-pedimentos-staging-v1.sql
```

Resultado LIVE en `ANEXO24_DEV`:

```text
MIGRATION = PASS
app24.CargaPedimento = presente una vez, 0 filas
app24.CargaPedimentoFila = presente una vez, 0 filas
app24.ErrorCargaPedimento = presente una vez, 0 filas
procedimientos = 4
```

El staging está aislado por `carga_id` y no depende de las tablas globales legacy.
El hash SHA-256 es único para impedir duplicados silenciosos. Las filas se
persisten como JSON validado; los errores no almacenan el valor original y usan
`valorEnmascarado = no almacenado`.

Procedimientos versionados:

- `APP24_C_PEDIMENTO_CARGA_CREAR`;
- `APP24_Q_PEDIMENTO_CARGA_POR_HASH`;
- `APP24_Q_PEDIMENTO_CARGA_OBTENER`;
- `APP24_Q_PEDIMENTO_CARGA_ERRORES`.

El adapter Java invoca exclusivamente esos cuatro procedimientos. No contiene
SQL de negocio inline. El command usa transacción SQL, `XACT_ABORT` y rollback en
caso de error; no tiene endpoint de confirmación.

Estados persistibles V1:

- `PREVISUALIZADA`;
- `CON_ERRORES`.

El command registra, dentro de la misma transacción, las acciones controladas
`CARGA_PEDIMENTO_VALIDADA` y `CARGA_PEDIMENTO_CON_ERRORES` mediante el SP de
bitácora existente. No se agregó `CONFIRMADA`.

## 4. API y seguridad

Endpoints implementados bajo `/api/v1/operaciones/pedimentos`:

| Método | Endpoint | Uso |
|---|---|---|
| `POST` | `/cargas` | recibir, validar y guardar staging |
| `GET` | `/cargas/{id}` | consultar metadata y preview paginado |
| `GET` | `/cargas/{id}/errores` | consultar errores paginados |

No existe `/confirmar`.

Permiso exacto:

```text
PEDIMENTOS_CARGAR
```

La migración crea el permiso de forma idempotente y lo asigna a
`ADMINISTRADOR` sólo si la asignación no existe. Resultado LIVE:

```text
permission rows = 1
ADMINISTRADOR mappings = 1
duplicates = 0
```

Pruebas de autorización:

- sin autenticación → `401`;
- autenticado sin `PEDIMENTOS_CARGAR` → `403`;
- autoridad `PEDIMENTOS_CARGAR` → controller autorizado.

No había una credencial autorizada disponible para una ejecución HTTP autenticada
contra el entorno, por lo que el smoke autenticado queda:

```text
AUTHENTICATED_RUNTIME_SMOKE = NOT_EXECUTED
```

Esto no convierte el staging en fallo: la seguridad está cubierta por tests
MockMvc, y no se fabricó ningún JWT.

## 5. Parser y validación

El parser Apache POI admite `.xls` y `.xlsx` cuando la firma binaria coincide con
la extensión. Los límites son:

- 10 MiB por archivo;
- 2.000 filas;
- 80 columnas;
- 100.000 celdas;
- una hoja.

Se validan encabezados, duplicados, columnas desconocidas, fórmulas, filas vacías,
campos requeridos, fechas, enteros, decimales, duplicados de fila y valores
permitidos de `TipoOperacion` documentados por la auditoría. Los valores
numéricos se validan con `BigDecimal` y se serializan de forma canónica; no se
usa `float` en Java.

Los mensajes de error muestran fila, columna, código y regla sin incluir valores
de negocio completos ni detalles internos de SQL.

## 6. Resultados de pruebas

### Backend

```text
./gradlew.bat clean test
PASS
425 tests
425 passed
0 failed
0 skipped
```

La cobertura específica incluye:

- parser XLSX sintético válido;
- normalización de decimal y fecha;
- encabezado faltante;
- fecha/cantidad inválidas;
- valores de error enmascarados;
- rechazo de hash duplicado;
- persistencia de hash nuevo;
- mapper de `CARGA_PEDIMENTO_VALIDADA`;
- mapper de `CARGA_PEDIMENTO_CON_ERRORES`;
- correlación del handler de upload;
- `401`, `403` y autorización con `PEDIMENTOS_CARGAR`.

```text
./gradlew.bat build
PASS
```

### Frontend

La suite existente de la feature pasó:

```text
pnpm exec ng test --watch=false
87 passed
25 test files
pnpm lint
PASS
pnpm build
PASS
parent-relative imports = 0
```

La UI implementa `/operaciones/pedimentos` con selección de archivo, validación
previa, upload, totales, preview, errores, retry/limpiar y tabla responsive. El
mensaje de confirmación es informativo y explícito:

> Previsualización validada. La confirmación operativa todavía no está habilitada.

No se agrega botón ni ruta de confirmación.

Los warnings de presupuesto de bundle Angular y SCSS del sidebar son warnings
preexistentes del proyecto; no alteran el resultado de los gates ejecutados.

## 7. SQL y efectos

| Operación | Resultado |
|---|---:|
| Escrituras de datos en `CALE_IMMEX` | 0 |
| DDL de tablas legacy en `CALE_IMMEX` | 0 |
| DDL de SP read-only en `CALE_IMMEX` | 0 en esta feature |
| Ejecuciones de SP legacy mutables | 0 |
| DDL de aplicación en `ANEXO24_DEV` | migración 09 aplicada |
| DML de aplicación por runtime | 0 registros de carga; la migración sólo sembró permiso/asignación idempotente |
| SQL de negocio inline en Java | 0 |

La migración crea cuatro SP de aplicación y tres tablas de staging. El command
LIVE se ejecutó con payload sintético dentro de una transacción exterior: los
queries por hash, detalle y errores devolvieron resultados coherentes. El evento
sintético fue `OPERACIONES / CARGA_PEDIMENTO_CON_ERRORES / FALLO`. Después del
rollback quedaron 0 cargas, 0 filas, 0 errores y 0 eventos. La prueba adicional
de hash duplicado produjo conflicto y también dejó 0 datos persistentes.

`STRUCTURAL_MATCH = PASS` entre los cuatro SP versionados y sus objetos LIVE.
`PEDIMENT_COMMAND_SP_TRANSACTION_SAFE = YES`: sólo escribe `app24`, no referencia
`CALE_IMMEX` ni SP legacy, y el rollback exterior revierte el commit anidado.

## 8. Runtime y límites pendientes

No se ejecutó el smoke HTTP autenticado de upload porque no había credencial
autorizada disponible. El smoke SQL sintético sí se ejecutó y terminó con
rollback; no se ejecutó el pipeline legacy ni se dejaron registros persistentes.

Queda pendiente para una siguiente ejecución autorizada:

- upload de un XLSX sintético;
- consulta de preview mediante API;
- recuperación después de reinicio;
- validaciones de catálogo contra SP read-only de `CALE_IMMEX`, sólo si el
  mapping funcional las confirma;
- decisión de negocio sobre layout oficial y reglas de confirmación.

## 9. Paridad

En el cierre/publicación de esta feature, `LEGACY-016` cambia de `MISSING` a
`PARTIAL`: upload, validación estructural, errores, preview, hash, RBAC y staging
durable están implementados. `LEGACY-017` permanece `MISSING` porque las reglas
autoritativas de validación legacy antes del proceso operativo aún no están
cerradas. No debe marcarse `IMPLEMENTED_REDESIGNED` mientras la confirmación
operativa que se decida conservar siga sin contrato.

## 10. Decisión

```text
PEDIMENT_LEGACY_CONTRACT = PARTIAL
PEDIMENT_LAYOUT_CONTRACT = PARTIAL
PEDIMENT_STAGING_V1 = PASS
PEDIMENT_COMMAND_SP_TRANSACTION_SAFE = YES
PEDIMENT_AUDITLOG_ENUM_COMPATIBILITY = PASS
SP_VERSIONED_LIVE_MATCH = PASS
DUPLICATE_HASH_BEHAVIOR = PASS
PEDIMENT_STAGING_TRANSACTIONAL_SMOKE = PASS
PERSISTENT_TEST_DATA = 0
PEDIMENT_CONFIRMATION = NOT_IMPLEMENTED
AUTHENTICATED_RUNTIME_SMOKE = NOT_EXECUTED
```
