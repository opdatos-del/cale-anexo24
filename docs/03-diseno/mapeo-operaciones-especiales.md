# Mapeo de Operaciones Especiales V1

## Alcance de la auditoría

Esta primera pasada revisa Actas de destrucción, Constancias, Transferencias de
Submaquila y CTM usando la evidencia documental y auditorías read-only existentes.
No se ejecutaron procedimientos legacy mutables, no se hicieron escrituras en
`CALE_IMMEX` y no se agregaron tablas, endpoints, permisos ni navegación nueva.

| Capacidad | Legacy objects | Contract | New scope | State |
|---|---|---|---|---|
| Actas de destrucción | `CARGAACTAS`, `ACTA`, `ERRORACTA`, `DescargaDesp` y variantes | `UNKNOWN` | Ninguno | Discovery |
| Constancias | `CARGACONSTANCIAS`, `CONSTANCIATRANSF`, `ERRORCARGA`, `productos`, `settings` | `UNKNOWN` | Ninguno | Discovery |
| Transferencias de Submaquila | `CARGA_SUBMAQUILA`, `TMPSUBMAQUILA`, `RelacionSubmaquila`, `Encabezadotransubmaquila`, `Detalletransubmaquila` | `UNKNOWN` | Ninguno | Discovery |
| CTM | `CTMDESCARGA`, `SALDOSCTM`, `LIGACTMA`, `LIGACTMFACTURA`, `V_F4CTMA`, `V_F4DESP` | `PARTIAL` | Ninguno; vistas sólo como referencias | Discovery |

```text
ACTAS_CONTRACT = UNKNOWN
CONSTANCIAS_CONTRACT = UNKNOWN
SUBMAQUILA_CONTRACT = UNKNOWN
CTM_CONTRACT = PARTIAL
SPECIAL_OPERATIONS_DISCOVERY = COMPLETE
```

`PARTIAL` para CTM significa que existen objetos de lectura relacionados, no que
exista un contrato API aprobado ni una capacidad implementada.

## Actas de destrucción

### Objetos y clasificación

| Objeto | Clasificación | Evidencia | Decisión |
|---|---|---|---|
| `CARGAACTAS` | `MIXED` / `PROCESS` | La definición documentada borra `ERRORACTA` y procesa `ACTA` | No ejecutar |
| `ACTA` | Destino/proceso | Tabla referida por `CARGAACTAS`; columnas, ciclo de vida y relación de carga no cerrados en el contrato versionado | No usar como layout automáticamente |
| `ERRORACTA` | Errores legacy | Referida por el proceso; localización por archivo/hoja/fila/código no demostrada | No reutilizar |
| `DescargaDesp` y variantes | Efectos de desperdicio/descargo | Procesos y reportes relacionados; no fuente canónica de staging | Excluidos |

No se confirmó un stage aislado, layout oficial o layout inequívocamente derivado.
Tampoco se cerraron reglas de validación, aceptación, duplicados o efectos
permitidos para una V1 de preview.

No se implementan `ACT-001` ni reglas equivalentes porque no hay condiciones
read-only completas con campo, mensaje y contrato reproducible.

## Constancias

### Objetos y clasificación

| Objeto | Clasificación | Evidencia | Decisión |
|---|---|---|---|
| `CARGACONSTANCIAS` | `WRITE` / `IMPORT/PROCESS` | Borra errores y usa `CONSTANCIATRANSF`, productos y settings | No ejecutar |
| `CONSTANCIATRANSF` | Stage/destino referido | No hay layout, aislamiento por carga ni ciclo de preview cerrado | No reutilizar |
| `ERRORCARGA` | Errores compartidos legacy | No demuestra identidad por archivo/lote ni contrato de error por celda | No reutilizar |
| `productos` | Maestro afectado | El proceso puede crear/validar productos | No escribir |
| `settings` | Configuración afectada | Dependencia mutable documentada | No tocar |

El pipeline mezcla carga, validación y procesamiento. No hay evidencia suficiente
para separar una capacidad coherente de upload/parse/validate/preview.

## Transferencias de Submaquila

### Objetos y clasificación

| Objeto | Clasificación | Evidencia | Decisión |
|---|---|---|---|
| `CARGA_SUBMAQUILA` | `MIXED` / `PROCESS` | Proceso de carga y validación desde stage; usa productos, salidas y partidas según auditoría | No ejecutar |
| `TMPSUBMAQUILA` | Stage global | Existente y observado vacío; sin aislamiento por usuario, lote, archivo o hash | No reutilizar |
| `RelacionSubmaquila` | Relación/proceso | No demuestra maestro canónico ni contrato de importación | No implementar |
| `Encabezadotransubmaquila` | Cabecera de transferencia | No se cerraron columnas, identidad ni reglas | No implementar |
| `Detalletransubmaquila` | Detalle de transferencia | No se cerraron columnas, relación ni reglas fiscales | No implementar |

La evidencia disponible describe una transferencia/proceso, no un simple catálogo
de submaquiladores. La dependencia con CTM y descargos permanece sin contrato
operativo autorizado.

## CTM

### Objetos y clasificación

| Objeto | Clasificación | Evidencia | Decisión |
|---|---|---|---|
| `CTMDESCARGA` | Proceso/flujo especializado | Relacionado con CTM y descargos | No ejecutar |
| `SALDOSCTM` | `CALCULATION` / `WRITE` | Actualiza descargas, saldos y trazo CTM | Excluido |
| `LIGACTMA` | `PROCESS` / `WRITE` | Vincula datos CTMA y afecta flujo especializado | No ejecutar |
| `LIGACTMFACTURA` | `PROCESS` / `WRITE` | Vincula facturas/desperdicios con descargas | No ejecutar |
| `V_F4CTMA` | `READ ONLY` / referencia | Vista especializada F4/CTM sin contrato V1 aprobado | No exponer |
| `V_F4DESP` | `READ ONLY` / referencia | Vista especializada desperdicio/F4 sin contrato V1 aprobado | No exponer |
| `DESCARGA_CTMA` | `READ ONLY` / referencia | Vista especializada relacionada, no fuente canónica V1 | No exponer |

Existe una posible capacidad de consulta read-only, pero no están cerrados la
fuente canónica, granularidad, filtros, campos, semántica de CTM ni aceptación.
Por ello no se crea un endpoint GET ni se cambia `LEGACY-028` o `LEGACY-063`.

## Separación de staging y procesamiento

La auditoría confirma que los pipelines especiales mezclan operaciones. Una futura
V1 sólo podría implementar, después de cerrar contrato:

```text
upload → parse → validate → preview → errors
```

sin ejecutar:

- `CARGAACTAS`;
- `CARGACONSTANCIAS`;
- `CARGA_SUBMAQUILA`;
- `CTMDESCARGA`;
- `SALDOS*`;
- `DESCARGATSALIDA*`;
- `DESCARGASALIDAPEPS`;
- `DESCDIRIGIDA`;
- `LIGACTMA`;
- `LIGACTMFACTURA`.

No se construye un `GenericImportEngine`.

## Reglas

No se registran reglas `ACT-XXX`, `CON-XXX`, `SUB-XXX` o `CTM-XXX` como
`CONFIRMED` en esta pasada. Los procedimientos observados no proporcionan un
contrato aislado de condición, campo, mensaje y efecto que pueda trasladarse de
forma segura a una validación read-only.

## API, UI y permisos

```text
API = NOT_IMPLEMENTED
UI = NOT_IMPLEMENTED
PERMISSIONS = UNCHANGED
```

No se agregan `/operaciones/especiales`, tabs, cards ni permisos de carga. El
sidebar no cambia.

## SQL y efectos

```text
legacy mutable SP executed = 0
CALE_IMMEX writes = 0
ANEXO24_DEV migrations = 0
inline Java SQL = 0
transactional smoke = NOT_APPLICABLE
```

Los conteos LIVE de tablas especiales no se vuelven a ejecutar en esta pasada;
se conservan únicamente las observaciones agregadas ya documentadas. No se
imprimieron filas de negocio.

## Paridad

No se modifican estados:

```text
LEGACY-025 = MISSING
LEGACY-026 = MISSING
LEGACY-027 = MISSING
LEGACY-028 = MISSING
LEGACY-060 = MISSING
LEGACY-063 = UNKNOWN
```

`SALDOS` y el motor de descargos permanecen fuera de alcance y no se convierten
en una dependencia implícita de Operaciones Especiales.

## Evidencia necesaria para reabrir cada capacidad

La ausencia de evidencia contractual mantiene una capacidad en `UNKNOWN`; no se
interpreta como una decisión de negocio bloqueada. Para reabrir una capacidad se
requiere, como mínimo, lo siguiente:

### Actas de destrucción

- layout y formato de entrada;
- campos y obligatoriedad;
- reglas de validación reproducibles;
- stage y contrato de errores aislados por carga;
- comportamiento ante duplicados;
- preview esperado;
- side effects permitidos y explícitamente separados del preview;
- caso de aceptación verificable.

### Constancias

- layout y formato de entrada;
- campos y obligatoriedad;
- reglas de validación reproducibles;
- stage y contrato de errores aislados por carga;
- comportamiento ante duplicados;
- preview esperado;
- side effects permitidos y explícitamente separados del preview;
- caso de aceptación verificable.

### Transferencias de Submaquila

- layout completo de cabecera y detalle;
- maestro o catálogo de submaquilador;
- relación con CTM;
- reglas fiscales y operativas;
- stage, errores, duplicados y caso de aceptación.

### CTM

- dataset read-only aprobado;
- significado de las columnas;
- granularidad de cada fila;
- filtros soportados;
- relación demostrada entre F4, HDE y CTM;
- caso de aceptación verificable.

Hasta reunir esta evidencia no se crean endpoints, permisos, tablas de staging ni
rutas para estas capacidades.

## Segunda pasada — Actas de destrucción (reauditoría LEGACY-060)

Reauditoría read-only focalizada para decidir si existe contrato suficiente para
una V1 de `upload → validación → staging → errores → preview`. Ningún objeto
mutable fue ejecutado.

```text
ACTAS_LAYOUT_CONTRACT = NOT_SUFFICIENT
ACTAS_FILE_SURFACE    = NONE
LEGACY-060            = MISSING (sin cambio)
```

### Respuestas del discovery (10 preguntas)

| Pregunta | Evidencia LIVE |
|---|---|
| ¿Qué archivo recibe el legacy? | Ninguno demostrado: no existe tabla de carga por archivo/hash/lote ni proceso de lectura de archivos; `dbo.Acta` es una tabla stage global |
| ¿Qué columnas tiene? | `dbo.Acta`: `actakey` (PK bigint), `Folio`, `fecha`, `clave`, `linea`, `cantidad`, `umc`, `descargadirigida`, `VALORCOMERCIAL` |
| ¿Obligatorias? | Ninguna declarada: todas las columnas de negocio son nullable; `CARGAACTAS` no valida campos |
| ¿Tipo/formato? | Tipos de tabla únicamente (varchar/datetime/int/float/numeric); sin formato de archivo, hoja ni encabezados demostrados |
| ¿Qué SP procesa? | `dbo.CARGAACTAS` (sin parámetros, `MIXED`/`WRITE`) |
| ¿Qué tablas toca? | Lee `Acta`; borra `generadores` (`TABLA='ACTA'`); inserta `salidas`, `psalidas`, `dirigido`; consulta `material` y `productos` como fallback de descripción |
| ¿Qué validaciones aplica? | Ninguna: no hay chequeos de existencia, formato, duplicados ni obligatoriedad |
| ¿Qué errores produce? | Ninguno: la limpieza `DELETE FROM ERRORACTA` está comentada y `ERRORACTA` no existe en LIVE |
| ¿Hay staging legacy? | Sí: `dbo.Acta` (stage global sin aislamiento por archivo, hash, usuario o lote) |
| ¿Grano de fila? | Folio + línea (`actakey` PK; `CARGAACTAS` resuelve `PSALIDAS` por `DOCUMENTO=@FOLIO` y `PARTIDA=@LINEA`) |

### Clasificación SP-FIRST

| Candidato | Tipo | Efecto | Clasificación | Decisión |
|---|---|---|---|---|
| `dbo.CARGAACTAS` | SP | WRITE/MIXED (cursor + INSERT en salidas/psalidas/dirigido, claves `MAX+1`, sin transacción) | `SP_EXISTING_NOT_REUSABLE` | No ejecutar; no sirve para preview |
| `dbo.Acta` | TABLE | stage global | `SP_EXISTING_NOT_REUSABLE` (como contrato de archivo) | No adoptar como layout de archivo; sólo evidencia del modelo de fila |
| `dbo.ERRORACTA` | — | ausente en LIVE | `FALSE_POSITIVE` | No reutilizar |
| `GENERADORES` (scratch `TABLA='ACTA'`) | TABLE | scratch global | `SP_EXISTING_NOT_REUSABLE` | Excluido |
| Nuevo SP | — | — | `NEW_SP_REQUIRED` = NOT_PROVEN | Sin layout de archivo ni contrato de errores no se define proyección |

### Conteos

```text
dbo.Acta  = 0 filas
ERRORACTA = no existe
```

### Evidencia faltante para reabrir LEGACY-060

- superficie real de entrada (¿archivo? ¿formato XLS/XLSX? ¿pantalla?): no demostrada;
- encabezados, orden y formato de columnas del archivo;
- obligatoriedad y reglas de validación reproducibles (el SP legacy no valida);
- contrato de errores por archivo/hoja/fila/columna (ausente en legacy);
- comportamiento ante duplicados (`Folio`+`línea` ya existentes: el SP omite silenciosamente);
- caso de aceptación verificable y semántica de `descargadirigida`/`umc`.

### Controles de la segunda pasada

```text
LIVE reads = SELECT/metadata
LIVE writes = 0
legacy mutable SP executed = 0
new SP = 0
new staging tables = 0
API/UI = NOT_IMPLEMENTED
```
