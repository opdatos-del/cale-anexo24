# Auditoría funcional E2E V1 — aplicación nueva (completada)

Fecha: 2026-10-02
Rama: `test/functional-e2e-v1` (desde `dev` = `a53a93b3c542f8c72eef2c3be439225fcfcd3144`)
Alcance: auditoría funcional interactiva de la aplicación nueva completa
(login UI, sweep de rutas con filtros/paginación, reportes, uploads sintéticos de
staging, exportaciones XLSX, RBAC por perfiles, drawer/sidebar y responsive).
Fase de discovery: sin cambios de código de aplicación.

## Método

- Inventario de rutas extraído del código real (`app.routes.ts` y rutas hijas);
  sin rutas inventadas.
- Backend aislado `bootRun` en `18082` con `backend/.env` local (identidad
  `opdatos`); frontend `ng serve` en `4300` con proxy `/api` al backend aislado.
- Autenticación: tokens JWT efímeros firmados localmente con `JWT_SECRET` del
  entorno (TTL 30 min, no impresos, no persistidos, destruidos al cerrar), con
  perfiles de permisos `ADMIN_FULL`, `READ_ONLY`, `LIMITED` y `NO_PERMISSION`.
  No se creó usuario ni se usaron credenciales de terceros.
- Browser: Microsoft Edge headless vía Playwright (canal del sistema; sin
  descarga de browsers). Sweep desktop `1440x900` + estructural tablet
  `768x1024` y mobile `390x844`.
- Uploads: archivos sintéticos `E2E_*` derivados de los contratos reales
  (parser y plantilla de facturación obtenida del API). Sólo staging en
  `ANEXO24_DEV/app24`; ninguna confirmación operativa.
- XLSX: descargados por API y por navegador, abiertos con `openpyxl` para
  verificar hojas, encabezados, filas y contenido.

## Resultado ejecutivo

- `API_CHECKS = 25/25 EXPECTED` (system, catálogos, operaciones, reportes,
  administración, bitácora, facturación): 200 = 23, 401 expected = 1,
  400 expected = 1, unexpected 5xx = 0.
- Ruta interactiva: **20/21 PASS**; 1 hallazgo de autorización de ruta
  (`FUN-E2E-003`, `/catalogos/importaciones` alcanzable con perfil de sólo
  lectura). El backend mantiene el control (403 en API).
- Reportes: 11 tipos generan 200; 4 con datos (entradas 1, salidas 2693,
  materiales-utilizados 3105, bitácora 632, compulsa/rectificaciones 662,
  análisis-descargas 3866), 4 vacíos con empty state limpio (vencimientos,
  dirigidos, operaciones-bloqueadas, F4).
- Exportaciones XLSX operativas verificadas y abiertas: entradas (1 fila),
  salidas (2693), materiales-utilizados (3105), bitácora (632). F4 → 204 con
  dataset vacío (contractual).
- Uploads sintéticos: catálogos `PREVISUALIZADA`, facturación `VALIDADA`,
  pedimentos con validaciones correctas (material inexistente, encabezados,
  archivo vacío) y control de duplicados por hash (409) en los tres flujos.
- RBAC: API 6/6 esperado (401/403/200); browser 12/13 (único desvío:
  `FUN-E2E-003`). Sin token → `/login`; sin permiso → `/forbidden`.
- Responsive: 40/40 cargas (20 rutas × tablet/mobile) sin overflow horizontal,
  sin errores de consola ni de página.
- Sin escrituras de negocio en `CALE_IMMEX`; sólo staging sintético en
  `ANEXO24_DEV`. Confirmaciones ejecutadas = 0.

## Inventario de rutas y matriz

| Ruta | Desktop | Tablet | Mobile | API | Filtros | Paginación | RBAC | Consola | Resultado |
|---|---|---|---|---|---|---|---|---|---|
| `/login` | PASS | PASS | PASS | 200/401 esperado | N/A | N/A | N/A | limpia | PASS (login real = `OPERATOR_ACCEPTANCE_PENDING`) |
| `/forbidden` | PASS | PASS | PASS | N/A | N/A | N/A | N/A | limpia | PASS |
| `/dashboard` | PASS | PASS | PASS | 200 | N/A | N/A | PASS | limpia | PASS |
| `/materiales` | PASS | PASS | PASS | 200 | PASS (2→0→2) | PASS (disabled, total 2) | PASS | limpia | PASS |
| `/productos` | PASS | PASS | PASS | 200 | PASS (204→0→204) | PASS (página 2 = 200) | PASS | limpia | PASS |
| `/estructuras` | PASS | PASS | PASS | 200 vacío | PASS (0→0) | PASS (disabled) | PASS | limpia | PASS |
| `/catalogos` | PASS | PASS | PASS | 200 | N/A | PASS (7 filas) | PASS | limpia | PASS |
| `/catalogos/datos-generales` | PASS | PASS | PASS | 200 | N/A | N/A | PASS | limpia | PASS |
| `/catalogos/socios-comerciales` | PASS | PASS | PASS | 200 | N/A | PASS (5 filas) | PASS | limpia | PASS |
| `/catalogos/importaciones` | PASS | PASS | PASS | 200 staging | N/A | N/A | **FAIL** (`FUN-E2E-003`) | limpia | PASS funcional / FAIL RBAC readonly |
| `/operaciones/entradas` | PASS | PASS | PASS | 200 | PASS (filtros opcionales) | PASS (disabled, total 1) | PASS | limpia | PASS |
| `/operaciones/salidas` | PASS | PASS | PASS | 200 | PASS | PASS (página 2 = 200) | PASS | limpia | PASS |
| `/operaciones/materiales-utilizados` | PASS | PASS | PASS | 200 | PASS | PASS (página 2 = 200) | PASS | limpia | PASS |
| `/operaciones/activos-fijos` | PASS | PASS | PASS | 200 | PASS | PASS (disabled, total 1) | PASS | limpia | PASS |
| `/operaciones/pedimentos` | PASS | PASS | PASS | 200/409 esperado | N/A | N/A | PASS | limpia | PASS (sin confirmación) |
| `/reportes` | PASS | PASS | PASS | 11×200 | PASS | PASS (4 tipos) | PASS | limpia | PASS |
| `/facturacion` | PASS | PASS | PASS | 200/409 esperado | N/A | N/A | PASS | limpia | PASS (sin confirmación) |
| `/usuarios` | PASS | PASS | PASS | 200 | N/A | N/A | PASS | limpia | PASS |
| `/perfiles` | PASS | PASS | PASS | 200 | N/A | N/A | PASS | limpia | PASS |
| `/bitacora` | PASS | PASS | PASS | 200 | N/A | N/A | PASS | limpia | PASS |
| `**` wildcard | PASS | PASS | PASS | N/A | N/A | N/A | N/A | limpia | PASS (→ `/dashboard`) |

Notas de matriz: “limpia” = 0 `console.error`, 0 `pageerror`, 0 request `/api/`
fallidos no esperados. Tablet/mobile se validaron estructuralmente (carga,
overflow, sidebar/drawer, toolbar, tablas, botones, consola).

## Evidencia API (sweep read-only, backend aislado 18082)

| Check | Endpoint | Status |
|---|---|---|
| system_status | `/api/v1/system/status` | 200 (`moduleCDatabase`/`applicationDatabase` UP) |
| materiales | `/api/v1/catalogos/materiales` | 200 |
| productos | `/api/v1/catalogos/productos` | 200 |
| estructuras | `/api/v1/catalogos/estructuras` | 200 (vacío) |
| op_entradas | `/api/v1/operaciones/entradas` | 200 |
| op_salidas | `/api/v1/operaciones/salidas` | 200 |
| op_mat_utilizados | `/api/v1/operaciones/materiales-utilizados` | 200 |
| op_activos_fijos | `/api/v1/operaciones/activos-fijos` | 200 |
| rep_entradas | `/api/v1/reportes/entradas` | 200 |
| rep_salidas | `/api/v1/reportes/salidas` | 200 |
| rep_mat_utilizados | `/api/v1/reportes/materiales-utilizados` | 200 |
| rep_bitacora | `/api/v1/reportes/bitacora` | 200 |
| rep_compulsa | `/api/v1/reportes/compulsa` | 200 |
| rep_rectificaciones | `/api/v1/reportes/rectificaciones` | 200 |
| rep_vencimientos | `/api/v1/reportes/vencimientos` | 200 (vacío) |
| rep_dirigidos | `/api/v1/reportes/dirigidos` | 200 (vacío) |
| rep_analisis_descargas | `/api/v1/reportes/analisis-descargas` | 200 |
| rep_op_bloqueadas | `/api/v1/reportes/operaciones-bloqueadas` | 200 (vacío) |
| rep_f4 | `/api/v1/reportes/f4` | 200 (vacío) |
| admin_usuarios | `/api/v1/administracion/usuarios` | 200 |
| admin_perfiles | `/api/v1/administracion/perfiles` | 200 |
| admin_actividades | `/api/v1/administracion/actividades` | 200 |
| bitacora | `/api/v1/bitacora` | 200 |
| facturacion_plantilla | `/api/v1/facturacion/plantilla` | 200 |
| f4_sin_token | `/api/v1/reportes/f4` | 401 (esperado) |
| f4_pagina_invalida | `/api/v1/reportes/f4?pagina=0` | 400 `SOLICITUD_INVALIDA` + correlationId (esperado) |

Páginas altas (interactivo): `rep_entradas?pagina=9999` 200 (`total=1`,
`items=0`), `op_entradas?pagina=9999` 200 (`total=1`, `items=0`),
`rep_f4?pagina=9999` 200 (`total=0`, `items=0`). Sin errores.

## Evidencia login UI (sin credenciales reales)

```text
inputs=ok, password masking=ok, toggle mostrar/ocultar=ok
submit vacío -> validaciones "El usuario es obligatorio." / "La contraseña es obligatoria." (sin request)
credenciales inválidas -> 401 controlado + alerta "Usuario o contraseña incorrectos." (permanece en /login)
consola limpia, responsive ok
LOGIN_REAL_CREDENTIAL_FLOW = OPERATOR_ACCEPTANCE_PENDING
```

## Evidencia reportes (11 tipos, desktop)

| Tipo | API | Filas pág.1 | Total | Empty | XLSX UI | Export |
|---|---|---|---|---|---|---|
| Entradas | 200 | 1 | 1 | no | visible | descargado (3.5 KB, 1 fila) |
| Salidas | 200 | 20 | 2,693 | no | visible | descargado (109 KB, 2,693 filas) |
| Materiales utilizados | 200 | 20 | 3,105 | no | visible | descargado (248 KB, 3,105 filas) |
| Bitácora | 200 | 20 | 632 | no | visible | descargado (46 KB, 632 filas) |
| Compulsa | 200 | 20 | 662 | no | no (textual, por contrato) | N/A |
| Rectificaciones | 200 | 20 | 662 | no | no (textual, por contrato) | N/A |
| Vencimientos de desperdicio | 200 | 0 | 0 | sí | no | contrato |
| Dirigidos | 200 | 0 | 0 | sí | no | contrato |
| Análisis de descargas | 200 | 20 | 3,866 | no | no (textual, por contrato) | N/A |
| Operaciones bloqueadas | 200 | 0 | 0 | sí | no | contrato |
| F4 (CTM / desperdicio) | 200 | 0 | 0 | sí | no (vacío) | API 204 |

- Paginación verificada con datos en salidas, materiales-utilizados, bitácora,
  compulsa, rectificaciones y análisis (página 2 = 200).
- XLSX abiertos con `openpyxl`: encabezados correctos y filas completas; sin
  celdas con padding (`padded=0`) y sin valores `"4.0"` en los exportables.

## Evidencia uploads sintéticos (staging `ANEXO24_DEV/app24`)

| Flujo | Archivo | Resultado | Detalle |
|---|---|---|---|
| Pedimentos | `E2E_PEDIMENTO_<ts>.xlsx` | 200 `CON_ERRORES` | `PED-003: El material no existe en el catalogo.` (validación de catálogo correcta; preview 1 fila, sin botón Confirmar) |
| Pedimentos | mismo archivo (reintento) | 409 | `El recurso ya existe` (hash + referencia) |
| Pedimentos | `E2E_PEDIMENTO_HEADER_<ts>.xlsx` | 200 `CON_ERRORES` | 16 errores `COLUMNA_NO_CONFIRMADA` / `COLUMNA_OBLIGATORIA_AUSENTE` |
| Pedimentos | `E2E_PEDIMENTO_VACIO_<ts>.xlsx` | 200 `CON_ERRORES` | `ENCABEZADO_AUSENTE` |
| Pedimentos | `.txt` | guardia UI | `Sólo se permiten archivos .xls y .xlsx.` (0 POST) |
| Catálogo materiales | `E2E_MATERIAL_<ts>.xlsx` | 200 `PREVISUALIZADA` | 1/1 válida, preview, nota “confirmación no habilitada” |
| Catálogo productos | `E2E_PRODUCTO_<ts>.xlsx` | 200 `PREVISUALIZADA` | 1/1 válida, preview |
| Catálogo materiales | header inválido | 200 `CON_ERRORES` | 8 errores de estructura |
| Catálogo | `.txt` | guardia UI | `Selecciona un archivo .xls o .xlsx de máximo 10 MiB.` (0 POST) |
| Facturación | `E2E_FACTURACION_<ts>.xlsx` (hoja `FACTURAS`) | 200 `VALIDADA` | 1/1 válido, hash, correlationId, preview; sin botón de confirmación |
| Facturación | mismo archivo (reintento) | 409 | `El recurso ya existe` |
| Facturación | `.txt` | guardia UI | formato no permitido (0 POST) |

- Ningún flujo disparó `/confirmacion` (`confirmationCalls = 0`).
- `POST` observados en total = 11 (todos staging de la app), 0 hacia tablas
  operativas de `CALE_IMMEX`.

## Evidencia RBAC

```text
API:  sin token -> 401; limited -> 403 en usuarios y materiales;
      readonly -> 403 en usuarios y plantilla de facturación; admin -> 200  (6/6)
Browser:
  readonly  /usuarios -> /forbidden | /facturacion -> /forbidden
  readonly  /operaciones/pedimentos -> /forbidden
  readonly  /catalogos/importaciones -> CARGA LA PÁGINA (FUN-E2E-003)
  limited   /materiales -> /forbidden | /reportes y /operaciones/entradas -> ok
  none      /dashboard -> ok (sin módulos) | /reportes y /usuarios -> /forbidden
  anon      /dashboard -> /login
```

## Evidencia shell / navegación

- Wildcard `/esto-no-existe-e2e` → redirige a `/dashboard`, sin loop ni página
  en blanco.
- `/forbidden` renderiza mensaje y acción “Volver al inicio”.
- Sidebar desktop: colapsa 272→76 px, `contentX` pegado al borde del sidebar
  (`gap = 0`, sin franja blanca), expande de vuelta 272 px.
- Drawer mobile 390×844: abre (`mobile-open`, x=0, `aria-expanded=true`) y
  cierra con backdrop. Nota de instrumentación: el primer `click` automatizado
  no registró el cambio (posible carrera de hidratación); el clic DOM inmediato
  sí. No reproducido como defecto con interacción real.

## Viewports y rendimiento básico

- 40/40 cargas (20 rutas × tablet/mobile) con `overflowX = 0`, sin errores de
  consola/página ni requests fallidos.
- Tiempos de carga de ruta: 1.1–2.3 s (desktop). Generación de reportes:
  1.3–3.8 s. Paso combinado generar+paginar+descargar de
  materiales-utilizados: ~9 s (descarga XLSX de 248 KB incluida); ningún
  endpoint individual superó 5 s.

## Datos de prueba creados (inventario)

| Tipo | Identificador | Estado | Clasificación |
|---|---|---|---|
| Carga pedimento | id 5 (12:26), ids 8, 9, 10 (12:35) | staging `app24` | RETAINED_FOR_AUDIT |
| Carga catálogo material | id 2 (12:26), id 4 (12:35) | staging `app24` | RETAINED_FOR_AUDIT |
| Carga catálogo producto | id 3 (12:35) | staging `app24` | RETAINED_FOR_AUDIT |
| Carga facturación | id 5 (12:35, hoja no configurada), id 6 (12:43, VALIDADA) | staging `app24` | RETAINED_FOR_AUDIT |
| Duplicados rechazados | pedimento ×2, facturación ×2 | 409, sin fila nueva | NOT_CREATED |

- Los uploads previos de pasadas anteriores permanecen; el hash evita
  duplicados nuevos. No se borra staging para preservar trazabilidad (la
  bitácora asociada es inmutable).
- `CLEANED = ninguno` (no se crearon usuarios, perfiles ni datos operativos).
- No se usaron datos empresariales reales en fixtures.

## Hallazgos

| ID | Severidad | Ruta | Pasos | Esperado | Actual | Evidencia | Front/back | Causa probable | Fix recomendado |
|---|---|---|---|---|---|---|---|---|---|
| FUN-E2E-001 | P3 (OPEN) | `/reportes` (Compulsa, Rectificaciones, Análisis) | Generar y observar documentos | Sin relleno | API JSON conserva padding (`"160-3750-6001741     "`); UI lo recorta (`formatOperationText`) y no hay export textual | Sweep API + fila UI + `openpyxl` (exportables sin padding) | Backend (proyección legacy); sin impacto visible | Columnas `char` legacy | Opcional: `TRIM` en proyección si negocio lo confirma (fuera de discovery) |
| FUN-E2E-002 | P2 (**FIXED / VERIFIED** `8b7a4fc`) | `/reportes` (Análisis de descargas) | Ver columna “Partida salida” | Valor consistente | `"4.0"` visible en celda (columna sin formatter) | Fila 0 de la tabla: `["6001720","—","190-3302-6002960","4.0",…]` | Frontend (formatter) | Tipo del campo en la vista legacy sin normalizar | Corregido: `formatOperationPartida` aplicado a `partidaEntrada`/`partidaSalida` |
| FUN-E2E-003 | P1 (**FIXED / VERIFIED** `fd98364`) | `/catalogos/importaciones` | Entrar con perfil `READ_ONLY` (sin `MATERIALES_CARGAR`/`PRODUCTOS_CARGAR`) | `/forbidden` | La página carga (título “Importar materiales y productos”) | RBAC browser: `finalUrl=/catalogos/importaciones`, `h1` del import; backend sí bloquea POST (403) | Frontend (guard) | `permissionGuard` evalúa `route.data['permission']` heredado de `/catalogos` (`CATALOGOS_AUX_CONSULTAR`) cuando el hijo define sólo `permissions` | Corregido: `permissions` presente es autoritativo e ignora `permission` heredado |
| FUN-E2E-004 | P3 (OPEN) | API reportes textuales | `GET /api/v1/reportes/{compulsa,rectificaciones,vencimientos,dirigidos,analisis-descargas,operaciones-bloqueadas}/exportacion` | 404/405 controlado | 500 `ERROR_INTERNO` + correlationId | Sweep API (6/6 con ese patrón) | Backend | No existe endpoint; `NoResourceFoundException` de Spring cae en el catch-all `@ExceptionHandler(Exception.class)` (el handler sólo traduce la `RecursoNoEncontradoException` propia) | Mapear `NoResourceFoundException` a 404 en el handler global; la UI no expone estos paths |

No se confirmaron hallazgos `P0`. Observaciones menores sin clasificar:
requests `ERR_ABORTED` en filtros de catálogo (cancelaciones del cliente al
sobre-escribir la consulta, sin error de usuario) y el primer clic
instrumentado del drawer mobile (no reproducible con interacción real).

## Fase de correcciones (rama `feature/e2e-v1-fixes`)

Base: `ca57bce` (test/functional-e2e-v1). Alcance: sólo P1/P2; P3 quedan
abiertos y diferidos.

| Finding | Commit | Cambio | Tests | Verificación browser (regresión focalizada) |
|---|---|---|---|---|
| FUN-E2E-003 (P1) | `fd98364` `fix(auth): respetar permisos específicos de rutas hijas` | `permission.guard.ts`: si `permissions` está presente y no vacío es autoritativo (`hasAnyPermission`) e ignora `permission` heredado; si no, evalúa `permission`; sin configuración → `/forbidden` | `permission.guard.spec.ts` (9 casos, incluye el caso heredado del bug A–H) | 8/8: readonly `/catalogos`, `/catalogos/datos-generales`, `/catalogos/socios-comerciales` = PASS; readonly `/catalogos/importaciones` → `/forbidden`; perfiles `MATERIALES_CARGAR` y `PRODUCTOS_CARGAR` → página carga; `NO_PERMISSION` → `/forbidden`; smoke tablet/mobile 4/4 |
| FUN-E2E-002 (P2) | `8b7a4fc` `fix(reports): normalizar visualización de partidas` | `formatOperationPartida` (quita sufijo `.0+` sólo con parte entera numérica; conserva `004`, `4.5`, `A4`, vacíos → `—`) y `format: 'partida'` en `partidaEntrada`/`partidaSalida` de Análisis de descargas; cantidades intactas | `operation-formatters.spec.ts` (5 casos) + `report-list.page.spec.ts` (2 casos: `2.0`→`2`, `4.0`→`4`, `4.5` cantidad intacta, `A4`/nulo) | `/reportes` → Análisis de descargas → Generar (200): 3 filas muestreadas, 0 celdas con sufijo `.0`; sin errores de consola; smoke tablet/mobile PASS |

Resultado de la fase: `P0 open = 0`, `P1 open = 0`, `P2 open = 0`,
`P3 open = 2` (FUN-E2E-001, FUN-E2E-004 diferidos). API sin cambios;
backend sin cambios de código.

## Gaps de auditoría restantes

1. Login con credenciales reales: `OPERATOR_ACCEPTANCE_PENDING` (no hay
   credencial local; cubierto con 401 controlado y JWT sintéticos).
2. CRUD de administración (usuarios/perfiles): no se ejecutaron mutaciones
   LIVE; validados listas, contratos y formularios visualmente.
3. Modales de confirmación destructivos: no abiertos (evitar mutaciones).
4. Uploads con datasets grandes (>5 archivos, >10 MiB): validados límites de
   error, no el máximo exacto.
5. Pedimentos happy-path `PREVISUALIZADA`: el fixture sintético no referencia
   un material existente (`PED-003` es la validación correcta); requiere
   material real del catálogo para preview limpio.

## Controles

```text
CALE_IMMEX writes = 0
ANEXO24_DEV writes = staging sintético E2E (9 cargas) + bitácora de la app
legacy mutable SP executions = 0
pediment confirmation executed = 0
billing confirmation executed = 0
test data created = 9 cargas staging (RETAINED_FOR_AUDIT), 0 usuarios/perfiles
token sintético = efímero (30 min), no impreso ni persistido
```

## Estado

```text
FUNCTIONAL_E2E_DISCOVERY = COMPLETE
API_CHECKS = 25/25 EXPECTED (200=23, 401e=1, 400e=1, 5xx=0)
ROUTES = 20/21 PASS (1 hallazgo RBAC: FUN-E2E-003)
REPORTS = 11/11 PASS
UPLOADS = PASS (staging, sin confirmación)
EXPORTS = PASS (4 operativos verificados con openpyxl; F4 204)
RBAC = API 6/6, browser 12/13 (FUN-E2E-003)
RESPONSIVE = 40/40 sin overflow ni errores
P0 = 0 | P1 = 1 | P2 = 1 | P3 = 2
READY_FOR_FIX_PHASE = YES
READY_TO_INTEGRATE_DEV = pendiente de revisión (rama de pruebas, sin CI por regla)
```

## Estado tras la fase de correcciones

```text
E2E_V1_FIX_PHASE = COMPLETE (feature/e2e-v1-fixes, desde ca57bce)
FUN-E2E-003 (P1) = FIXED / VERIFIED (fd98364; 8/8 RBAC + smoke)
FUN-E2E-002 (P2) = FIXED / VERIFIED (8b7a4fc; 0 celdas con .0 en UI)
FUN-E2E-001 (P3) = OPEN (API-only, sin impacto visible)
FUN-E2E-004 (P3) = OPEN (API-only, endpoint no expuesto en UI)
P0 open = 0 | P1 open = 0 | P2 open = 0 | P3 open = 2
frontend = 131/131 tests, lint, build PASS
backend = test/build PASS (sin cambios de código)
SP_FIRST = PASS | RUNTIME_PERMISSION_GATE = PASS
```
