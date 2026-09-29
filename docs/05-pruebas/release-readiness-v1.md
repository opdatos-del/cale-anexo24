# Release Readiness V1 — Auditoría inicial

Fecha: 2026-09-29
Rama: `feature/release-readiness-v1`
HEAD auditado: `db9a139f580b26196922f10cb57a5ae4a7d74cb4`
Base: `dev` integrado desde `feature/admin-crud-e2e-v1` mediante fast-forward.

## Alcance y método

Esta fase es **AUDIT ONLY**. Se revisaron código, rutas Angular, controllers,
casos de uso, adapters JDBC, SP versionados, configuración Spring, tests,
documentación existente y resultados E2E ya validados. No se hicieron fixes,
migraciones, despliegues de SP ni escrituras runtime adicionales.

Estados usados exclusivamente:

- `COMPLETE`
- `PARTIAL`
- `BLOCKED_EXTERNAL`
- `BLOCKED_DOMAIN`
- `NOT_STARTED`
- `NOT_APPLICABLE`

## Integration

- Admin CRUD antes: `db9a139f580b26196922f10cb57a5ae4a7d74cb4`
- `dev` después: `db9a139f580b26196922f10cb57a5ae4a7d74cb4`
- `origin/dev`: `db9a139f580b26196922f10cb57a5ae4a7d74cb4`
- Fast-forward: `PASS`
- Merge commit: no creado
- Admin CRUD integrado: `PASS`

## System status

| Módulo | Estado | Backend | Frontend | DB/SP | Tests/evidencia | Brecha principal |
|---|---|---|---|---|---|---|
| Autenticación | COMPLETE | Login, JWT, 401/403 y BCrypt | Login, interceptor, guard, logout y `/forbidden` | `APP24_Q_USUARIO_ACCESO` | Unit + browser smoke + autorización | Sesión expirada avanzada no está evidenciada como flujo E2E dedicado |
| Dashboard | PARTIAL | Resumen de materiales | KPIs/accesos, estados vacíos y error recuperable | Consulta paginada de materiales | Unit + browser smoke | Sólo muestra total de materiales; avisos/estado son estáticos y no hay resumen operativo completo |
| Materiales | COMPLETE | GET paginado y filtrable | Listado, filtros, paginación y empty state | `APP24_Q_MATERIALES_LISTAR` | Unit + browser smoke | Sólo lectura V1 |
| Productos | COMPLETE | GET paginado y filtrable | Listado, filtros y paginación | `APP24_Q_PRODUCTOS_LISTAR` | Unit + browser smoke | Sólo lectura V1 |
| Estructuras | COMPLETE | GET paginado y filtrable | Consulta de estructuras | `APP24_Q_ESTRUCTURAS_LISTAR` | Unit + browser smoke | Semántica histórica de algunos campos sigue documentada como pendiente |
| Entradas | COMPLETE | GET con rango obligatorio y filtros | Consulta paginada | `APP24_Q_ENTRADAS_LISTAR` | Unit + browser smoke | No hay captura/edición; fuera del alcance observado |
| Salidas | COMPLETE | GET con rango obligatorio y filtros | Consulta paginada | `APP24_Q_SALIDAS_LISTAR` | Unit + browser smoke | Sólo consulta |
| Materiales utilizados | COMPLETE | GET con rango obligatorio y filtros | Consulta paginada | `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | Unit + browser smoke | Relaciones legacy requieren validación de negocio adicional si se amplía alcance |
| Activos fijos | COMPLETE | GET con rango obligatorio y filtros | Consulta paginada | `APP24_Q_ACTIVOS_FIJOS_LISTAR` | Unit + browser smoke | Contrato V1 read-only; semántica de algunos campos históricos pendiente |
| Reportes | PARTIAL | Consultas y exportación XLSX para entradas, salidas, consumo y bitácora | Filtros, paginación y exportación | Reutiliza queries read-only | Unit + browser smoke | No existe reporte/contrato implementado de saldos |
| Bitácora | PARTIAL | Listado, filtros, paginación y exportación | Pantalla y filtros | `APP24_C_BITACORA_REGISTRAR`, `APP24_Q_BITACORA_LISTAR` | Unit + smoke; eventos administrativos E2E | Exhaustive post-run scan no ejecutado; cobertura de eventos 401/403 y dominios no administrativos pendiente |
| Usuarios | COMPLETE | CRUD contractual, estado, perfil, vigencia y password | CRUD administrativo | Commands/queries `APP24_*_USUARIO_*` | 80/80 frontend, Admin CRUD 10/10 x2 | Creación browser no se repitió porque el fixture persistente ya existía |
| Perfiles | COMPLETE | CRUD contractual, estado y permisos | CRUD administrativo | Commands/queries `APP24_*_PERFIL_*` | Admin CRUD 10/10 x2 | Creación browser no se repitió por reutilización idempotente |
| Permisos | COMPLETE | Actividades read-only y reemplazo atómico | Edición, guards y sidebar | `APP24_Q_ACTIVIDADES_LISTAR`, `APP24_C_PERFIL_REEMPLAZAR_PERMISOS` | Autorización E2E PASS | No se permite inferir nuevas actividades fuera del catálogo |
| Facturación | PARTIAL | Plantilla, carga, validación, detalle y staging durable | Selección múltiple, preview y errores | `APP24_Q_FACTURACION_*`, `APP24_C_FACTURACION_CARGA_CREAR` | Unit + E2E focal | Confirmación hacia módulo operativo/legacy sin contrato autoritativo |
| Saldos | BLOCKED_DOMAIN | No hay contrato V1 suficiente | No hay módulo de saldos operativo | No ejecutar SP mutable por inferencia | Evidencia documental solamente | Falta fórmula, fuentes, corte, descargos/retornos y decisión funcional |

## Autenticación y autorización

- Login: implementado con endpoint público `POST /api/v1/auth/login`.
- JWT: filtro stateless, interceptor Angular y guards de autenticación/permisos.
- 401: entry point de API y manejo de token inválido/expirado en interceptor.
- 403: `@PreAuthorize` en controllers y `permissionGuard` en rutas.
- `/forbidden`: implementado y probado.
- Logout: implementado en `AuthService` y layout.
- Landing autenticada: `/dashboard`.
- Sesión expirada: hay comportamiento de interceptor hacia `/login`; no hay prueba E2E dedicada de expiración.
- RBAC: validado focalmente con usuario fixture: `/materiales` permitido, `/usuarios` denegado y API de usuarios `403`.

## Dashboard

El dashboard actual no es sólo un placeholder: carga el total de materiales
mediante `DashboardSummaryService`, condiciona los accesos por permisos y
muestra estados de carga/error. Sin embargo, el contenido es parcial: Productos,
Estructuras y operaciones se presentan como accesos descriptivos; los avisos y
el estado operativo son estáticos. No hay KPIs de entradas, salidas, consumo,
saldos o cargas.

Estado: `PARTIAL`.

## Facturación

| Capacidad | Estado | Evidencia |
|---|---|---|
| Plantilla | COMPLETE | GET de metadata y descarga de archivo; guard de plantilla activa |
| Carga/validación | COMPLETE | Multipart XLS/XLSX, límites de tamaño/cantidad y errores detallados |
| Staging durable | COMPLETE | Persistencia en `app24`, hash, detalle paginado y lectura posterior |
| Preview/readback | COMPLETE | Preview, conteos, errores por fila/columna y endpoint de detalle |
| Confirmación operativa | BLOCKED_EXTERNAL | La respuesta contractual mantiene `confirmacionDisponible=false`; no existe pipeline autoritativo para escribir al módulo operativo |

Se mantiene `FACTURACION_CONFIRM_BLOCKED_LEGACY_EXECUTION`. Staging funcional
no equivale a facturación completa. No se ejecutaron comandos legacy write.

## Saldos

- Estado: `BLOCKED_DOMAIN`.
- Evidencia: los requerimientos describen saldo inicial, entradas, consumo,
salidas/retornos, saldo final y fecha de corte, pero el código actual no expone
un endpoint/módulo de saldos ni una fórmula autoritativa.
- Falta decisión: definir fuente de cada componente, reglas de descargo/retorno,
fecha de corte, granularidad, tratamiento de históricos y contrato de reporte.
- Decisión de esta fase: no inferir reglas ni ejecutar SP mutables.

## Administración

Administración se considera `COMPLETE` para el alcance V1 implementado:

- Usuarios, perfiles y permisos tienen endpoints, commands, queries y SP
versionados.
- `RUN_1 = 10/10 PASS` y `RUN_2 = 10/10 PASS`.
- `ADMIN_USERS_CRUD_E2E = PASS`.
- `ADMIN_PROFILES_CRUD_E2E = PASS`.
- `ADMIN_PERMISSIONS_E2E = PASS`.
- `ADMIN_AUTHORIZATION_E2E = PASS`.
- Fixtures restaurados y sin duplicados.

Limitación no bloqueante: `AUDIT_LOG_EXHAUSTIVE_POST_RUN = NOT_EXECUTED`.
Las acciones generaron eventos, no se observaron enum errors ni HTTP 500, pero
no se recorrieron exhaustivamente todas las páginas de bitácora después de ambos
runs.

## Bitácora y enum drift

`BitacoraAccion` contiene actualmente acciones controladas para login, usuarios,
perfiles y cargas (`CARGA_VALIDADA`, `CARGA_CON_ERRORES`). Los use cases revisados
registran enums, no strings libres. Los SP de bitácora son:

- `APP24_C_BITACORA_REGISTRAR`
- `APP24_Q_BITACORA_LISTAR`

La pantalla y el reporte exponen filtros de fechas, usuario, módulo, resultado y
correlation ID. No se detectó en código revisado un `valueOf` sobre una acción
externa sin enum, pero la cobertura de acciones 401/403 y de todos los dominios
queda pendiente. Esta brecha es `SHOULD_FIX`, no un blocker funcional inmediato.

## Arquitectura y SP-first

- Commands y queries de negocio revisados usan `CallableStatement` con nombres de
SP versionados y `JdbcTemplate.call`.
- No se observó SQL inline de negocio en el escaneo estático.
- `SystemStatusController` usa `SELECT 1` únicamente como health check técnico;
se clasifica como excepción técnica documentada, no como violación SP-first.
- Los adapters de administración usan `appJdbcTemplate` y los catálogos/
operaciones usan `jdbcTemplate` del Módulo C.
- No se detectaron commands propios que escriban en `CALE_IMMEX`.
- La evidencia de las fases anteriores reporta `CALE_IMMEX writes = 0` y legacy
SP writes = 0.

## Datasources

La separación de origen está explícita en `DataSourceConfig`:

- `jdbcTemplate` primario: CALE_IMMEX, variables `DB_*`.
- `appJdbcTemplate` secundario: ANEXO24_DEV/app24, variables `APP_DB_*`.
- Administración y facturación usan `appJdbcTemplate`.
- Catálogos y operaciones read-only usan `jdbcTemplate`.

### Hallazgo de producción

`application-prod.yml` define `spring.datasource` y CORS, pero no define:

- `app.datasource.url`
- `app.datasource.username`
- `app.datasource.password`
- `jwt.secret`
- `jwt.expiration-minutes`

`DataSourceConfig` y el filtro JWT sí requieren esos beans/propiedades. Con la
configuración versionada actual no se puede afirmar que el perfil `prod` arranca
correctamente contra ambos orígenes y JWT.

Clasificación: `HIGH`, `INTERNAL_FIXABLE`, `MUST_FIX_BEFORE_V1`.
No se corrigió durante esta auditoría.

## Seguridad

- RBAC/JWT: implementados; autorización API y UI focal validada.
- Hash de password: BCrypt.
- CORS: patrón configurable en prod mediante `APP_CORS_ALLOWED_ORIGINS`.
- Secretos: no hardcodeados en YAML productivo; se resuelven por variables.
- Correlation ID: presente en errores/cargas/bitácora y commands auditados.
- Runtime DB least privilege: sigue siendo backlog conocido (`SECURITY_RUNTIME_LEAST_PRIVILEGE`); el usuario runtime continúa requiriendo privilegios que deben revisarse antes de producción.
- No se evaluó migración de usuario DB en esta fase.

## Frontend

- Lazy routes: 15 rutas funcionales descubiertas.
- Parent-relative imports en `frontend/src/app/**/*.ts`: 0.
- Guards: autenticación y permiso por ruta.
- Error global: interceptor y mensajes seguros en UI.
- Responsive: baseline PASS en 1440, 1024, 768 y 390.
- Accessibility smoke: incluido en baseline anterior.
- Bundle: build PASS con warnings existentes por presupuesto inicial y SCSS del
sidebar. Impacto clasificado `LOW`; no se marca como blocker sin evidencia de
regresión de carga.

## Tests

| Layer | Suite | Cobertura | Último resultado |
|---|---|---|---|
| Backend | 67 archivos de test Java | Unit/integration según configuración | No ejecutada en esta fase; baseline de esta rama anterior disponible |
| Frontend | 22 archivos spec | Componentes, servicios, mappers y rutas | `80/80 PASS` |
| Browser smoke | specs baseline | Rutas, responsive, billing, reports, forbidden | `34/34 PASS` |
| Admin CRUD | usuarios/perfiles/permisos/autorización | Escrituras sintéticas vía API/UI, restore e idempotencia | RUN_1 `10/10`, RUN_2 `10/10` |

### Gaps de cobertura E2E

- Administración: CRUD E2E real y autorización.
- Facturación: upload/staging y validación focal; no confirmación operativa.
- Catálogos: principalmente smoke read-only.
- Operaciones: principalmente smoke read-only.
- Reportes: smoke y contratos de API; no cobertura browser exhaustiva de cada
exportación.
- Bitácora: listado/reportes y eventos administrativos; scan exhaustivo
post-runs no ejecutado.
- Saldos: sin E2E porque está bloqueado por dominio.

Estos gaps no implican automáticamente un blocker; deben alinearse con el
alcance de release.

## Requerimientos vs implementación

| RF | Implementación/evidencia | Estado |
|---|---|---|
| RF-001 login y permisos | Auth, JWT, guards, `@PreAuthorize`, smoke y autorización | COMPLETE |
| RF-003 usuarios, perfiles y actividades | APIs, SP, unit, CRUD E2E e idempotencia | COMPLETE |
| RF-010 catálogos | Materiales, productos y estructuras read-only paginados | COMPLETE |
| RF-020 entradas, salidas, consumo y activo fijo | APIs/read-only, filtros por rango y browser smoke | COMPLETE |
| RF-024 saldos/descargos/retornos | Requerimiento documentado, sin fórmula/contrato completo de saldos | BLOCKED_DOMAIN |
| RF-030 reportes | Entradas, salidas, consumo y bitácora con exportación | PARTIAL |
| RF-036 exportar con resultados | Exportadores XLSX devuelven no-content sin filas | COMPLETE |
| RF-040 carga XLS/XLSX validada | Plantilla, upload, validación, preview y staging | PARTIAL por confirmación |
| RF-050 bitácora | Registro/listado/reportes y correlation ID | PARTIAL |

## Documentación

| Área | Estado | Observación |
|---|---|---|
| Arquitectura | ACTUAL | Refleja arquitectura modular y SP-first; validar continuamente contra nuevos módulos |
| BD/datasources | ACTUAL con gap operativo | La separación está documentada; falta cerrar configuración prod del app datasource/JWT |
| Contratos | ACTUAL | Controllers y SP versionados sostienen los módulos implementados |
| Administración | ACTUAL | Auditoría CRUD E2E integrada en `dev` |
| Facturación | ACTUAL, con alcance explícito | Staging documentado; confirmación legacy permanece bloqueada |
| Saldos | ACTUAL como pendiente | Requiere decisión de dominio; no debe presentarse como implementado |
| Bitácora | OUTDATED/PARCIAL | El documento marca pendientes de 401/403 y scan exhaustivo; requiere actualización cuando se cierre evidencia |
| Requerimientos | ACTUAL con ambigüedades heredadas | Saldos, retornos, lockout y campos legacy requieren decisión explícita |
| Release readiness | MISSING antes de este documento | Este archivo cubre la primera auditoría de brechas |

## Release blockers

### MUST_FIX_BEFORE_V1

1. **HIGH — Configuración `prod` incompleta para el segundo datasource y JWT.**
   Agregar/configurar de forma segura `app.datasource.*` y `jwt.*`, probar
   arranque con ambos orígenes y no publicar secretos.

### SHOULD_FIX_BEFORE_V1

1. Ejecutar un scan exhaustivo de Bitácora posterior a una ejecución CRUD y
   ampliar cobertura de acciones 401/403 si el contrato las exige.
2. Añadir una prueba de sesión expirada si la expiración JWT forma parte del
   criterio de release.
3. Revisar warnings de bundle/SCSS y establecer presupuesto aceptado o reducir
   tamaño; no es blocker funcional actual.
4. Aumentar E2E de exportaciones y contratos de reportes si el release requiere
   validación browser más allá del smoke.

### SCOPE DECISION REQUIRED

1. **Confirmación de facturación:** decidir el pipeline operativo autoritativo,
   ownership, SP/contrato y transacción. No ejecutar legacy write por inferencia.
2. **Saldos:** aprobar fórmula, fuentes, fecha de corte y relación con descargos/
   retornos antes de implementar.
3. Definir si dashboard V1 requiere KPIs operativos adicionales o si el resumen
   actual es suficiente.
4. Definir si lockout, verificación de correo y campos `Usuario SAT` pertenecen
   realmente al V1; no están en el contrato nuevo actual.

## Recommended next phases

1. `release-fix-prod-config`: completar y probar `application-prod.yml` para
   app datasource, JWT y arranque seguro.
2. `domain-decision-saldos`: cerrar contrato de saldos, descargos y retornos
   con evidencia autoritativa.
3. `billing-confirmation-contract`: definir pipeline de confirmación sin
   invocar legacy write hasta contar con aprobación.
4. `auditlog-completeness`: recorrer bitácora exhaustivamente y cubrir acciones
   HTTP/operativas faltantes.
5. `release-validation-v1`: ejecutar pruebas backend/frontend/browser completas
   sólo después de resolver los blockers y decisiones de alcance.

## Decision

- `CORE_V1_READY = NO`
- `INTERNAL_BLOCKERS = 1`
- `EXTERNAL_BLOCKERS = 0` como blockers técnicos inmediatos
- `SCOPE_DECISION_REQUIRED = YES` para confirmación de facturación y saldos
- `READY_FOR_FIX_PHASE = YES`

La integración de Administration CRUD está completa, pero el sistema completo no
está listo para declarar release V1 mientras la configuración productiva no
incluya ambos datasources y JWT, y mientras no se resuelvan las decisiones de
alcance de facturación y saldos.

## Git de la auditoría

- Rama: `feature/release-readiness-v1`
- HEAD: `db9a139f580b26196922f10cb57a5ae4a7d74cb4`
- Working tree: sólo `docs/03-diseno/mapeo-bitacora.md` ajeno
- Bitácora ajena: preservada, no modificada por esta fase
- Stashes: 2 intactos
- Commit/push de esta auditoría: no realizados
