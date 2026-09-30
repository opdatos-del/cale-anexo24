# Mapeo de reportes extendidos V1

## Alcance y seguridad

Auditoría read-only sobre metadata, definiciones, dependencias, columnas y conteos agregados de `CALE_IMMEX`. No se ejecutó ningún procedimiento legacy mutable ni se escribió información en `CALE_IMMEX`.

## Contratos

| Candidato | Evidencia | Clasificación | Decisión V1 |
|---|---|---|---|
| Vencimientos | `vDESPERDICIOS.VENCIMIENTO` y `VReporteAplicaciondesperdicios.VENCIMIENTO`; ambas views tienen 0 filas | PARTIAL | No implementar sin semántica funcional y casos activos |
| Compulsa | `v_compulsa_gen` (8 columnas, 662 filas) y `v_compulsa` (33 columnas, 3394 filas) comparan glosa contra Anexo 24 | PARTIAL | Implementar sólo consulta resumen paginada desde `v_compulsa_gen`; detalle y generación quedan pendientes |
| Scrap / desperdicios | `vDESPERDICIOS` y `VReporteAplicaciondesperdicios`; esquema demostrado, 0 filas actuales | PARTIAL | No implementar hasta cerrar diferencia entre desperdicio, aplicado y detalle |
| Dirigidos | Tablas y procedimientos de descarga dirigida; predominan objetos mutables | UNKNOWN | No ejecutar ni exponer como reporte |
| Rectificaciones | `v_rectificaciones` tiene detalle explícito pero 0 filas; `v_total_rectificaciones` tiene 662 agregados | PARTIAL | No mezclar agregado y detalle sin regla funcional |
| Activo fijo especializado | Consulta operativa `APP24_Q_ACTIVOS_FIJOS_LISTAR` ya implementada | CONSOLIDATE | No duplicar dentro de Reportes |
| Consolidado materiales | Sin contrato independiente confirmado | UNKNOWN | No implementar |
| Consolidado productos | Sin contrato independiente confirmado | UNKNOWN | No implementar |
| Consolidado estructuras | `PR_INFORME_ESTRUCTURAS` usa procesamiento interno y no ofrece contrato seguro independiente | PARTIAL | No ejecutar; mantener consulta de estructuras existente |
| CTM / F4 / HDE | Múltiples tablas, views y procedimientos de descarga/cumplimiento | UNKNOWN | No existe aún una decisión de negocio ni contrato funcional suficiente |

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

Fuente detallada auditada pero no implementada: `dbo.v_compulsa`.

Columnas del resumen: pedimento, fecha, clave y fracción, cada una en versión glosa y Anexo 24. La consulta nueva no ejecuta generadores legacy: expone el snapshot read-only existente.

- SP versionado: `dbo.APP24_Q_COMPULSA_LISTAR`.
- API: `GET /api/v1/reportes/compulsa`.
- Filtros: búsqueda textual sobre pedimentos, claves y fracciones.
- Paginación: página 1-based; máximo 100 filas.
- Permiso: `REPORTES_GENERAR`.
- UI: opción `Compulsa` dentro de `/reportes`; sin nueva entrada lateral.
- `COMPULSA_SUMMARY_READ = IMPLEMENTED`.
- `COMPULSA_DETAIL_READ = NOT_IMPLEMENTED`; `dbo.v_compulsa` permanece sólo auditada.
- `COMPULSA_GENERATION = NOT_IMPLEMENTED`; `PR_CompulsaDSA24` y `PR_COMPULSACANTIDADES` no se ejecutan.
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

## Controles

- Procedimientos legacy mutables ejecutados: 0.
- Escrituras a datos `CALE_IMMEX`: 0.
- SQL funcional inline Java: 0.
- SP read-only nuevos: 1.
