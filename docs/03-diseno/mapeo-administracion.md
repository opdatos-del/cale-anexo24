# Administración (usuarios, perfiles y permisos)

## 1. Objetivo y decisión arquitectónica

Este documento define el estado del módulo Administración: modelo de datos,
dominio, autenticación, commands, permisos runtime y contratos V1. SP-1C cerró
la migración de commands a Stored Procedures app24.

**IMPLEMENTADO E INTEGRADO EN DEV:** FASE 1, FASE 2, FASE 3A y FASE 3B consumen
Stored Procedures mediante `appJdbcTemplate` y `appTransactionManager`.
**IMPLEMENTADO Y VALIDADO LIVE EN FASE 3C:** reset administrativo de contraseña.
Commands sensibles de Fase 3B conservan aislamiento `SERIALIZABLE`, locking
`UPDLOCK`/`HOLDLOCK` y guardrail atómico en SQL. CRUD de perfiles/actividades y
frontend de administración de perfiles/permisos permanecen pendientes.

Esta segunda pasada **cierra las decisiones funcionales/técnicas V1** previas a
cualquier command administrativo. No contradice la evidencia confirmada: la
amplía. Todo lo marcado `DECISIÓN V1` es contrato de diseño, no capacidad
actual.

> **DECISIÓN V1.** El módulo Administración usa `ANEXO24_DEV.app24`, no
> Módulo C. La regla STORED PROCEDURE FIRST aplica a operaciones funcionales:
> queries y commands se ejecutan mediante SP propios app24. La matriz de
> integración registra estas fuentes como `APP DATABASE`.

## 2. Fuentes de evidencia

| Fuente | Aporta | Estado |
|---|---|---|
| `infra/sql/02-app-schema.sql` | DDL de `app24` (PerfilApp, Actividad, PerfilActividad, UsuarioApp, BitacoraEvento, CargaFacturacion, ErrorCarga, ConfiguracionPlantilla) | **CONFIRMADO** para esquema versionado |
| `infra/sql/03-app-seed-security.sql` | Perfiles `ADMINISTRADOR`/`CONSULTA`, 12 actividades, asignaciones y usuario `admin` | **CONFIRMADO** |
| `infra/sql/04-app-runtime-permissions.sql` | Política mínima por objeto para `anexo24_app` vía rol `app24_runtime` | **APLICADO LIVE** dos veces; idempotencia PASS; EXECUTE-only |
| `administration/users/**` | Puertos separados de autenticación, lectura y command; GETs, POST, PUT y PATCH de usuarios | **IMPLEMENTADO, INTEGRADO EN DEV, VALIDADO LIVE** |
| `security/**` | Login, JWT, filtro, contexto de usuario, entry point y excepciones | **IMPLEMENTADO EN REPOSITORIO** |
| `auditlog/**` | Catálogo de módulos/acciones y writer append-only | **IMPLEMENTADO, INTEGRADO EN DEV**; login y acciones de usuarios Fase 3A/3B conectados |
| `shared/config/DataSourceConfig.java` | Dos orígenes, templates y transaction managers explícitos para Módulo C y `app24` | **CONFIRMADO**; `transactionManager` y `appTransactionManager` documentados en §29 |
| `frontend/src/app/features/administration/**` | Solo `audit-log` implementado; `users`, `profiles`, `permissions` vacíos (`.gitkeep`) | **IMPLEMENTADO EN REPOSITORIO** (bitácora); **NO IMPLEMENTADO** (resto) |
| `docs/03-diseno/modelo-datos.md`, `docs/04-desarrollo/api.md` | Modelo lógico y contrato API documentado | **CONFIRMADO**; usuarios GET/POST/PUT/PATCH están implementados e integrados en DEV |

SP-1C validó metadata y ejecutó pruebas LIVE sintéticas reversibles en
`ANEXO24_DEV`; no se consultó ni modificó `CALE_IMMEX`. SP-1E aplicó
`04-app-runtime-permissions.sql` dos veces en `ANEXO24_DEV`; idempotencia PASS.

## 3. Modelo de datos `app24` (DDL versionado)

### 3.1 PerfilApp

| Campo | DDL | Conclusión |
|---|---|---|
| `id` | `BIGINT IDENTITY(1,1)` PK | Identidad técnica |
| `nombre` | `VARCHAR(80) NOT NULL UNIQUE` | Nombre canónico de perfil |
| `estado` | `VARCHAR(20) DEFAULT 'ACTIVO'` | **CONFIRMADO** catálogo V1: `ACTIVO`/`INACTIVO` (§12); sin `CHECK` en DDL |

### 3.2 Actividad

| Campo | DDL | Conclusión |
|---|---|---|
| `id` | `BIGINT IDENTITY(1,1)` PK | Identidad técnica |
| `clave` | `VARCHAR(60) NOT NULL UNIQUE` | Clave de permiso de API (`recurso_ACCION`) |
| `nombre` | `VARCHAR(120) NOT NULL` | Etiqueta |
| `recurso` | `VARCHAR(120) NOT NULL` | Recurso afectado |
| `accion` | `VARCHAR(40) NOT NULL` | Acción (`CONSULTAR`, `ADMINISTRAR`, ...) |

**CONFIRMADO:** la clave de permiso coincide con lo que `@PreAuthorize` evalúa
(`MATERIALES_CONSULTAR`, `BITACORA_CONSULTAR`, ...).

### 3.3 PerfilActividad

`perfil_id` + `actividad_id` PK compuesta; FKs a `PerfilApp` y `Actividad`. Sin
`ON DELETE CASCADE`: el borrado físico de perfil o actividad con asignaciones
fallará por `NO ACTION`. **DECISIÓN V1:** inactivación, no borrado físico
(§17, §18).

### 3.4 UsuarioApp

| Campo | DDL | Conclusión |
|---|---|---|
| `id` | `BIGINT IDENTITY(1,1)` PK | Identidad técnica |
| `clave` | `VARCHAR(30) NOT NULL UNIQUE` | Login idempotente; **NO editable en V1** (§14) |
| `correo` | `VARCHAR(150) NOT NULL UNIQUE` | Identidad correo |
| `password_hash` | `VARCHAR(100) NOT NULL` | bcrypt (60 chars, ADR-002); hash seed `$2b$10$...` compatible |
| `estado` | `VARCHAR(20) DEFAULT 'ACTIVO'` | **CONFIRMADO** catálogo V1: `ACTIVO`/`INACTIVO` (§12); sin `CHECK` en DDL |
| `vigencia` | `DATE NULL` | Vencimiento opcional de cuenta (§19) |
| `perfil_id` | `BIGINT NOT NULL` FK a `PerfilApp` | Un solo perfil por usuario en V1 |

**CONFIRMADO:** coincide con `modelo-datos.md` («N:1 PerfilApp»). La FK exige
`perfil_id` obligatorio: no existe usuario sin perfil.

### 3.5 Tablas complementarias (fuera del alcance V1 de Administración)

`BitacoraEvento` (auditado en `mapeo-bitacora.md`), `CargaFacturacion`,
`ErrorCarga` y `ConfiguracionPlantilla` existen en DDL sin backend de
escritura. **PENDIENTE:** su administración/consulta es de otros dominios.

## 4. Dominio actual (`administration/users`)

| Archivo | Contenido | Conclusión |
|---|---|---|
| `UsuarioApp` | record: `id`, `clave`, `nombre`, `correo`, `passwordHash`, `estado`, `vigencia`, `perfilId`; `estaActiva()` = `ACTIVO` y (`vigencia == null` o no vencida) | **IMPLEMENTADO EN REPOSITORIO**; lógica de vigencia solo en memoria, no en SQL |
| `UsuarioAcceso` | record: `perfilId`, `perfilEstado`, `permisos`; `perfilActivo()` = `ACTIVO` ignorando mayúsculas; permisos con copia inmutable | **IMPLEMENTADO EN REPOSITORIO** (FASE 1); distingue perfil ACTIVO con/sin permisos e INACTIVO |
| `UsuarioRepository` | `findByClave(String)`, `findAccesoByUsuario(Long)` → `Optional<UsuarioAcceso>` | **IMPLEMENTADO EN REPOSITORIO** (FASE 1); proyección de acceso sin filtrar el estado del perfil |
| `UsuarioJdbcAdapter` | `@Qualifier("appJdbcTemplate")`; consume `APP24_Q_USUARIO_POR_CLAVE` y `APP24_Q_USUARIO_ACCESO` | **IMPLEMENTADO EN REPOSITORIO** (SP-1B); sin SQL funcional inline |
| `UsuarioAdministracion` | record: `id`, `clave`, `nombre`, `correo`, `estado`, `vigencia`, `perfilId`, `perfilNombre` | **IMPLEMENTADO EN REPOSITORIO** (FASE 2); sin secretos, jamás `passwordHash` |
| `UsuarioConsultaRepository` | `findPage(...)` y `findById(Long)` → `Optional<UsuarioAdministracion>` | **IMPLEMENTADO EN REPOSITORIO** (FASE 2); puerto separado de autenticación |
| `UsuarioConsultaJdbcAdapter` | `@Qualifier("appJdbcTemplate")`; consume `APP24_Q_USUARIOS_LISTAR` y `APP24_Q_USUARIO_OBTENER` | **IMPLEMENTADO EN REPOSITORIO** (SP-1B); no selecciona `password_hash` |
| `UsuarioComandoJdbcAdapter` | Invoca cinco `APP24_C_USUARIO_*`; traduce códigos SQL 51101–51108/51150 y 2601/2627 | **IMPLEMENTADO EN REPOSITORIO** (SP-1C); sin SQL funcional inline |
| `BitacoraJdbcAdapter` | Invoca `APP24_C_BITACORA_REGISTRAR`; valida `EventoId` | **IMPLEMENTADO EN REPOSITORIO** (SP-1C); sin SQL funcional inline |
| `ListarUsuariosUseCase` / `ObtenerUsuarioUseCase` | normalizan filtros y validan longitudes, estado, perfilId y paginación; 404 si no existe | **IMPLEMENTADO EN REPOSITORIO** (FASE 2) |
| `UsuarioAdministracionController` + DTOs | GETs FASE 2, `POST`/`PUT` FASE 3A y PATCH FASE 3B con `USUARIOS_ADMINISTRAR` | **IMPLEMENTADO, INTEGRADO EN DEV, RUNTIME READY** |
| Perfiles read-only Fase 5A | `GET /administracion/perfiles`; filtros `nombre` parcial escapado/`estado`/paginación; item `id`, `nombre`, `estado`, `cantidadPermisos` | **IMPLEMENTADO EN BACKEND**; lectura con `USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR` |

**CONFIRMADO:** el paquete `administration/users` conserva la separación:
autenticación (`UsuarioRepository`/`UsuarioJdbcAdapter`) vs. consulta
administrativa read-only (`UsuarioConsultaRepository`/`UsuarioConsultaJdbcAdapter`).
No hay dominio para PerfilApp ni Actividad; sus invariantes de persistencia son validadas por commands SQL. `PerfilReferenciaRepository` fue eliminado en SP-1C.

## 5. Autenticación (`security/**`)

| Componente | Comportamiento | Conclusión |
|---|---|---|
| `LoginController` | `POST /api/v1/auth/login`, público; pasa correlationId normalizado | **IMPLEMENTADO EN REPOSITORIO** |
| `LoginRequest` | `clave` y `password` `@NotBlank` | **IMPLEMENTADO EN REPOSITORIO** |
| `LoginResponse` | `token`, `expiraEn`, `usuario`, `permisos` | **IMPLEMENTADO EN REPOSITORIO** |
| `LoginService` | orden fijo: `findByClave` → `estaActiva()` → `passwordEncoder.matches` → `findAccesoByUsuario` → `perfilActivo()`; 401 genérico en cada fallo; `LOGIN_OK`/`LOGIN_FALLIDO`; JWT con permisos (lista vacía permitida) | **IMPLEMENTADO EN REPOSITORIO** (FASE 1) |
| `JwtTokenService` | HS256; claims `subject=clave`, `uid`, `auth`; expiración `${jwt.expiration-minutes}` | **IMPLEMENTADO EN REPOSITORIO** |
| `JwtAuthFilter` | Bearer; valida firma; `uid` numérico entero positivo obligatorio; falla → anónimo (fail-closed) | **IMPLEMENTADO EN REPOSITORIO** |
| `AuthenticatedUserPrincipal` / `AuthenticatedUserContext` | Identidad confiable desde contexto; `currentUser()` solo con principal esperado | **IMPLEMENTADO EN REPOSITORIO** |
| `SecurityConfig` | Stateless; `permitAll` login/health/info/swagger; resto `authenticated`; `@EnableMethodSecurity`; bcrypt | **IMPLEMENTADO EN REPOSITORIO** |
| `ApiAuthenticationEntryPoint` | 401 JSON con correlación | **IMPLEMENTADO EN REPOSITORIO** |
| `GlobalExceptionHandler` | 401 credenciales, 403 acceso, 401 auth, 400 validación/formato, 404 `RECURSO_NO_ENCONTRADO`, 409 `RECURSO_DUPLICADO`/`ESTADO_INCOMPATIBLE`, 503 datos, 500 inesperado | **IMPLEMENTADO EN REPOSITORIO** (FASE 1) |

**CONFIRMADO:** la autorización por endpoint usa `@PreAuthorize("hasAuthority('...')")`
— 9 controladores existentes (bitácora, materiales, productos, estructuras,
entradas, salidas, materiales utilizados, activos fijos). La autorización recae
enteramente en el claim `auth` del token.

## 6. HALLAZGO HISTÓRICO — permisos sin validar `PerfilApp.estado` (RESUELTO EN FASE 1)

Antes (FASE 0): `UsuarioJdbcAdapter.findPermisosByUsuario` ejecutaba:

```sql
SELECT a.clave FROM app24.UsuarioApp u
JOIN app24.PerfilActividad pa ON pa.perfil_id = u.perfil_id
JOIN app24.Actividad a ON a.id = pa.actividad_id
WHERE u.id = ? AND u.estado = 'ACTIVO'
```

**HISTÓRICO / RESUELTO:** la implementación anterior filtraba
`UsuarioApp.estado = 'ACTIVO'` pero no consultaba `PerfilApp.estado`. La
implementación vigente usa `APP24_Q_USUARIO_ACCESO`, devuelve `perfil_estado` sin
filtrarlo y `LoginService` decide explícitamente si el perfil permite login.

**Consecuencia histórica:** un perfil `INACTIVO` podía seguir concediendo
permisos. Hallazgo cerrado: perfil INACTIVO produce rechazo genérico; perfil
ACTIVO sin permisos sigue siendo distinguible y permitido por la política V1.

La decisión de diseño que cierra este hallazgo está en **§17 — Semántica de
perfil INACTIVO** (login debe rechazar, no emitir JWT vacío) y **§18 —
Sesiones JWT ya emitidas** (consistencia eventual; sin revocación inmediata en
V1). **RESUELTO EN FASE 1** (IMPLEMENTADO, INTEGRADO EN DEV, VALIDADO LIVE):

`findAccesoByUsuario` consulta el estado del perfil **sin filtrarlo** y el login
decide (401 genérico si INACTIVO, ausente o inconsistente):

Contrato vigente: `UsuarioJdbcAdapter` invoca
`app24.APP24_Q_USUARIO_ACCESO`; el SP devuelve `perfil_id`, `perfil_estado` y
`permiso`, con cero permisos representado por una fila cuyo permiso es `NULL`.

El perfil `ACTIVO` sin permisos autentica con JWT de `auth` vacía; el rechazo
por perfil se registra en bitácora como `LOGIN_FALLIDO` con el actor real.

## 7. Bitácora y eventos de Administración

**CONFIRMADO:** `BitacoraModulo` incluye `ADMINISTRACION`; `BitacoraAccion`
define `LOGIN_OK`, `LOGIN_FALLIDO`, `USUARIO_CREADO` y `USUARIO_ACTUALIZADO`.

En Fases 3A/3B/3C, el writer append-only (`RegistrarEventoBitacoraService` +
`BitacoraJdbcAdapter`) registra creación, edición, estado, perfil, vigencia y
restablecimiento de contraseña con `AuthenticatedUserContext` como actor.
`USUARIO_PASSWORD_RESTABLECIDA` guarda sólo `usuarioObjetivoId`; acciones futuras
de perfiles permanecen pendientes.

**CONFIRMADO (hallazgo de diseño):** `AuthenticationEntryPoint` responde 401 sin
registrar evento; 403 de `@PreAuthorize` tampoco registra evento. Política de
registro de 401/403 sigue **PENDIENTE** (mapeo-bitacora.md §10).

## 8. Semilla y permisos de API

**CONFIRMADO (seed):** `03-app-seed-security.sql`:

| Perfil | Permisos |
|---|---|
| `ADMINISTRADOR` | Todas las actividades existentes (cross join) |
| `CONSULTA` | Solo `MATERIALES_CONSULTAR`, `PRODUCTOS_CONSULTAR`, `ESTRUCTURAS_CONSULTAR`, `OPERACIONES_CONSULTAR` |

**CONFIRMADO:** 12 actividades sembradas: 4 consultas de catálogos,
`OPERACIONES_CONSULTAR`, `REPORTES_GENERAR`, `REPORTES_EXPORTAR`,
`FACTURACION_CARGAR`, `FACTURACION_GUARDAR`, `BITACORA_CONSULTAR`,
`USUARIOS_ADMINISTRAR`, `PERFILES_ADMINISTRAR`, `ACTIVIDADES_ADMINISTRAR`.

**CONFIRMADO:** `CONSULTA` **no** incluye `BITACORA_CONSULTAR` ni permisos de
administración. Usuario `admin` (hash bcrypt de reemplazo obligatorio en
producción) pertenece a `ADMINISTRADOR`.

**IMPLEMENTADO:** `USUARIOS_ADMINISTRAR` protege `GET` listado, `GET` detalle,
`POST`, `PUT`, los tres `PATCH` y `POST /{id}/password` de `/api/v1/administracion/usuarios`.
`GET /api/v1/administracion/perfiles` (Fase 5A) admite
`USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR` exclusivamente como lectura de
catálogo. Los futuros commands de perfiles serán exclusivos de
`PERFILES_ADMINISTRAR`; `ACTIVIDADES_ADMINISTRAR` está reservado, sin uso V1.

## 9. Permisos runtime de base de datos — estado actual

**CONFIRMADO LIVE (SP-1E):** `04-app-runtime-permissions.sql` fue aplicado dos veces en `ANEXO24_DEV` y dejó `app24_runtime` como rol de ejecución, con `anexo24_app` como miembro.

Estado efectivo:

| Alcance | Estado |
|---|---|
| `db_datareader` / `db_datawriter` | membership removida |
| EXECUTE database/global | revocado |
| tablas `app24` objetivo | SELECT/INSERT/UPDATE/DELETE directos = 0 |
| 13 SP app24 | EXECUTE específico concedido |
| idempotencia | segunda ejecución PASS, sin diferencias |

Ownership chain validada de forma práctica: impersonation de `anexo24_app` ejecutó `APP24_Q_USUARIOS_LISTAR` sin SELECT directo sobre tablas; SELECT directo `TOP (0)` fue denegado.

## 10. Frontend

| Ruta | Estado |
|---|---|
| `features/administration/audit-log` | **IMPLEMENTADO EN REPOSITORIO** — ruta `/bitacora` lazy con `permissionGuard` + `data.permission: 'BITACORA_CONSULTAR'`; sidebar «Administración» → «Bitácora» (`manage_search`) |
| `features/administration/users` | **FRONTEND USUARIOS V1 COMPLETO (FASE 4B)** — ruta `/usuarios`; listado, filtros incluidos perfiles, paginación, alta, edición de nombre/correo, estado, cambio de perfil, vigencia y restablecimiento de contraseña |
| `features/administration/profiles` | **INFRAESTRUCTURA READ-ONLY IMPLEMENTADA** — catálogo reutilizable sin ruta, pantalla ni CRUD de perfiles |
| `features/administration/permissions` | **NO IMPLEMENTADO** — solo `.gitkeep` |

**CONFIRMADO:** `permissionGuard` redirige a `/forbidden` cuando el usuario no
tiene el permiso; `app.routes.ts` usa el patrón `canActivate + data.permission`
para las rutas protegidas. `users` está implementado e integrado en DEV. Los
placeholders `profiles`/`permissions` aún no tienen rutas ni menú; la estructura
hexagonal de `audit-log` es el patrón a replicar cuando se implementen.

## 11. Documentación declarada vs. realidad

`docs/04-desarrollo/api.md` (§ endpoints) declara: «Los endpoints de
administración siguen `/usuarios`, `/perfiles` y `/actividades»...».

**HISTÓRICO / PARCIALMENTE RESUELTO:** esa declaración era prospectiva. Las rutas
reales implementadas son bajo `/api/v1/administracion/*` (§32), incluidos
usuarios y el listado read-only de perfiles de Fase 5A. Actividades y los
commands de perfiles siguen pendientes. `api.md` refleja las rutas y permisos
implementados.

## 12. Estados — catálogo V1 cerrado

| Entidad | Catálogo V1 | Validación |
|---|---|---|
| `UsuarioApp.estado` | `ACTIVO`, `INACTIVO` | Aplicación/dominio obligatoria |
| `PerfilApp.estado` | `ACTIVO`, `INACTIVO` | Aplicación/dominio obligatoria |

**CONFIRMADO:** el DDL no define `CHECK` para ninguno de los dos. Por tanto la
validación vive en application/domain (enums/constantes + validación de
request), no en la base. Un estado libre (`BORRADO`, `SUSPENDIDO`, ...) se
rechaza con 400 hasta que el catálogo se apruebe por decisión explícita.

**PENDIENTE NO BLOQUEANTE:** evaluar `CHECK` SQL después de validada la lógica
de aplicación. **No se modifica DDL en esta fase.**

## 13. Ciclo de vida de UsuarioApp — DECISIÓN V1

| Operación | V1 | Contracto/nota |
|---|---|---|
| CREAR | Sí | `POST /api/v1/administracion/usuarios`; estado inicial `ACTIVO`, vigencia opcional, perfil obligatorio (§33) |
| LISTAR | Sí | `GET /api/v1/administracion/usuarios`; paginado + filtros (§33, §37) |
| CONSULTAR | Sí | `GET /api/v1/administracion/usuarios/{id}`; nunca expone `password_hash` |
| EDITAR DATOS | Sí | `PUT /api/v1/administracion/usuarios/{id}` — solo `nombre`, `correo` (§14) |
| ACTIVAR | Sí | `PATCH /api/v1/administracion/usuarios/{id}/estado` → `ACTIVO` |
| INACTIVAR | Sí | mismo endpoint → `INACTIVO`; con guardrail de auto-bloqueo (§25) |
| CAMBIAR PERFIL | Sí | `PATCH /api/v1/administracion/usuarios/{id}/perfil` → `perfilId` |
| CAMBIAR VIGENCIA | Sí | `PATCH /api/v1/administracion/usuarios/{id}/vigencia` → fecha o `null` (§19) |
| RESTABLECER CONTRASEÑA | Sí (Fase 3C) | `POST /api/v1/administracion/usuarios/{id}/password`; BCrypt en Java, sólo hash a SQL (§15) |
| **DELETE físico** | **NO** | FK `BitacoraEvento.usuario_id`, FK `CargaFacturacion.usuario_id`, trazabilidad histórica; `estado` cubre la necesidad |

**DECISIÓN V1 — NO DELETE físico de UsuarioApp.** Justificación: (a) FK de
`BitacoraEvento.usuario_id` sin cascade conserva el histórico de auditoría; (b)
FK de `CargaFacturacion.usuario_id` liga cargas a un actor; (c) trazabilidad
histórica de quién hizo qué; (d) la columna `estado` ya permite usar
`INACTIVO`. No se propone endpoint `DELETE /.../usuarios/{id}` en V1.

## 14. Datos editables de usuario — DECISIÓN V1

| Campo | Editable V1 | Vía |
|---|---|---|
| `nombre` | Sí | `PUT /{id}` |
| `correo` | Sí | `PUT /{id}` |
| `estado` | Sí | `PATCH /{id}/estado` |
| `vigencia` | Sí | `PATCH /{id}/vigencia` |
| `perfilId` | Sí | `PATCH /{id}/perfil` |
| `clave` | **NO** (inmutable tras creación) | — |

**DECISIÓN V1 — `clave` NO editable después de crear el usuario.** Motivo: (a)
la clave es `subject` del JWT y el `LoginRequest` valida contra ella; (b) la
auditabilidad/correlación histórica (incl. `BitacoraEvento`) se apoya en una
identidad estable; (c) no existe necesidad funcional demostrada de rename. No
se encontró evidencia contraria en código ni documentos. Si el negocio llegara
a exigirlo, será una decisión separada con migración de identidad y Bitácora
explícita.

## 15. Contraseñas — comportamiento real y decisiones

**CONFIRMADO (código):** `SecurityConfig` declara
`new BCryptPasswordEncoder()` — fuerza bcrypt **10** por defecto. El seed
`03-app-seed-security.sql` usa hash `$2b$10$n9NvR0IcyoA0SnbZbiYi2ezi3wyOfj26dpQNaIFBgfOJXUYKhn3.K`
(coste 10, formato bcrypt compatible).

**DECISIÓN V1:**

- **CREAR USUARIO:** recibe la contraseña inicial solo en el request de
  creación (`POST`); se codifica con `PasswordEncoder.encode()`; **solo
  `password_hash` llega a DB**.
- **RESTABLECER CONTRASEÑA (IMPLEMENTADO / VALIDADO LIVE):** endpoint/command
  separado (`POST /{id}/password`), no la edición general (`PUT`). Reutiliza
  `PasswordPolicy`, codifica BCrypt en Java, ejecuta
  `APP24_C_USUARIO_RESTABLECER_PASSWORD` y registra evento propio en la misma
  transacción `appTransactionManager`. No revoca JWT existentes.

**PROHIBIDO (por diseño):** devolver o registrar —en response, logs, Bitácora o
frontend— `password`, `password_hash`, la contraseña inicial, el hash anterior
ni el hash nuevo.

**PENDIENTE / FUERA DE V1** (no inventar columnas): `must_change_password`,
password history, recovery token, reset token, MFA. No existen hoy y no se
agregan al esquema sin decisión explícita.

## 16. Unicidad y conflictos — DECISIÓN V1

**CONFIRMADO (DDL):** restricciones únicas aplicadas:

| Columna | Restricción |
|---|---|
| `UsuarioApp.clave` | `UNIQUE` |
| `UsuarioApp.correo` | `UNIQUE` |
| `PerfilApp.nombre` | `UNIQUE` |
| `Actividad.clave` | `UNIQUE` |

**DECISIÓN V1 — contrato candidato:** un duplicado funcional conocido (clave,
correo o nombre de perfil ya existentes) responde **HTTP 409 CONFLICT** con
código `RECURSO_DUPLICADO` y mensaje genérico accionable. Regla: detectar el
duplicado con consulta previa parametrizada **o** capturar
`DuplicateKeyException`/`DataIntegrityViolationException` y traducirla; **nunca**
se exponen `SQLException`, nombres de constraint ni mensajes de SQL Server.

Nota: `GlobalExceptionHandler` hoy traduce `DataAccessException` a 503. Se
añadirá el handler 409 sin filtrar detalles en la fase de implementación. **No
implementar todavía.**

## 17. Semántica de perfil INACTIVO — DECISIÓN V1

El hallazgo §6 se cierra con **decisión, no con parche de filtrado**:

- Un usuario cuyo `PerfilApp` esté `INACTIVO` **no debe obtener una sesión
  utilizable**.
- **NO basta** con filtrar sus permisos y emitir un JWT vacío: eso crea una
  sesión autenticada sin capacidades y complica el diagnóstico.
- **Al implementar:** el login debe rechazar genéricamente el acceso como
  cuenta no disponible — mismo contrato externo actual:

```text
401 CREDENCIALES_INVALIDAS
"Credenciales inválidas o cuenta no disponible."
```

- **Prohibido** revelar «perfil inactivo» (ni en message, ni code, ni
  Bitácora pública).

**Precisión de hardening (DECISIÓN V1):** la validez del perfil se comprueba
**explícitamente**, nunca se infiere por la lista de permisos. **NO usar
`permisos.isEmpty()` como señal de perfil INACTIVO**: un perfil ACTIVO puede
existir legítimamente con cero actividades asignadas. Orden de validación en
login:

1. usuario activo (`UsuarioApp.estado = 'ACTIVO'`);
2. usuario vigente (`vigencia` inclusiva, §19);
3. perfil existente;
4. perfil ACTIVO (`PerfilApp.estado = 'ACTIVO'`) — **validación explícita**;
5. solo después: cargar permisos.

**Opciones técnicas aceptables para FASE 1** (preferir la que mantenga
cohesión y evite consultas innecesarias):

- **A.** ampliar la consulta/puerto de autenticación para devolver un contexto
  de autenticación con el estado del perfil; o
- **B.** añadir una consulta explícita `isPerfilActivo(perfilId)` (o
  equivalente).

**Caso perfil ACTIVO con cero permisos:** puede autenticarse si esa es la
política existente, generando una sesión sin `authorities` — pero **nunca** se
confunde con perfil INACTIVO. Prohibir perfiles activos sin permisos sería una
regla distinta y **NO está aprobada en V1**.

**Estado: IMPLEMENTADO, INTEGRADO EN DEV Y VALIDADO LIVE.** Fase 1 cerrada con
SP de acceso, decisión explícita en `LoginService` y tests.

## 18. Sesiones JWT ya emitidas — DECISIÓN V1

**CONFIRMADO POR ARQUITECTURA ACTUAL (código):** el JWT incluye claims
`subject` (clave), `uid` y `auth` (lista de autoridades), y es **stateless**.
`JwtAuthFilter` valida firma, expiración y claims, pero **no vuelve a
consultar** `UsuarioApp.estado`, `vigencia`, `PerfilApp.estado` ni
`PerfilActividad` en cada request.

Por tanto, si después del login se inactiva usuario, vence la vigencia, cambia
el perfil, se revoca un permiso o se inactiva un perfil, **un JWT ya emitido
conserva sus `authorities` hasta expirar**. La expiración la define
`jwt.expiration-minutes` (`JwtTokenService`), que es **CONFIGURABLE POR
AMBIENTE**: `application-local.yml` la inyecta como `${JWT_EXPIRATION_MINUTES}`
(sin default) y `backend/.env.example` muestra `480` solo como ejemplo
versionado. No afirmar un valor fijo de política: ni 15 ni 480 están
demostrados para producción.

**DECISIÓN V1:** aceptar **consistencia eventual hasta la expiración
configurada** del JWT. La duración debe mantenerse suficientemente acotada
según la política de despliegue/seguridad; el valor real de producción queda
**PENDIENTE DE VALIDAR** (no se fija 15 ni 480 como política V1). La
alternativa de revocación inmediata (denylist/JTI indexado, versión de token,
re-validación por request) se marca como **trabajo adicional, NO capacidad
actual**, y solo si auditoría/negocio la exigen.

## 19. Vigencia — semántica exacta

`UsuarioApp.estaActiva()` evalúa:

| Caso | Resultado |
|---|---|
| `vigencia == null` | Sin vencimiento definido → válida mientras el estado sea ACTIVO |
| `vigencia >= LocalDate.now()` | Válida (fecha inclusiva) |
| `vigencia < LocalDate.now()` | Vencida → cuenta no disponible |

**CONFIRMADO (código):** `!vigencia.isBefore(LocalDate.now())` — el día de la
fecha de vigencia sigue siendo válido (inclusivo). La regla vive **en memoria**
en el dominio; SQL Server no la valida.

**Advertencia operacional:** `LocalDate.now()` usa el timezone de la JVM. **No**
convertir esto en un problema de `Instant`: el campo SQL es `DATE` (fecha
civil, sin hora). La consideración de despliegue es fijar/documentar el
timezone de JVM (p. ej. `-Duser.timezone`) consistente con la región del
negocio; no se requiere cambio de esquema.

## 20. Perfil — casos de uso V1

**DECISIÓN V1:**

| Operación | V1 | Contracto (§34) |
|---|---|---|
| LISTAR | Sí | `GET /api/v1/administracion/perfiles` |
| CREAR | Sí | `POST /api/v1/administracion/perfiles` — `nombre` obligatorio y no vacío |
| EDITAR NOMBRE | Sí | `PUT /api/v1/administracion/perfiles/{id}` |
| ACTIVAR | Sí | `PATCH /{id}/estado` → `ACTIVO` |
| INACTIVAR | Sí | `PATCH /{id}/estado` → `INACTIVO` (§21) |
| CONSULTAR PERMISOS | Sí | `GET /{id}/permisos` (o incluido en detalle) |
| REEMPLAZAR ASIGNACIÓN DE PERMISOS | Sí | `PUT /{id}/permisos` — reemplazo transaccional (§24) |
| **DELETE físico** | **NO** | §26 |

Reglas: no crear perfil sin `nombre` válido (no blank, dentro de 80 chars,
único); no aceptar `estado` libre (solo `ACTIVO`/`INACTIVO`, §12).

## 21. Inactivación de perfil — DECISIÓN V1

**Se permite inactivar perfiles** (no se bloquea la operación en general),
con estas reglas:

1. **Login debe considerar `PerfilApp.estado`** — sin esto la inactivación no
   tiene efecto (§17).
2. **No puede provocar bloqueo administrativo total** — se evalúa la
   capacidad administrativa efectiva residual (§25): si únicamente el actor y
   el perfil a inactivar la proveen, la operación se rechaza.
3. **Los usuarios asociados permanecen en DB** — su fila `UsuarioApp` no se
   toca; quedan con perfil inactivo y su login se rechaza (§17) hasta que se
   les asigne otro perfil o se reactive su perfil.
4. **El perfil puede reactivarse** (`PATCH .../estado` → `ACTIVO`), restaurando
   la asignación de permisos intacta.

**NO** se migra automáticamente a los usuarios a otro perfil. **NO** se borra
`PerfilActividad` al inactivar (la asignación se conserva; el reemplazo solo
ocurre vía `PUT /{id}/permisos`, §24).

## 22. Actividades — catálogo controlado por código/seed

**DECISIÓN V1: `Actividad` es CATÁLOGO CONTROLADO POR CÓDIGO/SEED.** No hay
CRUD arbitrario de Actividad.

Justificación: crear una fila `FOO_ADMINISTRAR` no crea automáticamente un
`@PreAuthorize("hasAuthority('FOO_ADMINISTRAR')")` ni la lógica backend/
frontend correspondiente. El permiso real existe cuando código + catálogo se
sincronizan; la creación libre de filas produce permisos fantasma.

| Operación | V1 |
|---|---|
| GET/listado de actividades | **Sí** — alimenta la pantalla de edición de permisos |
| POST Actividad | **NO** |
| PUT Actividad | **NO** |
| DELETE Actividad | **NO** |

## 23. `ACTIVIDADES_ADMINISTRAR` — significado concreto

**DECISIÓN V1:** **NO usar `ACTIVIDADES_ADMINISTRAR` para crear/modificar el
catálogo `Actividad`.** Queda **RESERVADO / NO UTILIZADO EN V1** hasta definir
un mantenimiento formal del catálogo (migración/seed + cambio de código
coordinado).

- La **asignación/revocación de actividades a perfiles** se protege con
  `PERFILES_ADMINISTRAR`: modifica el agregado/configuración del perfil.
- El **GET del catálogo `Actividad`** necesario para editar perfiles también se
  protege con `PERFILES_ADMINISTRAR`.
- **PENDIENTE (no ahora):** posible limpieza futura del seed si
  `ACTIVIDADES_ADMINISTRAR` nunca se usa. No modificar seed en esta fase.

## 24. PerfilActividad — modelo y command

**CONFIRMADO (DDL + modelo):**

```text
UsuarioApp   N → 1   PerfilApp
PerfilApp    N ↔ N   Actividad   (mediante PerfilActividad)
```

Los permisos de un usuario se derivan **SIEMPRE del perfil**
(consulta de acceso `findAccesoByUsuario`). **NO implementar permisos directos
por usuario en V1** — exigiría cambio DDL y complejiza la auditoría.

**DECISIÓN V1 — command de reemplazo:** implementar
`PUT /{id}/permisos` que reemplace el conjunto completo de actividades del
perfil **de forma transaccional** (`DELETE` de la asignación antigua +
`INSERT` de la nueva dentro de la misma transacción, §29), en vez de endpoints
incrementales de agregar/quitar uno a uno. Simplifica consistencia: el estado
post-command es exactamente el conjunto enviado, y hay un único evento de
Bitácora.

## 25. Auto-bloqueo administrativo — guardrails mínimos V1

**DECISIÓN V1 — guardrails:**

- **A.** Un usuario **no puede inactivarse a sí mismo** desde el flujo
  administrativo.
- **B.** Un usuario **no puede cambiar su propio perfil** vía el flujo
  administrativo V1.
- **C.** No se permite una operación que deje al sistema **sin al menos un
  usuario**: `ACTIVO` + vigente + con `PerfilApp ACTIVO` + con **capacidad
  administrativa efectiva**.

**Capacidad administrativa mínima (definición V1):** el conjunto efectivo de
permisos del usuario debe incluir `USUARIOS_ADMINISTRAR` **y**
`PERFILES_ADMINISTRAR`. `ACTIVIDADES_ADMINISTRAR` NO es requisito (queda
reservado, §23). No se depende del nombre del perfil (`ADMINISTRADOR`) si la
capacidad puede evaluarse por permisos — preferir permisos.

**Implementado en Fase 3B:** después de cada mutación de estado, perfil o
vigencia, se valida que subsista al menos un usuario con capacidad
administrativa efectiva: `UsuarioApp` ACTIVO, vigencia `null` o
`>= LocalDate.now()` de la JVM, `PerfilApp` ACTIVO y ambas actividades
`USUARIOS_ADMINISTRAR` y `PERFILES_ADMINISTRAR`. La comprobación se ejecuta en
la misma transacción serializable del command. La auto-inactivación y el cambio
real del propio perfil se rechazan; la vigencia propia sí se permite, sujeta al
guardrail global.

## 26. Borrado de perfil — DECISIÓN V1

**NO DELETE físico de `PerfilApp` en V1.** Razones: `UsuarioApp.perfil_id` FK,
`PerfilActividad.perfil_id` FK (ambas sin cascade), historial/semántica de
asignaciones y estados, y `estado` ya disponible. Usar `INACTIVO` (§21).

## 27. Borrado de actividad — DECISIÓN V1

Como `Actividad` es catálogo seed/controlado (§22): **NO DELETE desde API**. Los
cambios del catálogo se versionan mediante migración/seed y cambio de código
coordinado (§23). Un `DELETE` directo rompería la correspondencia con
`@PreAuthorize` en código ya desplegado.

## 28. Bitácora administrativa — catálogo V1

**DECISIÓN V1 — acciones candidatas para la primera implementación** (módulo
`ADMINISTRACION`):

```text
USUARIO_CREADO
USUARIO_ACTUALIZADO
USUARIO_ESTADO_CAMBIADO
USUARIO_PERFIL_CAMBIADO
USUARIO_VIGENCIA_CAMBIADA
USUARIO_PASSWORD_RESTABLECIDA

PERFIL_CREADO
PERFIL_ACTUALIZADO
PERFIL_ESTADO_CAMBIADO
PERFIL_PERMISOS_CAMBIADOS
```

**No implementar todavía.** Los nombres se agregan al enum `BitacoraAccion`
junto con la implementación, no antes.

**Detalle seguro permitido:** IDs técnicos, `estado` anterior/nuevo,
`perfilId` anterior/nuevo, cantidad o IDs de permisos si el tamaño es
razonable. **Nunca:** password, hash, request completo, JWT, `Authorization`.

Resultado: `EXITO`/`FALLO` (catálogo de `mapeo-bitacora.md` §8). Actor:
`AuthenticatedUserContext.currentUser()`, nunca identidad de body/query.

## 29. Transaccionalidad — IMPLEMENTADA EN REPOSITORIO

**CONFIRMADO (código, `shared/config/DataSourceConfig.java`):** existen los
beans:

```text
primaryDataSourceProperties, primaryDataSource   (@Primary — Módulo C / CALE_IMMEX)
jdbcTemplate                                     (Módulo C)
transactionManager                               (primaryDataSource)
appDataSourceProperties, appDataSource           (ANEXO24_DEV / app24)
appJdbcTemplate                                  (app24)
appTransactionManager                            (appDataSource)
```

Los commands de usuarios usan
`@Transactional(transactionManager = "appTransactionManager")`. Fase 3B
configura aislamiento `SERIALIZABLE` para las mutaciones de estado, perfil y
vigencia, incluido su guardrail global; los paths críticos usan `UPDLOCK` y
`HOLDLOCK`. La escritura de negocio en `UsuarioApp` y el evento de
`BitacoraEvento` participan en la misma transacción; no usan `REQUIRES_NEW`.
Runtime: **READY**, con `app24_runtime`, membership correcta, 12 EXECUTE
específicos, cero grants directos de tablas y ownership chain compatible.

## 30. Estrategia de persistencia app24 — DECISIÓN V1

Para operaciones funcionales de `CALE_IMMEX` y `ANEXO24_DEV.app24` aplica
política **STORED PROCEDURE FIRST**:

- discovery de objetos existentes;
- reuse cuando contrato coincide;
- adapt cuando requiere encapsulación compatible;
- create de procedimientos propios `APP24_*` cuando no existe objeto reusable.

Los adapters Java sólo ejecutan SP, pasan parámetros y mapean resultsets,
parámetros OUTPUT y errores. No contienen SQL funcional de negocio. Excepción
technical explícita: `SystemStatusController` conserva `SELECT 1` para health.
Las transacciones usan los transaction managers correspondientes (§29) y los
grants runtime son mínimos por objeto (§31).

## 31. Runtime permissions — estado versionado

`04-app-runtime-permissions.sql` versiona el estado final least-privilege: membership exclusiva en `app24_runtime`, sin grants directos de tablas y EXECUTE únicamente sobre los 13 SP app24 aprobados, incluido `APP24_Q_PERFILES_LISTAR` de Fase 5A. El script conserva REVOKE explícito para retirar permisos amplios heredados y es idempotente.

## 32. Rutas API V1 — DECISIÓN

**DECISIÓN V1:** agrupar bajo prefijo explícito de dominio:

```text
/api/v1/administracion/usuarios
/api/v1/administracion/perfiles
/api/v1/administracion/actividades
```

Motivo: dominio explícito, consistente con la agrupación funcional, evita
contaminar la raíz `/api/v1` general, y el frontend ya agrupa
`features/administration`.

**ACTUALIZADO:** `docs/04-desarrollo/api.md` documenta las rutas reales bajo
`/api/v1/administracion/*`, incluidos usuarios y `GET /perfiles` de Fase 5A.

## 33. Contratos — USUARIOS (candidatos)

### GET /api/v1/administracion/usuarios

- **Permiso:** `USUARIOS_ADMINISTRAR`.
- **Query filtros:** `clave` (exacta), `correo` (exacta), `perfilId` (exacto),
  `estado` (exacto: ACTIVO/INACTIVO), `nombre` (parcial, contiene,
  case-insensitive, con escape de comodines). Semántica §37.
- **Paginación:** `pagina` default 1, `tamano` default 20, max 100.
- **Orden estable:** `clave ASC, id ASC` (clave única como clave natural de
  orden; `id` desempata inserciones).
- **Response item:** `id, clave, nombre, correo, estado, vigencia, perfilId` +
  etiqueta de perfil opcional. **NUNCA `passwordHash`.**

### POST /api/v1/administracion/usuarios

- **Permiso:** `USUARIOS_ADMINISTRAR`.
- **Request:** `clave`, `nombre`, `correo`, `password`, `vigencia` (opcional),
  `perfilId`. `estado` inicial: **`ACTIVO`** (implícito; no se acepta estado
  distinto en creación).
- **Validación:** clave/correo únicos (409, §16), perfilId existente y ACTIVO,
  password de 10..50 caracteres con mayúscula, número y carácter especial.

### Acciones sensibles — commands explícitos (preferidos a PATCH genérico)

```text
PUT    /{id}                        → edición general: nombre, correo
PATCH  /{id}/estado                 → ACTIVO | INACTIVO  (guardrail §25)
PATCH  /{id}/perfil                 → { perfilId }
PATCH  /{id}/vigencia               → { vigencia | null }
POST   /{id}/password               → { password } (reset) (§15)
```

Justificación: estado/perfil/vigencia/password son acciones con evento de
Bitácora específico (§28) y validación propia; separarlas evita PATCH
multi-propósito ambiguo. Los tres `PATCH` de Fase 3B están implementados, integrados en DEV y
runtime ready; reset de Fase 3C está implementado y validado LIVE en su rama.

## 34. Contratos — PERFILES

### Fase 5A implementada — lectura de catálogo

- **Ruta:** `GET /api/v1/administracion/perfiles`.
- **Autorización:** `hasAnyAuthority('USUARIOS_ADMINISTRAR',
  'PERFILES_ADMINISTRAR')`. Esta excepción de lectura permite a Usuarios cargar
  el catálogo requerido; no extiende permisos de escritura.
- **Filtros:** `nombre` parcial, contiene, case-insensitive y con comodines
  escapados; `estado` exacto (`ACTIVO`/`INACTIVO`); `pagina` y `tamano`.
- **Paginación y orden:** defaults y límites de §37; `nombre ASC, id ASC`.
- **Item:** `id`, `nombre`, `estado`, `cantidadPermisos`.
- **Persistencia:** `APP24_Q_PERFILES_LISTAR`, `READ_ONLY`; sin DML ni bitácora.

### Futuros commands de perfiles

- **Permiso exclusivo:** `PERFILES_ADMINISTRAR`.

```text
GET    /api/v1/administracion/perfiles
       → página: id, nombre, estado, cantidad de permisos; filtros nombre/estado (§37)

POST   /api/v1/administracion/perfiles
       → { nombre } → estado inicial ACTIVO; 201; 409 si nombre duplicado

PUT    /api/v1/administracion/perfiles/{id}
       → { nombre } (edición de nombre); 404 si no existe; 409 si duplicado

PATCH  /api/v1/administracion/perfiles/{id}/estado
       → { estado } ACTIVO|INACTIVO; guardrail de capacidad (§25) al inactivar

GET    /api/v1/administracion/perfiles/{id}/permisos
       → lista de actividades asignadas (clave/id)

PUT    /api/v1/administracion/perfiles/{id}/permisos
       → { actividadIds: [...] } reemplazo transaccional completo (§24)
```

- **Errores:** 400 validación (nombre blank/estado inválido), 404 perfil
  inexistente, 409 nombre duplicado, 409/400 regla de auto-bloqueo (§36).
- **Bitácora:** `PERFIL_CREADO`, `PERFIL_ACTUALIZADO`,
  `PERFIL_ESTADO_CAMBIADO`, `PERFIL_PERMISOS_CAMBIADOS` (§28).
- **Transacción:** negocio + evento de Bitácora en el transaction manager app24
  (§29). **NO implementar.**

## 35. Contrato — ACTIVIDADES (read-only)

```text
GET /api/v1/administracion/actividades
```

- **Permiso:** `PERFILES_ADMINISTRAR` — alimenta el editor de permisos de
  perfiles; `ACTIVIDADES_ADMINISTRAR` no se usa en V1 (§23).
- **Response:** catálogo completo: `id, clave, nombre, recurso, accion`,
  ordenado por `clave ASC`.
- **Paginación: NO en V1** — justificación: dataset controlado por seed
  (12 filas) y estable; paginar agrega complejidad sin beneficio. Si el
  catálogo creciera sustancialmente, se revisa después.
- **NO POST / PUT / PATCH / DELETE V1** (§22).

## 36. Errores HTTP candidatos

| HTTP | Código | Caso |
|---|---|---|
| 400 | `VALIDACION_INVALIDA` (existente) | validación de campos/formato |
| 401 | `CREDENCIALES_INVALIDAS` / `AUTENTICACION_REQUERIDA` (existentes) | login fallido / sin token |
| 403 | `ACCESO_DENEGADO` (existente) | sin permiso |
| 404 | `RECURSO_NO_ENCONTRADO` (nuevo) | usuario/perfil inexistente |
| 409 | `RECURSO_DUPLICADO` (nuevo) | clave/correo/nombre de perfil duplicado (§16) |
| 409 | `ESTADO_INCOMPATIBLE` (nuevo) | reglas de auto-bloqueo/estado incompatible (§25) — elegido 409 y no 400: es conflicto con la regla de integridad administrativa vigente |
| 503 | `DEPENDENCIA_NO_DISPONIBLE` (existente) | fallo de dependencia DB |

**Regla transversal:** nunca filtrar detalles SQL (`SQLException`, constraint
names, mensajes de SQL Server). `GlobalExceptionHandler` ya enmascara
`DataAccessException` → 503; los handlers 404/409 se incorporan en la fase de
implementación. **No implementar todavía.**

## 37. Filtros y paginación V1 — cerrados

| Recurso | Filtros V1 | Semántica |
|---|---|---|
| Usuarios | `clave`, `correo`, `perfilId`, `estado` | **exacta** (identificadores/claves naturales) |
| Usuarios | `nombre` | **parcial** (contiene, case-insensitive, comodines escapados) |
| Perfiles | `nombre` | parcial (contiene) |
| Perfiles | `estado` | exacta |
| Actividades | — | listado completo ordenado (§35) |

**Paginación común (V1):** `pagina >= 1` (base 1), `tamano` en `1..100`
(convención ya usada por Bitácora), `tamano` default 20, `max 100`. **Orden
estable:** Usuarios `clave ASC, id ASC`; Perfiles `nombre ASC, id ASC`;
Actividades `clave ASC`. No se aceptan filtros ni orden arbitrario por
columnas sensibles (`password_hash`, etc.).

## 37.1 Compatibilidad usuario legacy → usuario nuevo

| Aspecto | Legacy | Nuevo | Estado |
|---|---|---|---|
| Clave | máximo 35 | máximo 30 | Rediseñado; pendiente validación negocio |
| Nombre | máximo 100 | máximo 120 | Rediseñado; pendiente validación negocio |
| Password | máximo 50; mínimo 10, mayúscula, número y especial | baseline 10..50, mayúscula, número y especial | Baseline funcional conservado |
| Vigencia | sin vigencia / 2 / 4 / 8 / 12 semanas | `LocalDate` nullable | Rediseñado; backend más flexible; frontend futuro puede ofrecer presets equivalentes |
| Usuario SAT | existe | sin campo equivalente explícito | Pendiente de definición funcional |
| Bloqueo | existe | estado `ACTIVO`/`INACTIVO` | Equivalencia parcial; validar si bloqueo legacy tenía semántica adicional |
| Administrador | no editable | nombre/correo técnicamente editables en Fase 3A | Decisión de rediseño pendiente de validar antes de runtime productivo |

## 38. Roadmap de implementación recomendado

Estado posterior a integración SP en `dev`. Fases implementadas quedan marcadas
como **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**; no comenzar FASE N+1 sin
cerrar su alcance y gates:

```text
FASE 1  Hardening previo: **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**:
        - PerfilApp.estado efectivo en auth: consulta única `findAccesoByUsuario`
          sin filtrar el estado; login rechaza con 401 genérico si el perfil es
          INACTIVO, no existe o el registro es inconsistente (§17);
        - distinción explícita: perfil ACTIVO con permisos; ACTIVO sin permisos
          (autentica con JWT de auth vacía); INACTIVO; inexistente-inconsistente;
        - transaction managers explícitos `transactionManager` (primario) y
          `appTransactionManager` (aplicación) + tests (§29);
        - errores base 404/409 y handlers en `GlobalExceptionHandler`;
        - tests: ACTIVO con permisos, ACTIVO sin permisos, INACTIVO,
          acceso inconsistente y rollback del gestor de aplicación;
        - SQL 04 versionado: `GRANT SELECT` sobre `PerfilApp`, sin escrituras.

FASE 2  Usuarios read-only: **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**:
        - contrato: GET /api/v1/administracion/usuarios y /{id};
        - permiso único: USUARIOS_ADMINISTRAR;
        - fuentes: app24.UsuarioApp JOIN app24.PerfilApp, sin password_hash;
        - filtros: clave/correo/perfilId/estado exactos, nombre parcial
          con comodines escapados (\, %, _); paginación estricta 1..100;
        - orden estable: u.clave ASC, u.id ASC;
        - puerto separado UsuarioConsultaRepository (no se amplía el de
          autenticación); appJdbcTemplate únicamente;
        - sin DML, sin @Transactional, sin bitácora por GET.

FASE 3A Usuarios commands: **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**:
        create, edit de nombre/correo, bitácora y errores controlados.

FASE 3B Estado, perfil, vigencia y guardrails: **COMPLETADA / INTEGRADA EN DEV /
        VALIDADA LIVE**. Incluye los tres `PATCH`, auto-inactivación y cambio
        real de perfil propio prohibidos, guardrail global de al menos un
        administrador efectivo, `SERIALIZABLE`, `UPDLOCK`/`HOLDLOCK` y
        `appTransactionManager`.

Migración SP app24/Módulo C: **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**.
Runtime permissions: **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**.

FASE 3C reset password: **COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**.

FASE 4A **IMPLEMENTADA / INTEGRADA EN DEV / VALIDADA FRONTEND**: Usuarios con
        listado, filtros, paginación, edición de nombre/correo, estado, vigencia y
        restablecimiento de contraseña. Suite Angular 22 con Vitest/jsdom valida
        mapper, HTTP, presentación, RBAC y boundary DI route-scoped → MatDialog.

FASE 4B **IMPLEMENTADA / VALIDADA FRONTEND**: completa Usuarios V1 con catálogo
        real read-only de perfiles, filtro por perfil, alta de usuario y cambio de
        perfil. Los selectores consumen IDs reales y sólo ofrecen perfiles activos
        para commands; no hay IDs manuales ni hardcodeados. No implementa pantalla
        ni CRUD de perfiles.

FASE 5A **IMPLEMENTADA EN BACKEND**: `GET /api/v1/administracion/perfiles`
        read-only con `nombre` parcial escapado, `estado`, página/tamaño e items
        `id`, `nombre`, `estado`, `cantidadPermisos`; lectura RBAC con
        `USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR`. Usa
        `APP24_Q_PERFILES_LISTAR`; **VALIDADA LIVE** y por suite backend.

FASE 5B **AUDITORÍA Y DISEÑO CERRADOS; IMPLEMENTACIÓN PENDIENTE**:
        - evidencia LIVE, contratos, guardrail y plan de SP/grants en
          `auditoria-diseno-perfiles-permisos-fase-5b.md`;
        - aún no existen commands, consultas de Actividades/permisos, Bitácora
          de perfiles ni grants adicionales.

FASE 5C **PENDIENTE**: backend Perfiles + consulta Actividades, commands,
        reemplazo transaccional PerfilActividad, Bitácora y grants mínimos.

FASE 6 **PENDIENTE**: Frontend Perfiles/Permisos.

E2E administrativo completo: **PENDIENTE** si no se ejecuta una corrida API
        controlada con cleanup aprobado.
```

Ajustar únicamente si la evidencia de implementación justifica otro orden.

## 39. Riesgos y pendientes

1. **PerfilInactivo-concede-permisos (§6/§17):** corregido en FASE 1
   (**COMPLETADA / INTEGRADA EN DEV / VALIDADA LIVE**).
2. JWT emitidos no revocables (consistencia eventual aceptada, §18) — revisar
   con auditoría si la ventana definida por la expiración JWT configurada no
   es aceptable.
3. Runtime least-privilege de app24: **COMPLETADO / VALIDADO LIVE** en SP-1E;
   mantener verificación en despliegues posteriores.
4. Confirmar timezone JVM en producción para semántica de `vigencia` (§19).
5. Política V1 de contraseñas implementada: provista por administrador, 10..50,
   mayúscula, número y especial; `PasswordPolicy` compartida por creación/reset.
6. Aprobar preventivamente `must_change_password`/historial/MFA si negocio los
   exige — FUERA DE V1 (§15).
7. Posible limpieza futura de `ACTIVIDADES_ADMINISTRAR` del seed (§23).
8. Compatibilidad legacy pendiente: semántica de usuario SAT, bloqueo y edición
   administrativa deben validarse antes de runtime productivo (§37.1).

## 40. Restricciones cumplidas

- Fase 0 documental: cero código nuevo, cero SQL remoto, cero bypass TLS.
- Solo se modificó documentación en esta pasada; no se modificó backend,
  frontend, SQL versionado ni SQL LIVE.
- Solo se modificó `docs/03-diseno/mapeo-administracion.md` en esta segunda
  pasada; la matriz de integración se dejó **intacta** (las filas existentes
  de Administración siguen siendo correctas; ninguna decisión nueva las
  contradice).
- No se modificó `backend/**`, `frontend/**` ni `infra/sql/**`; esta pasada
  modificó únicamente documentación bajo `docs/**`.
- Sin PR, sin merge, sin code review automático.