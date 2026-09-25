# Facturación

## 1. Objetivo y decisión de alcance

Esta auditoría determina qué significa **Facturación** en `CALE_IMMEX`, separa
la consulta o validación de la escritura real, y evalúa el diseño V1 ya
propuesto. No implementa API, frontend, esquema `app24`, procedimientos ni
cargas.

> **DECISIÓN V1 — contrato no cerrado.** Facturación V1 requiere validación
> funcional adicional antes de implementar `POST /api/v1/facturacion/cargas` o
> cualquier confirmación. Hay al menos dos flujos legacy de escritura con
> destinos, granularidad y riesgos distintos; no se demostró cuál debe cubrir
> la aplicación nueva.

## 2. Fuentes y calidad de evidencia

| Fuente | Aporta | Estado |
|---|---|---|
| Auditoría funcional web de 2026-09-03 | Pantalla con selección múltiple XLS/XLSX y acciones `CARGAR`, `GUARDAR`, `LIMPIAR`, `DESCARGAR`; plantilla sintética rechazada | **CONFIRMADO** para UX observada |
| Snapshots previos de metadata/definiciones legacy en `mapeo-salidas.md`, `mapeo-productos.md` y `procedimientos-almacenados.md` | Objetos, parámetros, lecturas/escrituras y algunos flujos estáticos | **CONFIRMADO** para evidencia de snapshot |
| `infra/sql/02-app-schema.sql` y `03-app-seed-security.sql` | Diseño DDL y permisos `app24` existentes | **CONFIRMADO** para diseño actual, no para legado |
| Prototipo, API y documentos de diseño | Intención V1 | **DISEÑO PROPUESTO**, no comportamiento legacy |
| SQL Server actual | Metadata, row counts, columnas completas, índices, FKs, definiciones vigentes y datos | **PENDIENTE DE REVALIDACIÓN**; TLS bloquea conexión confiable y no se aplicó bypass |

No se ejecutó SQL contra `CALE_IMMEX`, incluido `SELECT`, porque el acceso
actual falla validación de certificado antes de ejecutar consultas. No se usó
`TrustServerCertificate`, `-C` ni otra excepción TLS. Los conteos citados de
snapshots no representan una medición actual.

## 3. Definición funcional observada

**CONFIRMADO:** pantalla legacy llamada Facturación permite seleccionar varios
archivos `.xls`/`.xlsx` y presenta `CARGAR`, `GUARDAR`, `LIMPIAR` y
`DESCARGAR`.

**CONFIRMADO:** existen procesos legacy que cargan facturas y pueden crear
productos, clientes, facturas, salidas o líneas de salida.

**INFERIDO:** "Facturación" no es un único dominio homogéneo. Agrupa al menos
una carga que termina en `FACTURA` y otra que materializa líneas en
`PSALIDAS`; además existe un tercer flujo `TmpFC`/`FacturasCreadas` aún sin
semántica demostrada.

Por ello no debe modelarse aún como CRUD de `Factura` ni asumirse que todo
archivo representa una única factura comercial.

## 4. Inventario de objetos legacy

| Objeto | Rol observado | Lectura/escritura | Evidencia y estado |
|---|---|---|---|
| `TFACTURA` | Staging de `CARGA_FACTURAS` | **PENDIENTE** columnas/lifecycle actuales | Referido por definición snapshot |
| `TERRORFACTURA` | Errores de flujo `TFACTURA` | WRITE | Referido por definición snapshot |
| `FACTURA` | Destino final o histórico del flujo A | WRITE | Referido por definición snapshot; granularidad pendiente |
| `CARGAFACTURA` | Staging del flujo B | WRITE | Snapshot anterior: 0 filas |
| `ERRCARGAFACTURA` | Errores y mensaje de resultado del flujo B | WRITE | Snapshot anterior: 0 filas |
| `PRODUCTOS` | Catálogo validado/creado por ambos flujos | WRITE | Snapshot anterior: 204 filas |
| `CLIENTES` | Catálogo modificado por flujo A | WRITE | Referido por definición snapshot |
| `SALIDAS` | Encabezado lógico de exportación asociado a flujo B | WRITE indirecta / lectura | Snapshot anterior: 660 filas |
| `PSALIDAS` | Líneas de salida creadas por flujo B | WRITE | Snapshot anterior: 3,392 filas |
| `GENERADORES` | Generador/estado auxiliar limpiado por flujo B | WRITE | Referido por definición snapshot |
| `TmpFC` | Staging de flujo C | **PENDIENTE** | Referido por `INSTERTAFACTURASFC` |
| `TMPFCERROR` | Errores de flujo C | WRITE | Referido por `INSTERTAFACTURASFC` |
| `FacturasCreadas` | Destino o resultado de flujo C | WRITE | Referido por `INSTERTAFACTURASFC`; snapshot anterior: 0 filas |
| `FACTURASCTM` | Vínculo especializado CTM/factura | WRITE | Referido por `LIGACTMFACTURA` |
| `LIGACTMFACTURA` | Proceso de vinculación de facturas/desperdicios con descargas | WRITE | Clasificado por definición snapshot |

PK, identity, índices, FKs, columnas completas, tipos, row counts actuales,
fechas y relaciones físicas de estas tablas siguen **PENDIENTES DE
REVALIDACIÓN**. Las relaciones siguientes son lógicas, no FKs demostradas.

## 5. Flujos legacy encontrados

### 5.1 Flujo A: factura con staging `TFACTURA`

```text
TFACTURA
  └─ dbo.CARGA_FACTURAS (sin parámetros)
       ├─ TERRORFACTURA
       ├─ PRODUCTOS
       ├─ CLIENTES
       ├─ FACTURA
       └─ SALIDAS y detalles (referencia estática previa)
```

| Aspecto | Evidencia |
|---|---|
| Tipo | **CONFIRMADO:** `IMPORT / PROCESS / WRITE`. Nunca ejecutar como consulta o validación HTTP. |
| Parámetros | **CONFIRMADO:** ninguno en snapshot de `sys.parameters`. |
| Lecturas/escrituras | **CONFIRMADO:** referencia `PRODUCTOS`, `TFACTURA`, `TERRORFACTURA`, `CLIENTES`, `FACTURA`; documentación estática previa además registra modificación de salidas y detalles. |
| Productos/clientes | **CONFIRMADO:** crea productos faltantes y modifica clientes. |
| Cursores | **CONFIRMADO:** auditoría estática previa detectó cursor. |
| Alcance compartido | **INFERIDO:** al no recibir identificador de lote ni usuario, procesa staging no aislado; debe tratarse como riesgo de concurrencia hasta revisar definición vigente. |
| Validación, deletes, orden, transacción, rollback, parcialidad, duplicados e idempotencia | **PENDIENTE DE REVALIDACIÓN** con definición completa. |

No se demostró que `TFACTURA` sea temporal SQL, tabla de archivo, captura
manual o buffer persistente; sólo que participa como staging.

### 5.2 Flujo B: carga de líneas de salida

```text
CARGAFACTURA
  └─ dbo.CARGAFACTURASENPSALIDAS(@PEDIMENTO, @FACTURA)
       ├─ SALIDAS (busca SALIDAKEY por DOCUMENTO)
       ├─ PRODUCTOS (validación)
       ├─ ERRCARGAFACTURA (errores y éxito)
       ├─ PSALIDAS (inserta líneas)
       └─ GENERADORES (limpia)
```

| Aspecto | Evidencia |
|---|---|
| Tipo | **CONFIRMADO:** `IMPORT / PROCESS / WRITE`; no se ejecutó. |
| Parámetros | **CONFIRMADO:** `@PEDIMENTO varchar(50)`, `@FACTURA varchar(50)`. |
| Validaciones | **CONFIRMADO:** fracción, producto, unidad, cantidad y valores en `CARGAFACTURA`. |
| Errores | **CONFIRMADO:** borra `ERRCARGAFACTURA`, inserta errores y registra mensaje de éxito allí. |
| Materialización | **CONFIRMADO:** encuentra `SALIDAKEY` desde `SALIDAS.DOCUMENTO`; si no hay errores inserta filas en `PSALIDAS`. |
| Generación de identidad | **CONFIRMADO:** usa `MAX(PSALIDAKEY) + 1` y `ROW_NUMBER()`. Riesgo de carrera. |
| Limpieza | **CONFIRMADO:** borra `GENERADORES`. |
| Transacción/errores | **CONFIRMADO:** definición snapshot no mostró `BEGIN TRAN`, `TRY/CATCH`, `RAISERROR` ni `THROW`. No equivale a demostrar ausencia en versión actual. |
| Duplicados/idempotencia | **PENDIENTE:** no se demostró restricción ni regla de negocio. |

La fila natural del destino B es una **línea de salida** (`PSALIDAS`), no una
factura/encabezado: `PSALIDAS` tiene `Psalidakey`, producto, factura, fecha,
fracción, cantidad, unidad, valores, cliente y `Salidalink`. Esta conclusión
no demuestra que la fila de archivo sea idéntica a una línea `PSALIDAS`.

### 5.3 Flujo C: `TmpFC` / `FacturasCreadas`

```text
TmpFC → dbo.INSTERTAFACTURASFC (sin parámetros) → FacturasCreadas
                                      └──────────→ TMPFCERROR
```

**CONFIRMADO:** procedimiento `IMPORT / WRITE`, sin parámetros, con esas tres
tablas en snapshot de metadata.

**PENDIENTE:** propósito, tabla final, origen, formato, columnas, reglas,
relación con flujos A/B, transacción, idempotencia y uso vigente. No debe
mezclarse con la carga general hasta comprobarlos.

### 5.4 CTM

`LIGACTMFACTURA` se describe como vinculación de facturas/desperdicios con
descargas y usa `FACTURASCTM`. Es **CONFIRMADO** como proceso especializado
mutable. Su relación con CTM/cambio de régimen y su entrada/salida funcional
son **PENDIENTES**. Queda fuera de Facturación V1 general.

## 6. Diferencia entre `TFACTURA` y `CARGAFACTURA`

| Dimensión | `TFACTURA` | `CARGAFACTURA` |
|---|---|---|
| Procedimiento asociado | `CARGA_FACTURAS` | `CARGAFACTURASENPSALIDAS` |
| Parámetros del proceso | Ninguno | Pedimento y factura |
| Error asociado | `TERRORFACTURA` | `ERRCARGAFACTURA` |
| Destino demostrado | `FACTURA`, además productos/clientes y referencias a salidas/detalles | `PSALIDAS` para una `SALIDAS` localizada por documento |
| Grano de destino | **PENDIENTE** | Línea de salida |
| Estado | Staging/flujo independiente confirmado; semántica pendiente | Staging de carga a líneas de salida confirmado |

No son sinónimos ni se deben unificar por nombre.

## 7. Errores, granularidad e identidad

**CONFIRMADO:** ambos flujos principales usan tablas de error separadas:
`TERRORFACTURA` y `ERRCARGAFACTURA`. El flujo B borra su tabla de errores antes
de validar y también escribe un mensaje de éxito. Este diseño no prueba que
los errores sean aislados por archivo, usuario o lote.

**PENDIENTE:** columnas de error, localización por archivo/hoja/fila/columna,
códigos, valor original, ciclo de vida y retención. No se demostró que el
legado indique celda exacta.

**PENDIENTE:** identidad funcional de una factura, relación encabezado/detalle,
`FacturaKey`, número, línea, folio, cliente o combinación antidual. La API no
debe inventar `invoiceId` ni usar hash de archivo como identidad fiscal.

**INFERIDO:** flujo B recibe y materializa información a grano línea; el flujo
A y C requieren evidencia para definir grano.

## 8. Formato, plantilla y columnas

| Tema | Conclusión |
|---|---|
| XLS/XLSX y selección múltiple | **CONFIRMADO:** observado en UI legacy. |
| `.xls`/`.xlsx` como única extensión aceptada por negocio | **PENDIENTE:** UI no demuestra parser, MIME ni exclusiones técnicas. |
| Máximo de cinco archivos y 10 MB por archivo | **DECISIÓN PROPUESTA**, sólo aparece en wireframe; no proviene de legado demostrado. |
| Plantilla oficial descargable | **PENDIENTE:** botón `DESCARGAR` observado, contenido/versionado no observado. |
| Plantilla sintética | **CONFIRMADO:** fue rechazada por columnas/información; no revela headers correctos. |
| Archivo físico, hoja, encabezados, orden, tipos y catálogos | **PENDIENTE DE REVALIDACIÓN**. |

Los campos visibles como indicio de plantilla son `Documento`, `Fecha`,
`Almacén`, `Observaciones`, `Descarga`, `Tipo`, `Línea`, `Clave`, `Lote`,
`Cantidad`, `Unidad`, `Dirigido` y `Cliente`. Son **propuestos/observados en
UI**, no contrato de archivo confirmado. En especial, precisión de cantidad,
semántica de Descarga y catálogos no están cerradas.

## 9. Transacción, duplicados e idempotencia

- **PENDIENTE:** no se confirmó si los flujos legacy son todo-o-nada o aceptan
  filas parciales. El flujo B evita insertar `PSALIDAS` si detecta errores,
  pero no basta para concluir atomicidad total.
- **PENDIENTE:** no se confirmó deduplicación por factura, cliente, línea,
  pedimento, archivo o hash.
- **DECISIÓN V1 NO APROBADA:** SHA-256 es fingerprint técnico útil, pero no
  identidad fiscal. Un hash global único puede bloquear reintentos legítimos,
  archivos corregidos o cargas equivalentes con diferente contenido; su
  alcance requiere regla de negocio.
- **RIESGO CONFIRMADO:** flujo B deriva claves con `MAX + 1`; usuarios
  concurrentes pueden competir por el mismo rango si no existe protección no
  observada.
- **RIESGO INFERIDO:** `CARGA_FACTURAS` sin parámetros puede procesar staging
  compartido de más de un usuario.

## 10. Diseño `app24` actual y contradicciones

### 10.1 Estado actual de DDL

`app24.CargaFacturacion` es una tabla **persistente** por archivo: `id`,
`archivo`, `hash` único, `fecha`, `usuario_id`, `estado` y tres conteos. Tiene
PK `BIGINT IDENTITY`, FK a `UsuarioApp` y único global sobre `hash`.

`app24.ErrorCarga` persiste `carga_id`, `hoja`, `fila`, `columna`, `valor`,
`regla` y `mensaje`; tiene FK a carga e índice por `carga_id`.

`app24.ConfiguracionPlantilla` contiene `nombre`, `version`, `extension`,
`columnas_json` y `activa`, con único `(nombre, version)`.

### 10.2 Brechas observadas

| Tema | Estado |
|---|---|
| `POST /facturacion/cargas` dice "lote no persistido" en `api.md`, pero el DDL dispone carga persistente y confirmación por `{id}` | **CONTRADICCIÓN CONFIRMADA** de diseño |
| Lote con hasta cinco archivos, pero `CargaFacturacion` representa un solo archivo y no existe entidad padre de lote | **CONTRADICCIÓN CONFIRMADA** de diseño |
| `CargaFacturacion` no referencia `ConfiguracionPlantilla` ni conserva versión/correlación/fecha de validación/confirmación/confirmador/tipo | **CONFIRMADO** en DDL; necesidad funcional de cada campo, **PENDIENTE** |
| `ErrorCarga.valor` no obliga enmascaramiento | **CONFIRMADO** en DDL; debe protegerse antes de usar datos comerciales |
| `ConfiguracionPlantilla` no guarda MIME, hojas, headers, tipos, obligatoriedad o catálogos fuera de `columnas_json`; no tiene restricción de una activa | **CONFIRMADO** en DDL; suficiencia, **PENDIENTE** |
| Estados y conteos no tienen `CHECK` ni esquema de transición | **CONFIRMADO** en DDL |
| Hash único global como regla de idempotencia | **CONFIRMADO** en DDL; idoneidad, **PENDIENTE** |

No se modifica DDL en esta fase.

### 10.3 Estados y permisos

`EN_PROCESO`, `VALIDADA`, `GUARDADA` y `FALLIDA` pertenecen al prototipo;
`VALIDANDO`, `RECHAZADA`, `LISTA`, `GUARDADA` y `FALLIDA` a documentos de
diseño. Ningún conjunto es comportamiento legacy confirmado.

**PENDIENTE:** cerrar conjunto mínimo y transiciones sólo después de decidir
persistencia de validación. `FACTURACION_CARGAR` y `FACTURACION_GUARDAR` sí
existen en seed. Interpretación candidata, aún no aprobada:

- `FACTURACION_CARGAR`: recepción, parseo y validación sin persistir negocio;
- `FACTURACION_GUARDAR`: confirmación que escribe negocio.

No hay evidencia para crear `FACTURACION_VALIDAR`, `FACTURACION_HISTORIAL` ni
`FACTURACION_DESCARGAR`.

## 11. Validación de flujo V1 propuesto

| Regla del prototipo | Clasificación |
|---|---|
| Seleccionar XLS/XLSX y acciones cargar/guardar/limpiar/descargar | **CONFIRMADO** en UI, sin semántica técnica de cada botón |
| Validar antes de persistir negocio | **DECISIÓN V1** compatible con riesgo legacy; no probado como regla legacy |
| Preview y errores por archivo/hoja/fila/columna | **DECISIÓN V1**; el legado no lo demuestra |
| Extensión, tamaño, máximo 5, tipos, headers, catálogos y reglas | Extensión/múltiples: **CONFIRMADO** visualmente; demás **DECISIÓN V1/PENDIENTE** según plantilla |
| Todo-o-nada | **DECISIÓN V1**, no confirmado en legado |
| Plantilla versionada | **DECISIÓN V1**; plantilla oficial legacy pendiente |
| Historial, `correlationId`, bitácora | **DECISIÓN V1**; la bitácora legacy canónica sigue pendiente |
| SHA-256 para bloquear duplicado | **PENDIENTE** como regla; puede conservarse sólo como fingerprint técnico |

Separar parseo/validación de escritura de negocio es viable como objetivo de
arquitectura V1, pero no se decide aún si fase 1 no persiste, persiste sólo
metadata/errores, o persiste un draft. La contradicción DDL/API obliga elegir
esa política antes de implementar.

## 12. Source policy para escritura futura

No aplica regla “SP first” de consultas. Ningún procedimiento mutable legacy
se reutiliza directamente sin fase propia de diseño y pruebas.

Antes de elegir SP legacy, wrapper propio, transacción Java, staging propio o
combinación se requiere: definición vigente completa, mapa de side effects,
aislación por lote/usuario, transacción/rollback, protección de concurrencia,
regla de idempotencia, autorización, bitácora y pruebas controladas con datos
reversibles.

## 13. Riesgos

1. **Crítico:** cargas legacy pueden crear productos y clientes sin decisión
   explícita; V1 no debe realizar altas silenciosas.
2. **Crítico:** flujo B escribe `PSALIDAS`; una factura afecta inventario,
   salidas y posteriormente descargos/saldos.
3. **Alto:** staging/errores globales y procedimientos sin lote favorecen
   contaminación entre usuarios concurrentes.
4. **Alto:** `MAX(PSALIDAKEY) + 1` es vulnerable a carreras.
5. **Alto:** no hay transacción/rollback demostrados en la definición snapshot
   de flujo B.
6. **Alto:** fórmulas, macros, ZIP bombs, contenido MIME falso, límites de
   hojas/filas/columnas y nombres con path traversal requieren controles.
7. **Alto:** no evaluar fórmulas ni ejecutar macros; sanitizar nombre, verificar
   magic bytes/MIME, limitar recursos, procesar temporal controlado y borrar
   temporales.
8. **Medio:** no guardar archivos completos ni valores comerciales sensibles en
   logs, bitácora o `ErrorCarga.valor`; enmascarar antes de persistir.
9. **Medio:** hash SHA-256 no resuelve por sí solo identidad fiscal ni reintento.
10. **Medio:** CTM/F4 y flujo `TmpFC` son subdominios no comprendidos.

## 14. Contrato y endpoints candidatos

No se aprueba contrato HTTP en este corte. Se conservan únicamente como
**candidatos no implementables**:

```text
POST /api/v1/facturacion/cargas
POST /api/v1/facturacion/cargas/{id}/confirmar
GET  /api/v1/facturacion/cargas
GET  /api/v1/facturacion/cargas/{id}
GET  /api/v1/facturacion/plantilla
```

No se definen aún `multipart/form-data`, cantidad/tamaño, extensiones,
respuesta, `cargaId`, preview, errores, transiciones, idempotencia ni
retención. Esos elementos dependen de los pendientes siguientes.

## 15. Pendientes de cierre funcional/técnico

1. Recuperar acceso TLS confiable y revalidar metadata/definiciones vigentes
   en modo read-only.
2. Inventariar columnas, PK, FKs, índices, row counts y calidad de `TFACTURA`,
   `TERRORFACTURA`, `FACTURA`, `CARGAFACTURA`, `ERRCARGAFACTURA`, `TmpFC`,
   `TMPFCERROR`, `FacturasCreadas`, `FACTURASCTM`, `CLIENTES` y `PRODUCTOS`.
3. Obtener plantilla oficial, ejemplos anonimizados, formatos reales, headers,
   hojas, límites y reglas por columna.
4. Confirmar dueño funcional y propósito vigente de cada flujo; decidir si A,
   B, C y CTM se reemplazan, integran o excluyen.
5. Revisar definición completa de `CARGA_FACTURAS`,
   `CARGAFACTURASENPSALIDAS`, `CREAPRODUCTOSCARGAFACTURA`,
   `INSTERTAFACTURASFC` y `LIGACTMFACTURA` para DML, transacción, errores,
   duplicados, locking y dependencias.
6. Definir identidad fiscal, granularidad header/detalle y política de
   duplicados/reintentos.
7. Resolver política de persistencia de validación y corregir después la
   contradicción API/DDL de lote no persistido frente a `CargaFacturacion`.
8. Definir fuente canónica y retención de bitácora para confirmaciones.

## 16. Actualización de implementación V1 (2026-09-25)

Esta sección supersede las declaraciones de alcance/estado de implementación
anteriores; las secciones previas documentan auditoría y decisiones de diseño.

- `POST /api/v1/facturacion/cargas` está implementado para archivos `.xls` y
  `.xlsx`, con límites de 5 archivos, 10 MiB por archivo y 50 MiB por lote.
- La respuesta entrega preview, errores estructurados, fingerprint SHA-256 y
  estado de validación. La definición de columnas sigue provisional porque no
  existe plantilla activa verificada.
- `FACTURACION_CARGAR` está exigido en backend. No se implementó confirmación ni
  botón funcional de guardar; permanece `FACTURACION_CONFIRM_PENDING_LEGACY_CONTRACT`.
- Staging/errores se escriben mediante SP versionados en `ANEXO24_DEV`; evento
  de bitácora sólo contiene metadata de carga. No se guarda el workbook.
- Verificación LIVE posterior a una carga sintética: `DB_NAME() = ANEXO24_DEV`,
  `CargaFacturacion = 1`, `ErrorCarga = 0`, `ConfiguracionPlantilla = 0`,
  plantillas activas `= 0`, eventos de FACTURACION `= 1`. Se verificaron PK,
  índices únicos y FKs esperadas. La fila es evidencia sintética de staging;
  no eliminarla porque su evento asociado forma parte de bitácora inmutable.
- La carga sintética no escribió datos operativos: escrituras en `CALE_IMMEX`
  `= 0`; ningún procedimiento legacy mutable se ejecutó.
- Smoke HTTP: login `200`, upload XLSX sintético válido `200`, extensión
  inválida `400`, sin token `401`, token inválido `401`. Pruebas automatizadas
  cubren `401`, `403` para otra authority y acceso a validación con
  `FACTURACION_CARGAR`.
- UI `/facturacion` está implementada. No hay descarga de plantilla ni
  confirmación; se comunica que la plantilla V1 es provisional.
- Reglas de headers obligatorios, catálogos, formato oficial y semántica de
  negocio no se inventan: pendientes de validación funcional.

## 17. Restricciones cumplidas

- Implementación V1 limitada a carga, validación, preview y staging de aplicación.
- Cero cargas operativas, inserciones de factura, altas de producto/cliente o
  cambios a `PSALIDAS` en `CALE_IMMEX`.
- Cero procedimientos legacy ejecutados y cero cambios al esquema legacy.
- No se persisten credenciales, JWT ni archivos XLS/XLSX.
