# Matriz de cobertura de pantallas legacy vs Angular

## Alcance y regla de evidencia

Auditoría estática del commit c3b4cf0b20ee8d5df928a8e65b0efe83fd90a6db. No ejecuta login legacy, DML/DDL, ni procedimientos mutables. Un componente, controller, SP o prueba unitaria no prueba por sí solo equivalencia pantalla a pantalla. COMPLETE_VERIFIED y CONSOLIDATED_VERIFIED requieren contrato de acciones legacy identificable y evidencia de comportamiento actual.

- LEGACY_ROUTES_REPORTED = 83.
- LEGACY_ROUTES_NOMINALLY_VERIFIED = 2: `rptDeModuloActivoFijo.aspx` e `intPedimentos.aspx`, citadas por el Controller desde `CONTEXTO_CONSOLIDADO_AUDITORIAS_ANEXO24(1).md`. Archivo externo no disponible en workspace; evidencia identifica nombre, no formulario ni comportamiento.
- LEGACY_ROUTES_STILL_UNVERIFIED = 81 por nombre. Las 83 rutas aún requieren contrato de acciones: rol, campos, filtros, botones, paginación, exportación y resultado, mediante observación legacy read-only.
- La matriz base conserva 79 capacidades, no 79 rutas.
- Estados de esta auditoría son más estrictos que clasificación V1 previa: implementaciones existentes sin comparación de todas las acciones se marcan NOT_VERIFIED; bloqueos conservan su causa.

## Inventario Angular actual

ANGULAR_ROUTES_TOTAL = 22 rutas navegables, excluyendo redirect y wildcard. ANGULAR_FUNCTIONAL_SURFACES_TOTAL = 51: 22 rutas + 3 subdatasets auxiliares + 2 tabs socios + 4 tabs importaciones + 2 modos facturación + 18 selectores adicionales de reportes. Diálogos no se cuentan como superficies separadas.

| Ruta | Componente principal | Subvistas/acciones actuales | API o SP principal | Evidencia actual |
|---|---|---|---|---|
| /login | LoginPage | Login | POST auth/login; APP24_Q_USUARIO | auth spec; no CI E2E |
| /forbidden | ForbiddenComponent | Denegación | Guard | route smoke |
| /dashboard | DashboardPage | Resumen/enlaces | Dashboard API | route smoke |
| /materiales | MaterialListPage | filtros/paginación | APP24_Q_MATERIALES_LISTAR | route smoke, unit |
| /productos | ProductListPage | filtros/paginación | APP24_Q_PRODUCTOS_LISTAR | route smoke, unit |
| /estructuras | StructureListPage | filtros/paginación | APP24_Q_ESTRUCTURAS_LISTAR | route smoke, unit |
| /catalogos | AuxiliaryCatalogsPage | tipos, unidades, categorías, almacenes | cuatro APP24_Q listar | unit; sin browser smoke |
| /catalogos/datos-generales | CompanyGeneralDataPage | consulta | APP24_Q_DATOS_GENERALES_OBTENER | API/unit; sin browser smoke |
| /catalogos/socios-comerciales | BusinessPartiesPage | clientes/proveedores/agentes | tres APP24_Q listar | unit; sin browser smoke |
| /catalogos/importaciones | CatalogImportPage | 5 tipos, preview, CSV errores, confirmar | app24 staging + APP24_C confirmar | unit; sin browser smoke |
| /operaciones/entradas | EntryListPage | periodo/filtros/página | APP24_Q_ENTRADAS_LISTAR | route smoke, unit |
| /operaciones/salidas | ExitListPage | periodo/filtros/página | APP24_Q_SALIDAS_LISTAR | route smoke, unit |
| /operaciones/materiales-utilizados | UsedMaterialListPage | periodo/filtros/página | APP24_Q_MATERIALES_UTILIZADOS_LISTAR | route smoke, unit |
| /operaciones/activos-fijos | FixedAssetListPage | periodo/filtros/página | APP24_Q_ACTIVOS_FIJOS_LISTAR | route smoke, unit |
| /operaciones/pedimentos | PedimentUploadPage | upload, preview, CSV, confirmación | app24 staging + APP24_C_PEDIMENTO_CONFIRMAR | unit/SQL IT; sin browser smoke |
| /operaciones/actas | ActaUploadPage | upload, preview, CSV, confirmación | app24 staging + APP24_C_ACTA_CARGA_CONFIRMAR | unit/SQL; sin browser smoke |
| /operaciones/constancias | ConstanciaUploadPage | upload, preview, CSV, confirmación | app24 staging + APP24_C_CONSTANCIA_CARGA_CONFIRMAR | unit/SQL; sin browser smoke |
| /facturacion | BillingUploadPage | nueva carga, historial, detalle/deep-link | app24 APP24_Q/C_FACTURACION | unit + IDOR backend; browser sólo plantilla |
| /reportes | ReportListPage | 19 datasets, filtros, paginación, XLSX | APP24_Q y exportadores | unit; browser sólo selector |
| /usuarios | UserListPage | CRUD, estado, perfil, vigencia, password | app24 APP24_Q/C_USUARIO | E2E existente, no CI |
| /perfiles | ProfileManagementPage | crear, nombre, estado, permisos | app24 APP24_Q/C_PERFIL | E2E existente, no CI |
| /bitacora | AuditLogListPage | filtros, paginación, CSV | APP24_Q_BITACORA_LISTAR | browser mínimo + unit |

## Inventario legacy nominal

La auditoría funcional original observó Web Forms autenticado con ADMINISTRADOR: datos generales, materiales, productos, estructuras, operaciones por periodo, gestión, cinco reportes y facturación. Sólo conserva campos concretos para Datos generales (razón social, RFC, IMMEX, domicilio), columnas de catálogos y acciones de Facturación (CARGAR, GUARDAR, LIMPIAR, DESCARGAR). No conserva URLs ASPX, formularios completos, permisos por pantalla, ni totalidad de botones de las 83 rutas.

| Ruta ASPX real identificada | Pantalla | Campos/acciones verificadas | Fuente |
|---|---|---|---|
| `rptDeModuloActivoFijo.aspx` | Nombre de ruta observado; contrato no recuperado | Error de carga histórico; no inferir campos, permisos ni acciones | Controller: `CONTEXTO_CONSOLIDADO_AUDITORIAS_ANEXO24(1).md` (externo) |
| `intPedimentos.aspx` | Nombre de ruta observado; contrato no recuperado | Error de carga histórico; no inferir campos, permisos ni acciones | Controller: `CONTEXTO_CONSOLIDADO_AUDITORIAS_ANEXO24(1).md` (externo) |
| Otras 81 rutas reportadas | Catálogo nominal pendiente | No inventar rutas, formularios ni permisos | `auditoria-sistema-actual.md` y matriz V1 |

## Matriz de capacidades funcionales y cobertura de pantalla

La columna Capability de fuente expresa propósito/acción legacy. New equivalent, Frontend, API y DB/SP fueron contrastados contra código actual. State fue normalizado para auditoría; no altera alcance V1.

| ID | Área | Pantalla/ruta legacy | Acciones legacy | Evidencia | Ruta/acciones nuevas | Frontend | API | DB/SP | Estado | Prioridad | Brecha | Destino |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| LEGACY-001 | Catálogos y maestros | `NOT_CAPTURED` — Datos generales | Consultar y mantener datos generales de la empresa | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/catalogos/datos-generales` — consulta read-only de la ficha empresarial | `CompanyGeneralDataPage` | `GET /api/v1/catalogos/datos-generales` | `dbo.DatosGenerales`, `dbo.APP24_Q_DATOS_GENERALES_OBTENER` | PARTIAL | P1 | Mantenimiento bloqueado: no hay writer, campos editables, validación ni permiso de guardado confirmados | Catálogos / configuración empresarial |
| LEGACY-002 | Catálogos y maestros | `NOT_CAPTURED` — Materiales | Consultar catálogo de materiales | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/materiales` | `MaterialListPage` | `GET /api/v1/catalogos/materiales` | `dbo.APP24_Q_MATERIALES_LISTAR` | NOT_VERIFIED | P0 | Ninguno interno identificado | Catálogos / materiales |
| LEGACY-003 | Catálogos y maestros | `NOT_CAPTURED` — Productos | Consultar catálogo de productos | AUDIT_UI, SQL_METADATA, CODE | `/productos` | `ProductListPage` | `GET /api/v1/catalogos/productos` | `dbo.APP24_Q_PRODUCTOS_LISTAR` | NOT_VERIFIED | P0 | Ninguno interno identificado | Catálogos / productos |
| LEGACY-004 | Catálogos y maestros | `NOT_CAPTURED` — Estructuras | Consultar estructura o BOM | AUDIT_UI, SQL_METADATA, CODE | `/estructuras` | `StructureListPage` | `GET /api/v1/catalogos/estructuras` | `dbo.APP24_Q_ESTRUCTURAS_LISTAR` | NOT_VERIFIED | P1 | El snapshot SQL tenía cero estructuras | Catálogos / estructuras |
| LEGACY-005 | Catálogos y maestros | `NOT_CAPTURED` — Tipos de material | Consultar catálogo de tipos de material | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Tipos de material | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/tipos-material` | `dbo.TipoMaterial`, `dbo.APP24_Q_TIPOS_MATERIAL_LISTAR` | NOT_VERIFIED | P2 | Sólo lectura V1; no CRUD | Catálogos auxiliares |
| LEGACY-006 | Catálogos y maestros | `NOT_CAPTURED` — Unidades | Consultar catálogo de unidades | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Unidades | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/unidades` | `dbo.unidad`, `dbo.APP24_Q_UNIDADES_LISTAR` | NOT_VERIFIED | P2 | Sólo lectura V1; no CRUD | Catálogos auxiliares |
| LEGACY-007 | Catálogos y maestros | `NOT_CAPTURED` — Categorías | Consultar categorías y temporalidad | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Categorías | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/categorias` | `dbo.categorias`, `dbo.APP24_Q_CATEGORIAS_LISTAR` | NOT_VERIFIED | P2 | Sólo lectura V1; no CRUD | Catálogos auxiliares |
| LEGACY-008 | Catálogos y maestros | `NOT_CAPTURED` — Divisiones/almacenes | Consultar divisiones o almacenes | SQL_METADATA, CODE, RUNTIME | `/catalogos` — Almacenes (División resuelta a almacén, sin catálogo separado) | `AuxiliaryCatalogPage` | `GET /api/v1/catalogos/almacenes` | `dbo.almacen`, `dbo.APP24_Q_ALMACENES_LISTAR`; `dbo.ENTIDAD(@DIVISION)` → `ALMACENKEY` | NOT_VERIFIED | P1 | Semántica División = Almacén demostrada; sin CRUD | Catálogos auxiliares |
| LEGACY-009 | Catálogos y maestros | `NOT_CAPTURED` — Proveedores | Consultar proveedores | AUDIT_UI, SQL_METADATA, CODE | `/catalogos/socios-comerciales` — Proveedores | `BusinessPartiesPage` | `GET /api/v1/catalogos/proveedores` | `dbo.Proveedores`, `dbo.APP24_Q_PROVEEDORES_LISTAR` | NOT_VERIFIED | P1 | Sólo lectura V1; importación/confirmación fuera de alcance | Catálogos / socios comerciales |
| LEGACY-010 | Catálogos y maestros | `NOT_CAPTURED` — Clientes | Consultar clientes | SQL_METADATA, CODE | `/catalogos/socios-comerciales` — Clientes | `BusinessPartiesPage` | `GET /api/v1/catalogos/clientes` | `dbo.clientes`, `dbo.APP24_Q_CLIENTES_LISTAR` | NOT_VERIFIED | P1 | Sólo lectura V1; importación/confirmación fuera de alcance | Catálogos / socios comerciales |
| LEGACY-011 | Catálogos y maestros | `NOT_CAPTURED` — Agentes aduanales | Consultar agentes aduanales | AUDIT_UI, SQL_METADATA, CODE | `/catalogos/socios-comerciales` — Agentes aduanales | `BusinessPartiesPage` | `GET /api/v1/catalogos/agentes-aduanales` | `dbo.agentes`, `dbo.APP24_Q_AGENTES_ADUANALES_LISTAR` | NOT_VERIFIED | P2 | Sólo lectura V1; importación fuera de alcance | Catálogos / socios comerciales |
| LEGACY-012 | Catálogos y maestros | `NOT_CAPTURED` — Submaquilas | Consultar submaquilas | SQL_METADATA | No identificado | No | No | `RelacionSubmaquila`, `Encabezadotransubmaquila`, `Detalletransubmaquila`, `TMPSUBMAQUILA` (0 filas), `CARGA_SUBMAQUILA` no ejecutado; `SUBMAQUILA_CATALOG_CONTRACT = NOT_A_MASTER_CATALOG` | BLOCKED_EVIDENCE | P2 | Sólo transacciones/transferencia; no hay maestro canónico | Catálogos auxiliares |
| LEGACY-013 | Catálogos y maestros | `NOT_CAPTURED` — Activo fijo | Consultar partidas marcadas como activo fijo | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/activos-fijos` | `FixedAssetListPage` | `GET /api/v1/operaciones/activos-fijos` | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` | NOT_VERIFIED | P1 | La granularidad nueva es partida, no activo individual | Operaciones / activo fijo |
| LEGACY-014 | Catálogos y maestros | `NOT_CAPTURED` — Consultas guardadas | Guardar y reutilizar filtros estructurados propios | SQL_METADATA, CODE, RUNTIME | Control reutilizable en Reportes y consultas operativas | `SavedQueriesControlComponent` | `GET/POST/PUT/DELETE /api/v1/consultas-guardadas` | `ANEXO24_DEV.app24.ConsultaGuardada`; `app24.APP24_Q/C_CONSULTA_GUARDADA_*` | NOT_VERIFIED | P3 | SQL legacy no se migra ni ejecuta; filtros estructurados propios implementados | Consultas estructuradas propias |
| LEGACY-015 | Operación aduanera/inventario | `NOT_CAPTURED` — Entradas / Importaciones | Consultar entradas e importaciones por rango y filtros | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/entradas` | `EntryListPage` | `GET /api/v1/operaciones/entradas` | `dbo.APP24_Q_ENTRADAS_LISTAR` | NOT_VERIFIED | P0 | Ninguno interno identificado | Operaciones / entradas |
| LEGACY-016 | Operación aduanera/inventario | `NOT_CAPTURED` — Carga de pedimentos | Cargar pedimentos desde staging | SQL_METADATA, CODE, RUNTIME | `/operaciones/pedimentos` — staging, preview y confirmación | `PedimentUploadPage` | `POST/GET /api/v1/operaciones/pedimentos/cargas`, `POST .../cargas/{id}/confirmacion` | `app24.APP24_C_PEDIMENTO_CARGA_CREAR`, `APP24_Q_PEDIMENTO_CARGA_POR_HASH`, `APP24_Q_PEDIMENTO_CARGA_OBTENER`, `APP24_Q_PEDIMENTO_CARGA_ERRORES`, `dbo.APP24_C_PEDIMENTO_CONFIRMAR` | NOT_VERIFIED | P0 | Rediseñado con staging aislado propio; no replica el staging global legacy `CargaPedimentosIE` | Importaciones / staging seguro |
| LEGACY-017 | Operación aduanera/inventario | `NOT_CAPTURED` — Validación de pedimentos | Validar pedimentos antes de procesar | SQL_METADATA, CODE | `/operaciones/pedimentos` — validación batch read-only durante staging | `PedimentUploadPage` | `POST /api/v1/operaciones/pedimentos/cargas` | `APP24_Q_PEDIMENTO_VALIDAR_REGLAS`, `VALIDAPEDIMENTO`, `VALIDA_I_DETALLENP` | PARTIAL | P0 | PED-001..004 y PED-007 confirmadas; PED-005/PED-006 siguen sin contrato implementable porque `INVENTARIO` no existe LIVE | Importaciones |
| LEGACY-018 | Operación aduanera/inventario | `NOT_CAPTURED` — Salidas / Exportaciones | Consultar salidas y líneas por rango y filtros | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/salidas` | `ExitListPage` | `GET /api/v1/operaciones/salidas` | `dbo.APP24_Q_SALIDAS_LISTAR` | NOT_VERIFIED | P0 | Ninguno interno identificado | Operaciones / salidas |
| LEGACY-019 | Operación aduanera/inventario | `NOT_CAPTURED` — Carga de exportaciones | Cargar o procesar operaciones de salida | SQL_METADATA, CODE | `/operaciones/pedimentos` — mismo staging, preview y confirmación para `TipoOperacion = 2` | `PedimentUploadPage` | `POST/GET /api/v1/operaciones/pedimentos/cargas`, `POST .../cargas/{id}/confirmacion` | `CargaPedimento`, `CargaPedimentoFila`, `ErrorCargaPedimento`; `dbo.APP24_C_PEDIMENTO_CONFIRMAR` (`SALIDAS`/`PSALIDAS`/`DIRIGIDO` condicional) | NOT_VERIFIED | P1 | No ejecuta descargos, PEPS ni saldos; sólo persistencia autoritativa de la operación | Importaciones y operaciones |
| LEGACY-020 | Operación aduanera/inventario | `NOT_CAPTURED` — Materiales utilizados | Consultar asignaciones históricas entrada → salida → material | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/materiales-utilizados` | `UsedMaterialListPage` | `GET /api/v1/operaciones/materiales-utilizados` | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | NOT_VERIFIED | P0 | No ejecuta descargos ni recalcula saldos | Operaciones / trazabilidad |
| LEGACY-021 | Operación aduanera/inventario | `NOT_CAPTURED` — Saldos | Consultar saldo con semántica fiscal y corte definidos | AUDIT_UI, SQL_METADATA | Reporte `Saldos` deshabilitado; sin contrato | Parcial sólo como opción no disponible | No hay endpoint de saldo aprobado | `PR_INFORME_SALDOS`, `v_saldos`, `v_saldosdesp` como candidatos | BLOCKED_BUSINESS | P0 | Fórmula, fuentes, granularidad y corte pendientes | Operaciones / saldos |
| LEGACY-022 | Operación aduanera/inventario | `NOT_CAPTURED` — Activo fijo | Consultar activos derivados de entradas | AUDIT_UI, SQL_METADATA, CODE | `/operaciones/activos-fijos` | `FixedAssetListPage` | `GET /api/v1/operaciones/activos-fijos` | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` | NOT_VERIFIED | P1 | No representa alta/baja/retorno de activo individual | Operaciones / activo fijo |
| LEGACY-023 | Operación aduanera/inventario | `NOT_CAPTURED` — Cambios de régimen | Procesar o consultar cambios de régimen | SQL_METADATA | No identificado | No | No | Referencias F4/F5/A3/DE y procesos legacy; contrato no cerrado | BLOCKED_EVIDENCE | P1 | Pantalla confirmada a nivel de auditoría; faltan ruta, contrato visible, fuente canónica y flujo autorizado | Operaciones especiales |
| LEGACY-024 | Operación aduanera/inventario | `NOT_CAPTURED` — Regularizaciones | Procesar regularizaciones | SQL_METADATA | No identificado | No | No | `INSERTAPEDIMENTO` y procesos relacionados; contrato no cerrado | BLOCKED_EVIDENCE | P1 | Pantalla confirmada a nivel de auditoría; faltan ruta, contrato visible, fuente canónica, regla y aceptación | Operaciones especiales |
| LEGACY-025 | Operación aduanera/inventario | NOT_CAPTURED — Actas de destrucción | Cargar y procesar actas de destrucción | SQL_METADATA, CODE, TEST | /operaciones/actas — staging, preview y confirmación | ActaUploadPage | POST/GET /api/v1/operaciones/actas/importaciones, POST /api/v1/operaciones/actas/{cargaId}/confirmacion | app24 CargaActa*; dbo.APP24_C_ACTA_CARGA_CONFIRMAR -> dbo.CARGAACTAS | NOT_VERIFIED | P1 | Reutiliza la regla legacy en wrapper transaccional; conserva side effects documentados | Operaciones especiales |
| LEGACY-026 | Operación aduanera/inventario | `NOT_CAPTURED` — Transferencias de submaquila | Cargar o procesar transferencias | SQL_METADATA | No identificado | No | No | `CARGA_SUBMAQUILA`, referencias de submaquila/CTM | BLOCKED_BUSINESS | P1 | Contrato y reglas fiscales no cerrados | Operaciones especiales |
| LEGACY-027 | Operación aduanera/inventario | NOT_CAPTURED — Constancias | Cargar y procesar constancias | SQL_METADATA, CODE, TEST | /operaciones/constancias — staging, preview y confirmación | ConstanciaUploadPage | POST/GET /api/v1/operaciones/constancias/importaciones, POST /api/v1/operaciones/constancias/{cargaId}/confirmacion | app24 CargaConstancia*; dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR -> dbo.CARGACONSTANCIAS | NOT_VERIFIED | P2 | Reutiliza validaciones/efectos legacy dentro de una transacción y falla cerrado ante error stage ajeno | Operaciones especiales |
| LEGACY-028 | Operación aduanera/inventario | `NOT_CAPTURED` — CTM | Consultar o procesar CTM | SQL_METADATA | No identificado | No | No | `CTMDESCARGA`, `SALDOSCTM`, `LIGACTMA`, `LIGACTMFACTURA` | BLOCKED_BUSINESS | P1 | Flujo especializado sin contrato común | Operaciones especiales |
| LEGACY-029 | Descargos y trazabilidad | `NOT_CAPTURED` — Descargo automático | Generar o reprocesar descargos | SQL_METADATA | No identificado | No | No | `DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`, `SALDOS*` | BLOCKED_BUSINESS | P0 | Algoritmo, autorización, rollback e idempotencia pendientes | Descargos / motor controlado |
| LEGACY-030 | Descargos y trazabilidad | `NOT_CAPTURED` — Descargo dirigido | Generar descargo dirigido | SQL_METADATA | No identificado | No | No | `DESCDIRIGIDA`, `SALDOSDIRIGIDOS`, `DIRIGIDO` | BLOCKED_BUSINESS | P1 | Regla dirigida y separación de funciones pendientes | Descargos / dirigidos |
| LEGACY-031 | Descargos y trazabilidad | `NOT_CAPTURED` — Descargo bloqueado | Consultar o resolver operaciones bloqueadas | SQL_METADATA, CODE, RUNTIME | `/reportes` — consulta read-only parcial del snapshot histórico persistente observado | `ReportListPage` | `GET /api/v1/reportes/operaciones-bloqueadas` | `dbo.DESCARGOSBLOQUEADOS`, `dbo.APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR`; `BLOQUEA_DOCUMENTO` como escritor observado | PARTIAL | P1 | Resolver, desbloquear, reprocesar y estado activo fuera de alcance | Descargos / excepciones |
| LEGACY-032 | Descargos y trazabilidad | `NOT_CAPTURED` — Análisis de descarga | Analizar faltantes, trazo y resultado de descarga | SQL_METADATA, CODE, RUNTIME | `/reportes` — análisis read-only parcial de relaciones históricas | `ReportListPage` | `GET /api/v1/reportes/analisis-descargas` | `dbo.V_INFORMEDESCARGAS`, `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; `DESCARGA` como grano físico | PARTIAL | P1 | Sólo relación importación → descarga → salida; faltantes, trazo, saldos fiscales y motor fuera de alcance | Descargos / análisis |
| LEGACY-033 | Descargos y trazabilidad | `NOT_CAPTURED` — Historial por importación | Consultar historial de asignaciones por entrada | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/reportes` — superficie consolidada con filtro textual de importación y relaciones entrada → descarga → salida | `ReportListPage` | `GET /api/v1/reportes/analisis-descargas` | `dbo.V_INFORMEDESCARGAS`, `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; `DESCARGA` como grano físico | NOT_VERIFIED | P1 | No requiere pantalla separada; faltantes/trazo/saldos fiscales siguen fuera | Descargos / consultas |
| LEGACY-034 | Descargos y trazabilidad | `NOT_CAPTURED` — Historial por exportación | Consultar historial de asignaciones por salida | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/reportes` — superficie consolidada con filtro textual de salida y relaciones salida → descarga → entrada | `ReportListPage` | `GET /api/v1/reportes/analisis-descargas` | `dbo.V_INFORMEDESCARGAS`, `dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR`; `DESCARGA` como grano físico | NOT_VERIFIED | P1 | No requiere pantalla separada; faltantes/trazo/saldos fiscales siguen fuera | Descargos / consultas |
| LEGACY-035 | Descargos y trazabilidad | `NOT_CAPTURED` — Consulta de descargos persistidos | Consultar filas físicas de `DESCARGA` sin mutar | SQL_METADATA, CODE, RUNTIME | Materiales Utilizados es la superficie moderna | `/operaciones/materiales-utilizados` | `GET /api/v1/operaciones/materiales-utilizados` | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | NOT_VERIFIED | P0 | No equivale a generar descargos | Operaciones / trazabilidad |
| LEGACY-036 | Reportes y consolidados | `NOT_CAPTURED` — Entradas | Generar y exportar reporte de entradas | AUDIT_UI, CODE | `/reportes`, opción Entradas | `/reportes` | `GET /api/v1/reportes/entradas` y `/entradas/exportacion` | Reutiliza consulta de entradas | NOT_VERIFIED | P1 | Proyección nueva no replica las 76 columnas legacy | Reportes / operativos |
| LEGACY-037 | Reportes y consolidados | `NOT_CAPTURED` — Salidas | Generar y exportar reporte de salidas | AUDIT_UI, CODE | `/reportes`, opción Salidas | `/reportes` | `GET /api/v1/reportes/salidas` y `/salidas/exportacion` | Reutiliza consulta de salidas | NOT_VERIFIED | P1 | Proyección nueva no replica las 49 columnas legacy | Reportes / operativos |
| LEGACY-038 | Reportes y consolidados | `NOT_CAPTURED` — Saldos | Generar y exportar reporte de saldos | AUDIT_UI, SQL_METADATA, CODE, TEST | `/reportes` opción Saldos habilitada | `ReportListPage` | `GET /api/v1/reportes/saldos` y `/saldos/exportacion` | `dbo.PR_INFORME_SALDOS` + wrapper read-only `dbo.APP24_Q_SALDOS_LISTAR` (sin reescritura de formulas legacy) | NOT_VERIFIED | P0 | Read-only; reusa PR_INFORME_SALDOS sin ejecutar SALDOS* ni recalcular saldos. Paridad exacta de superficie/XLSX pendiente de segunda auditoria comparativa (XLSX_PARITY = PENDING_FINAL_COMPARATIVE_AUDIT). DOCUMENT_OVERRIDES_DATE_RANGE = YES. LIVE_ACTIVATION_PENDING = YES. | Reportes / saldos |
| LEGACY-039 | Reportes y consolidados | `NOT_CAPTURED` — Materiales utilizados | Generar y exportar reporte de consumo/descarga | AUDIT_UI, CODE, RUNTIME | `/reportes`, opción Materiales utilizados | `/reportes` | `GET /api/v1/reportes/materiales-utilizados` y exportación | Reutiliza `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | NOT_VERIFIED | P1 | Mantiene grano físico de `DESCARGA` | Reportes / operativos |
| LEGACY-040 | Reportes y consolidados | `NOT_CAPTURED` — Bitácora | Consultar y exportar bitácora | AUDIT_UI, CODE, RUNTIME | `/bitacora` y `/reportes` | `AuditLogListPage`, `ReportListPage` | `GET /api/v1/bitacora`, `/reportes/bitacora` y exportación | `app24.APP24_Q_BITACORA_LISTAR` | NOT_VERIFIED | P1 | La fuente legacy original no está vinculada | Administración / bitácora |
| LEGACY-041 | Reportes y consolidados | `NOT_CAPTURED` — CTM/F4/HDE | Generar reportes especializados CTM/F4/HDE | SQL_METADATA, CODE, RUNTIME | `/reportes` — consulta read-only de líneas dirigidas F4 (CTM/desperdicio) | `ReportListPage` | `GET /api/v1/reportes/f4` y `/f4/exportacion` | `dbo.V_F4CTMA`, `dbo.V_F4DESP`, `dbo.APP24_Q_F4_LISTAR`; LIVE_ROWS = 0 | PARTIAL | P2 | Sólo líneas dirigidas F4; CTM mutable y HDE fuera de alcance (HDE_CONTRACT = NOT_FOUND) | Reportes / cumplimiento |
| LEGACY-042 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de saldos | Generar concentrado de saldos | SQL_METADATA | No identificado | No | No | `INFORME_CONCENTRADOSALDOS` escribe `CONCENTRADOSALDOS` | BLOCKED_BUSINESS | P1 | Semántica de snapshot y fórmula pendientes | Reportes / saldos |
| LEGACY-043 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de materiales | Consolidar materiales | SQL_METADATA, CODE | Catálogo y reportes operativos separados | `/materiales`, `/reportes` | Catálogo/reportes existentes | Fuentes `MATERIAL` y reportes; consolidado específico no cerrado | NOT_APPLICABLE_APPROVED | P2 | Definir si requiere proyección adicional | Reportes / consolidados |
| LEGACY-044 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de productos | Consolidar productos | SQL_METADATA, CODE | Catálogo y reportes operativos separados | `/productos`, `/reportes` | Catálogo/reportes existentes | `PRODUCTOS` y reportes; consolidado específico no cerrado | NOT_APPLICABLE_APPROVED | P2 | Definir campos y propósito | Reportes / consolidados |
| LEGACY-045 | Reportes y consolidados | `NOT_CAPTURED` — Consolidado de estructuras | Consolidar estructuras/BOM | SQL_METADATA, CODE | Catálogo de estructuras; sin reporte separado | `/estructuras` | `GET /api/v1/catalogos/estructuras` | `APP24_Q_ESTRUCTURAS_LISTAR` | NOT_APPLICABLE_APPROVED | P2 | Dataset BOM actual vacío | Reportes / consolidados |
| LEGACY-046 | Reportes y consolidados | `NOT_CAPTURED` — Vencimientos | Consultar vencimientos | SQL_METADATA, CODE, RUNTIME | `/reportes` — vencimientos read-only de desperdicio | `ReportListPage` | `GET /api/v1/reportes/vencimientos` | `dbo.vDESPERDICIOS`, `dbo.APP24_Q_VENCIMIENTOS_LISTAR`; fórmula `DATEADD(month, categorias.meses, Importaciones.Fecha)` | PARTIAL | P1 | Sólo subconjunto de desperdicio; saldos, descargos y estados fuera de alcance | Reportes / cumplimiento |
| LEGACY-047 | Reportes y consolidados | `NOT_CAPTURED` — Compulsa | Ejecutar o consultar compulsa | SQL_METADATA, CODE | `/reportes` — Compulsa y Compulsa - detalle | `ReportListPage` | `GET /api/v1/reportes/compulsa`; `GET /api/v1/reportes/compulsa/detalle` | `dbo.v_compulsa_gen`, `dbo.APP24_Q_COMPULSA_LISTAR`; `dbo.v_compulsa`, `dbo.APP24_Q_COMPULSA_DETALLE_LISTAR` | PARTIAL | P2 | Resumen y detalle read-only implementados; generación, reconciliación mutable y paridad final pendientes. COMPULSA_SUMMARY_READ = IMPLEMENTED; COMPULSA_DETAIL_READ = IMPLEMENTED; COMPULSA_GENERATION = NOT_IMPLEMENTED; COMPULSA_MUTABLE_PROCESSES = NOT_EXECUTED | Reportes / cumplimiento |
| LEGACY-048 | Reportes y consolidados | `NOT_CAPTURED` — Scrap | Consultar scrap o desperdicio | SQL_METADATA, DOC | No identificado | No | No | `DESCARGA_DESPERDICIO`, `DescargaDesp` y variantes | BLOCKED_EVIDENCE | P2 | Pantalla confirmada a nivel de auditoría; faltan ruta, contrato visible y fuente canónica; ver `mapeo-desperdicios.md` | Reportes / cumplimiento |
| LEGACY-049 | Reportes y consolidados | `NOT_CAPTURED` — Dirigidos | Consultar operaciones dirigidas | SQL_METADATA, CODE, RUNTIME | `/reportes` — consulta read-only del subconjunto marcado como dirigido | `ReportListPage` | `GET /api/v1/reportes/dirigidos` | `dbo.V_STATUS_DESCARGAS`, `dbo.APP24_Q_DIRIGIDOS_LISTAR`; filtro estructural `DIRIGIDO = 'SI'` | PARTIAL | P1 | Sólo consulta de líneas marcadas; generación, PEPS, saldos y descargo dirigido permanecen fuera de alcance | Reportes / cumplimiento |
| LEGACY-050 | Reportes y consolidados | `NOT_CAPTURED` — Permisos | Reportar permisos o actividades | CODE | Administración de perfiles y actividades | `/perfiles` | `/api/v1/administracion/perfiles` y `/actividades` | `app24.PerfilApp`, `Actividad`, `PerfilActividad` | NOT_VERIFIED | P1 | No se justificó reporte separado | Administración |
| LEGACY-051 | Reportes y consolidados | `NOT_CAPTURED` — Rectificaciones | Consultar o reportar rectificaciones | SQL_METADATA, CODE, RUNTIME | `/reportes` — Rectificaciones (resumen) y Rectificaciones - detalle | `ReportListPage` | `GET /api/v1/reportes/rectificaciones`; `GET /api/v1/reportes/rectificaciones/detalle` | `dbo.v_total_rectificaciones`, `dbo.APP24_Q_RECTIFICACIONES_LISTAR`; `dbo.v_rectificaciones`, `dbo.APP24_Q_RECTIFICACIONES_DETALLE_LISTAR` | PARTIAL | P2 | Resumen y detalle read-only implementados; procesamiento mutable y paridad final pendientes. RECTIFICATIONS_SUMMARY_READ = IMPLEMENTED; RECTIFICATIONS_DETAIL_READ = IMPLEMENTED; RECTIFICATION_MUTABLE_PROCESSING = NOT_IMPLEMENTED | Reportes / cumplimiento |
| LEGACY-052 | Reportes y consolidados | `NOT_CAPTURED` — Activo fijo | Reportar activos fijos | AUDIT_UI, CODE | Consulta de operaciones de activo fijo y reportes comunes | `/operaciones/activos-fijos`, `/reportes` | Operaciones/reportes; sin reporte especializado | `APP24_Q_ACTIVOS_FIJOS_LISTAR` | NOT_VERIFIED | P1 | No se modela activo individual | Operaciones / reportes |
| LEGACY-053 | Reportes y consolidados | `NOT_CAPTURED` — Exportación de resultados | Descargar resultados en XLSX | AUDIT_UI, CODE, RUNTIME | Exportación común de Reportes V1 | `/reportes` | Cuatro endpoints `*/exportacion` | `ExportadorXlsxReportes`; consultas read-only | NOT_VERIFIED | P1 | No se replican formatos legacy no comprobados | Reportes / exportación |
| LEGACY-054 | Interfaces/importación | NOT_CAPTURED — Materiales | Importar materiales desde archivo | SQL_METADATA, CODE, TEST | /catalogos/importaciones — staging, preview y confirmación | CatalogImportPage | POST/GET /api/v1/catalogos/importaciones/materiales, POST /api/v1/catalogos/importaciones/materiales/{cargaId}/confirmacion | app24 CargaCatalogoMaterial*; dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR -> dbo.CARGA_MATERIALES | NOT_VERIFIED | P1 | Staging aislado y confirmación explícita; reutiliza reglas legacy sin stage global expuesto | Importaciones / catálogos / staging seguro |
| LEGACY-055 | Interfaces/importación | NOT_CAPTURED — Productos | Importar productos desde archivo | SQL_METADATA, CODE, TEST | /catalogos/importaciones — staging, preview y confirmación | CatalogImportPage | POST/GET /api/v1/catalogos/importaciones/productos, POST /api/v1/catalogos/importaciones/productos/{cargaId}/confirmacion | app24 CargaCatalogoProducto*; dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR -> dbo.CARGA_PRODUCTOS | NOT_VERIFIED | P1 | Staging aislado y confirmación explícita; productos existentes se preservan conforme a la regla legacy insert-only | Importaciones / catálogos / staging seguro |
| LEGACY-056 | Interfaces/importación | NOT_CAPTURED — Clientes/proveedores | Importar clientes y proveedores preservando claves existentes | SQL_METADATA, CODE, TEST | /catalogos/importaciones — tabs Clientes y Proveedores, staging, preview y confirmación | CatalogImportPage | POST/GET /api/v1/catalogos/importaciones/{clientes,proveedores}, POST .../{id}/confirmacion | app24 CargaCatalogoCliente*/CargaCatalogoProveedor*; dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR y dbo.APP24_C_PROVEEDOR_CARGA_CONFIRMAR | NOT_VERIFIED | P1 | Altas nuevas confirmadas; claves existentes se conservan porque legacy es INSERT-only | Importaciones / catálogos |
| LEGACY-057 | Interfaces/importación | `NOT_CAPTURED` — Pedimentos | Importar pedimentos | AUDIT_UI, SQL_METADATA, CODE, RUNTIME | `/operaciones/pedimentos` — confirmación autoritativa | `PedimentUploadPage` | `POST .../cargas/{id}/confirmacion` | `dbo.APP24_C_PEDIMENTO_CONFIRMAR`; legacy `CARGAPEDIMENTOS`/`CargaPedimentosIE`/`ERRORCARGA` no reutilizados | NOT_VERIFIED | P0 | No ejecuta descargos ni motor fiscal posterior; importación autoritativa cubierta en V1 | Importaciones / pedimentos |
| LEGACY-058 | Interfaces/importación | `NOT_CAPTURED` — Facturación | Cargar archivo, validar, previsualizar y confirmar efectos operativos | AUDIT_UI, EXCEL, SQL_METADATA, CODE | `/facturacion` cubre carga, validación, preview, errores, hash y staging durable | `BillingUploadPage` | `POST /api/v1/facturacion/cargas`, `GET /cargas/{id}`, plantilla | `app24.CargaFacturacion`, `ErrorCarga`; confirmación legacy no integrada | BLOCKED_BUSINESS | P0 | Staging/preview implementados; confirmation legacy bloqueada por entrypoint, mapping y side effects no cerrados | Facturación / confirmación |
| LEGACY-059 | Interfaces/importación | `NOT_CAPTURED` — Servicios | Importar servicios | SQL_METADATA | No identificado | No | No | Fuente exacta no localizada en contrato versionado | BLOCKED_EVIDENCE | P2 | Evidencia insuficiente | Importaciones / servicios |
| LEGACY-060 | Interfaces/importación | NOT_CAPTURED — Actas de destrucción | Importar actas de destrucción | SQL_METADATA, CODE, TEST | /operaciones/actas — misma superficie de LEGACY-025 para ingreso seguro | ActaUploadPage | POST/GET /api/v1/operaciones/actas/importaciones, POST /api/v1/operaciones/actas/{cargaId}/confirmacion | app24 CargaActa*; dbo.APP24_C_ACTA_CARGA_CONFIRMAR -> dbo.CARGAACTAS | NOT_VERIFIED | P1 | Cubre la interfaz de importación; LEGACY-025 registra el aspecto operativo de la misma confirmación | Importaciones / especiales |
| LEGACY-061 | Interfaces/importación | `NOT_CAPTURED` — Órdenes de fabricación | Importar órdenes de fabricación | SQL_METADATA | No identificado | No | No | Fuente exacta no localizada en contrato versionado | BLOCKED_EVIDENCE | P2 | Evidencia insuficiente | Importaciones / producción |
| LEGACY-062 | Interfaces/importación | `NOT_CAPTURED` — Procesos | Importar procesos | SQL_METADATA | No identificado | No | No | Fuentes exactas no localizadas | BLOCKED_EVIDENCE | P2 | Evidencia insuficiente | Importaciones / procesos |
| LEGACY-063 | Interfaces/importación | `NOT_CAPTURED` — CTM/Carta de materiales | Importar CTM o carta de materiales | SQL_METADATA | No identificado | No | No | `CARGA_SUBMAQUILA`, `CTMDESCARGA` y objetos relacionados | BLOCKED_BUSINESS | P2 | Layout y proceso autoritativo pendientes | Importaciones / CTM |
| LEGACY-064 | Ajuste anual | `NOT_CAPTURED` — Carga ajuste anual | Cargar archivos del ajuste anual | AUDIT_E2E, SQL_METADATA | No identificado | No | No | Fuente exacta no localizada en los contratos actuales | BLOCKED_EVIDENCE | P1 | Layouts, reglas y autoridad no cerrados | Cumplimiento / ajuste anual |
| LEGACY-065 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar informe principal del ajuste anual | AUDIT_E2E | No identificado | No | No | UNKNOWN | BLOCKED_EVIDENCE | P1 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-066 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar sección almacén | AUDIT_E2E | No identificado | No | No | UNKNOWN | BLOCKED_EVIDENCE | P2 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-067 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar sección ventas | AUDIT_E2E | No identificado | No | No | UNKNOWN | BLOCKED_EVIDENCE | P2 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-068 | Ajuste anual | `NOT_CAPTURED` — Informe ajuste anual | Generar secciones 4.3.16 I y II | AUDIT_E2E | No identificado | No | No | UNKNOWN | BLOCKED_EVIDENCE | P2 | Definición de resultado pendiente | Cumplimiento / ajuste anual |
| LEGACY-069 | Anexo 30 | `NOT_CAPTURED` — Informe de descargos | Generar informe Anexo 30 | AUDIT_E2E, SQL_METADATA | No identificado | No | No | `A31_DESCARGAS`, `A31_SALDOS`, `A31_TRAZO`, `DESCARGAS_A31` | BLOCKED_EVIDENCE | P1 | Reglas regulatorias y contrato pendientes | Cumplimiento / Anexo 30 |
| LEGACY-070 | Anexo 30 | `NOT_CAPTURED` — Parámetros de informe | Filtrar por destino aduanero, clave, año y sustitución | AUDIT_E2E | No identificado | No | No | UNKNOWN | BLOCKED_EVIDENCE | P1 | Semántica de filtros pendiente | Cumplimiento / Anexo 30 |
| LEGACY-071 | Anexo 30 | `NOT_CAPTURED` — Worksheet y errores | Revisar worksheet, errores y operaciones faltantes | AUDIT_E2E | No identificado | No | No | UNKNOWN | BLOCKED_EVIDENCE | P1 | Flujo y fuente no cerrados | Cumplimiento / Anexo 30 |
| LEGACY-072 | Anexo 30 | `NOT_CAPTURED` — TXT y Excel | Generar salidas TXT y Excel | AUDIT_E2E, SQL_METADATA | No identificado | No | No | `SP_GENERA_TXT_COMPLETO` es mixto y no se reutiliza | BLOCKED_EVIDENCE | P1 | Formato, seguridad y fuente pendientes | Cumplimiento / Anexo 30 |
| LEGACY-073 | Anexo 30 | NOT_CAPTURED - Revision Anexo 30 | Revisar entradas, inventario inicial, descargas, saldos, comparativa y vencimientos | AUDIT_E2E, CODE, SQL_DUMP, TEST | `/reportes` — revisión read-only de entradas, fracciones, descargas, comparativa e inventario inicial derivado de la vista legacy | ReportListPage | GET /api/v1/reportes/anexo30-revision-entradas; GET /api/v1/reportes/anexo30-revision-fracciones; GET /api/v1/reportes/anexo30-revision-descargas; GET /api/v1/reportes/anexo30-revision-comparativa; GET /api/v1/reportes/anexo30-revision-inventario-inicial | dbo.A31_ENTRADAS; dbo.A31_DESCARGASF; dbo.A31_DESCARGAS; dbo.A31_COMPARATIVADESCARGA; dbo.INVENTARIOINICIAL; dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR; dbo.APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR; dbo.APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR; dbo.APP24_Q_ANEXO30_REVISION_COMPARATIVA_LISTAR; dbo.APP24_Q_ANEXO30_INVENTARIO_INICIAL_LISTAR | PARTIAL | P1 | Entradas, fracciones, descargas, comparativa e inventario inicial read-only; la vista deriva saldos operacionales sin corte fiscal garantizado; tablas A31 vacías no prueban vista vacía; saldos/vencimientos/generación fuera | Cumplimiento / Anexo 30 |
| LEGACY-074 | Seguridad/administración | `NOT_CAPTURED` — Login | Autenticar usuario y cargar permisos | AUDIT_UI, CODE, RUNTIME | `/login` | `AuthService` y `auth.routes.ts` | `POST /api/v1/auth/login` | `app24.APP24_Q_USUARIO_POR_CLAVE`, `APP24_Q_USUARIO_ACCESO` | NOT_VERIFIED | P0 | Ninguno interno identificado | Seguridad |
| LEGACY-075 | Seguridad/administración | `NOT_CAPTURED` — Usuarios | Listar, crear, editar, cambiar estado, perfil, vigencia y password | AUDIT_UI, CODE, RUNTIME | `/usuarios` | `UserListPage` | `/api/v1/administracion/usuarios` y commands | `APP24_Q_*USUARIO*`, `APP24_C_USUARIO_*` | NOT_VERIFIED | P0 | Ninguno interno identificado | Administración / usuarios |
| LEGACY-076 | Seguridad/administración | `NOT_CAPTURED` — Perfiles | Listar y administrar perfiles | AUDIT_UI, CODE, RUNTIME | `/perfiles` | `ProfileManagementPage` | `/api/v1/administracion/perfiles` | `APP24_Q_PERFILES_LISTAR`, `APP24_C_PERFIL_*` | NOT_VERIFIED | P0 | Ninguno interno identificado | Administración / perfiles |
| LEGACY-077 | Seguridad/administración | `NOT_CAPTURED` — Actividades | Consultar catálogo interno de actividades/permisos | AUDIT_UI, CODE | Consolidado dentro de perfiles/permisos | Sin ruta independiente | `GET /api/v1/administracion/actividades` | `APP24_Q_ACTIVIDADES_LISTAR`, `Actividad` | NOT_VERIFIED | P1 | Confirmar si negocio exige pantalla separada | Administración / permisos |
| LEGACY-078 | Seguridad/administración | `NOT_CAPTURED` — Permisos | Asignar permisos a perfiles | AUDIT_UI, CODE, RUNTIME | Consolidado en `/perfiles` | `ProfileManagementPage` | `GET/PUT /api/v1/administracion/perfiles/{id}/permisos` | `PerfilActividad`, `APP24_Q_PERFIL_PERMISOS_LISTAR`, command de reemplazo | NOT_VERIFIED | P0 | Ninguno interno identificado | Administración / permisos |
| LEGACY-079 | Seguridad/administración | `NOT_CAPTURED` — Bitácora | Consultar eventos de auditoría | AUDIT_UI, CODE, RUNTIME | `/bitacora` | `AuditLogListPage` | `GET /api/v1/bitacora` | `app24.BitacoraEvento`, `APP24_Q_BITACORA_LISTAR` | NOT_VERIFIED | P0 | Ninguno interno identificado | Administración / bitácora |

## Resumen estricto de cobertura

| Estado | Conteo | Criterio |
|---|---:|---|
| COMPLETE_VERIFIED | 0 | No hay URL ASPX, contrato completo de acciones y prueba actual de equivalencia. |
| CONSOLIDATED_VERIFIED | 0 | Consolidaciones existentes no pueden acreditarse sin inventario de acciones legacy. |
| PARTIAL | 10 | LEGACY-001,017,031,032,041,046,047,049,051,073. |
| MISSING | 0 | Conteo histórico de capacidades; no concluye cobertura de ventanas. Corrección pantalla/acción identifica 22 equivalentes de interfaz ausentes. |
| BLOCKED_BUSINESS | 8 | LEGACY-021,026,028,029,030,042,058,063. |
| BLOCKED_EVIDENCE | 16 | LEGACY-012,023,024,048,059,061,062,064-072. |
| NOT_APPLICABLE_APPROVED | 3 | LEGACY-043,044,045; decisión V1, no prueba de reemplazo. |
| NOT_VERIFIED | 42 | Código/CI unitario sin comparación completa de acciones legacy. |

## Cobertura E2E y evidencia faltante

- CI exacta 38058585739 ejecutó SP-first, backend test/build y frontend test/lint/build; no ejecutó pnpm e2e.
- routes-smoke.spec.ts enumera 14 rutas. auth.spec.ts cubre /login: Playwright cubre 15/22 rutas por alguna spec.
- Sin browser smoke: /catalogos, /catalogos/datos-generales, /catalogos/socios-comerciales, /catalogos/importaciones, /operaciones/pedimentos, /operaciones/actas, /operaciones/constancias.
- Facturación browser sólo verifica plantilla y ausencia de confirmación; falta upload, historial, filtros, deep-link, preview paginado e IDOR multiusuario.
- Reportes browser sólo verifica selector básico; no genera datasets, no descarga XLSX y no valida contenido. `frontend/e2e/reports.spec.ts` es `STALE_TEST`: espera Saldos disabled mientras `ReportListPage` declara `available: true`, `ReportApiService` consulta y exporta Saldos. CI no ejecuta Playwright.
- E2E administrativo existe con fixtures sintéticos/restauración, pero depende de variables de entorno y tampoco está en CI.
- No hay Playwright para consultas guardadas, importaciones, pedimentos, actas, constancias ni aislamiento multiusuario de facturación.

## Brechas priorizadas

| Prioridad | ID/capacidad | Clasificación | Evidencia necesaria antes de implementar o aprobar |
|---|---|---|---|
| P0 | 017 validación pedimentos | REQUIRES_LEGACY_EVIDENCE | Reglas PED-005/006 y fuente INVENTARIO inexistente LIVE. |
| P0 | 021/038 saldos | REQUIRES_BUSINESS_DECISION + comparativa | Fórmula, corte, fuentes, XLSX legacy, activación autorizada. |
| P0 | 029 descargo automático | REQUIRES_BUSINESS_DECISION | Algoritmo, idempotencia, autorización, transacción/rollback. |
| P0 | 058 confirmación Facturación | REQUIRES_BUSINESS_DECISION | Entry point, mapping, duplicados, clientes/productos, errores y atomicidad. |
| P1 | 001 datos generales editar | REQUIRES_LEGACY_EVIDENCE | Identidad de fila, campos, validación, permiso y writer. |
| P1 | 023/024 | REQUIRES_LEGACY_EVIDENCE | Ruta/formulario, fuente, reglas y proceso autorizado. |
| P1 | 026/028/030 | REQUIRES_BUSINESS_DECISION | Contrato fiscal, roles, efectos/rollback. |
| P1 | QA E2E | IMPLEMENTABLE_NOW | Ruta, acciones, export y multiusuario con fixtures sintéticos. |
| P2 | 012,048,059,061,062,064-072 | REQUIRES_LEGACY_EVIDENCE | Nueva observación autorizada, contract freeze y SP-first. |
| P2 | 014 consultas guardadas | IMPLEMENTABLE_NOW | Browser multiusuario y criterios estructurados sin SQL raw. |

## Conclusión

No existe evidencia suficiente para declarar reemplazo legacy. Clasificar 79 capacidades elimina UNKNOWN, pero no sustituye inventario nominal de 83 ASPX ni demostración pantalla/acción. APPLICATION_COMPLETE = NO.


## Corrección Sprint 13 — separación ventana, acción y capacidad

Esta sección sustituye cualquier lectura de `MISSING = 0` como cobertura total. Los 79 `LEGACY-*` son capacidades; las 83 rutas ASPX son ventanas observadas. No existe relación uno-a-uno demostrada entre ambos inventarios.

### Evidencia nominal de ventanas legacy

| Clase | Cantidad | Estado | Evidencia y límite |
|---|---:|---|---|
| Rutas Web Forms reportadas | 83 | `UI_NOT_VERIFIED` | Conteo de auditoría consolidada. |
| Rutas ASPX nominalmente identificadas | 2 | `UI_NOT_VERIFIED` | `rptDeModuloActivoFijo.aspx`, `intPedimentos.aspx`; ambas con error de carga histórico. Nombre no demuestra formulario ni acción. |
| Rutas aún sin nombre nominal | 81 | `UI_NOT_VERIFIED` | Requieren nueva observación legacy autorizada y read-only. |
| Contratos de acción legacy demostrados | 0/83 | `NOT_VERIFIED` | Aún faltan campos, botones, filtros, permisos, paginación, exportación y efecto por ventana. |

No hubo acceso al sistema legacy ni al archivo externo citado durante esta corrección. Próxima evidencia requerida: catálogo nominal obtenido por navegación autenticada read-only, con captura reproducible por ruta. No se deben ejecutar cargas, confirmaciones ni otros POST o acciones mutables.

### Inventario Angular efectivo por ruta y acción

Leyenda: `UI_PRESENT` confirma control estático; `API_PRESENT` y `SP_PRESENT` confirman contrato código/SP; `TEST_PRESENT` nombra prueba existente. `BEHAVIOR_VERIFIED` requiere ejecución reproducible: componente o endpoint no basta.

| Ruta Angular | Superficies y acciones observadas | UI | API / SP | Prueba actual | Comportamiento |
|---|---|---|---|---|---|
| `/login` | abrir; enviar credenciales; crear sesión | `UI_PRESENT` | `API_PRESENT` / `SP_PRESENT` | `auth.spec.ts` | `NOT_VERIFIED` fuera de CI |
| `/forbidden` | mostrar denegación; navegar atrás | `UI_PRESENT` | guard, sin SP | route smoke | `NOT_VERIFIED` fuera de CI |
| `/dashboard` | abrir resumen; navegar enlaces | `UI_PRESENT` | `API_PRESENT` / contrato dashboard | route smoke | `NOT_VERIFIED` fuera de CI |
| `/materiales` | listar; filtrar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_MATERIALES_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/productos` | listar; filtrar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_PRODUCTOS_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/estructuras` | listar; filtrar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_ESTRUCTURAS_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/catalogos` | cambiar dataset; listar tipos, unidades, categorías, almacenes | `UI_PRESENT` | `API_PRESENT` / cuatro `APP24_Q_*_LISTAR` | unit | `NOT_TESTED` |
| `/catalogos/datos-generales` | consultar ficha; editar/guardar ausente | `UI_PARTIAL` | `API_PRESENT` / `APP24_Q_DATOS_GENERALES_OBTENER` | API/unit | consulta `NOT_TESTED`; edición `UI_MISSING` |
| `/catalogos/socios-comerciales` | cambiar cliente/proveedor/agente; listar | `UI_PRESENT` | `API_PRESENT` / tres `APP24_Q_*_LISTAR` | unit | `NOT_TESTED` |
| `/catalogos/importaciones` | seleccionar tipo; cargar; validar; preview; errores CSV; confirmar | `UI_PRESENT` | `API_PRESENT` / staging + `APP24_C_*_CARGA_CONFIRMAR` | unit/SQL | `NOT_TESTED` |
| `/operaciones/entradas` | filtrar periodo; listar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_ENTRADAS_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/operaciones/salidas` | filtrar periodo; listar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_SALIDAS_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/operaciones/materiales-utilizados` | filtrar periodo; listar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/operaciones/activos-fijos` | filtrar periodo; listar; paginar | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_ACTIVOS_FIJOS_LISTAR` | unit + route smoke | `NOT_VERIFIED` fuera de CI |
| `/operaciones/pedimentos` | cargar; validar; preview; errores CSV; confirmar | `UI_PRESENT` | `API_PRESENT` / staging + `APP24_C_PEDIMENTO_CONFIRMAR` | unit/SQL | `NOT_TESTED` |
| `/operaciones/actas` | cargar; validar; preview; errores CSV; confirmar | `UI_PRESENT` | `API_PRESENT` / staging + `APP24_C_ACTA_CARGA_CONFIRMAR` | unit/SQL | `NOT_TESTED` |
| `/operaciones/constancias` | cargar; validar; preview; errores CSV; confirmar | `UI_PRESENT` | `API_PRESENT` / staging + `APP24_C_CONSTANCIA_CARGA_CONFIRMAR` | unit/SQL | `NOT_TESTED` |
| `/facturacion` | nueva carga; plantilla; validar; preview/error; historial propio; detalle/deep-link; paginar; confirmar ausente | `UI_PARTIAL` | `API_PRESENT` / `APP24_Q/C_FACTURACION_*` | unit + API IDOR | `NOT_VERIFIED`; confirmación `UI_MISSING` |
| `/reportes` | seleccionar 19 datasets; filtrar; generar; paginar; exportar XLSX; criterios guardados | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_*` y exportadores | unit + `reports.spec.ts` | `NOT_VERIFIED`; `STALE_TEST` para Saldos |
| `/usuarios` | listar/buscar/paginar; crear; editar; perfil; vigencia; password; activar/inactivar; CSV | `UI_PRESENT` | `API_PRESENT` / `APP24_Q/C_USUARIO_*` | unit + E2E fuente | `NOT_VERIFIED` fuera de CI |
| `/perfiles` | listar; crear; editar nombre; activar; permisos | `UI_PRESENT` | `API_PRESENT` / `APP24_Q/C_PERFIL_*` | unit + E2E fuente | `NOT_VERIFIED` fuera de CI |
| `/bitacora` | filtrar; paginar; reintentar; exportar CSV | `UI_PRESENT` | `API_PRESENT` / `APP24_Q_BITACORA_LISTAR` | unit + browser mínimo | `NOT_VERIFIED` fuera de CI |

`ANGULAR_ROUTES_VERIFIED = 22`; `ANGULAR_FUNCTIONAL_SURFACES_VERIFIED = 51` (22 rutas, 3 datasets auxiliares, 2 tabs socios, 4 tabs importación, 2 modos facturación y 18 selectores adicionales de reportes). Verificado significa inventariado en código, no comportamiento equivalente legacy.

### Estados independientes y conteos reconciliados

**Relación UI de las 79 capacidades con una superficie Angular.** No demuestra ruta ASPX equivalente.

| Estado de interfaz | Conteo | IDs / criterio |
|---|---:|---|
| `UI_PRESENT` | 47 | Existe superficie Angular, aunque acciones no estén verificadas. |
| `UI_PARTIAL` | 10 | 001, 017, 031, 032, 041, 046, 047, 049, 051, 073. |
| `UI_MISSING` | 22 | 012, 023, 024, 026, 028, 029, 030, 042, 048, 059, 061, 062, 063, 064–072. |
| `UI_NOT_VERIFIED` | 0 | No oculta faltantes; incertidumbre de equivalencia queda en 83 ventanas legacy. |
| **Total capacidades** | **79** | Inventario de capacidad, no ventanas. |

**Estado funcional de las 79 capacidades.** `FUNCTIONAL_MISSING = 0` no afirma cobertura legacy: casos sin UI están clasificados por bloqueo.

| Estado funcional | Conteo | Criterio |
|---|---:|---|
| `FUNCTIONAL_VERIFIED` | 0 | Sin contrato completo de acciones legacy y ejecución equivalente. |
| `FUNCTIONAL_PARTIAL` | 10 | Acciones read-only/staging disponibles, cobertura total no demostrada. |
| `FUNCTIONAL_MISSING` | 0 | No sustituye `UI_MISSING`, `BLOCKED_BUSINESS` o `BLOCKED_EVIDENCE`. |
| `BLOCKED_BUSINESS` | 8 | 021, 026, 028, 029, 030, 042, 058, 063. |
| `BLOCKED_EVIDENCE` | 16 | 012, 023, 024, 048, 059, 061, 062, 064–072. |
| `NOT_VERIFIED` | 45 | 42 previas más 043–045; no hay exclusión de reemplazo aprobada. |
| **Total capacidades** | **79** | Estados mutuamente excluyentes. |


### Corrección E2E y CI

- `routes-smoke.spec.ts` enumera 14 de 22 rutas; `auth.spec.ts` añade `/login`. Cobertura de navegación fuente: 15/22. Sin smoke: `/catalogos`, `/catalogos/datos-generales`, `/catalogos/socios-comerciales`, `/catalogos/importaciones`, `/operaciones/pedimentos`, `/operaciones/actas`, `/operaciones/constancias`.
- Existen specs administración, facturación, bitácora, responsive y reportes. `.github/workflows/ci.yml` no ejecuta `pnpm e2e`; ninguna acción E2E tiene resultado CI verificable.
- `frontend/e2e/reports.spec.ts` es `STALE_TEST`: exige Saldos deshabilitado. `ReportListPage` lo habilita y `ReportApiService` consulta/exporta el contrato actual. No se debe deshabilitar producto para satisfacer prueba histórica.
- `E2E_ACTION_GAPS = 22` flujos principales sin evidencia E2E ejecutada en CI, además de subacciones carga/confirmación, detalle, exportación, RBAC e IDOR. No mide botones legacy todavía desconocidos.

### Brechas de interfaz y acción visibles

| Brecha | Ruta/superficie | Acción | Estado | Prioridad | Evidencia necesaria |
|---|---|---|---|---|---|
| `GAP-UI-001` | `/catalogos/datos-generales` | editar y guardar | `UI_MISSING` + `BLOCKED_EVIDENCE` | P1 | identidad fila, campos, reglas, permiso y writer legacy seguro. |
| `GAP-UI-017` | `/operaciones/pedimentos` | validar PED-005/PED-006 | `UI_PARTIAL` + `BLOCKED_EVIDENCE` | P0 | fuente y reglas legacy de `INVENTARIO`. |
| `GAP-UI-021` | `/reportes` | Saldos con paridad fiscal y XLSX | `UI_PRESENT` + `NOT_VERIFIED` | P0 | aceptación fórmula/corte/activación y comparación legacy. |
| `GAP-UI-029` | sin ruta Angular | generar/reprocesar descargo automático | `UI_MISSING` + `BLOCKED_BUSINESS` | P0 | algoritmo, permisos, idempotencia, transacción, concurrencia y rollback. |
| `GAP-UI-058` | `/facturacion` | confirmar efectos operativos | `UI_MISSING` + `BLOCKED_BUSINESS` | P0 | entrypoint, mapping, duplicados, side effects, atomicidad y retry. |
| `GAP-E2E-001` | 22 rutas | navegación y acción crítica browser/CI | `NOT_TESTED` | P0 | fixtures aislados, credenciales/entorno CI y resultado reproducible. |
| `GAP-E2E-002` | `/reportes` | Saldos disponible/generar/exportar | `STALE_TEST` | P0 | actualizar spec contra contrato actual y ejecutar sintético. |
| `GAP-E2E-003` | `/facturacion` | upload, historial, deep-link, paginación e IDOR multicuenta | `NOT_TESTED` | P1 | fixtures de dos usuarios y cargas aisladas. |

## Conclusión corregida

`APPLICATION_COMPLETE = NO`. `READY_FOR_LEGACY_REPLACEMENT = NO`. Dos nombres nominales reducen vacío de rutas de 83 a 81, pero no prueban equivalencia de una ventana. Interfaces Angular presentes no sustituyen contrato legacy, prueba de comportamiento ni ejecución Playwright en CI.
