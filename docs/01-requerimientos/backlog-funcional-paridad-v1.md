# Backlog funcional de paridad V1

## 1. Propósito

Este backlog deriva de `matriz-paridad-legacy-v1.md`. Agrupa gaps por épica para
planear trabajo posterior sin duplicar la matriz ni convertir hipótesis legacy en
requerimientos aprobados.

Estados de la matriz y prioridades son provisionales. Ningún ítem de este
backlog implica que se deba ejecutar un procedimiento mutable, modificar una
base externa o iniciar implementación sin evidencia y aceptación funcional.

## 2. Reglas de entrada a desarrollo

Antes de implementar una épica se debe contar con:

- capacidad y grano funcional confirmados;
- pantalla o flujo legacy reproducible, cuando exista;
- layout y versión del archivo, si aplica;
- contrato API y permisos;
- fuente SQL aprobada y clasificación read-only/write;
- comportamiento de errores, duplicados, reintentos e idempotencia;
- caso de aceptación con datos autorizados;
- decisión explícita para cualquier `BLOCKED_BUSINESS`.

Los objetos `SALDOS*`, `DESCARGATSALIDA*`, `CARGAPEDIMENTOS`, cargas de
facturación legacy y demás procesos mutables no se invocan desde consultas GET ni
se ejecutan como parte de este inventario.

## 3. Épicas

### EPIC-CATALOGS-AUX — Catálogos auxiliares

**Prioridad:** P1/P2 · **Estado:** `PARTIAL`.

**Incluye:** tipos de material, unidades, categorías, divisiones/almacenes,
proveedores, clientes, agentes aduanales, submaquilas y consultas guardadas.

**Implementado en `feature/legacy-catalogs-aux-v1`:** Unidades, Tipos de
material, Categorías y Almacenes como consultas `READ_ONLY`, cada uno con fuente
SQL, procedimiento versionado, endpoint, permiso común y superficie agrupada en
`/catalogos`. La capacidad de División independiente permanece `UNKNOWN`.

**Implementado en `feature/legacy-business-parties-v1`:** Clientes, Proveedores y
Agentes aduanales como consultas `READ_ONLY`, paginadas y agrupadas en
`/catalogos/socios-comerciales`. Reutilizan `CATALOGOS_AUX_CONSULTAR`.

**Pendiente:** submaquilas, datos relacionados y consultas guardadas; también
confirmar si existe un catálogo canónico independiente de divisiones. El staging
de importación de socios queda pendiente por falta de layout aislado y contrato
de validación suficiente.

**Evidencia:** los catálogos foundation y los maestros `dbo.clientes`,
`dbo.Proveedores` y `dbo.agentes` tienen fuentes y contratos read-only cerrados.
`ENTIDAD(DIVISION)` y submaquila permanecen sin maestro canónico suficiente.

**Trabajo restante:**

1. cerrar fuente canónica, clave, etiqueta, filtros y permisos de cada candidato;
2. separar catálogos de lectura de efectos de cargas legacy;
3. decidir si se consolidan en una superficie configurable o en endpoints
   específicos;
4. agregar casos de aceptación de búsqueda y selección;
5. mantener explícita la frontera entre almacén implementado y división no
   demostrada.

**No hacer:** construir un framework genérico ni CRUD antes de cerrar esos
contratos.

### EPIC-IMPORTS-PEDIMENTS — Importación de pedimentos

**Prioridad:** P0 · **Estado:** `PARTIAL / V1 STAGING DONE`.

**Incluye:** layout/staging de `CARGAPEDIMENTOSIE`, validación, errores,
encabezados `IMPORTACIONES`, partidas y la rama de operación 2.

**Evidencia:** `mapeo-entradas.md`, `CARGAPEDIMENTOS`, `VALIDAPEDIMENTO`,
`VALIDA_I_DETALLENP`, `ERRORCARGA`,
`docs/03-diseno/mapeo-carga-pedimentos.md` y
`docs/05-pruebas/pedimentos-staging-v1.md`.

**Entregado en `feature/legacy-imports-pediments-v1`:** upload `.xls/.xlsx`,
parser, validación estructural, hash, RBAC `PEDIMENTOS_CARGAR`, staging aislado
en `app24`, errores, preview paginado, bitácora controlada, transacción con
rollback verificado e idempotencia de hash. No se usa staging global legacy.

**Pendiente:**

1. obtener y aprobar layout oficial y archivo controlado;
2. cerrar reglas de materiales/productos faltantes, unidades y operación;
3. definir la confirmación autoritativa hacia `IMPORTACIONES` y `PARTIDAS`;
4. definir efectos, duplicados, reintentos, rollback y aceptación de la
   confirmación operativa;
5. ejecutar una prueba autorizada del pipeline mutable sólo después de esa
   decisión.

**Dependencias:** decisión operativa de negocio y permisos de escritura en
ambiente controlado. La confirmación no forma parte de V1.

### EPIC-CATALOG-IMPORTS — Cargas de materiales y productos

**Prioridad:** P1 · **Estado:** `PARTIAL / V1 STAGING DONE`.

**Incluye:** `CARGA_MATERIALES`, `CARGA_PRODUCTOS`, staging, validación y tablas
de error.

**Evidencia legacy:** `CargaMaterial`, `ECargaMaterial`, `FactoresMP`, `material`,
`tmpproductos`, `ECargaProducto`, `productos`, `CARGA_MATERIALES` y
`CARGA_PRODUCTOS`. Ambos SP legacy son `MIXED` y no se ejecutan desde V1.

**Entregado en `feature/legacy-catalog-imports-v1`:** upload `.xls/.xlsx`, parser
explícito por contrato de stage, validación estructural, hash, errores, RBAC
`MATERIALES_CARGAR`/`PRODUCTOS_CARGAR`, staging aislado en `app24`, preview
paginado y bitácora controlada. `LEGACY-054` y `LEGACY-055` pasan de `MISSING` a
`PARTIAL`.

**Pendiente:** cerrar layout oficial y reglas completas de catálogo; diseñar y
autorizar una confirmación separada hacia `dbo.MATERIAL`/`FactoresMP` y
`dbo.PRODUCTOS`. No escribir `CARGAMATERIAL`, `tmpproductos` ni tablas legacy
durante preview.

### EPIC-SPECIAL-OPERATIONS — Operaciones especiales

**Prioridad:** P1/P2 · **Estado:** `MISSING`/`UNKNOWN`.

**Incluye:** cambios de régimen, regularizaciones, actas de destrucción,
transferencias de submaquila, constancias y CTM.

**Evidencia:** procesos y tablas documentados en `mapeo-entradas.md`,
`mapeo-salidas.md`, `mapeo-descargos.md` y `mapeo-facturacion.md`.

**Trabajo:** inventariar cada flujo por separado, capturar layout y efectos,
clasificar si es consulta o comando, y documentar reglas fiscales y autorización.
No agruparlos bajo Operaciones genéricas sólo por compartir tablas.

### EPIC-DISCHARGES — Descargos y trazabilidad avanzada

**Prioridad:** P0/P1 · **Estado:** `BLOCKED_BUSINESS` para generación; `PARTIAL`
para históricos.

**Incluye:** descargo automático, dirigido, bloqueado, análisis, historial por
entrada/salida y consulta de asignaciones.

**Evidencia:** `DESCARGA`, `TRAZO`, `DIRIGIDO`, `DESCARGATSALIDA*`,
`DESCARGASALIDAPEPS`, `SALDOS*`, `v_descarga` y
`APP24_Q_MATERIALES_UTILIZADOS_LISTAR`.

**Trabajo:**

1. conservar Materiales Utilizados como consulta read-only del histórico físico;
2. pedir a negocio definición del algoritmo, PEPS, dirigidos y excepciones;
3. definir aprobación, permisos separados, locking, duración y rollback;
4. diseñar simulación/dry-run antes de cualquier comando real;
5. definir trazabilidad de ejecución, usuario, fecha y resultado.

**Bloqueador:** no existe todavía una regla autoritativa que permita elegir entre
las variantes `SALDOS*` y `DESCARGATSALIDA*`.

### EPIC-REPORTS-CONSOLIDATED — Reportes y consolidados

**Prioridad:** P1/P2 · **Estado:** `IMPLEMENTED_REDESIGNED` para reportes V1,
`CONSOLIDATE`/`UNKNOWN` para especializados.

**Ya disponible:** Entradas, Salidas, Materiales Utilizados y Bitácora en la
superficie `/reportes`, con paginación y exportación XLSX; las consultas y
exports reales están en `ReportesController`.

**Implementado en `feature/legacy-reports-extended-v1`:** consulta resumen
read-only de Compulsa dentro de `/reportes`, con filtro textual y paginación
estable basada en las columnas proyectadas de `v_compulsa_gen`. `READ SUMMARY V1 = DONE`.

**Pendiente:** detalle de `v_compulsa`, generación/reconciliación legacy, XLSX
específico, concentrados de materiales/productos/estructuras, Scrap completo,
CTM/F4/HDE, estados/procesamiento de rectificaciones y otros informes sólo
cuando exista una proyección aprobada. El resumen read-only de rectificaciones
está implementado en `GET /api/v1/reportes/rectificaciones`; el subconjunto
read-only de vencimientos de desperdicio está implementado en
`GET /api/v1/reportes/vencimientos`; la consulta read-only parcial de líneas
marcadas como dirigidas está implementada en `GET /api/v1/reportes/dirigidos`; y
el análisis read-only parcial de relaciones históricas está implementado en
`GET /api/v1/reportes/analisis-descargas`. El detalle permanece sin datos, los
estados operativos, faltantes, trazo y la aplicación no forman parte de V1. El detalle y generación de Compulsa son
`PENDING CONTRACT / OUT OF V1`. CTM/F4/HDE permanece `UNKNOWN`, no
`BLOCKED_BUSINESS`, hasta contar con decisión de negocio explícita. No se debe copiar el número de columnas
legacy por defecto.

**Reutilización:** filtros, paginación, permisos y `ExportadorXlsxReportes`.

### EPIC-SALDOS — Contrato de saldos

**Prioridad:** P0 · **Estado:** `BLOCKED_BUSINESS`.

**Evidencia:** `PARTIDAS.Saldo`, `PR_INFORME_SALDOS`, `v_saldos`,
`v_saldosdesp`, `INFORME_CONCENTRADOSALDOS` y reglas documentadas en
`mapeo-saldos.md`.

**Desbloqueo obligatorio:** definición, fórmula, fuentes, granularidad, corte,
tratamiento de ajustes/retornos/desperdicios/activos fijos, casos reales y
fuente oficial aprobada. `PARTIDAS.Saldo` no se presenta como fórmula oficial.

### EPIC-BILLING-CONFIRMATION — Confirmación de Facturación

**Prioridad:** P0 · **Estado:** `PARTIAL` y `PENDING_BUSINESS`.

**Ya disponible:** layout validado, upload, validación, preview, errores por
archivo/hoja/fila/columna, fingerprint/hash y staging durable en `app24`.

**Pendiente:** elegir el pipeline autoritativo entre `CARGA_FACTURAS`,
`CARGAFACTURASENPSALIDAS`, `CREAPRODUCTOSCARGAFACTURA` y posibles flujos C;
definir tablas afectadas, operación resultante, duplicados, reintentos,
transacción, rollback y caso de aceptación de 1–3 filas.

**No hacer:** implementar confirmación suponiendo que `CARGAR` o `GUARDAR`
representan un proceso específico.

### EPIC-ANNUAL-ADJUSTMENT — Ajuste anual

**Prioridad:** P1/P2 · **Estado:** `UNKNOWN`.

**Incluye:** carga de ventas, CTM, inventario inicial/final y otros archivos; e
informe principal, almacén, ventas, 4.3.16 I y 4.3.16 II.

**Entrada requerida:** nombres exactos de pantallas, layouts, fuente SQL, fórmula,
periodo, tratamiento de errores y archivo esperado. No se diseña el modelo con
los nombres de las secciones solamente.

### EPIC-ANNEX30 — Anexo 30

**Prioridad:** P1 · **Estado:** `UNKNOWN`.

**Incluye:** informe de descargos, destino aduanero, clave, año, sustitución,
worksheet, errores, faltantes, TXT/Excel y revisión comparativa.

**Evidencia:** `A31_DESCARGAS`, `A31_SALDOS`, `A31_TRAZO`, `DESCARGAS_A31` y
referencias de auditoría consolidada.

**Entrada requerida:** regla regulatoria vigente, fuente canónica, parámetros,
caso esperado, formato de salida y reconciliación contra entradas, inventario
inicial, descargas, saldos y vencimientos.

### EPIC-ADMIN-PARITY — Administración y seguridad

**Prioridad:** P0/P1 · **Estado:** implementado/rediseñado, con una decisión de
consolidación pendiente.

**Ya disponible:** login, usuarios, perfiles, permisos y bitácora con app24,
API protegida y UI moderna. Actividades se consulta mediante API y se administra
como parte de perfiles/permisos.

**Pendiente:** confirmar con negocio si Actividades necesita una pantalla propia.
Si no, conservar `CONSOLIDATE` y documentar la equivalencia funcional.

### EPIC-UX-DASHBOARD — Dashboard

**Prioridad:** según decisión de negocio · **Estado:** fuera de esta matriz de
paridad funcional detallada.

El dashboard actual tiene accesos por permiso, avisos, estado visual, total
 dinámico de Materiales y empty state. `DASHBOARD_V1` sigue
`PENDING_BUSINESS`; no se agregan KPIs por inferencia.

## 4. Backlog priorizado

### P0

- `LEGACY-016` y `LEGACY-017`: contrato seguro de carga/validación de pedimentos.
- `LEGACY-021` y `LEGACY-038`: decisión funcional completa de Saldos.
- `LEGACY-029`: decisión y diseño seguro del motor de descargos.
- `LEGACY-057`: pipeline reproducible de pedimentos.
- `LEGACY-058`: confirmación de Facturación con caso controlado.
- `LEGACY-074` a `LEGACY-079`: mantener regresión de seguridad y administración.

### P1

- `LEGACY-008` (subcaso de división), `LEGACY-009` a `LEGACY-012`: catálogos
  auxiliares pendientes de fuente canónica y contrato.
- `LEGACY-019`: carga de exportaciones separada de consulta.
- `LEGACY-023` a `LEGACY-028`: operaciones especiales.
- `LEGACY-030` y `LEGACY-031`: generación dirigida y operaciones bloqueadas
  pendientes; el subconjunto analítico read-only de `LEGACY-032` está implementado
  como PARTIAL y `LEGACY-033`/`LEGACY-034` están cubiertos como
  `IMPLEMENTED_REDESIGNED` por la superficie consolidada.
- `LEGACY-041`, `LEGACY-046`, `LEGACY-049`, `LEGACY-051`: cumplimiento y reportes
  operativos especializados.
- `LEGACY-060`, `LEGACY-064`, `LEGACY-065`, `LEGACY-069` a `LEGACY-073`:
  interfaces de cumplimiento cuando exista evidencia suficiente.

### P2

- Consultas guardadas y proyecciones auxiliares sin caso de uso confirmado.
- `LEGACY-027`, `LEGACY-041`, `LEGACY-043` a `LEGACY-048` y `LEGACY-059` a
  `LEGACY-068`: reportes, consolidados e interfaces especializadas.
- `LEGACY-061` a `LEGACY-063`: órdenes, procesos y CTM cuando se confirme la
  fuente.

### P3

- `LEGACY-014`: consultas guardadas.
- Proyecciones legacy sin caso de uso vigente después de la revisión de negocio.

## 5. Recomendación de siguiente feature funcional

### Feature cerrada: `feature/legacy-catalogs-aux-v1`

**Alcance entregado:** consultas `READ_ONLY` de unidades, tipos de material,
categorías y almacenes, con SP versionados, endpoints protegidos por
`CATALOGOS_AUX_CONSULTAR`, UI agrupada en `/catalogos` y reconciliación LIVE
contra las fuentes de `CALE_IMMEX`. No se implementaron CRUD ni divisiones como
catálogo independiente.

**Resultado:** la épica pasa de `UNKNOWN` a `PARTIAL`. Los cuatro catálogos
entregados salen del backlog de implementación; permanecen como trabajo los
candidatos sin contrato y la división independiente.

**Resultado:** `EPIC-IMPORTS-PEDIMENTS` queda en `PARTIAL / V1 STAGING DONE`.
La confirmación autoritativa continúa separada y no implementada.

**Resultado:** `EPIC-CATALOG-IMPORTS` queda en `PARTIAL / V1 STAGING DONE`.
La confirmación autoritativa de materiales y productos continúa separada y no
implementada.

**Siguiente recomendación:** auditar y aprobar el contrato de confirmación de
materiales/productos con casos sintéticos antes de permitir cualquier escritura
en `CALE_IMMEX`.

**Blocker de negocio:** la confirmación autoritativa y las reglas completas de
catálogo siguen pendientes. Saldos, confirmación de Facturación, Dashboard,
descargos, ajuste anual y Anexo 30 permanecen fuera de alcance.

## 6. Criterio de cierre del backlog de auditoría

Una épica sale de `UNKNOWN`, `MISSING` o `BLOCKED_BUSINESS` únicamente cuando se
actualiza la matriz con evidencia concreta y una decisión autorizada. La
actualización debe conservar qué se observó, qué se implementó, qué se descartó y
qué permanece fuera de V1.
