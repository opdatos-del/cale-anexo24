# Auditoría E2E V1 — estabilización de la aplicación nueva

Fecha: 2026-09-25
Rama: `feature/e2e-stabilization-v1`
Alcance: inventario estático, smoke HTTP autenticado y baseline Playwright. Se corrigieron E2E-001, E2E-002 y E2E-003; la baseline browser autenticada quedó verificada.

## Resultado ejecutivo

La integración de Facturación Staging V2 se completó en `dev` mediante fast-forward y se publicó en `origin/dev`.

El smoke backend confirmó:

- `POST /api/v1/auth/login`: `200` con credencial efímera autorizada;
- `GET /actuator/health`: `200`;
- `GET /api/v1/bitacora` con rango válido: `200`;
- `GET /api/v1/facturacion/plantilla`: `200`;
- `GET /api/v1/facturacion/plantilla/archivo`: `200`;
- request sin token: `401`;
- contraseña incorrecta: `401`.

El primer `GET /api/v1/bitacora` sin rango devolvió `400`, comportamiento esperado por el contrato. No se observaron respuestas `500` en el smoke.

La baseline Playwright quedó configurada con Chromium y cuatro proyectos de viewport. La ejecución autenticada final ejecutó 34 tests y obtuvo 34 PASS. Se verificaron el redirect inicial, `/forbidden`, el layout responsive de Usuarios y los contratos browser de Facturación.

El puerto estándar `8080` estaba ocupado por `com.jovyweb.kiosco.KioscoApplication`, un proceso ajeno que no se detuvo. El backend de esta aplicación se arrancó de forma aislada en `18081` para el segundo smoke.

## Integration

| Campo | Resultado |
|---|---|
| Billing staging SHA | `8fff634f571370c74d757827e4fe3f7ed07c2127` |
| `dev` antes | `78720e68dd357b38e601e572802b4b177d976200` |
| `dev` después | `8fff634f571370c74d757827e4fe3f7ed07c2127` |
| `origin/dev` | `8fff634f571370c74d757827e4fe3f7ed07c2127` |
| Fast-forward | PASS |
| Smoke backend | PASS; carga 3 y Reportes Bitácora confirmados |
| Playwright baseline | 34 tests; 34 PASS / 0 FAIL / 0 skipped |
| Puerto usado en smoke | `18081`; `8080` ocupado por proceso ajeno |
| CALE_IMMEX writes | 0 |

## Playwright baseline

- Dependency: `@playwright/test@1.63.0`.
- Browser: Chromium.
- Projects: desktop `1440x900`, laptop `1024x768`, tablet `768x1024`, mobile `390x844`.
- Base URL: variable `E2E_BASE_URL`, fallback local `http://127.0.0.1:4300`.
- Backend usado: variable `E2E_BACKEND_URL`, smoke aislado en `18081`.
- Credentials: `E2E_USERNAME` / `E2E_PASSWORD`; no se versionan.
- Tests listados: `34`.
- Ejecución final autenticada: `34 PASS`, `0 FAIL`, `0 skipped`.
- Responsive `/usuarios`: PASS en desktop, laptop, tablet y mobile.
- Screenshots: sólo en failure.
- Trace: retain-on-failure.
- Video: off.

## Inventario de rutas Angular

Se encontraron 15 rutas funcionales, además de redirect raíz y wildcard; `/forbidden` tiene smoke browser dedicado. Playwright lista 34 tests con la cobertura actual:

| Ruta | Feature | Componente / carga | Permiso | Estado de auditoría |
|---|---|---|---|---|
| `/login` | auth | `LoginPage` | público | inventariada; cubierta por auth helper |
| `/forbidden` | core | `ForbiddenComponent` | público | browser PASS |
| `/dashboard` | dashboard | `DashboardPage` | sesión | browser PASS |
| `/materiales` | catalogs | `MaterialListPage` | `MATERIALES_CONSULTAR` | browser PASS |
| `/productos` | catalogs | `ProductListPage` | `PRODUCTOS_CONSULTAR` | browser PASS |
| `/estructuras` | catalogs | `StructureListPage` | `ESTRUCTURAS_CONSULTAR` | browser PASS |
| `/operaciones/entradas` | operations | `EntryListPage` | `OPERACIONES_CONSULTAR` | browser PASS; overflow checks PASS |
| `/operaciones/salidas` | operations | `ExitListPage` | `OPERACIONES_CONSULTAR` | browser PASS |
| `/operaciones/materiales-utilizados` | operations | `UsedMaterialListPage` | `OPERACIONES_CONSULTAR` | browser PASS |
| `/operaciones/activos-fijos` | operations | `FixedAssetListPage` | `OPERACIONES_CONSULTAR` | browser PASS |
| `/perfiles` | administration | `ProfileListPage` | `PERFILES_ADMINISTRAR` | browser PASS |
| `/usuarios` | administration | `UserListPage` | `USUARIOS_ADMINISTRAR` | browser PASS; responsive PASS en 4 viewports |
| `/bitacora` | administration | `AuditLogListPage` | `BITACORA_CONSULTAR` | browser PASS; API smoke PASS |
| `/reportes` | reports | `ReportListPage` | `REPORTES_GENERAR` | browser PASS |
| `/facturacion` | billing | `BillingUploadPage` | `FACTURACION_CARGAR` | browser PASS; E2E-001 fixed |

## Inventario de endpoints backend

Se encontraron 37 mappings de aplicación en controladores Java. La matriz siguiente agrupa los contratos reales; `READ/WRITE` describe el contrato de la API, no implica escritura en CALE_IMMEX.

| HTTP | Ruta | Authority | Consumidor | Tipo | Base / nota |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | público | auth | READ/issue token | ANEXO24_DEV |
| GET | `/api/v1/system/status` | sesión | system | READ | app |
| GET | `/api/v1/bitacora` | `BITACORA_CONSULTAR` | bitácora | READ | ANEXO24_DEV |
| GET | `/api/v1/reportes/entradas` | `REPORTES_GENERAR` | reportes | READ | CALE_IMMEX vía SP autorizado |
| GET | `/api/v1/reportes/salidas` | `REPORTES_GENERAR` | reportes | READ | CALE_IMMEX vía SP autorizado |
| GET | `/api/v1/reportes/materiales-utilizados` | `REPORTES_GENERAR` | reportes | READ | CALE_IMMEX vía SP autorizado |
| GET | `/api/v1/reportes/bitacora` | `REPORTES_GENERAR` | reportes | READ | ANEXO24_DEV |
| GET | `/api/v1/reportes/{tipo}/exportacion` | `REPORTES_EXPORTAR` | reportes | READ/export | según reporte |
| GET | `/api/v1/catalogos/materiales` | `MATERIALES_CONSULTAR` | materiales | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/catalogos/productos` | `PRODUCTOS_CONSULTAR` | productos | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/catalogos/estructuras` | `ESTRUCTURAS_CONSULTAR` | estructuras | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/operaciones/entradas` | `OPERACIONES_CONSULTAR` | entradas | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/operaciones/salidas` | `OPERACIONES_CONSULTAR` | salidas | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/operaciones/materiales-utilizados` | `OPERACIONES_CONSULTAR` | materiales utilizados | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/operaciones/activos-fijos` | `OPERACIONES_CONSULTAR` | activos fijos | READ | CALE_IMMEX vía SP |
| GET | `/api/v1/administracion/actividades` | `PERFILES_ADMINISTRAR` | perfiles | READ | ANEXO24_DEV |
| GET/POST/PUT/PATCH | `/api/v1/administracion/perfiles...` | perfiles/admin usuarios | perfiles | READ/WRITE | ANEXO24_DEV |
| GET/POST/PUT/PATCH | `/api/v1/administracion/usuarios...` | `USUARIOS_ADMINISTRAR` | usuarios | READ/WRITE | ANEXO24_DEV |
| POST | `/api/v1/facturacion/cargas` | `FACTURACION_CARGAR` | facturación | WRITE staging | ANEXO24_DEV; no CALE_IMMEX |
| GET | `/api/v1/facturacion/cargas/{id}` | `FACTURACION_CARGAR` | facturación | READ | ANEXO24_DEV |
| GET | `/api/v1/facturacion/plantilla` | `FACTURACION_CARGAR` | facturación | READ | ANEXO24_DEV |
| GET | `/api/v1/facturacion/plantilla/archivo` | `FACTURACION_CARGAR` | facturación | READ/export | generado desde configuración |

No se inventarió endpoint de Saldos: la UI lo mantiene deshabilitado y no existe mapping funcional equivalente en los controladores revisados.

## Hallazgos

| ID | Severity | Módulo | Ruta | Endpoint | Hallazgo | Reproducción / evidencia | Fix sugerido | MISSING_TEST |
|---|---|---|---|---|---|---|---|---|
| E2E-001 | MEDIUM | Facturación | `/facturacion` | `GET /api/v1/facturacion/plantilla` | Error de plantilla silenciado por `catchError(() => EMPTY)`. | Corregido con `templateError`, alert visible, correlationId, retry y bloqueo de acciones. | Cerrado; mantener cobertura unitaria y browser. | no |
| E2E-002 | MEDIUM | Navegación | login | `POST /api/v1/auth/login` | Login exitoso navegaba a `/materiales` en lugar de `/dashboard`. | Redirect corregido a `/dashboard`; test unitario y helper Playwright actualizados. | Cerrado; mantener prueba de navegación inicial. | no |
| E2E-003 | MEDIUM | Administración | `/usuarios` | — | Overflow global en tablet: viewport `768`, document `953`. | Runtime confirmó que el cambio a cards hasta `lg` elimina el overflow; tabla desde `lg` y wrapper acotado. | Cerrado; mantener cobertura responsive. | no |

No se confirmaron hallazgos CRITICAL o HIGH.

## HTTP

- 500 observados: `0` en el smoke ejecutado.
- 400 esperado: Bitácora sin `desde/hasta`.
- 400 esperado: `GET /facturacion/cargas/0`, código `FACTURACION_ARCHIVO_INVALIDO`, correlationId presente.
- 401: sin token y contraseña incorrecta, ambos `401`.
- 403: no probado con un usuario sintético de permisos reducidos.
- 404: `GET /facturacion/cargas/4` con ID válido inexistente, `404`.
- Carga 3: `200`, `id=3`, `estado=PREVISUALIZADA`, `total=1`, `filas=1`.
- `GET /api/v1/reportes/bitacora`: `200`, total observado `121`.
- Exportación Bitácora: `200`, MIME XLSX, longitud observada `11530`.

## Módulos

| Módulo | Resultado |
|---|---|
| Dashboard | Browser PASS en desktop/laptop/tablet/mobile seleccionados |
| Catálogos | Browser PASS en materiales, productos y estructuras |
| Operaciones | Browser PASS en las cuatro rutas; responsive entradas PASS |
| Reportes | Browser PASS; Bitácora `200` y exportación XLSX `200` |
| Administración | Listados browser PASS; `/usuarios` responsive PASS; sin writes |
| Bitácora | Browser PASS; API y exportación PASS |
| Facturación | Plantilla/XLSX browser PASS; carga 3 recuperable; confirmación bloqueada |

## Saldos

- Estado: `BLOCKED_DOMAIN_RULE`.
- SP inseguro ejecutado: `0`.
- Endpoint ficticio: `NO`.
- La opción permanece deshabilitada en la UI.

## Facturación

| Caso | Resultado |
|---|---|
| Plantilla | `200` |
| Descarga XLSX | `200`, MIME XLSX y `Layout_Facturas.xlsx` |
| Upload nuevo | no ejecutado en esta pasada |
| Preview/reload | carga 3 recuperada con `200`, una fila staging |
| Confirmación | no existe / permanece bloqueada |
| Escrituras CALE_IMMEX | `0` |

## Frontend console y UX

- Page errors: `0` en tests PASS.
- Console errors inesperados: `0` en tests PASS; el `409` mockeado se excluyó explícitamente como respuesta negativa esperada.
- HTTP 500: `0`.
- Accesibilidad smoke: controles principales localizables por role/label; auditoría WCAG completa fuera de alcance.
- Responsive: PASS en desktop `1440x900`, laptop `1024x768`, tablet `768x1024` y mobile `390x844`; overflow global `0`.

## Datos y cleanup

- Usuarios sintéticos creados: `0`.
- Perfiles sintéticos creados: `0`.
- Cargas sintéticas nuevas: `0`.
- Cambios ANEXO24_DEV durante esta auditoría: `0`.
- Evidencia de Bitácora: no se generaron nuevas operaciones mutables.
- `CALE_IMMEX`: sólo lectura; procedimientos mutables no ejecutados.

## Gaps de pruebas

1. Ejecutar matriz de permisos `401/403` con usuarios sintéticos, con conteos antes/después y cleanup.
2. Recorrer filtros, paginación, exportaciones y estados vacíos con casos de datos controlados.
5. Agregar cobertura browser de refresh directo y navegación atrás/adelante.
6. Mantener el contrato de carga 3 y ampliar pruebas de paginación staging.

## Git

- Rama: `feature/e2e-stabilization-v1`.
- Working tree: contiene cambios propios de fix/tests/Playwright y este reporte, además del cambio ajeno preservado.
- Cambio ajeno preservado: `docs/03-diseno/mapeo-bitacora.md`; no fue modificado ni stageado.
- Stashes preservados: 2.
- Commit/push de auditoría: no realizados.

## Estado

`E2E_AUDIT_COMPLETE = YES` para inventario, HTTP smoke y baseline Playwright.
`E2E-001 = FIXED`.
`E2E-002 = FIXED`.
`E2E-003 = FIXED`.
`STATIC_INVENTORY = COMPLETE`.
`HTTP_SMOKE = COMPLETE`.
`BROWSER_SMOKE = COMPLETE`.
`RESPONSIVE_SMOKE = COMPLETE`.
`A11Y_SMOKE = COMPLETE`.
`BROWSER_SMOKE_BASELINE_COMPLETE = YES`: 34/34 PASS.
`READY_FOR_INTEGRATION = YES`; CRUD browser write, confirmación de Facturación y Saldos permanecen fuera de alcance.
