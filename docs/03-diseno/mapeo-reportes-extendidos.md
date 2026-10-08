# Mapeo de reportes extendidos V1

## Alcance y seguridad

Auditoría read-only sobre metadata, definiciones, dependencias, columnas y conteos agregados de `CALE_IMMEX`. No se ejecutó ningún procedimiento legacy mutable ni se escribió información en `CALE_IMMEX`.

## Contratos

| Candidato | Evidencia | Clasificación | Decisión V1 |
|---|---|---|---|
| Vencimientos | `vDESPERDICIOS.VENCIMIENTO` y `VReporteAplicaciondesperdicios.VENCIMIENTO`; ambas views tienen 0 filas; fórmula `DATEADD(month, categorias.meses, Importaciones.Fecha)` | PARTIAL | Implementar consulta read-only del subconjunto de desperdicios; saldos, descargos y estados quedan fuera de V1 |
| Compulsa | `v_compulsa_gen` (8 columnas, 662 filas) y `v_compulsa` (33 columnas, 3394 filas) comparan glosa contra Anexo 24 | PARTIAL | Implementar resumen y detalle read-only paginados desde `v_compulsa_gen` y `v_compulsa`; generación y reconciliación mutable quedan fuera |
| Scrap / desperdicios | `vDESPERDICIOS`, `VReporteAplicaciondesperdicios`, `vDesperdiciosDetalleAplicacion`, `DESCARGA_DESPERDICIO`, `PED_DESPERDICIOS` y `DescargaDesp`; todos los datasets auditados tienen 0 filas | PARTIAL | Reutilizar sólo el agregado de desperdicio ya expuesto por Vencimientos; no afirmar equivalencia con aplicación, detalle, descargo o pendientes; `LEGACY-048` permanece UNKNOWN |
| Dirigidos | `V_STATUS_DESCARGAS` expone flag read-only `DIRIGIDO`; `DIRIGIDO` está vacío; `DESCDIRIGIDA`, `SALDOSDIRIGIDOS` y `Trazo_report` son mutables | PARTIAL | Exponer sólo consulta paginada de líneas marcadas como dirigidas; no generar descargos ni calcular saldos |<!--  -->
| Análisis de descargas | `V_INFORMEDESCARGAS` tiene 3866 filas y depende de `DESCARGA`, `PARTIDAS`, `IMPORTACIONES`, `PSALIDAS` y `SALIDAS`; `TRAZO` está vacío y `Trazo_report` es WRITE/MIXED | PARTIAL | Exponer relaciones históricas enriquecidas importación → descarga → salida; no exponer saldos fiscales, faltantes, trazo ni motor |<!--  -->
| Operaciones bloqueadas | `DESCARGOSBLOQUEADOS` tiene 0 filas, PK `DESCARGOSBLOQUEADOSKEY` NOT NULL/única y snapshot generado por `BLOQUEA_DOCUMENTO`; no hay referencias LIVE a DELETE/TRUNCATE/MERGE; `PSALIDAS.bloqueado` está NULL en 3392/3392 filas | PARTIAL | Exponer sólo snapshot histórico paginado; resolver, desbloquear, reprocesar y estado activo quedan fuera |<!--  -->
| Rectificaciones | `v_rectificaciones` tiene detalle explícito pero 0 filas; `v_total_rectificaciones` tiene 662 agregados | PARTIAL | Resumen read-only paginado desde `v_total_rectificaciones` y detalle read-only paginado desde `v_rectificaciones`; procesamiento mutable y paridad final pendientes |
| Activo fijo especializado | Consulta operativa `APP24_Q_ACTIVOS_FIJOS_LISTAR` ya implementada | CONSOLIDATE | No duplicar dentro de Reportes |
| Consolidado materiales | Sin contrato independiente confirmado | UNKNOWN | No implementar |
| Consolidado productos | Sin contrato independiente confirmado | UNKNOWN | No implementar |
| Consolidado estructuras | `PR_INFORME_ESTRUCTURAS` usa procesamiento interno y no ofrece contrato seguro independiente | PARTIAL | No ejecutar; mantener consulta de estructuras existente |
| CTM / F4 / HDE | `V_F4CTMA`/`V_F4DESP` delimitan líneas dirigidas de salidas F4/A3 por `TipoDescarga` CTMAPAA/DESP (0 filas); tablas CTM y `V_INFORME_F4_CTMAPAA` sin contrato; sin objetos HDE | PARTIAL | Implementar sólo la consulta read-only F4 con XLSX; CTM mutable y HDE quedan fuera (`HDE_CONTRACT = NOT_FOUND`) |

## Procedimientos legacy candidatos

| Procedimiento | Clasificación | Evidencia relevante |
|---|---|---|
| `PR_INFORME_ESTRUCTURAS` | MIXED | definición contiene procesamiento DML interno |
| `PR_INFORME_EXPORTACIONES` | MIXED | definición contiene procesamiento DML interno |
| `PR_INFORME_IMPORTACIONES` | MIXED | definición contiene procesamiento DML interno |
| `PROC_HISTORIADESCARGASALIDA` | WRITE/MIXED | escribe `HISTORIADESCARGASALIDA` |
| `PROC_HISTORIADESCARGASALIDAFALTANTES` | WRITE/MIXED | escribe `HISTORIADESCARGASALIDA` |
| `PR_CompulsaDSA24` | MIXED | genera datos intermedios; no se ejecutó |
| `PR_COMPULSACANTIDADES` | WRITE/MIXED | escribe `COMPULSACANTIDADES` |

## Compulsa V1

Fuente del resumen implementado: `dbo.v_compulsa_gen`.

Fuente detallada implementada: `dbo.v_compulsa` mediante wrapper read-only versionado.

Columnas del resumen: pedimento, fecha, clave y fracción, cada una en versión glosa y Anexo 24. La consulta nueva no ejecuta generadores legacy: expone el snapshot read-only existente.

- SP versionado: `dbo.APP24_Q_COMPULSA_LISTAR`.
- API: `GET /api/v1/reportes/compulsa`.
- Filtros: búsqueda textual sobre pedimentos, claves y fracciones.
- Paginación: página 1-based; máximo 100 filas.
- Permiso: `REPORTES_GENERAR`.
- UI: opción `Compulsa` dentro de `/reportes`; sin nueva entrada lateral.
- `COMPULSA_SUMMARY_READ = IMPLEMENTED`.
- Detalle: `GET /api/v1/reportes/compulsa/detalle` mediante `dbo.APP24_Q_COMPULSA_DETALLE_LISTAR`; filtro técnico opcional, paginación 1-100 y 33 columnas legacy sin recalcular estatus.
- `COMPULSA_DETAIL_READ = IMPLEMENTED`; `COMPULSA_DETAIL_SOURCE = dbo.v_compulsa`; `PHYSICAL_ROW_KEY = NONE`; `STABLE_ORDER_FOR_NON_IDENTICAL_ROWS = YES`; `EXACT_DUPLICATE_RELATIVE_ORDER = NOT_GUARANTEED`.
- SQL IT: `FIXTURE_PARITY_SCOPE = VIEW_PROJECTION_CONTRACT_ONLY`; `REAL_DBSQL_VIEW_DEFINITION = CONFIRMED_BY_CONTROLLER`; `REAL_LEGACY_VIEW_RUNTIME_PARITY = NOT_EXECUTED`.
- `COMPULSA_GENERATION = NOT_IMPLEMENTED`; `COMPULSA_MUTABLE_PROCESSES = NOT_EXECUTED`; `QUERY_MUTABLE_PRECONDITION_REQUIRED = NO`; `DATA_FRESHNESS_EXTERNAL_DEPENDENCY = YES`; `CURRENT_DATA_GUARANTEED = NO`. `PR_CompulsaDSA24` y `PR_COMPULSACANTIDADES` no se ejecutan.
- `COMPULSA_XLSX = NOT_IMPLEMENTED`; se evita exportar un contrato que aún no incluye detalle de estatus.
- Filtro: sólo pedimentos, claves y fracciones demostrados en `v_compulsa_gen`.
- Orden: `Pedimento A24`, `Pedimento Glosa`, `Fecha A24`, `Fecha Glosa`, `Clave A24`, `Clave Glosa`, `Fraccion A24`, `Fraccion Glosa`; se usan todas las columnas proyectadas porque la vista no expone una PK aprobada.
- Paginación: `@Total` cuenta el resultado filtrado antes de `OFFSET/FETCH`; páginas 1-based.

## Reconciliación LIVE

- `v_compulsa_gen`: 662 filas.
- `APP24_Q_COMPULSA_LISTAR` total sin filtro: 662 filas.
- Paginación LIVE: página 1/tamaño 20 devuelve `total=662`, `rows=20`; página 34 devuelve `total=662`, `rows=2`; página 35 devuelve `total=662`, `rows=0`.
- Filtro sintético improbable devuelve `total=0`, `rows=0`.
- Resultado focal capturado sin imprimir datos: 1 fila para tamaño 1.
- `COMPULSA_PAGINATION = PASS`.
- `COMPULSA_EMPTY_FILTER = PASS`.
- `COMPULSA_STABLE_ORDERING = PASS`: el `ORDER BY` usa las ocho columnas proyectadas; no se inventa una PK ausente en la vista.
- Definición versionada vs `sys.sql_modules`: `STRUCTURAL_MATCH = PASS`, normalizando encabezado, whitespace y literales para evitar diferencias de codificación de `sqlcmd`.

## Rectificaciones V1

El resumen implementado usa `dbo.v_total_rectificaciones`, que proyecta una fila
por `pedimento` distinto de `dbo.v_operaciones` y calcula `Total` como el número
de pedimentos distintos cuyo `pedimentooriginal` coincide con ese pedimento. En
LIVE se observaron 662 filas, todas con `Total = 0`; esto describe el dataset
actual y no cambia el contrato de consulta.

- SP versionado: `dbo.APP24_Q_RECTIFICACIONES_LISTAR`.
- API: `GET /api/v1/reportes/rectificaciones`.
- Permiso: `REPORTES_GENERAR`.
- Filtro: sólo `pedimento`, por ser la única columna con significado confirmado para búsqueda.
- Paginación: página 1-based, tamaño máximo 100; `@Total` se calcula después del filtro y antes de `OFFSET/FETCH`.
- Orden: `pedimento`, con `NULL` primero; la vista proyecta únicamente esa columna como identidad lógica y no se inventa una PK.
- UI: opción `Rectificaciones` dentro de `/reportes`; sin nueva entrada lateral.
- XLSX: `NOT_IMPLEMENTED` en esta V1.
- Detalle: `GET /api/v1/reportes/rectificaciones/detalle` sobre `dbo.v_rectificaciones` mediante `dbo.APP24_Q_RECTIFICACIONES_DETALLE_LISTAR` (filtro opcional, paginación 1-100, `Status` literal legacy, sin llave física). La auditoría LIVE previa observó `0` filas; no se revalidó.
- UI detalle: opción `Rectificaciones - detalle` dentro de `/reportes`; reporte textual sin XLSX.
- `RECTIFICATIONS_SUMMARY_READ = IMPLEMENTED`; `RECTIFICATIONS_DETAIL_READ = IMPLEMENTED`; `RECTIFICATION_DETAIL_SOURCE = dbo.v_rectificaciones`; `RECTIFICATION_MUTABLE_PROCESSING = NOT_IMPLEMENTED`.
- Procesamiento: no se ejecutan `INSERTAPEDIMENTO`, `CARGAPEDIMENTOS` ni otros procedimientos mutables.

## Reconciliación de Rectificaciones

- `dbo.v_rectificaciones`: `0` filas; dataset de detalle con pedimento rectificado,
  clave, descarga, pedimento original, existencia, clave/descarga original y
  estatus calculado.
- `dbo.v_total_rectificaciones`: `662` filas; dataset agregado por pedimento,
  con `Total` de relaciones encontradas.
- `RECTIFICATION_VIEW_RELATION = PARTIAL`: ambas vistas dependen de
  `dbo.v_operaciones`, pero una es detalle condicionado a pedimentos originales y
  la otra es un agregado por pedimento; no se afirma una correspondencia 1:1.
- Fuentes: `dbo.v_operaciones` es una vista read-only sobre `Importaciones`,
  `partidas`, `psalidas` y `salidas`. Las tablas de operación no se modifican.
- `RECTIFICATIONS_SUMMARY_CONTRACT = CONFIRMED`.
- `RECTIFICATIONS_DETAIL_CONTRACT = NOT_IMPLEMENTED / DATASET_EMPTY`.

## Análisis de descargas V1

El contrato es parcial: `V_INFORMEDESCARGAS` representa una relación histórica
por fila física de `DESCARGA`, no un resultado del motor de análisis completo.
La medición LIVE demostró 3,866 filas de `DESCARGA`, 3,866 `Descargakey` distintos,
3,643 grupos de links y 223 duplicados legítimos de links; las 3,866 filas son
unibles a importación, partida, salida y partida de salida. La view proyecta
campos de entrada/salida, materiales, fechas, cantidades y `SaldoActual`; este
último proviene de `PARTIDAS.Saldo` y no se expone por no tener contrato fiscal
cerrado. `TRAZO` tiene 0 filas y `Trazo_report` trunca/escribe resultados, por lo
que no se ejecuta ni se usa como fuente.

- `DISCHARGE_ANALYSIS_GRAIN = DESCARGA_ROW`.
- `DISCHARGE_ANALYSIS_OVERLAP = PARTIAL` con Materiales Utilizados: comparte la
  asignación histórica persistida, pero añade contexto de entrada/salida y fechas;
  no se duplica la consulta base ni se declara cobertura de faltantes/trazo.
- `DISCHARGE_ANALYSIS_CONTRACT = PARTIAL`.
- SP: `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`.
- API: `GET /api/v1/reportes/analisis-descargas`.
- UI: opción `Análisis de descargas` dentro de `/reportes`; sin sidebar nuevo.
- Filtros: texto sobre pedimento de importación, pedimento de salida, material y
  producto; no se agrega filtro de fecha por costumbre.
- Proyección: identificadores de descarga/entrada/salida, material, producto,
  fechas de importación/salida/vencimiento, cantidades persistidas y unidad.
- Orden determinístico: `SALIDAS.Fecha DESC`, `SALIDAS.SalidaKey DESC`,
  `PSALIDAS.Psalidakey DESC`, `DESCARGA.Descargakey DESC`; el último es PK
  observable de la fila física.
- `SaldoActual`, faltantes derivados y estado operativo: `NOT_IMPLEMENTED`.
- `LEGACY-032 = PARTIAL`; faltantes y trazo continúan bloqueados por snapshots/procesos mutables.
- `ANALISIS_DESCARGAS_XLSX = IMPLEMENTED` mediante `GET /api/v1/reportes/analisis-descargas/exportacion`.
- Permiso de exportación: `REPORTES_EXPORTAR`; reutiliza `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`, el filtro actual y un máximo de 10,000 filas.
- No agrega fuentes, grano, reglas de negocio ni campos derivados.

Reconciliación LIVE del SP, sin imprimir filas:

```text
source V_INFORMEDESCARGAS = 3866
SP total = 3866
page 1 rows = 20
empty synthetic filter = total 0, rows 0
extreme page = total 3866, rows 0
STRUCTURAL_MATCH = PASS
stable ordering = PASS
mutable SP executed = 0
CALE_IMMEX business writes = 0
inline Java SQL = 0
```

`LEGACY-032` pasa de `UNKNOWN` a `PARTIAL` únicamente para esta consulta
analítica read-only. La superficie consolidada permite consultar por entrada y
por salida; `LEGACY-033` y `LEGACY-034` quedan cubiertos como
`IMPLEMENTED_REDESIGNED`, sin exigir pantallas separadas ni ejecutar mutaciones.

## F4 (CTM / desperdicio) V1

La consulta implementada expone las líneas dirigidas de las vistas legacy
read-only `dbo.V_F4CTMA` (TipoDescarga `CTMAPAA`) y `dbo.V_F4DESP`
(TipoDescarga `DESP`), ambas filtradas por `Cve_pedimento` F4/A3. El grano es la
relación de `dirigido`; `dirigidokey` (PK not nullable) sostiene el orden
determinístico. En LIVE ambas vistas tienen 0 filas (`dirigido` vacío).

- SP versionado: `dbo.APP24_Q_F4_LISTAR`.
- API: `GET /api/v1/reportes/f4` y `GET /api/v1/reportes/f4/exportacion` (XLSX
  con la misma proyección vía `ExportadorXlsxReportes`).
- Filtros: búsqueda textual sobre tipo de descarga, F4, importación y clave.
- Orden: `FECHA DESC`, `F4`, `CLAVE`, `DIRIGIDOKEY`.
- Proyección: tipo de descarga, F4, fecha, importación, clave, incorporado y
  saldo.
- Permiso: `REPORTES_GENERAR` / `REPORTES_EXPORTAR`.
- UI: opción `F4 (CTM / desperdicio)` dentro de `/reportes`; sin sidebar nuevo.
- `F4_READ_V1 = IMPLEMENTED`; `LIVE_ROWS = 0` documentado sin inventar caso real.
- `CTM` mutable (`LIGACTMA`, `LIGACTMFACTURA`, `SALDOSCTM`, `CTMDESCARGA`) y
  `V_INFORME_F4_CTMAPAA` fuera de alcance; `HDE_CONTRACT = NOT_FOUND`.
- Pruebas: `F4LineasSqlIT` (contrato de columnas, conteo, páginas, filtros y
  orden determinístico sobre SQL Server efímero) y pruebas de use case/API/UI.

## Controles

- Procedimientos legacy mutables ejecutados: 0.
- Escrituras a datos `CALE_IMMEX`: 0.
- SQL funcional inline Java: 0.
- SP read-only nuevos: 1.
