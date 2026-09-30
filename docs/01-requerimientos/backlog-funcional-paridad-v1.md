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

**Prioridad:** P1/P2 · **Estado:** `UNKNOWN` en la mayoría de capacidades.

**Incluye:** tipos de material, unidades, categorías, divisiones/almacenes,
proveedores, clientes, agentes aduanales, submaquilas y consultas guardadas.

**Evidencia:** `MATERIAL`, `CATEGORIAS`, `unidad`, `ALMACENKEY`,
`ENTIDAD(DIVISION)`, `PROVEEDORES`, `CLIENTES` y referencias de submaquila en
mapeos SQL. No existe todavía un contrato común de catálogo.

**Trabajo:**

1. identificar la fuente canónica de cada catálogo;
2. documentar clave, etiqueta, estado, filtros y permisos;
3. separar catálogos de lectura de efectos de cargas legacy;
4. decidir si se consolidan en una superficie configurable o en endpoints
   específicos;
5. agregar casos de aceptación de búsqueda y selección.

**No hacer:** construir un framework genérico antes de cerrar esos contratos.

### EPIC-IMPORTS-PEDIMENTS — Importación de pedimentos

**Prioridad:** P0 · **Estado:** `MISSING` y parcialmente `UNKNOWN`.

**Incluye:** layout/staging de `CARGAPEDIMENTOSIE`, validación, errores,
encabezados `IMPORTACIONES`, partidas y la rama de operación 2.

**Evidencia:** `mapeo-entradas.md`, `CARGAPEDIMENTOS`, `VALIDAPEDIMENTO`,
`VALIDA_I_DETALLENP`, `ERRORCARGA` y datos de staging documentados.

**Trabajo:**

1. obtener layout oficial y archivo controlado;
2. definir lote, usuario, aislamiento y estados;
3. separar parseo/validación de confirmación de negocio;
4. cerrar reglas de materiales/productos faltantes, unidades, duplicados y
   errores por fila;
5. definir transacción, rollback e idempotencia;
6. probar que la carga produce las filas esperadas en `IMPORTACIONES` y
   `PARTIDAS`, sin ejecutar legacy en producción sin autorización.

**Dependencias:** decisión operativa de negocio y permisos de escritura en
ambiente controlado.

### EPIC-CATALOG-IMPORTS — Cargas de materiales y productos

**Prioridad:** P1 · **Estado:** `MISSING`.

**Incluye:** `CARGA_MATERIALES`, `CARGA_PRODUCTOS`, staging, validación y tablas
de error.

**Evidencia:** `tmpproductos`, `ECargaProducto`, `CARGA_MATERIALES`,
`CARGA_PRODUCTOS` y mapeo de Productos.

**Trabajo:** reutilizar el patrón de staging, hash, preview y errores de
Facturación como candidato; no reutilizar automáticamente reglas de negocio ni
procedimientos mutables. El caso de aceptación debe demostrar altas válidas,
rechazos y repetición segura.

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

**Pendiente:** concentrados de materiales/productos/estructuras, vencimientos,
compulsa, scrap, dirigidos, CTM/F4/HDE, rectificaciones y otros informes sólo
cuando exista una proyección aprobada. No se debe copiar el número de columnas
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

- `LEGACY-008` a `LEGACY-012`: catálogos auxiliares con fuente canónica.
- `LEGACY-019`: carga de exportaciones separada de consulta.
- `LEGACY-023` a `LEGACY-028`: operaciones especiales.
- `LEGACY-030` a `LEGACY-034`: dirigidos, análisis e históricos especializados.
- `LEGACY-041`, `LEGACY-046`, `LEGACY-049`, `LEGACY-051`: cumplimiento y reportes
  operativos especializados.
- `LEGACY-060`, `LEGACY-064`, `LEGACY-065`, `LEGACY-069` a `LEGACY-073`:
  interfaces de cumplimiento cuando exista evidencia suficiente.

### P2

- `LEGACY-005` a `LEGACY-007`: catálogos auxiliares restantes.
- `LEGACY-027`, `LEGACY-041`, `LEGACY-043` a `LEGACY-048` y `LEGACY-059` a
  `LEGACY-068`: reportes, consolidados e interfaces especializadas.
- `LEGACY-061` a `LEGACY-063`: órdenes, procesos y CTM cuando se confirme la
  fuente.

### P3

- `LEGACY-014`: consultas guardadas.
- Proyecciones legacy sin caso de uso vigente después de la revisión de negocio.

## 5. Recomendación de siguiente feature funcional

### `feature/legacy-catalogs-aux-read-v1`

**Alcance recomendado:** cerrar e implementar únicamente consultas read-only de
catálogos auxiliares con fuente y contrato confirmados, empezando por unidades,
categorías y divisiones/almacenes si la empresa confirma que son necesarios para
la operación V1. Incluir UI de selección/listado, permisos, paginación sólo si el
volumen lo requiere y evidencia SQL `READ_ONLY`.

**Por qué:**

- reduce dependencias de futuras cargas de pedimentos, productos y facturación;
- puede aprovechar el patrón hexagonal, paginación, permisos y adapters/SP ya
  usado por Materiales y Productos;
- no requiere ejecutar procesos de descargo ni elegir una fórmula de Saldos;
- tiene evidencia SQL parcial (`unidad`, `CATEGORIAS`, `ALMACENKEY` y
  `ENTIDAD(DIVISION)`), por lo que el primer entregable puede cerrar la fuente
  antes de construir un framework genérico;
- evita convertir Facturación, Saldos, Ajuste anual o Anexo 30 en una
  implementación por inferencia.

**Dependencias:** una revisión funcional breve debe confirmar nombres, columnas,
permisos y uso operativo. Si esa revisión descubre que el catálogo auxiliar no es
necesario para V1, la siguiente opción es `EPIC-IMPORTS-PEDIMENTS`, pero sólo
después de cerrar layout, transacción, idempotencia y aceptación.

**Blocker de negocio:** ninguno identificado para una consulta auxiliar bien
acotada; el contrato específico todavía debe ser confirmado. No se recomienda
iniciar una carga write hasta obtener la aceptación del flujo.

## 6. Criterio de cierre del backlog de auditoría

Una épica sale de `UNKNOWN`, `MISSING` o `BLOCKED_BUSINESS` únicamente cuando se
actualiza la matriz con evidencia concreta y una decisión autorizada. La
actualización debe conservar qué se observó, qué se implementó, qué se descartó y
qué permanece fuera de V1.
