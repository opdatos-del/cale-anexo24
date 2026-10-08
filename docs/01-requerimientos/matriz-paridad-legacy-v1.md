# Matriz de paridad legacy V1

## 1. Propósito

Este documento inventaría capacidades observadas o identificadas en el sistema
legacy y las contrasta con la aplicación nueva. No convierte el número de rutas
legacy en porcentaje de avance: una ruta puede contener varias capacidades y una
capacidad puede aparecer en varias rutas.

La matriz es una auditoría de inventario. No implementa Java, Angular, SQL,
procedimientos ni migraciones.

## 2. Cobertura y método

### Fuentes utilizadas

- `docs/02-analisis/auditoria-sistema-actual.md`: auditoría funcional Web Forms
  autenticada, datos sintéticos y pantallas observadas.
- `docs/02-analisis/procesos-actuales.md`: procesos funcionales generales.
- `docs/03-diseno/mapeo-entradas.md`, `mapeo-salidas.md`,
  `mapeo-materiales-utilizados.md`, `mapeo-activos-fijos.md`,
  `mapeo-productos.md`, `mapeo-estructuras.md`, `mapeo-saldos.md`,
  `mapeo-descargos.md`, `mapeo-facturacion.md` y `mapeo-reportes.md`.
- `docs/03-diseno/auditoria-stored-procedures.md` y
  `docs/03-diseno/procedimientos-almacenados.md`.
- `docs/03-diseno/mapeo-administracion.md`.
- Rutas reales de `frontend/src/app/app.routes.ts` y módulos lazy.
- Controllers reales bajo `backend/src/main/java/**/api`.
- Scripts versionados bajo `infra/sql/`, únicamente como evidencia de contrato o
  metadata ya documentada. No se ejecutaron SP mutables durante esta fase.
- Auditorías E2E consolidadas y reportes de validación previamente versionados.

La auditoría consolidada reporta `LEGACY_ROUTES_OBSERVED = 83`. Las 83 rutas no
están versionadas en un único catálogo con nombre ASPX y por eso la columna de
ruta usa `NOT_CAPTURED` cuando el nombre exacto no está respaldado por una fuente
versionada.

### Evidencia

- `AUDIT_UI`: pantalla o flujo visto en la auditoría funcional.
- `AUDIT_E2E`: recorrido automatizado o consolidado previamente documentado.
- `EXCEL`: layout o reporte Excel observado/documentado.
- `SQL_METADATA`: definición, metadata o snapshot read-only documentado.
- `CODE`: ruta Angular, controller, caso de uso o adapter real.
- `RUNTIME`: validación runtime previamente documentada.

### Estados permitidos

- `IMPLEMENTED_EQUIVALENT`: existe la misma capacidad esencial.
- `IMPLEMENTED_REDESIGNED`: existe la capacidad, pero con UX o arquitectura
  modernizada.
- `PARTIAL`: sólo una parte del flujo está disponible.
- `PARTIAL_EVIDENCE_BLOCKED_CONTRACT`: existe presencia o evidencia parcial, pero falta el contrato visible o de fuente necesario para implementar.
- `MISSING`: capacidad confirmada sin equivalente nuevo identificado.
- `BLOCKED_BUSINESS`: no se puede implementar responsablemente sin una regla o
  decisión de negocio.
- `CONSOLIDATE`: varias capacidades legacy se representan deliberadamente en una
  superficie moderna menor; requiere mantener trazabilidad funcional.
- `NOT_REQUIRED`: sólo se usará con decisión explícita respaldada por evidencia.
- `UNKNOWN`: evidencia insuficiente para clasificar con seguridad.

## 3. Matriz detallada

| ID | Área | Legacy route/screen | Capability | Evidence | New equivalent | Frontend | API | DB/SP | State | Priority | Blocker | Proposed destination |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| LEGACY-001 | Catálogos y maestros | `NOT_CAPTURED` — Datos generales | Consultar y mantener datos generales de la empresa | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/catalogos/datos-generales` — consulta read-only de la ficha empresarial | `CompanyGeneralDataPage` | `GET /api/v1/catalogos/datos-generales` | `dbo.DatosGenerales`, `dbo.APP24_Q_DATOS_GENERALES_OBTENER` | PARTIAL | P1 | Edición/mantenimiento no confirmado ni implementado | Catálogos / configuración empresarial |
| LEGACY-002 | Catálogos y maestros | `NOT_CAPTURED` — Materiales | Consultar catálogo de materiales | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/materiales` | `MaterialListPage` | `GET /api/v1/catalogos/materiales` | `dbo.APP24_Q_MATERIALES_LISTAR` | IMPLEMENTED_EQUIVALENT | P0 | Ninguno interno identificado | Catálogos / materiales |
| LEGACY-003 | Catálogos y maestros | `NOT_CAPTURED` — Productos | Consultar catálogo de productos | AUDIT_UI, SQL_METADATA, CODE | `/productos` | `ProductListPage` | `GET /api/v1/catalogos/productos` | `dbo.APP24_Q_PRODUCTOS_LISTAR` | IMPLEMENTED_EQUIVALENT | P0 | Ninguno interno identificado | Catálogos / productos |
| LEGACY-004 | Catálogos y maestros | `NOT_CAPTURED` — Estructuras | Consultar estructura o BOM | AUDIT_UI, SQL_METADATA, CODE | `/estructuras` | `StructureListPage` | `GET /api/v1/catalogos/estructuras` | `dbo.APP24_Q_ESTRUCTURAS_LISTAR` | IMPLEMENTED_EQUIVALENT | P1 | El snapshot SQL tenía cero estructuras | Catálogos / estructuras |
| LEGACY-005 | Catálogos y maestros | `NOT_CAPTURED` — Tipos de material | Consultar catálogo de tipos de material | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Tipos de material | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/tipos-material` | `dbo.TipoMaterial`, `dbo.APP24_Q_TIPOS_MATERIAL_LISTAR` | IMPLEMENTED_REDESIGNED | P2 | Sólo lectura V1; no CRUD | Catálogos auxiliares |
| LEGACY-006 | Catálogos y maestros | `NOT_CAPTURED` — Unidades | Consultar catálogo de unidades | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Unidades | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/unidades` | `dbo.unidad`, `dbo.APP24_Q_UNIDADES_LISTAR` | IMPLEMENTED_REDESIGNED | P2 | Sólo lectura V1; no CRUD | Catálogos auxiliares |
| LEGACY-007 | Catálogos y maestros | `NOT_CAPTURED` — Categorías | Consultar categorías y temporalidad | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Categorías | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/categorias` | `dbo.categorias`, `dbo.APP24_Q_CATEGORIAS_LISTAR` | IMPLEMENTED_REDESIGNED | P2 | Sólo lectura V1; no CRUD | Catálogos auxiliares |
| LEGACY-008 | Catálogos y maestros | `NOT_CAPTURED` — Divisiones/almacenes | Consultar divisiones o almacenes | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Almacenes (División resuelta a almacén, sin catálogo separado) | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/almacenes` | `dbo.almacen`, `dbo.APP24_Q_ALMACENES_LISTAR`; `dbo.ENTIDAD(@DIVISION)` → `ALMACENKEY` | IMPLEMENTED_REDESIGNED | P1 | Semántica División = Almacén demostrada; sin CRUD | Catálogos auxiliares |
| LEGACY-009 | Catálogos y maestros | `NOT_CAPTURED` — Proveedores | Consultar proveedores | AUDIT_UI, SQL_METADATA, CODE | `/catalogos/socios-comerciales` — Proveedores | `BusinessPartiesPage` | `GET /api/v1/catalogos/proveedores` | `dbo.Proveedores`, `dbo.APP24_Q_PROVEEDORES_LISTAR` | IMPLEMENTED_REDESIGNED | P1 | Sólo lectura V1; importación/confirmación fuera de alcance | Catálogos / socios comerciales |
| LEGACY-010 | Catálogos y maestros | `NOT_CAPTURED` — Clientes | Consultar clientes | SQL_METADATA, CODE | `/catalogos/socios-comerciales` — Clientes | `BusinessPartiesPage` | `GET /api/v1/catalogos/clientes` | `dbo.clientes`, `dbo.APP24_Q_CLIENTES_LISTAR` | IMPLEMENTED_REDESIGNED | P1 | Sólo lectura V1; importación/confirmación fuera de alcance | Catálogos / socios comerciales |
| LEGACY-011 | Catálogos y maestros | `NOT_CAPTURED` — Agentes aduanales | Consultar agentes aduanales | AUDIT_UI, SQL_METADATA, CODE | `/catalogos/socios-comerciales` — Agentes aduanales | `BusinessPartiesPage` | `GET /api/v1/catalogos/agentes-aduanales` | `dbo.agentes`, `dbo.APP24_Q_AGENTES_ADUANALES_LISTAR` | IMPLEMENTED_REDESIGNED | P2 | Sólo lectura V1; importación fuera de alcance | Catálogos / socios comerciales |
| LEGACY-012 | Catálogos y maestros | `NOT_CAPTURED` — Submaquilas | Consultar submaquilas | SQL_METADATA | No identificado | No | No | `RelacionSubmaquila`, `Encabezadotransubmaquila`, `Detalletransubmaquila`, `TMPSUBMAQUILA` (0 filas), `CARGA_SUBMAQUILA` no ejecutado; `SUBMAQUILA_CATALOG_CONTRACT = NOT_A_MASTER_CATALOG` | UNKNOWN | P2 | Sólo transacciones/transferencia; no hay maestro canónico | Catálogos auxiliares |
| LEGACY-013 | Catálogos y maestros | `NOT_CAPTURED` — Activo fijo | Consultar partidas marcadas como activo fijo | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/activos-fijos` | `FixedAssetListPage` | `GET /api/v1/operaciones/activos-fijos` | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` | IMPLEMENTED_REDESIGNED | P1 | La granularidad nueva es partida, no activo individual | Operaciones / activo fijo |
| LEGACY-014 | Catálogos y maestros | `NOT_CAPTURED` — Consultas guardadas | Guardar y reutilizar consultas o filtros | AUDIT_UI | No identificado | No | No | UNKNOWN | UNKNOWN | P3 | No existe contrato versionado | Catálogos auxiliares / consultas |
| LEGACY-015 | Operación aduanera/inventario | `NOT_CAPTURED` — Entradas / Importaciones | Consultar entradas e importaciones por rango y filtros | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/entradas` | `EntryListPage` | `GET /api/v1/operaciones/entradas` | `dbo.APP24_Q_ENTRADAS_LISTAR` | IMPLEMENTED_EQUIVALENT | P0 | Ninguno interno identificado | Operaciones / entradas |
| LEGACY-016 | Operación aduanera/inventario | `NOT_CAPTURED` — Carga de pedimentos | Cargar pedimentos desde staging | SQL_METADATA, CODE, RUNTIME | `/operaciones/pedimentos` — staging, preview y confirmación | `PedimentUploadPage` | `POST/GET /api/v1/operaciones/pedimentos/cargas`, `POST .../cargas/{id}/confirmacion` | `app24.APP24_C_PEDIMENTO_CARGA_CREAR`, `APP24_Q_PEDIMENTO_CARGA_POR_HASH`, `APP24_Q_PEDIMENTO_CARGA_OBTENER`, `APP24_Q_PEDIMENTO_CARGA_ERRORES`, `dbo.APP24_C_PEDIMENTO_CONFIRMAR` | IMPLEMENTED_REDESIGNED | P0 | Rediseñado con staging aislado propio; no replica el staging global legacy `CargaPedimentosIE` | Importaciones / staging seguro |
| LEGACY-017 | Operación aduanera/inventario | `NOT_CAPTURED` — Validación de pedimentos | Validar pedimentos antes de procesar | SQL_METADATA, CODE | `/operaciones/pedimentos` — validación batch read-only durante staging | `PedimentUploadPage` | `POST /api/v1/operaciones/pedimentos/cargas` | `APP24_Q_PEDIMENTO_VALIDAR_REGLAS`, `VALIDAPEDIMENTO`, `VALIDA_I_DETALLENP` | PARTIAL | P0 | PED-001..004 y PED-007 confirmadas; PED-005/PED-006 siguen sin contrato implementable porque `INVENTARIO` no existe LIVE | Importaciones |
| LEGACY-018 | Operación aduanera/inventario | `NOT_CAPTURED` — Salidas / Exportaciones | Consultar salidas y líneas por rango y filtros | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/salidas` | `ExitListPage` | `GET /api/v1/operaciones/salidas` | `dbo.APP24_Q_SALIDAS_LISTAR` | IMPLEMENTED_EQUIVALENT | P0 | Ninguno interno identificado | Operaciones / salidas |
| LEGACY-019 | Operación aduanera/inventario | `NOT_CAPTURED` — Carga de exportaciones | Cargar o procesar operaciones de salida | SQL_METADATA, CODE | `/operaciones/pedimentos` — mismo staging, preview y confirmación para `TipoOperacion = 2` | `PedimentUploadPage` | `POST/GET /api/v1/operaciones/pedimentos/cargas`, `POST .../cargas/{id}/confirmacion` | `CargaPedimento`, `CargaPedimentoFila`, `ErrorCargaPedimento`; `dbo.APP24_C_PEDIMENTO_CONFIRMAR` (`SALIDAS`/`PSALIDAS`/`DIRIGIDO` condicional) | IMPLEMENTED_REDESIGNED | P1 | No ejecuta descargos, PEPS ni saldos; sólo persistencia autoritativa de la operación | Importaciones y operaciones |
| LEGACY-020 | Operación aduanera/inventario | `NOT_CAPTURED` — Materiales utilizados | Consultar asignaciones históricas entrada → salida → material | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/materiales-utilizados` | `UsedMaterialListPage` | `GET /api/v1/operaciones/materiales-utilizados` | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | IMPLEMENTED_REDESIGNED | P0 | No ejecuta descargos ni recalcula saldos | Operaciones / trazabilidad |
| LEGACY-021 | Operación aduanera/inventario | `NOT_CAPTURED` — Saldos | Consultar saldo con semántica fiscal y corte definidos | AUDIT_UI, SQL_METADATA | Reporte `Saldos` deshabilitado; sin contrato | Parcial sólo como opción no disponible | No hay endpoint de saldo aprobado | `PR_INFORME_SALDOS`, `v_saldos`, `v_saldosdesp` como candidatos | BLOCKED_BUSINESS | P0 | Fórmula, fuentes, granularidad y corte pendientes | Operaciones / saldos |
| LEGACY-022 | Operación aduanera/inventario | `NOT_CAPTURED` — Activo fijo | Consultar activos derivados de entradas | AUDIT_UI, SQL_METADATA, CODE | `/operaciones/activos-fijos` | `FixedAssetListPage` | `GET /api/v1/operaciones/activos-fijos` | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` | IMPLEMENTED_REDESIGNED | P1 | No representa alta/baja/retorno de activo individual | Operaciones / activo fijo |
| LEGACY-023 | Operación aduanera/inventario | `NOT_CAPTURED` — Cambios de régimen | Procesar o consultar cambios de régimen | SQL_METADATA | No identificado | No | No | Referencias F4/F5/A3/DE y procesos legacy; contrato no cerrado | PARTIAL_EVIDENCE_BLOCKED_CONTRACT | P1 | Pantalla confirmada a nivel de auditoría; faltan ruta, contrato visible, fuente canónica y flujo autorizado | Operaciones especiales |
| LEGACY-024 | Operación aduanera/inventario | `NOT_CAPTURED` — Regularizaciones | Procesar regularizaciones | SQL_METADATA | No identificado | No | No | `INSERTAPEDIMENTO` y procesos relacionados; contrato no cerrado | PARTIAL_EVIDENCE_BLOCKED_CONTRACT | P1 | Pantalla confirmada a nivel de auditoría; faltan ruta, contrato visible, fuente canónica, regla y aceptación | Operaciones especiales |
| LEGACY-025 | Operación aduanera/inventario | NOT_CAPTURED — Actas de destrucción | Cargar y procesar actas de destrucción | SQL_METADATA, CODE, TEST | /operaciones/actas — staging, preview y confirmación | ActaUploadPage | POST/GET /api/v1/operaciones/actas/importaciones, POST /api/v1/operaciones/actas/{cargaId}/confirmacion | app24 CargaActa*; dbo.APP24_C_ACTA_CARGA_CONFIRMAR -> dbo.CARGAACTAS | IMPLEMENTED_REDESIGNED | P1 | Reutiliza la regla legacy en wrapper transaccional; conserva side effects documentados | Operaciones especiales |
| LEGACY-026 | Operación aduanera/inventario | `NOT_CAPTURED` — Transferencias de submaquila | Cargar o procesar transferencias | SQL_METADATA | No identificado | No | No | `CARGA_SUBMAQUILA`, referencias de submaquila/CTM | MISSING | P1 | Contrato y reglas fiscales no cerrados | Operaciones especiales |
| LEGACY-027 | Operación aduanera/inventario | NOT_CAPTURED — Constancias | Cargar y procesar constancias | SQL_METADATA, CODE, TEST | /operaciones/constancias — staging, preview y confirmación | ConstanciaUploadPage | POST/GET /api/v1/operaciones/constancias/importaciones, POST /api/v1/operaciones/constancias/{cargaId}/confirmacion | app24 CargaConstancia*; dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR -> dbo.CARGACONSTANCIAS | IMPLEMENTED_REDESIGNED | P2 | Reutiliza validaciones/efectos legacy dentro de una transacción y falla cerrado ante error stage ajeno | Operaciones especiales |
| LEGACY-028 | Operación aduanera/inventario | `NOT_CAPTURED` — CTM | Consultar o procesar CTM | SQL_METADATA | No identificado | No | No | `CTMDESCARGA`, `SALDOSCTM`, `LIGACTMA`, `LIGACTMFACTURA` | MISSING | P1 | Flujo especializado sin contrato común | Operaciones especiales |
| LEGACY-029 | Descargos y trazabilidad | `NOT_CAPTURED` — Descargo automático | Generar o reprocesar descargos | SQL_METADATA | No identificado | No | No | `DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`, `SALDOS*` | BLOCKED_BUSINESS | P0 | Algoritmo, autorización, rollback e idempotencia pendientes | Descargos / motor controlado |
| LEGACY-030 | Descargos y trazabilidad | `NOT_CAPTURED` — Descargo dirigido | Generar descargo dirigido | SQL_METADATA | No identificado | No | No | `DESCDIRIGIDA`, `SALDOSDIRIGIDOS`, `DIRIGIDO` | BLOCKED_BUSINESS | P1 | Regla dirigida y separación de funciones pendientes | Descargos / dirigidos |
| LEGACY-031 | Descargos y trazabilidad | `NOT_CAPTURED` — Descargo bloqueado | Consultar o resolver operaciones bloqueadas | SQL_METADATA, CODE, RUNTIME | `/reportes` — consulta read-only parcial del snapshot histórico persistente observado | `ReportListPage` | `GET /api/v1/reportes/operaciones-bloqueadas` | `dbo.DESCARGOSBLOQUEADOS`, `dbo.APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR`; `BLOQUEA_DOCUMENTO` como escritor observado | PARTIAL | P1 | Resolver, desbloquear, reprocesar y estado activo fuera de alcance | Descargos / excepciones |
| LEGACY-032 | Descargos y trazabilidad | `NOT_CAPTURED` — Análisis de descarga | Analizar faltantes, trazo y resultado de descarga | SQL_METADATA, CODE, RUNTIME | `/reportes` — análisis read-only parcial de relaciones históricas | `ReportListPage` | `GET /api/v1/reportes/analisis-descargas` | `dbo.V_INFORMEDESCARGAS`, `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; `DESCARGA` como grano físico | PARTIAL | P1 | Sólo relación importación → descarga → salida; faltantes, trazo, saldos fiscales y motor fuera de alcance | Descargos / análisis |
| LEGACY-033 | Descargos y trazabilidad | `NOT_CAPTURED` — Historial por importación | Consultar historial de asignaciones por entrada | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/reportes` — superficie consolidada con filtro textual de importación y relaciones entrada → descarga → salida | `ReportListPage` | `GET /api/v1/reportes/analisis-descargas` | `dbo.V_INFORMEDESCARGAS`, `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; `DESCARGA` como grano físico | IMPLEMENTED_REDESIGNED | P1 | No requiere pantalla separada; faltantes/trazo/saldos fiscales siguen fuera | Descargos / consultas |
| LEGACY-034 | Descargos y trazabilidad | `NOT_CAPTURED` — Historial por exportación | Consultar historial de asignaciones por salida | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/reportes` — superficie consolidada con filtro textual de salida y relaciones salida → descarga → entrada | `ReportListPage` | `GET /api/v1/reportes/analisis-descargas` | `dbo.V_INFORMEDESCARGAS`, `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; `DESCARGA` como grano físico | IMPLEMENTED_REDESIGNED | P1 | No requiere pantalla separada; faltantes/trazo/saldos fiscales siguen fuera | Descargos / consultas |
| LEGACY-035 | Descargos y trazabilidad | `NOT_CAPTURED` — Consulta de descargos persistidos | Consultar filas físicas de `DESCARGA` sin mutar | SQL_METADATA, CODE, RUNTIME | Materiales Utilizados es la superficie moderna | `/operaciones/materiales-utilizados` | `GET /api/v1/operaciones/materiales-utilizados` | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | IMPLEMENTED_REDESIGNED | P0 | No equivale a generar descargos | Operaciones / trazabilidad |
| LEGACY-036 | Reportes y consolidados | `NOT_CAPTURED` — Entradas | Generar y exportar reporte de entradas | AUDIT_UI, CODE | `/reportes`, opción Entradas | `/reportes` | `GET /api/v1/reportes/entradas` y `/entradas/exportacion` | Reutiliza consulta de entradas | IMPLEMENTED_REDESIGNED | P1 | Proyección nueva no replica las 76 columnas legacy | Reportes / operativos |
| LEGACY-037 | Reportes y consolidados | `NOT_CAPTURED` — Salidas | Generar y exportar reporte de salidas | AUDIT_UI, CODE | `/reportes`, opción Salidas | `/reportes` | `GET /api/v1/reportes/salidas` y `/salidas/exportacion` | Reutiliza consulta de salidas | IMPLEMENTED_REDESIGNED | P1 | Proyección nueva no replica las 49 columnas legacy | Reportes / operativos |
| LEGACY-038 | Reportes y consolidados | `NOT_CAPTURED` — Saldos | Generar y exportar reporte de saldos | AUDIT_UI, SQL_METADATA, CODE, TEST | `/reportes` opción Saldos habilitada | `ReportListPage` | `GET /api/v1/reportes/saldos` y `/saldos/exportacion` | `dbo.PR_INFORME_SALDOS` + wrapper read-only `dbo.APP24_Q_SALDOS_LISTAR` (sin reescritura de formulas legacy) | IMPLEMENTED_REDESIGNED | P0 | Read-only; reusa PR_INFORME_SALDOS sin ejecutar SALDOS* ni recalcular saldos. Paridad exacta de superficie/XLSX pendiente de segunda auditoria comparativa (XLSX_PARITY = PENDING_FINAL_COMPARATIVE_AUDIT). DOCUMENT_OVERRIDES_DATE_RANGE = YES. LIVE_ACTIVATION_PENDING = YES. | Reportes / saldos |
| LEGACY-039 | Reportes y consolidados | `NOT_CAPTURED` — Materiales utilizados | Generar y exportar reporte de consumo/descarga | AUDIT_UI, CODE, RUNTIME | `/reportes`, opción Materiales utilizados | `/reportes` | `GET /api/v1/reportes/materiales-utilizados` y exportación | Reutiliza `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | IMPLEMENTED_REDESIGNED | P1 | Mantiene grano físico de `DESCARGA` | Reportes / operativos |
| LEGACY-040 | Reportes y consolidados | `NOT_CAPTURED` — Bitácora | Consultar y exportar bitácora | AUDIT_UI, CODE, RUNTIME | `/bitacora` y `/reportes` | `AuditLogListPage`, `ReportListPage` | `GET /api/v1/bitacora`, `/reportes/bitacora` y exportación | `app24.APP24_Q_BITACORA_LISTAR` | IMPLEMENTED_REDESIGNED | P1 | La fuente legacy original no está vinculada | Administración / bitácora |
| LEGACY-041 | Reportes y consolidados | `NOT_CAPTURED` — CTM/F4/HDE | Generar reportes especializados CTM/F4/HDE | SQL_METADATA, CODE, RUNTIME | `/reportes` — consulta read-only de líneas dirigidas F4 (CTM/desperdicio) | `ReportListPage` | `GET /api/v1/reportes/f4` y `/f4/exportacion` | `dbo.V_F4CTMA`, `dbo.V_F4DESP`, `dbo.APP24_Q_F4_LISTAR`; LIVE_ROWS = 0 | PARTIAL | P2 | Sólo líneas dirigidas F4; CTM mutable y HDE fuera de alcance (HDE_CONTRACT = NOT_FOUND) | Reportes / cumplimiento |
| LEGACY-042 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de saldos | Generar concentrado de saldos | SQL_METADATA | No identificado | No | No | `INFORME_CONCENTRADOSALDOS` escribe `CONCENTRADOSALDOS` | BLOCKED_BUSINESS | P1 | Semántica de snapshot y fórmula pendientes | Reportes / saldos |
| LEGACY-043 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de materiales | Consolidar materiales | SQL_METADATA, CODE | Catálogo y reportes operativos separados | `/materiales`, `/reportes` | Catálogo/reportes existentes | Fuentes `MATERIAL` y reportes; consolidado específico no cerrado | CONSOLIDATE | P2 | Definir si requiere proyección adicional | Reportes / consolidados |
| LEGACY-044 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de productos | Consolidar productos | SQL_METADATA, CODE | Catálogo y reportes operativos separados | `/productos`, `/reportes` | Catálogo/reportes existentes | `PRODUCTOS` y reportes; consolidado específico no cerrado | CONSOLIDATE | P2 | Definir campos y propósito | Reportes / consolidados |
| LEGACY-045 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de estructuras | Consolidar estructuras/BOM | SQL_METADATA, CODE | Catálogo de estructuras; sin reporte separado | `/estructuras` | `GET /api/v1/catalogos/estructuras` | `APP24_Q_ESTRUCTURAS_LISTAR` | CONSOLIDATE | P2 | Dataset BOM actual vacío | Reportes / consolidados |
| LEGACY-046 | Reportes y consolidados | `NOT_CAPTURED` — Vencimientos | Consultar vencimientos | SQL_METADATA, CODE, RUNTIME | `/reportes` — vencimientos read-only de desperdicio | `ReportListPage` | `GET /api/v1/reportes/vencimientos` | `dbo.vDESPERDICIOS`, `dbo.APP24_Q_VENCIMIENTOS_LISTAR`; fórmula `DATEADD(month, categorias.meses, Importaciones.Fecha)` | PARTIAL | P1 | Sólo subconjunto de desperdicio; saldos, descargos y estados fuera de alcance | Reportes / cumplimiento |
| LEGACY-047 | Reportes y consolidados | `NOT_CAPTURED` — Compulsa | Ejecutar o consultar compulsa | SQL_METADATA, CODE | `/reportes` — Compulsa y Compulsa - detalle | `ReportListPage` | `GET /api/v1/reportes/compulsa`; `GET /api/v1/reportes/compulsa/detalle` | `dbo.v_compulsa_gen`, `dbo.APP24_Q_COMPULSA_LISTAR`; `dbo.v_compulsa`, `dbo.APP24_Q_COMPULSA_DETALLE_LISTAR` | PARTIAL | P2 | Resumen y detalle read-only implementados; generación, reconciliación mutable y paridad final pendientes. COMPULSA_SUMMARY_READ = IMPLEMENTED; COMPULSA_DETAIL_READ = IMPLEMENTED; COMPULSA_GENERATION = NOT_IMPLEMENTED; COMPULSA_MUTABLE_PROCESSES = NOT_EXECUTED | Reportes / cumplimiento |
| LEGACY-048 | Reportes y consolidados | `NOT_CAPTURED` — Scrap | Consultar scrap o desperdicio | SQL_METADATA, DOC | No identificado | No | No | `DESCARGA_DESPERDICIO`, `DescargaDesp` y variantes | PARTIAL_EVIDENCE_BLOCKED_CONTRACT | P2 | Pantalla confirmada a nivel de auditoría; faltan ruta, contrato visible y fuente canónica; ver `mapeo-desperdicios.md` | Reportes / cumplimiento |
| LEGACY-049 | Reportes y consolidados | `NOT_CAPTURED` — Dirigidos | Consultar operaciones dirigidas | SQL_METADATA, CODE, RUNTIME | `/reportes` — consulta read-only del subconjunto marcado como dirigido | `ReportListPage` | `GET /api/v1/reportes/dirigidos` | `dbo.V_STATUS_DESCARGAS`, `dbo.APP24_Q_DIRIGIDOS_LISTAR`; filtro estructural `DIRIGIDO = 'SI'` | PARTIAL | P1 | Sólo consulta de líneas marcadas; generación, PEPS, saldos y descargo dirigido permanecen fuera de alcance | Reportes / cumplimiento |
| LEGACY-050 | Reportes y consolidados | `NOT_CAPTURED` — Permisos | Reportar permisos o actividades | CODE | Administración de perfiles y actividades | `/perfiles` | `/api/v1/administracion/perfiles` y `/actividades` | `app24.PerfilApp`, `Actividad`, `PerfilActividad` | CONSOLIDATE | P1 | No se justificó reporte separado | Administración |
| LEGACY-051 | Reportes y consolidados | `NOT_CAPTURED` — Rectificaciones | Consultar o reportar rectificaciones | SQL_METADATA, CODE, RUNTIME | `/reportes` — Rectificaciones (resumen) y Rectificaciones - detalle | `ReportListPage` | `GET /api/v1/reportes/rectificaciones`; `GET /api/v1/reportes/rectificaciones/detalle` | `dbo.v_total_rectificaciones`, `dbo.APP24_Q_RECTIFICACIONES_LISTAR`; `dbo.v_rectificaciones`, `dbo.APP24_Q_RECTIFICACIONES_DETALLE_LISTAR` | PARTIAL | P2 | Resumen y detalle read-only implementados; procesamiento mutable y paridad final pendientes. RECTIFICATIONS_SUMMARY_READ = IMPLEMENTED; RECTIFICATIONS_DETAIL_READ = IMPLEMENTED; RECTIFICATION_MUTABLE_PROCESSING = NOT_IMPLEMENTED | Reportes / cumplimiento |
| LEGACY-052 | Reportes y consolidados | `NOT_CAPTURED` — Activo fijo | Reportar activos fijos | AUDIT_UI, CODE | Consulta de operaciones de activo fijo y reportes comunes | `/operaciones/activos-fijos`, `/reportes` | Operaciones/reportes; sin reporte especializado | `APP24_Q_ACTIVOS_FIJOS_LISTAR` | CONSOLIDATE | P1 | No se modela activo individual | Operaciones / reportes |
| LEGACY-053 | Reportes y consolidados | `NOT_CAPTURED` — Exportación de resultados | Descargar resultados en XLSX | AUDIT_UI, CODE, RUNTIME | Exportación común de Reportes V1 | `/reportes` | Cuatro endpoints `*/exportacion` | `ExportadorXlsxReportes`; consultas read-only | IMPLEMENTED_REDESIGNED | P1 | No se replican formatos legacy no comprobados | Reportes / exportación |
| LEGACY-054 | Interfaces/importación | NOT_CAPTURED — Materiales | Importar materiales desde archivo | SQL_METADATA, CODE, TEST | /catalogos/importaciones — staging, preview y confirmación | CatalogImportPage | POST/GET /api/v1/catalogos/importaciones/materiales, POST /api/v1/catalogos/importaciones/materiales/{cargaId}/confirmacion | app24 CargaCatalogoMaterial*; dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR -> dbo.CARGA_MATERIALES | IMPLEMENTED_REDESIGNED | P1 | Staging aislado y confirmación explícita; reutiliza reglas legacy sin stage global expuesto | Importaciones / catálogos / staging seguro |
| LEGACY-055 | Interfaces/importación | NOT_CAPTURED — Productos | Importar productos desde archivo | SQL_METADATA, CODE, TEST | /catalogos/importaciones — staging, preview y confirmación | CatalogImportPage | POST/GET /api/v1/catalogos/importaciones/productos, POST /api/v1/catalogos/importaciones/productos/{cargaId}/confirmacion | app24 CargaCatalogoProducto*; dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR -> dbo.CARGA_PRODUCTOS | IMPLEMENTED_REDESIGNED | P1 | Staging aislado y confirmación explícita; productos existentes se preservan conforme a la regla legacy insert-only | Importaciones / catálogos / staging seguro |
| LEGACY-056 | Interfaces/importación | NOT_CAPTURED — Clientes/proveedores | Importar o actualizar clientes y proveedores | SQL_METADATA, CODE, TEST | /catalogos/importaciones — tabs Clientes y Proveedores, staging, preview y confirmación | CatalogImportPage | POST/GET /api/v1/catalogos/importaciones/{clientes,proveedores}, POST .../{id}/confirmacion | app24 CargaCatalogoCliente*/CargaCatalogoProveedor*; dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR y dbo.APP24_C_PROVEEDOR_CARGA_CONFIRMAR | PARTIAL | P1 | Altas válidas nuevas confirmadas; registros con la misma clave se preservan sin actualización | Importaciones / catálogos |
| LEGACY-057 | Interfaces/importación | `NOT_CAPTURED` — Pedimentos | Importar pedimentos | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/pedimentos` — confirmación autoritativa | `PedimentUploadPage` | `POST .../cargas/{id}/confirmacion` | `dbo.APP24_C_PEDIMENTO_CONFIRMAR`; legacy `CARGAPEDIMENTOS`/`CargaPedimentosIE`/`ERRORCARGA` no reutilizados | IMPLEMENTED_REDESIGNED | P0 | No ejecuta descargos ni motor fiscal posterior; importación autoritativa cubierta en V1 | Importaciones / pedimentos |
| LEGACY-058 | Interfaces/importación | `NOT_CAPTURED` — Facturación | Cargar archivo, validar, previsualizar y confirmar efectos operativos | AUDIT_UI, EXCEL, SQL_METADATA, CODE | `/facturacion` cubre carga, validación, preview, errores, hash y staging durable | `BillingUploadPage` | `POST /api/v1/facturacion/cargas`, `GET /cargas/{id}`, plantilla | `app24.CargaFacturacion`, `ErrorCarga`; confirmación legacy no integrada | PARTIAL | P0 | Pipeline autoritativo y side effects pendientes de negocio | Facturación / confirmación |
| LEGACY-059 | Interfaces/importación | `NOT_CAPTURED` — Servicios | Importar servicios | SQL_METADATA | No identificado | No | No | Fuente exacta no localizada en contrato versionado | UNKNOWN | P2 | Evidencia insuficiente | Importaciones / servicios |
| LEGACY-060 | Interfaces/importación | NOT_CAPTURED — Actas de destrucción | Importar actas de destrucción | SQL_METADATA, CODE, TEST | /operaciones/actas — misma superficie de LEGACY-025 para ingreso seguro | ActaUploadPage | POST/GET /api/v1/operaciones/actas/importaciones, POST /api/v1/operaciones/actas/{cargaId}/confirmacion | app24 CargaActa*; dbo.APP24_C_ACTA_CARGA_CONFIRMAR -> dbo.CARGAACTAS | IMPLEMENTED_REDESIGNED | P1 | Cubre la interfaz de importación; LEGACY-025 registra el aspecto operativo de la misma confirmación | Importaciones / especiales |
| LEGACY-061 | Interfaces/importación | `NOT_CAPTURED` — Órdenes de fabricación | Importar órdenes de fabricación | SQL_METADATA | No identificado | No | No | Fuente exacta no localizada en contrato versionado | UNKNOWN | P2 | Evidencia insuficiente | Importaciones / producción |
| LEGACY-062 | Interfaces/importación | `NOT_CAPTURED` — Procesos | Importar procesos | SQL_METADATA | No identificado | No | No | Fuentes exactas no localizadas | UNKNOWN | P2 | Evidencia insuficiente | Importaciones / procesos |
| LEGACY-063 | Interfaces/importación | `NOT_CAPTURED` — CTM/Carta de materiales | Importar CTM o carta de materiales | SQL_METADATA | No identificado | No | No | `CARGA_SUBMAQUILA`, `CTMDESCARGA` y objetos relacionados | UNKNOWN | P2 | Layout y proceso autoritativo pendientes | Importaciones / CTM |
| LEGACY-064 | Ajuste anual | `NOT_CAPTURED` — Carga ajuste anual | Cargar archivos del ajuste anual | AUDIT_E2E, SQL_METADATA | No identificado | No | No | Fuente exacta no localizada en los contratos actuales | UNKNOWN | P1 | Layouts, reglas y autoridad no cerrados | Cumplimiento / ajuste anual |
| LEGACY-065 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar informe principal del ajuste anual | AUDIT_E2E | No identificado | No | No | UNKNOWN | UNKNOWN | P1 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-066 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar sección almacén | AUDIT_E2E | No identificado | No | No | UNKNOWN | UNKNOWN | P2 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-067 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar sección ventas | AUDIT_E2E | No identificado | No | No | UNKNOWN | UNKNOWN | P2 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-068 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar secciones 4.3.16 I y II | AUDIT_E2E | No identificado | No | No | UNKNOWN | UNKNOWN | P2 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-069 | Anexo 30 | `NOT_CAPTURED` — Informe de descargos | Generar informe Anexo 30 | AUDIT_E2E, SQL_METADATA | No identificado | No | No | `A31_DESCARGAS`, `A31_SALDOS`, `A31_TRAZO`, `DESCARGAS_A31` | UNKNOWN | P1 | Reglas regulatorias y contrato pendientes | Cumplimiento / Anexo 30 |
| LEGACY-070 | Anexo 30 | `NOT_CAPTURED` — Parámetros de informe | Filtrar por destino aduanero, clave, año y sustitución | AUDIT_E2E | No identificado | No | No | UNKNOWN | UNKNOWN | P1 | Semántica de filtros pendiente | Cumplimiento / Anexo 30 |
| LEGACY-071 | Anexo 30 | `NOT_CAPTURED` — Worksheet y errores | Revisar worksheet, errores y operaciones faltantes | AUDIT_E2E | No identificado | No | No | UNKNOWN | UNKNOWN | P1 | Flujo y fuente no cerrados | Cumplimiento / Anexo 30 |
| LEGACY-072 | Anexo 30 | `NOT_CAPTURED` — TXT y Excel | Generar salidas TXT y Excel | AUDIT_E2E, SQL_METADATA | No identificado | No | No | `SP_GENERA_TXT_COMPLETO` es mixto y no se reutiliza | UNKNOWN | P1 | Formato, seguridad y fuente pendientes | Cumplimiento / Anexo 30 |
| LEGACY-073 | Anexo 30 | NOT_CAPTURED - Revision Anexo 30 | Revisar entradas, inventario inicial, descargas, saldos, comparativa y vencimientos | AUDIT_E2E, CODE, SQL_DUMP, TEST | /reportes - Revision Anexo 30 - Entradas; Revision Anexo 30 - Fracciones de descarga; Revision Anexo 30 - Descargas; Revision Anexo 30 - Comparativa | ReportListPage | GET /api/v1/reportes/anexo30-revision-entradas; GET /api/v1/reportes/anexo30-revision-fracciones; GET /api/v1/reportes/anexo30-revision-descargas; GET /api/v1/reportes/anexo30-revision-comparativa | dbo.A31_ENTRADAS; dbo.A31_DESCARGASF; dbo.A31_DESCARGAS; dbo.A31_COMPARATIVADESCARGA; dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR; dbo.APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR; dbo.APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR; dbo.APP24_Q_ANEXO30_REVISION_COMPARATIVA_LISTAR | PARTIAL | P1 | Entradas y fracciones read-only. Descargas y comparativa consultan el ultimo snapshot persistido sin ejecutar recalculo. READ_SUBCAPABILITY_COMPARATIVA = IMPLEMENTED_READ_ONLY_LAST_PERSISTED_SNAPSHOT. DATA_FRESHNESS_MUTABLE_DEPENDENCY = YES. CURRENT_DATA_GUARANTEED = NO. Vencimientos y demas subcapacidades siguen pendientes. Paridad legacy final pendiente. LIVE_ACTIVATION_PENDING = YES. | Cumplimiento / Anexo 30 |
| LEGACY-074 | Seguridad/administración | `NOT_CAPTURED` — Login | Autenticar usuario y cargar permisos | AUDIT_UI, CODE, RUNTIME | `/login` | `AuthService` y `auth.routes.ts` | `POST /api/v1/auth/login` | `app24.APP24_Q_USUARIO_POR_CLAVE`, `APP24_Q_USUARIO_ACCESO` | IMPLEMENTED_REDESIGNED | P0 | Ninguno interno identificado | Seguridad |
| LEGACY-075 | Seguridad/administración | `NOT_CAPTURED` — Usuarios | Listar, crear, editar, cambiar estado, perfil, vigencia y password | AUDIT_UI, CODE, RUNTIME | `/usuarios` | `UserListPage` | `/api/v1/administracion/usuarios` y commands | `APP24_Q_*USUARIO*`, `APP24_C_USUARIO_*` | IMPLEMENTED_REDESIGNED | P0 | Ninguno interno identificado | Administración / usuarios |
| LEGACY-076 | Seguridad/administración | `NOT_CAPTURED` — Perfiles | Listar y administrar perfiles | AUDIT_UI, CODE, RUNTIME | `/perfiles` | `ProfileManagementPage` | `/api/v1/administracion/perfiles` | `APP24_Q_PERFILES_LISTAR`, `APP24_C_PERFIL_*` | IMPLEMENTED_REDESIGNED | P0 | Ninguno interno identificado | Administración / perfiles |
| LEGACY-077 | Seguridad/administración | `NOT_CAPTURED` — Actividades | Consultar catálogo interno de actividades/permisos | AUDIT_UI, CODE | Consolidado dentro de perfiles/permisos | Sin ruta independiente | `GET /api/v1/administracion/actividades` | `APP24_Q_ACTIVIDADES_LISTAR`, `Actividad` | CONSOLIDATE | P1 | Confirmar si negocio exige pantalla separada | Administración / permisos |
| LEGACY-078 | Seguridad/administración | `NOT_CAPTURED` — Permisos | Asignar permisos a perfiles | AUDIT_UI, CODE, RUNTIME | Consolidado en `/perfiles` | `ProfileManagementPage` | `GET/PUT /api/v1/administracion/perfiles/{id}/permisos` | `PerfilActividad`, `APP24_Q_PERFIL_PERMISOS_LISTAR`, command de reemplazo | IMPLEMENTED_REDESIGNED | P0 | Ninguno interno identificado | Administración / permisos |
| LEGACY-079 | Seguridad/administración | `NOT_CAPTURED` — Bitácora | Consultar eventos de auditoría | AUDIT_UI, CODE, RUNTIME | `/bitacora` | `AuditLogListPage` | `GET /api/v1/bitacora` | `app24.BitacoraEvento`, `APP24_Q_BITACORA_LISTAR` | IMPLEMENTED_REDESIGNED | P0 | Ninguno interno identificado | Administración / bitácora |

## 4. Resumen por área

| Área | Total | Implemented equivalent | Implemented redesigned | Partial | Missing | Blocked | Consolidate | Unknown |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Catálogos y maestros | 14 | 3 | 8 | 1 | 0 | 0 | 0 | 2 |
| Operación aduanera/inventario | 14 | 2 | 6 | 3 | 2 | 1 | 0 | 0 |
| Descargos y trazabilidad | 7 | 0 | 3 | 2 | 0 | 2 | 0 | 0 |
| Reportes y consolidados | 18 | 0 | 6 | 6 | 0 | 1 | 5 | 0 |
| Interfaces/importación | 10 | 0 | 4 | 2 | 0 | 0 | 0 | 4 |
| Ajuste anual | 5 | 0 | 0 | 0 | 0 | 0 | 0 | 5 |
| Anexo 30 | 5 | 0 | 0 | 1 | 0 | 0 | 0 | 4 |
| Seguridad/administración | 6 | 0 | 5 | 0 | 0 | 0 | 1 | 0 |
| **Total** | **79** | **5** | **32** | **15** | **2** | **4** | **6** | **15** |

`LEGACY_CAPABILITIES_TOTAL = 79`. Las filas que permanecen `UNKNOWN` conservan
capacidades identificadas por la auditoría consolidada, pero el repositorio no
contiene todavía un contrato funcional o una fuente SQL suficiente. `LEGACY-001`
es `PARTIAL`: la ficha read-only tiene fuente, cardinalidad y mapeo confirmados;
la edición/mantenimiento legacy continúa fuera de V1. `LEGACY-008` es
`IMPLEMENTED_REDESIGNED`: la auditoría de `dbo.ENTIDAD(@DIVISION)` demuestra que
División se resuelve contra `dbo.almacen.ALMACEN` y devuelve `ALMACENKEY`, por lo
que no existe un catálogo independiente y la consulta `/catalogos/almacenes` ya
cubre la capacidad. `LEGACY-012` conserva `UNKNOWN`: la evidencia sólo muestra
tablas y un procedimiento transaccional de transferencia, no un maestro de
submaquiladores.

`LEGACY-016` es `IMPLEMENTED_REDESIGNED`: upload, parser, staging durable aislado,
validación, errores, preview, hash, RBAC y confirmación autoritativa están
implementados y probados. Es un rediseño, no una réplica del staging global legacy
`CargaPedimentosIE`. `LEGACY-017` es `PARTIAL`: PED-001..004 y PED-007 están
implementadas; PED-005 y PED-006 siguen sin contrato implementable porque dependen
de `INVENTARIO`, ausente en LIVE.

`LEGACY-019` es `IMPLEMENTED_REDESIGNED`: la rama `TipoOperacion = 2` cubre
upload, validación y confirmación hacia `SALIDAS`/`PSALIDAS` (y `DIRIGIDO` sólo
cuando el contrato legacy lo exige). `LEGACY-057` es `IMPLEMENTED_REDESIGNED`: la
importación autoritativa `TipoOperacion = 1` hacia `IMPORTACIONES`/`PARTIDAS` está
implementada con transacción atómica, idempotencia y locks compatibles con el
motor legacy.

Limitación explícita: la confirmación V1 **no** ejecuta descargos, PEPS, saldos ni
el descargo dirigido generativo, aunque persista `DIRIGIDO` según el contrato de
salida. Importar/exportar no equivale a ejecutar el motor fiscal de descargos.

`LEGACY-054` y `LEGACY-055` son `PARTIAL`: materiales y productos cuentan con
parser `.xls/.xlsx`, validación estructural, hash, errores, preview, RBAC y
staging aislado durable en `app24`; no se ejecutan `CARGA_MATERIALES`,
`CARGA_PRODUCTOS` ni escrituras de confirmación a `MATERIAL`, `FactoresMP`,
`PRODUCTOS` o sus stages legacy.

### Conteo global

- `IMPLEMENTED_EQUIVALENT = 5`
- `IMPLEMENTED_REDESIGNED = 32` (LEGACY-038 Saldos reusando `PR_INFORME_SALDOS` + wrapper read-only)
- `PARTIAL = 15` (incluye LEGACY-023, LEGACY-024 y LEGACY-048 como PARTIAL_EVIDENCE_BLOCKED_CONTRACT; LEGACY-056 conserva alta sin actualización; LEGACY-073 cubre entradas, fracciones, descargas y comparativa A31 read-only)
- `MISSING = 2`
- `BLOCKED_BUSINESS = 4` (LEGACY-038 Saldos paso a IMPLEMENTED_REDESIGNED con PR_INFORME_SALDOS read-only)
- `CONSOLIDATE = 6`
- `UNKNOWN = 15`
- `NOT_REQUIRED = 0`

Estos conteos son cobertura por capacidad, no porcentaje de aplicación terminada.

### Estado de la línea base

- `LEGACY_PARITY_BASELINE_COMPLETE = YES`: todas las capacidades identificadas tienen una clasificación inicial.
- `LEGACY_FUNCTIONAL_CONTRACT_COMPLETE = NO`: permanecen 15 capacidades en `UNKNOWN`, por lo que aún no existe un contrato funcional completo.

La línea base no implica `LEGACY_FUNCTIONAL_PARITY_COMPLETE = YES` ni
`APPLICATION_FUNCTIONALLY_COMPLETE = YES`.

## 5. Diferencia entre ruta y capacidad

- `LEGACY_ROUTES_OBSERVED = 83` proviene de la auditoría E2E consolidada.
- `LEGACY_CAPABILITIES_TOTAL = 79` es el resultado de agrupar capacidades, no de
  contar rutas.
- Las filas con `NOT_CAPTURED` no inventan nombres ASPX.
- La capacidad de administración de permisos aparece en varias superficies
  legacy y se consolida en Perfiles en la aplicación nueva.
- Entradas, Salidas y Materiales Utilizados tienen consulta operativa y además
  aparecen como reportes; se mantienen como capacidades distintas porque el
  contrato, permiso y propósito de la superficie difieren.

## 6. Mapa moderno propuesto

Esta agrupación es una propuesta de navegación y ownership, no una decisión de
implementación:

```text
Catálogos
├── Materiales
├── Productos
├── Estructuras
└── Catálogos auxiliares

Operaciones
├── Entradas / importaciones
├── Salidas / exportaciones
├── Materiales utilizados / trazabilidad
├── Saldos
├── Activo fijo
└── Operaciones especiales

Descargos
├── Consulta histórica
├── Análisis
├── Dirigidos
└── Motor controlado de generación

Interfaces
├── Staging y validación reutilizable
├── Pedimentos
├── Catálogos
├── Facturación
└── Interfaces especiales

Reportes
├── Operativos
├── Consolidados
├── Cumplimiento
└── Exportación XLSX

Cumplimiento
├── Ajuste anual
└── Anexo 30

Administración
├── Usuarios
├── Perfiles y permisos
└── Bitácora
```

### Catálogos auxiliares

Tipos, unidades, categorías, divisiones/almacenes, proveedores, clientes,
agentes aduanales y submaquilas son candidatos a una superficie configurable o a
consultas especializadas. No se construye una arquitectura genérica hasta cerrar
fuente, clave, permisos y comportamiento de cada catálogo.

## 7. Reutilización arquitectónica a evaluar

| Capacidad | `REUSE_BILLING_STAGING_PATTERN` | `REUSE_REPORT_INFRASTRUCTURE` | Motivo |
|---|---|---|---|
| Materiales | YES | NO | Staging, validación y errores son transferibles; el catálogo tiene reglas propias. |
| Productos | YES | NO | `tmpproductos` y `ECargaProducto` son un patrón similar, pero requieren reglas propias. |
| Pedimentos | YES | YES | `CARGAPEDIMENTOSIE`/`ERRORCARGA` y consultas paginadas ya sugieren separación de carga y lectura. |
| Clientes/proveedores | YES | NO | Requiere separar altas/actualizaciones de efectos de facturación. |
| Facturación | YES | YES | El patrón ya existe; la confirmación operativa continúa pendiente. |
| Actas de destrucción | YES | YES | Hay evidencia de carga y resultados, pero faltan layout y reglas. |
| CTM/carta de materiales | YES | YES | Puede compartir errores y exportación, sin asumir el mismo dominio. |
| Reportes operativos | NO | YES | Reportes V1 ya reutilizan consultas, paginación y XLSX. |
| Consolidados | NO | YES | Candidato a reutilizar filtros/exportación cuando se cierre la semántica. |
| Ajuste anual | YES | YES | Reutilización posible, pero primero hay que documentar layouts y reglas. |
| Anexo 30 | NO | YES | La exportación puede compartirse; el modelo regulatorio no. |

`YES` significa candidato de diseño, no implementación ni aprobación funcional.

## 8. SQL y límites de ejecución

La evidencia SQL separa consultas de procesos mutables:

- Consultas nuevas confirmadas: `APP24_Q_MATERIALES_LISTAR`,
  `APP24_Q_PRODUCTOS_LISTAR`, `APP24_Q_ESTRUCTURAS_LISTAR`,
  `APP24_Q_ENTRADAS_LISTAR`, `APP24_Q_SALIDAS_LISTAR`,
  `APP24_Q_MATERIALES_UTILIZADOS_LISTAR`,
  `APP24_Q_ACTIVOS_FIJOS_LISTAR` y queries `app24` administrativas.
- Procesos mutables observados: `CARGAPEDIMENTOS`, `CARGA_PRODUCTOS`,
  `CARGA_MATERIALES`, `CARGA_FACTURAS`, `CARGAFACTURASENPSALIDAS`,
  `CARGAACTAS`, `CARGA_SUBMAQUILA`, `SALDOS*`, `DESCARGATSALIDA*`,
  `DESCARGASALIDAPEPS`, `INFORME_CONCENTRADOSALDOS` y otros.
- Ningún proceso mutable legacy fue ejecutado durante esta fase.
- Las views `v_saldos`, `v_saldosdesp`, `v_descarga` y reportes legacy son
  evidencia técnica, no contratos HTTP aprobados automáticamente.

## 9. Decisiones de alcance preservadas

Las siguientes decisiones de negocio permanecen fuera de esta auditoría:

- `SALDOS = PENDING_BUSINESS`.
- `FACTURACION_CONFIRM = PENDING_BUSINESS`.
- `DASHBOARD_V1 = PENDING_BUSINESS`.

La matriz añade blockers explícitos para algoritmo de descargos, reglas de ajuste
anual y contrato de Anexo 30 cuando la evidencia actual no permite implementar
responsablemente. No se presentan como respuestas de negocio.

## 10. Siguiente uso

Antes de implementar una fila `MISSING`, `BLOCKED_BUSINESS` o `UNKNOWN` se debe
conservar la evidencia nueva que cierre: pantalla, layout, contrato API, fuente
SQL, regla, caso de aceptación y tratamiento de errores. La matriz debe
actualizarse por capacidad, no marcando un módulo completo como terminado por la
existencia de una carpeta.
