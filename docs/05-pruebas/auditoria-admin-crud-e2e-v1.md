# Auditoría Administration CRUD E2E V1

Fecha: 2026-09-29
Rama: `feature/admin-crud-e2e-v1`
Base: `70077639c73c5e0b84d2c3f2bfbcce014104c5f2`

## Alcance

Auditoría browser autenticada de Usuarios, Perfiles, permisos y autorización usando fixtures sintéticos persistentes. Las mutaciones se ejecutaron mediante API sólo para preparación/restauración y mediante UI para los flujos funcionales. No se ejecutó SQL manual ni se tocaron bases legacy.

## Fixtures

| Fixture | Resultado |
|---|---|
| Perfil | `E2E_AUTOMATION_PROFILE`; fixture estable reutilizado después de la preparación inicial |
| Perfil alterno | `E2E_AUTOMATION_PROFILE_ALT`; fixture estable para transición de perfil |
| Usuario | `E2E_AUTOMATION_USER`; correo sintético `e2e-automation@invalid.local` |
| Duplicados | 0; el setup falla si encuentra más de uno |
| Passwords | sólo variables efímeras del proceso; no documentadas |
| Baseline restore | ejecutado por `finally` del fixture en las ejecuciones con setup exitoso |

No se imprimen IDs ni secretos de fixtures.

## Resultados de flujos

### Usuarios

| Flujo | Resultado |
|---|---|
| Listado y filtros por clave/nombre/correo/estado/perfil | PASS |
| Limpiar filtros | PASS |
| Editar nombre y correo desde UI y restaurar | PASS |
| Cambiar vigencia y restaurar sin vencimiento | PASS |
| Cambiar perfil a fixture alterno y restaurar | PASS |
| Inactivar y reactivar fixture | PASS |
| Restablecer password y login aislado del fixture | PASS |
| Crear por UI | No ejecutado: fixture persistente ya existía/reutilización idempotente |

### Perfiles

| Flujo | Resultado |
|---|---|
| Listado y filtro | PASS |
| Editar nombre y restaurar | PASS — `ADMIN-E2E-001` FIXED |
| Reemplazar permisos desde UI y restaurar baseline | PASS |
| Inactivar/reactivar perfil | PASS |
| Crear por UI | No ejecutado: fixture persistente preparado/reutilizado |

## Hallazgos y fixes

| ID | Severity | Flow | Finding | Expected | Actual / resultado | Fix o siguiente evidencia |
|---|---|---|---|---|---|---|
| ADMIN-E2E-001 | HIGH | Perfiles → Editar nombre | El formulario usaba `(ngSubmit)` sin una directiva de formulario emisora que interceptara el submit. | El submit debe emitir PUT al fixture y conservar la selección. | El navegador ejecutaba el submit nativo como GET `/perfiles?`, recargaba `/perfiles`, seleccionaba inicialmente `ADMINISTRADOR` y no emitía PUT. API, DB, JDBC y SP LIVE quedaron descartados mediante CASE A. | Corregido con `(submit)="$event.preventDefault(); saveName()"`; el PUT browser ahora llega al fixture y el flujo restaura correctamente. Regresión DOM/unitaria añadida. |
| ADMIN-E2E-002 | MEDIUM | Usuarios → responsive | La tabla de Usuarios desborda con datos sintéticos persistentes en laptop. | `scrollWidth <= clientWidth + 1`. | Confirmado antes: en `1024px`, `document=1101`; la tabla era el elemento fuera de viewport (`left≈305`, `right≈1157`). Después del fix: `responsive.spec.ts` laptop `/usuarios` PASS; `1 passed`. | Corregido localmente: cards hasta `xl`, tabla desde `xl`, conservando wrapper `min-w-0 max-w-full overflow-x-auto`; no se modificó el shell ni se ocultó overflow global. |

### Cambios de esta fase

- `UserListPage`: breakpoint de tabla/cards cambiado de `lg` a `xl` para respetar el ancho útil con sidebar.
- `ProfileManagementPage`: `loadProfiles()` descarta respuestas obsoletas mediante secuencia de solicitud e invalida cargas pendientes al seleccionar un perfil.
- Regresión unitaria para respuestas fuera de orden: PASS.
- `admin-authorization.spec.ts`: login del fixture, `/materiales` permitido, `/usuarios` → `/forbidden` y `403` API; `1/1 PASS`.
- Aislamiento API/SQL de `ADMIN-E2E-001`: `CASE A`; ambos SP LIVE coinciden con los archivos versionados después de normalizar `CRLF/LF`, whitespace, `CREATE OR ALTER` y `GO`.

No se modificó SQL, backend Java ni LIVE/SP.

### Evidencia de aislamiento frontend

- `selectProfile` call sites productivos: `2` — click de lista y respuesta de `loadProfiles()`.
- `loadProfiles` call sites productivos: `4` — init, refresh, retry y post-create.
- `clearSelection` call sites productivos: `1` — respuesta vacía de `loadProfiles()`.
- La transición a `ADMINISTRADOR` ocurrió después del submit nativo, no durante selección, permisos, idle ni edición.
- La protección de request-sequence queda como `HARDENING`: conserva selección ante respuestas obsoletas, pero no era la causa ni el fix de `ADMIN-E2E-001`.
- La causa exacta fue HTML: `(ngSubmit)` sin directiva emisora → handler no ejecutado → GET nativo y recarga → selección inicial de `ADMINISTRADOR`.

## Authorization

- Sesión admin: login y acceso administrativo PASS en ejecuciones previas.
- Permisos de perfil E2E: actividades `MATERIALES_CONSULTAR` y `PRODUCTOS_CONSULTAR` asignadas y restauradas PASS en preparación API.
- Spec añadida para ruta permitida, ruta denegada y `403` API: PASS `1/1` contra backend 18081.
- `ADMIN_AUTHORIZATION_E2E`: PASS; `/materiales` permitido, `/usuarios` redirigido a `/forbidden` y API de usuarios responde `403` para el fixture.

## Audit log

Las mutaciones ejecutadas generaron eventos de administración. La suite no elimina bitácora. Para esta prueba focal, las consultas API respondieron sin `500` ni enum drift; el recorrido exhaustivo de páginas y correlación de cada evento esperado queda pendiente.

## Idempotencia

- Preparación de fixtures repetida: no acumuló duplicados en las ejecuciones previas.
- `RUN_1`: `10/10 PASS`.
- `RUN_2`: `10/10 PASS`.
- Fixtures reutilizados sin duplicados; baseline restore ejecutado mediante API en `finally`.

## Existing smoke

La baseline browser posterior a los fixes pasó `34/34`. Responsive `/usuarios` pasó en `1440`, `1024`, `768` y `390`; billing, reports y `/forbidden` también PASS.

## Runtime y seguridad

- Page errors inesperados: 0.
- Console errors inesperados: 0.
- HTTP 500: 0 observado en los flujos CRUD, autorización y baseline.
- Backend aislado 18081: health `200` durante las ejecuciones focales.
- Aislamiento directo: API `200`, DB fixture actualizado/restaurado, DB ADMINISTRADOR intacta; el fallo queda en browser/frontend state.
- ANEXO24_DEV writes: sólo mediante API de la aplicación en ejecuciones previas.
- SQL directo: 0.
- CALE_IMMEX writes: 0.
- Legacy SP mutables: 0.
- Passwords/JWT/storageState: no persistidos.

## Git

- Cambio ajeno preservado: `docs/03-diseno/mapeo-bitacora.md`.
- Stashes preservados: 2.
- Commit de esta auditoría: no realizado.
- Push de esta auditoría: no realizado.

## Decisión

`ADMIN_USERS_CRUD_E2E = PASS`.
`ADMIN_PROFILES_CRUD_E2E = PASS` después del fix del submit nativo.
`ADMIN_PERMISSIONS_E2E = PASS` para reemplazo/restauración.
`ADMIN_AUTHORIZATION_E2E = PASS`.
`FIXTURE_RESTORE = PASS`.
`SECOND_RUN_IDEMPOTENT = PASS`; RUN_1 y RUN_2 completaron `10/10`.

`ADMIN-E2E-002 = FIXED` (responsive PASS en los cuatro viewports).
`ADMIN-E2E-001 = FIXED`; causa frontend/HTML native form submission.

Validación frontend: `79/79 PASS`, lint PASS, TypeScript PASS, build PASS. CRUD administrativo RUN_1: `10/10 PASS`; RUN_2: `10/10 PASS`. Baseline browser: `34/34 PASS`. Autorización: `1/1 PASS`.

`AUDIT_LOG_EXHAUSTIVE_POST_RUN = NOT_EXECUTED`: las acciones generaron eventos y no se observaron enum errors ni HTTP 500, pero no se recorrieron exhaustivamente todas las páginas posteriores a ambos runs.
