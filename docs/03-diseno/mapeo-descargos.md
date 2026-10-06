# Descargos — ingeniería inversa y delimitación de alcance

## 0. Estado y evidencia

- **Fecha:** 2026-09-22.
- **Rama:** `feature/backend-discharges`.
- **Base:** `dev` en `8a53824e4abcf11eccd0292bb6dad9495e9d1407`.
- **Modo:** auditoría documental y SQL estrictamente read-only.
- **Datos / objetos legacy:** no modificados; ningún procedimiento ejecutado.
- **Estado:** **ALCANCE V1 DELIMITADO — SIN ENDPOINT GET DESCARGOS INDEPENDIENTE**.

### Convención

- **CONFIRMADO:** definición SQL, metadata o consulta read-only de auditorías previas documentadas.
- **INFERIDO:** conclusión técnica consistente, no demostrada por una ejecución funcional del proceso.
- **DECISIÓN V1:** límite deliberado de nuevo sistema.
- **PENDIENTE:** requiere acceso autorizado adicional, sesión legacy o decisión negocio.

### Límite de revalidación actual

**PENDIENTE:** intento actual de conexión read-only a `CALE_IMMEX` con SQLCMD e
Integrated Authentication no llegó a ejecutar SQL: SQL Server presentó cadena de
certificado no confiable. No se debilitó validación TLS, no se expusieron ni usaron
credenciales y no se ejecutó ningún procedimiento. Por tanto, conteos y metadata
abajo son evidencia del snapshot read-only documentado el 2026-09-21 en
[`mapeo-materiales-utilizados.md`](mapeo-materiales-utilizados.md), no una nueva
medición de 2026-09-22.

## 1. Objetivo

Definir qué significa **Descargos** en `CALE_IMMEX` y separar:

```text
Consulta histórica de asignaciones
≠ ejecución/reproceso de descargos
≠ recálculo de saldos
```

## 2. Definición funcional observada

**CONFIRMADO:** `dbo.DESCARGA` conserva asignaciones históricas entre una partida
de entrada, material y línea de salida. Sus cantidades separan incorporación,
merma y desperdicio.

**CONFIRMADO:** procedimientos `DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`,
`DESCARGAXFECHA51` y `SALDOS*` generan o recalculan esas asignaciones. Borran o
recrean datos, actualizan `PARTIDAS.Saldo` y escriben trazabilidad.

**DECISIÓN V1:** “Descargos” no equivale a un CRUD ni a un `GET` que active esos
procesos. La consulta histórica base está cubierta por Materiales Utilizados; el
análisis read-only enriquecido de relaciones importación → descarga → salida se
expone separadamente como capacidad parcial, sin recalcular saldos ni ejecutar el
motor.

## 3. Diferencia con Materiales Utilizados

| Pregunta | Evidencia / conclusión |
|---|---|
| Fuente histórica | **CONFIRMADO:** ambas consultas potenciales parten de `dbo.DESCARGA`. |
| Granularidad | **CONFIRMADO:** una fila física `DESCARGA.Descargakey`; asignación entrada → salida + material. |
| Snapshot conocido | **CONFIRMADO (2026-09-21):** 3,866 filas; 3,643 combinaciones distintas `(Pentradalink, Psalidalink, Clave)`; 223 duplicados históricos legítimos. |
| Encabezado / folio / lote de descargo | **PENDIENTE:** no encontrado en columnas de `DESCARGA` documentadas ni demostrado en tablas de historia. |
| Fecha propia de ejecución | **PENDIENTE / no encontrada:** `DESCARGA` no documenta fecha de proceso; `SALIDAS.FECHADESCARGA` fue nula en 660/660 filas del snapshot. |
| Status / vínculo G5-G6 / tipo fiscal superior | **PENDIENTE:** existen reportes y subsistemas especializados, no una entidad canónica demostrada para V1. |
| Valor nuevo para GET Descargos | **No demostrado.** Repetir `DESCARGA` duplicaría `GET /api/v1/operaciones/materiales-utilizados`. |
| Valor nuevo para análisis | **Parcialmente demostrado:** `V_INFORMEDESCARGAS` agrega relación de entrada/salida, fechas y campos de contexto sobre las mismas 3,866 asignaciones; no aporta faltantes/trazo ni contrato de saldos. |

**DECISIÓN V1:** no crear `GET /api/v1/operaciones/descargos` genérico. Materiales
Utilizados sigue siendo la consulta base de filas físicas; el análisis parcial
se expone en `GET /api/v1/reportes/analisis-descargas` con una proyección propia
estable por `DESCARGA.Descargakey`.

## 4. Tablas y relaciones

### 4.1 Mapa confirmado

```text
IMPORTACIONES.Ipedimentokey
  ← PARTIDAS.Importacionlink
  ← DESCARGA.Entradalink / Pentradalink (float legacy)

SALIDAS.SalidaKey
  ← PSALIDAS.Salidalink
  ← DESCARGA.Salidalink / Psalidalink (float legacy)

DESCARGA
  = asignación histórica persistida
```

**CONFIRMADO:** relaciones principales son lógicas; no se observaron FKs DDL
entre estas tablas en auditoría previa. Links `DESCARGA` son `float`; cualquier
join debe validar integralidad antes de convertirlos.

### 4.2 `dbo.DESCARGA`

**CONFIRMADO (snapshot 2026-09-21):**

- PK identity: `Descargakey bigint`.
- Índices: `IHD1(Entradalink, Pentradalink)`, `INDICE2_G5(Pentradalink)`,
  `op4(Psalidalink)` con `Descargakey` incluido.
- Sin FKs documentadas.
- No hay campos documentados de fecha de proceso, usuario, folio o status.
- Campos operativos relevantes:

```text
Descargakey,
Entradalink, Pentradalink, Salidalink, Psalidalink,
CantUtil, Merma, Desperdicio, CantUtilT, MermaT, DesperdicioT,
Unidad, UnidadT, Importacion, Salida, Clave, PT,
linea, orden, PROMKEY, tipoItem, SALDOCTMA
```

**CONFIRMADO:** `linea = 1`, `orden = 10` y `PROMKEY = NULL` en las 3,866 filas
conocidas; no forman identidad funcional. Identidad segura: `Descargakey`.

### 4.3 Calidad conocida

| Medición snapshot | Resultado |
|---|---:|
| `DESCARGA` | 3,866 filas |
| IDs | 1 a 3,866; 3,866 distintos |
| `CantUtil` | no nulo; 0.4234 a 14664.0892; sin negativos ni ceros |
| `Merma` | no nulo; 0 a 320.632; sin negativos |
| `Desperdicio` | 3,866 nulos |
| `TRAZO` relacionado | 0 filas / 3,866 descargas sin vínculo |
| `PARTIDAS`, `IMPORTACIONES`, `PSALIDAS`, `SALIDAS` sin vínculo a descarga | 0 en mediciones inversas documentadas |

**PENDIENTE:** repetir conteos, nulos, blancos, huérfanos y validación de links
`float` con login read-only/certificado confiable.

## 5. Fecha y cantidades

### 5.1 Fecha

**CONFIRMADO:** no se encontró fecha propia utilizable en `DESCARGA`.

| Candidato | Snapshot | Uso / decisión |
|---|---|---|
| `SALIDAS.Fecha` | 660/660; 2025-10-31 a 2026-08-18 | Fecha operacional de salida e histórico. Materiales Utilizados V1 la usa. |
| `PSALIDAS.Fecha` | 3,392/3,392; mismo rango | Fecha de línea/factura; no elegida como fecha principal. |
| `SALIDAS.FECHADESCARGA` | 0/660 | No utilizable. |
| fecha proceso en `DESCARGA` | no documentada | No demostrada. |

**DECISIÓN V1:** no se crea fecha “de descargo” artificial. La consulta
histórica existente conserva `SALIDAS.Fecha`.

### 5.2 Cantidades

**CONFIRMADO:** `SALDOS` separa e inserta `CantUtil`, `Merma` y `Desperdicio`;
actualiza `PARTIDAS.Saldo` e inserta `TRAZO`.

**DECISIÓN V1 existente:** Materiales Utilizados normaliza origen `float` a
`DECIMAL(18,4)` y conserva componentes nulos. No recalcula saldos en frontend ni
backend.

## 6. Trazo, dirigido, saldos e históricos

### 6.1 `TRAZO`

**CONFIRMADO:** tabla de trazabilidad/faltantes del proceso; vacía en snapshot.
Procesos de descargo la borran y rellenan. No es vínculo obligatorio para leer
histórico `DESCARGA`.

**DECISIÓN V1:** no exponer `TRAZO` como API de Descargos.

### 6.2 Descargo dirigido

**CONFIRMADO:** `DIRIGIDO` estaba vacío en snapshot. `DESCDIRIGIDA` recibe
`@SALIDAKEY`, `@PSALIDAKEY` y delega flujos dirigidos hacia cálculo de saldo.
`CARGAPEDIMENTOS` puede crear filas dirigidas.

**INFERIDO:** dirigido es subtipo de generación, no listado histórico
independiente. Queda fuera de V1.

### 6.3 Saldos

**CONFIRMADO:** `SALDOS` selecciona partidas descargables, inserta `DESCARGA`,
actualiza `PARTIDAS.Saldo` e inserta `TRAZO`. `PARTIDAS.Saldo` es resultado
mutable/cached del cálculo, no fuente inmutable de consulta.

**DECISIÓN V1:** Saldos será auditoría/módulo independiente; no agregar saldo a
Materiales Utilizados ni crear GET Descargos basado en saldo actual.

### 6.4 Históricos y respaldos

**CONFIRMADO:** nombres `HISTORIADESCARGAS*` no implican lectura segura:

- `HISTORIADESCARGAS` borra/inserta historia.
- `HISTORIADESCARGASF` y `HISTORIADESCARGASP` ejecutan `LIGADESPERDICIOS`.
- `PROC_HISTORIADESCARGASALIDA*` truncan e insertan historia de salida.
- `DESCARGAXFECHA51` referencia `descargaRespaldo` y `MensajeDescarga`.

**PENDIENTE:** determinar con definición SQL actual si alguna tabla histórica
preserva ejecución, usuario, versión o snapshot apto para consulta independiente.
No se ejecutó ningún proceso para poblarla.

## 7. F4, CTM, desperdicio y A31

| Área | Evidencia | Alcance V1 |
|---|---|---|
| Desperdicio | `DESCARGA_DESPERDICIO`, `LIGADESPERDICIOS`, `DescargaDesp` y variantes. Procesos mutables. | Excluido; valor histórico nullable ya visible en Materiales Utilizados. |
| CTM | `DESCARGA_CTMA`, `CTMDESCARGA`, `SALDOSCTM`, `GUARDADESCARGACTMA`, `LIGACTMA`. | Excluido; flujo especializado con saldos/trazos propios. |
| F4 | `V_F4CTMA`, `V_F4DESP`, `PROC_DESCARGASF4CTMA`. | Excluido; subtipo/regla especializada sin contrato común demostrado. |
| A31 | `A31_DESCARGAS`, `A31_SALDOS`, `A31_TRAZO`, `DESCARGAS_A31`, hojas de trabajo. | Excluido; subsistema y proceso separado. |

## 8. Procedimientos y clasificación

| Objeto | Tipo | Efecto | Evidencia / dependencia |
|---|---|---|---|
| `DESCARGATSALIDA` | PROCESS | WRITE / confirmado por familia | Despacha gestión de descargas; cuerpo actual pendiente de releer. |
| `DESCARGATSALIDA1` | PROCESS | WRITE | “BORRO EL TRAZO”; `SETTINGS`; borra/recrea descargas y saldos. |
| `DESCARGATSALIDAFECHA(@HASTA)` | PROCESS | WRITE | Variante hasta fecha; limpia/recalcula y afecta saldos. |
| `DESCARGASALIDAPEPS(@SALIDAKEY)` | PROCESS/CALCULATION | WRITE | `GETPRODUCTSTRUCT` → `SALDOS`; borra/recrea `TRAZO`/`DESCARGA`. |
| `DESCARGAXFECHA51(@hasta)` | PROCESS/CALCULATION | WRITE | `alternativo`, `SETTINGS`, `descargaRespaldo`, `MensajeDescarga`, `estructurasML`. |
| `SALDOS`, `SALDOS_FAMILIA`, `SALDOS2` | CALCULATION | WRITE | Inserta descarga, actualiza saldo; variantes. |
| `SALDOSDIRIGIDOS`, `DESCDIRIGIDA`, `INSERTADIRIGIDOS` | CALCULATION/PROCESS | WRITE or UNKNOWN | Flujo dirigido; no ejecutar. |
| `HISTORIADESCARGAS*` | REPORT/PROCESS | WRITE/MIXED | Borrado/insert de historia y regularización desperdicio. |
| `LIGADESPERDICIOS`, `LIGACTMA`, `GUARDADESCARGACTMA` | PROCESS/COMMAND | WRITE | Regulariza/vincula flujos especiales y saldos. |
| `DESCARGASCTM*`, `SALDOSCTM` | PROCESS/CALCULATION | WRITE | CTM y trazo especializado. |
| `DESCARGAS_A31` | PROCESS | WRITE | Tablas y saldos A31. |

### Mapa de dependencias observado

```text
SALIDAS / PSALIDAS
  → GETPRODUCTSTRUCT
  → ESTRUCTURAS / PRODUCTOMATERIAL
  → DESCARGASALIDAPEPS o DESCARGATSALIDA*
      → DESCDIRIGIDA / SALDOSDIRIGIDOS (cuando aplica)
      → SALDOS
          → INSERT / recreación DESCARGA
          → UPDATE PARTIDAS.Saldo
          → INSERT / recreación TRAZO
```

**CONFIRMADO:** no ejecutar esta cadena desde GET ni durante auditoría.

## 9. Fuentes read-only y reportes

**CONFIRMADO (auditoría previa):** existen views `v_descarga`,
`V_INFORMEDESCARGAS`, `INFORMEDESCARGOS`, `V_STATUS_DESCARGAS`,
`DESCARGA_CTMA`, `DESCARGA_DESPERDICIO`, `v_saldos` y `v_saldosdesp`.

**DECISIÓN V1:** para un reporte genérico de descargos, ninguna de estas views
se reutiliza como API porque mezclan saldo actual, conciliación, valores fiscales
o subtipos y no prueban una entidad superior de descargo.

**Excepción documentada:** `V_STATUS_DESCARGAS` sí se reutiliza únicamente para
`GET /api/v1/reportes/dirigidos`, filtrando su flag calculado `DIRIGIDO = 'SI'`.
Esto no crea un GET genérico de descargos ni desbloquea generación dirigida.

- `V_INFORMEDESCARGAS` permanece fuera del contrato Dirigidos: no tiene flag ni
dependencia directa con `DIRIGIDO`;
- `TRAZO` y `Trazo_report` permanecen fuera por ser resultados/procesos mutables;
- el objeto fuente común para detalle histórico sigue siendo `DESCARGA`, ya
expuesto en Materiales Utilizados.

**DECISIÓN V1:** `V_INFORMEDESCARGAS` se aprueba como evidencia read-only
parcial para análisis, no como contrato total del motor. La implementación usa
los mismos joins demostrados por la view sobre `DESCARGA`, `PARTIDAS`,
`IMPORTACIONES`, `PSALIDAS`, `SALIDAS` y `CATEGORIAS`, conservando
`DESCARGA.Descargakey` como identificador/tie-breaker técnico.

- Fuente auditada: `dbo.V_INFORMEDESCARGAS`, 3,866 filas.
- Grano demostrado: una fila física `DESCARGA`; 3,866 IDs distintos, 3,643
  grupos de links y 223 duplicados legítimos de links.
- Links: 3,866/3,866 filas son unibles a importación, partida, salida y partida
  de salida.
- Filtros V1: texto sobre importación, exportación, material y producto.
- Orden: `SALIDAS.Fecha DESC`, `SALIDAS.SalidaKey DESC`,
  `PSALIDAS.Psalidakey DESC`, `DESCARGA.Descargakey DESC`.
- `SaldoActual` se audita pero no se expone: proviene de `PARTIDAS.Saldo` y su
  semántica fiscal permanece fuera de esta capacidad.
- `TRAZO` tiene 0 filas; `Trazo_report` es WRITE/MIXED y no se ejecuta.

#### Gate de equivalencia de proyección

La equivalencia se verificó agrupando ambos datasets por la proyección común y
comparando la multiplicidad de cada grupo. `Descargakey` se excluyó porque la
view no lo expone y sólo cumple identidad técnica/tie-breaker en la consulta
nueva.

```text
ANALYSIS_COMMON_PROJECTION =
(Pedimentoimportacion, SecuenciaImp, PedimentoExportacion,
 ProductoExp, FechaPagoExp, FechaVencimiento, CantidadUMCImportada,
 CantUMCExportada, NoParte)
SOURCE_GROUPS = 3643
SP_SOURCE_GROUPS = 3643
SOURCE_ONLY_GROUPS = 0
SP_ONLY_GROUPS = 0
MULTIPLICITY_MISMATCH_GROUPS = 0
DISCHARGE_PROJECTION_EQUIVALENCE = PASS
```

La expresión de `PedimentoExportacion` conserva la regla de la view para clave
`ct`; no se comparó únicamente por nombre de columna. El SP conserva además
campos de análisis derivados de `DESCARGA` que no forman parte de la proyección
común de la view.

`DESCARGA.Descargakey` es identidad técnica única: `DESCARGA` tiene 3,866 filas
y 3,866 IDs distintos, por lo que `DESCARGAKEY_UNIQUE = PASS`. El orden
`SALIDAS.Fecha DESC, SALIDAS.SalidaKey DESC, PSALIDAS.Psalidakey DESC,
DESCARGA.Descargakey DESC` queda justificado como
`DISCHARGE_ANALYSIS_STABLE_ORDERING = PASS`; los tres primeros campos preservan
el orden funcional observado y el ID único resuelve empates.

`SaldoActual` se audita pero no se expone: `SALDO_ACTUAL_SOURCE_FIELD =
NOT_EXPOSED`, porque su presencia en una view analítica no cierra el contrato
fiscal de Saldos. Asimismo, `CantUMCDescargada` fue `NULL` en 3,866/3,866 filas
y queda como `CANT_UMC_DESCARGADA = NOT_EXPOSED_DATASET_NULL`; no se deriva un
total alternativo en Java, SQL de aplicación ni UI.

`DISCHARGE_ANALYSIS_SECURITY = PASS`: el endpoint responde 401 sin token, 403
sin `REPORTES_GENERAR` y 200 con dicho permiso en las pruebas focalizadas.

### Reconciliación de historiales 033/034

| Legacy ID | Requirement | Modern surface | Coverage | Gap |
|---|---|---|---|---|
| LEGACY-033 | Historial por importación | `/reportes` → Análisis de descargas; filtro de importación | FULL | Faltantes, trazo y saldos fiscales no forman parte de la V1 |
| LEGACY-034 | Historial por exportación | `/reportes` → Análisis de descargas; filtro de salida | FULL | Faltantes, trazo y saldos fiscales no forman parte de la V1 |

La superficie consolidada permite identificar y filtrar la entrada o salida,
observar la relación histórica, material y cantidades, paginar y consultar sin
mutar. No se exige una pantalla legacy separada para declarar la cobertura
funcional rediseñada.

SP versionado: `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`.
API: `GET /api/v1/reportes/analisis-descargas`.
Permiso: `REPORTES_GENERAR`.
UI: opción `Análisis de descargas` dentro de `/reportes`, sin sidebar nuevo.
`ANALISIS_DESCARGAS_XLSX = IMPLEMENTED`: `GET /api/v1/reportes/analisis-descargas/exportacion`, protegido por `REPORTES_EXPORTAR`, reutiliza el mismo SP y filtro, y limita la exportación a 10,000 filas.
`LEGACY-032 = PARTIAL`: `FALTANTES = BLOCKED_MUTABLE_DEPENDENT_EMPTY_SNAPSHOT` y `TRAZO = BLOCKED_MUTABLE_DEPENDENT_EMPTY_WORKTABLE`; la exportación no modifica ese límite.

## Operaciones bloqueadas V1

La auditoría separa explícitamente la consulta de un bloqueo de la acción de
resolverlo. `dbo.BLOQUEA_DOCUMENTO` actualiza `PSALIDAS.bloqueado` y crea un
snapshot en `dbo.DESCARGOSBLOQUEADOS`; no se ejecutó ese procedimiento. La tabla
snapshot tiene una fila por asignación histórica bloqueada generada por el
proceso, una PK real `DESCARGOSBLOQUEADOSKEY` y campos de exportación,
importación, producto, material, cantidades, fechas y folio. No existe un objeto
independiente llamado `BLOQUEADO`: es la columna `PSALIDAS.bloqueado` y el
snapshot `DESCARGOSBLOQUEADOS` es el resultado persistido del proceso. La
relación con `DESCARGA` se demuestra por la definición de `BLOQUEA_DOCUMENTO`,
pero el snapshot no conserva `DESCARGAKEY` ni una FK directa.

#### Gate de retención y orden

El inventario LIVE de `sys.sql_modules` y `sys.sql_expression_dependencies` cubre
`BLOQUEA_DOCUMENTO`, `APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR` y la referencia de
`NUEVOFOLIOB`. `BLOQUEA_DOCUMENTO` es el único escritor observado: inserta filas
en el snapshot y actualiza `PSALIDAS.bloqueado`. El procedimiento read-only nuevo
y `NUEVOFOLIOB` son lectores; no se observaron referencias a `DELETE`, `TRUNCATE`
o `MERGE` sobre `DESCARGOSBLOQUEADOS`.

```text
DESCARGOSBLOQUEADOS_WRITERS = [dbo.BLOQUEA_DOCUMENTO (INSERT)]
DESCARGOSBLOQUEADOS_DELETERS = []
DESCARGOSBLOQUEADOS_TRUNCATORS = []
DESCARGOSBLOQUEADOS_READERS = [dbo.APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR, dbo.NUEVOFOLIOB]
BLOCKED_SNAPSHOT_RETENTION = NO_PURGE_PATH_OBSERVED
BLOCKED_STABLE_ORDERING = PASS
PK = DESCARGOSBLOQUEADOSKEY (PRIMARY KEY, NOT NULL, UNIQUE)
```

La clasificación `NO_PURGE_PATH_OBSERVED` se limita al alcance auditado:
`sys.sql_modules` y `sys.sql_expression_dependencies` para los objetos listados.
Sólo se observaron `INSERT`/`UPDATE` y lecturas; no se observaron
`DELETE`/`TRUNCATE`/`MERGE` sobre `DESCARGOSBLOQUEADOS`. Esto permite llamar al
resultado un snapshot persistido observado, pero no garantiza retención perpetua
fuera de esos objetos, procesos, permisos o políticas de infraestructura.

```text
BLOCKED_READ_CONTRACT = PARTIAL
BLOCKED_READ_V1 = IMPLEMENTED_SNAPSHOT_SUBSET
BLOCKED_SOURCE = dbo.DESCARGOSBLOQUEADOS
BLOCKED_SOURCE_ROWS = 0
NULL_KEY_ROWS = 0
DUPLICATE_KEY_GROUPS = 0
BLOCKED_DIRECTED_RELATION = NONE
BLOCKED_DESCARGA_RELATION = DERIVED_SNAPSHOT_NO_DIRECT_FK
ACTIVE_PSALIDAS_BLOCKED_ROWS = 0
ACTIVE_BLOCKED_STATE = NOT_DEMONSTRATED_IN_CURRENT_DATASET
```

`PSALIDAS.bloqueado` fue `NULL` en 3,392/3,392 filas; por ello la V1 no afirma
que existan operaciones actualmente bloqueadas. El snapshot actual está vacío,
pero su grano y sus campos están demostrados. La consulta usa filtros textuales
sólo sobre documentos, claves, producto, material y folio, paginación 1-based de
máximo 100 y orden `FECHA_BLOQUEO DESC, DESCARGOSBLOQUEADOSKEY DESC`.

- SP: `dbo.APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR`.
- API: `GET /api/v1/reportes/operaciones-bloqueadas`.
- UI: opción `Operaciones bloqueadas` dentro de `/reportes`, sin sidebar nuevo.
- Empty state: `No hay operaciones bloqueadas para los filtros seleccionados.`
- No existen comandos `resolver`, `desbloquear`, `reprocesar` ni `generar`.
- `DESCDIRIGIDA` es WRITE/MIXED por delegar en `SALDOSDIRIGIDOS`; ambos quedan
  fuera de la consulta y no fueron ejecutados.

Reconciliación LIVE del SP, sin imprimir filas:

```text
source total = 0
SP total = 0
synthetic empty filter = total 0, rows 0
extreme page = total 0, rows 0
STRUCTURAL_MATCH = PASS
stable ordering = PASS
mutable SP executed = 0
CALE_IMMEX business writes = 0
inline Java SQL = 0
```

`LEGACY-031` pasa de `UNKNOWN` a `PARTIAL` sólo por este subconjunto de
consulta histórica; resolver o desbloquear permanece fuera de alcance.

### 9.1 Clasificación de columnas de `V_INFORMEDESCARGAS`

La metadata LIVE registró tipo, longitud y nullable para las 42 columnas. La
clasificación semántica usada para la proyección V1 es:

- `CONFIRMED_MEANING`: `PatenteImp`, `AduanaSeccImp`, `Pedimentoimportacion`,
  `Fecha de Pago`, `Clave Pedimento`, `FacturaImp`, `FechaFacturaImp`, `NoParte`,
  `SecuenciaImp`, `FraccionImp`, `DescripcionImp`, `CantidadUMCImportada`,
  `UMCImp`, `CantUMTImportada`, `UMTImp`, `ValorAduana`, `Pais_Origen`,
  `FechaVencimiento` (fórmula SQL explícita), `PatenteExp`, `AduanaSeccExp`,
  `PedimentoExportacion`, `FechaPagoExp`, `ClaveExp`, `FacturaExp`,
  `FechaFacturaExp`, `SecuenciaExp`, `ProductoExp`, `FraccionExp`,
  `DescripcionExp`, `CantUMTExportada`, `UMTExp`, `CantUMCExportada`, `UMCExp`,
  `ValorComercialExp` y `Pais_Destino`.
- `TECHNICAL_OR_DERIVED`: `FolioPedimentoImp`, `FolioPedimentoExp`,
  `ValorUnit_AduMN`, `ValorUnitExp` y `CantUMCDescargada`. Los folios son
  substrings; los valores unitarios son divisiones SQL; `CantUMCDescargada` es
  suma SQL de `CantUtil + Merma + Desperdicio` y resultó NULL en 3,866/3,866
  filas por propagación de NULL, por lo que no se expone en V1.
- `UNKNOWN_MEANING_FOR_V1`: `SaldoActual` y `ValorAgregado`. `SaldoActual`
  proviene de `PARTIDAS.Saldo`, pero no se afirma que sea el contrato fiscal de
  Saldos; `ValorAgregado` se conserva auditado, no se proyecta hasta contar con
  significado funcional aprobado.

Las columnas del SP V1 se limitan a identificadores de relación, material,
producto, fechas demostradas, cantidades de `DESCARGA` y unidad. No se expone
`SaldoActual`, no se inventan faltantes y no se deriva un estado operativo.

### 9.2 `TRAZO` y proceso de análisis

`dbo.TRAZO` es tabla `READ SOURCE / MUTABLE PROCESS RESULT`, con 0 filas LIVE y
PK `trazokey bigint NOT NULL`. Sus columnas auditadas son: `psalidakey bigint`
nullable, `producto varchar(50)` nullable, `cantidad float` nullable,
`linea bigint` nullable, `descargo float` nullable, `falto float` nullable,
`padre varchar(50)` nullable, `salidakey bigint` nullable,
`observacion varchar(100)` nullable, `PRODMATKEY bigint` nullable,
`PRODMATKEYT bigint` nullable y `CLAVEORIGINAL varchar(50)` nullable. La
procedure `SALDOSDIRIGIDOS` inserta filas en `TRAZO`; `Trazo_report` no lee esta
tabla como consulta pura: trunca `ANALISIS_MATERIALES`, inserta resultados y
actualiza acumulados. Ninguno se ejecutó.

## 10. Pantalla legacy

**PENDIENTE:** no existió sesión autorizada Web Forms durante esta fase.

No se navegaron ni accionaron botones de procesar, recalcular, borrar, confirmar,
PEPS, descargo por fecha o historial. Se requiere observación read-only para
confirmar labels, filtros, acciones peligrosas y si UI presenta entidad distinta.

## 11. Decisión arquitectónica

**DECISIÓN ARQUITECTÓNICA V1:** no crear un GET genérico de Descargos que
repita Materiales Utilizados. Sí existe un contrato parcial de análisis histórico
enriquecido, con `GET /api/v1/reportes/analisis-descargas` y
`dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; no incluye trazo, faltantes, saldos
fiscales, reproceso ni generación.

## 12. Command futuro

**Sí, futuro proyecto separado:** ejecución/reproceso de descargos.

| Tema | Estado / riesgo |
|---|---|
| Raíces candidatas | `DESCARGASALIDAPEPS`, `DESCARGATSALIDA*`, `DESCARGAXFECHA51`; confirmar dispatcher actual. |
| Side effects | `DESCARGA`, `PARTIDAS.Saldo`, `TRAZO`, historia, ajustes dirigidos/CTM/desperdicio. |
| Idempotencia | PENDIENTE; nombres/definiciones indican borrado y recreación. |
| Rollback / transacción | PENDIENTE; inspeccionar definición actual. |
| Locking / concurrencia / duración | PENDIENTE; medir sólo en ambiente seguro con plan autorizado. |
| Audit trail | PENDIENTE; tablas de historia no son lectura segura por sí mismas. |
| Permiso | No reutilizar `OPERACIONES_CONSULTAR`; requiere permiso explícito y separación de funciones. |

Un comando futuro no es CRUD: necesita caso de uso aprobado, simulación o dry-run,
protección contra reproceso concurrente, auditoría, reversión y ownership fiscal.

## 13. Pendientes

1. Reestablecer conexión SQL con certificado confiable y login audit-only; repetir
   metadata, row counts, links float, tablas historia y definiciones completas.
2. Auditar UI legacy sin ejecutar acciones mutables.
3. Confirmar entidad/encabezado de ejecución, usuario, fecha proceso y status.
4. Inspeccionar `DESCARGATSALIDA1` vs `DESCARGATSALIDAFECHA` línea por línea.
5. Inspeccionar algoritmo PEPS, transacciones, cursores, errores, locks y rollback.
6. Decidir con negocio si comando futuro requiere simulación, aprobación dual y
   bitácora inmutable.
