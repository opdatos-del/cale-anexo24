# Auditoría funcional E2E V1 — aplicación nueva (discovery)

Fecha: 2026-10-02
Rama: `test/functional-e2e-v1` (desde `dev` = `a53a93b3c542f8c72eef2c3be439225fcfcd3144`)
Alcance: primera pasada de auditoría funcional de la aplicación nueva completa
(inventario real de rutas, sweep API autenticado read-only, smoke browser del
cierre F4/LEGACY-041). Fase de discovery: sin cambios de código de aplicación.

## Método

- Inventario de rutas extraído del código real (`app.routes.ts` y rutas hijas),
  sin inventar rutas.
- Backend aislado `bootRun` en `18082` con `backend/.env` local (identidad
  `opdatos`); frontend `ng serve` en `4300` con proxy `/api` al backend aislado.
- Autenticación de smoke: token JWT firmado localmente con `JWT_SECRET` del
  entorno; el token no se versiona ni se imprime y expira en 30 minutos. No se
  creó usuario ni se usaron credenciales de terceros.
- Sweep API read-only de 25 endpoints (GET).
- Browser Chromium (Playwright) con token inyectado en el `localStorage` del
  origen (`anexo24_token` / `anexo24_user_name` / `anexo24_permissions`):
  flujo F4 en desktop `1440x900` y chequeo de overflow en tablet `768x1024` y
  mobile `390x844`.

## Resultado ejecutivo

- API sweep: **25/25 = 200** (system, catálogos, operaciones, reportes,
  administración, bitácora, facturación). Sin `500`.
- Browser F4: **PASS** — opción visible, empty state correcto (`0 resultados`),
  `GET /api/v1/reportes/f4` 200 en los tres viewports, sin errores de consola ni
  de página, overflow 0, botón XLSX oculto con dataset vacío (comportamiento
  correcto por contrato).
- Cierre F4 (LEGACY-041): SP `dbo.APP24_Q_F4_LISTAR` desplegado en LIVE
  (`object_id 1149247149`), read-only verificado (`0|0|0|0|0`), ejecutado con
  `@Total = 0` en página 1 y página alta; API `200/200/400` (sin token/página/
  paginación inválida) y export `204` con dataset vacío.
- Sin escrituras de negocio; datos de prueba creados = `0`.

## Inventario de rutas (real, desde `app.routes.ts`)

| Ruta | Feature | Permiso | Estado en esta pasada |
|---|---|---|---|
| `/login` | auth | público | baseline browser previo (`auditoria-e2e-v1.md`); login API cubierto por contrato |
| `/forbidden` | core | público | baseline browser previo |
| `/dashboard` | dashboard | sesión | API `system_status` 200; baseline browser previo |
| `/materiales` | catalogs | `MATERIALES_CONSULTAR` | API 200; baseline browser previo |
| `/productos` | catalogs | `PRODUCTOS_CONSULTAR` | API 200; baseline browser previo |
| `/estructuras` | catalogs | `ESTRUCTURAS_CONSULTAR` | API 200 (dataset vacío); baseline browser previo |
| `/catalogos` | catalogs | `CATALOGOS_AUX_CONSULTAR` | API 200 indirecto; pendiente pasada interactiva |
| `/catalogos/datos-generales` | catalogs | `CATALOGOS_AUX_CONSULTAR` | pendiente pasada interactiva |
| `/catalogos/socios-comerciales` | catalogs | `CATALOGOS_AUX_CONSULTAR` | pendiente pasada interactiva |
| `/catalogos/importaciones` | catalogs | `MATERIALES_CARGAR`/`PRODUCTOS_CARGAR` | pendiente upload sintético |
| `/operaciones/entradas` | operations | `OPERACIONES_CONSULTAR` | API 200; baseline browser previo |
| `/operaciones/salidas` | operations | `OPERACIONES_CONSULTAR` | API 200; baseline browser previo |
| `/operaciones/materiales-utilizados` | operations | `OPERACIONES_CONSULTAR` | API 200; baseline browser previo |
| `/operaciones/activos-fijos` | operations | `OPERACIONES_CONSULTAR` | API 200; baseline browser previo |
| `/operaciones/pedimentos` | operations | `PEDIMENTOS_CARGAR` | API staging sin pedimentos nuevos; pendiente upload sintético |
| `/reportes` | reports | `REPORTES_GENERAR` | API 200 en 11 tipos; browser F4 PASS; exports pendientes con datos |
| `/facturacion` | billing | `FACTURACION_CARGAR` | API plantilla 200; pendiente upload sintético |
| `/usuarios` | administration | `USUARIOS_ADMINISTRAR` | API 200; baseline browser previo |
| `/perfiles` | administration | `PERFILES_ADMINISTRAR` | API 200; baseline browser previo |
| `/bitacora` | administration | `BITACORA_CONSULTAR` | API 200; baseline browser previo |
| `**` (wildcard) | core | — | redirige a `/`; sin cambios |

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

## Evidencia browser (F4)

```text
desktop 1440x900 : f4Visible=true, emptyState=true, totalText="0 resultados",
                   exportButtonCount=0, GET /api/v1/reportes/f4 200
tablet  768x1024 : GET f4 200, overflow=0, empty state correcto
mobile  390x844  : GET f4 200, overflow=0, empty state correcto
consoleErrors=[] pageErrors=[] result=PASS
```

## Hallazgos

| ID | Severidad | Ruta | Pasos | Esperado | Actual | Evidencia | Front/back | Causa probable | Fix recomendado |
|---|---|---|---|---|---|---|---|---|---|
| FUN-E2E-001 | P3 | `/reportes` (Compulsa, Rectificaciones, Análisis de descargas) | Generar reporte y observar campos documentales | Documentos sin relleno | Valores `char` con padding (`"160-3750-6001741     …"`) en la API | Sweep API (`pedimentoAnexo24`, `pedimento`, `pedimentoSalida`) | Backend (proyección legacy) / UI y XLSX por verificar | Columnas `char` del modelo legacy | Verificar en UI/XLSX; aplicar `TRIM` en la proyección si negocio lo confirma (cambio de contrato, fuera de discovery) |
| FUN-E2E-002 | P3 | `/reportes` (Análisis de descargas) | Observar `partidaSalida` | Número o texto consistente | `"4.0"` como string | Sweep API | Backend | Tipo del campo en la vista legacy | Verificar formato en UI; normalizar en DTO si aplica |

No se confirmaron hallazgos `P0` (bloquea uso), `P1` (funcional importante) ni
`P2` (UX/error menor) en el alcance ejecutado.

## Gaps de auditoría (no defectos, pendientes de ejecución)

1. Recorrido browser interactivo completo (filtros, búsqueda, paginación,
   modales, formularios, validaciones, RBAC 403, consola) requiere credenciales
   `E2E_USERNAME`/`E2E_PASSWORD` y fixtures `E2E_FIXTURE_PASSWORD*`; no
   disponibles para el agente. La baseline `auditoria-e2e-v1.md` (34 tests)
   cubre varios flujos y puede re-ejecutarse con credenciales del operador.
2. Uploads sintéticos de pedimentos, facturación y catálogos: no ejecutados en
   esta pasada.
3. RBAC con usuario de permisos reducidos: pendiente (existe spec
   `admin-authorization.spec.ts` condicionado a fixture).
4. Responsive de todas las rutas: ejecutado sólo en `/reportes` (F4) en esta
   pasada; baseline previo cubrió `/usuarios`, `/entradas` y dashboard.
5. Exportaciones con dataset no vacío: cubiertas por tests sintéticos; en LIVE
   sólo se verificó el 204 de F4 con dataset vacío.

## Controles

```text
CALE_IMMEX writes = 0
ANEXO24_DEV writes = 0
legacy mutable SP executions = 0
test data created = 0 (sin usuarios, perfiles ni cargas)
token sintético = efímero (30 min), no impreso ni persistido
servidores locales = detenidos al cierre
```

## Estado

```text
FUNCTIONAL_E2E_DISCOVERY = IN_PROGRESS
API_SWEEP = 25/25 OK
BROWSER_F4 = PASS
PENDIENTE = matriz interactiva completa con credenciales E2E
LEGACY_041_RUNTIME = PASS (F4 read-only; PARTIAL se mantiene)
```
