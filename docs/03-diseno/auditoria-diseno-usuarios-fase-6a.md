# Fase 6A — Auditoría y diseño de Administración → Usuarios V1

**Fecha de auditoría:** 2026-09-24

**Base:** `dev` / `origin/dev` = `de4fe8b44bdafe81aa74711e4588b7da2307e0fd`

**Rama F6A:** `feature/administration-users-v1`

**Tipo:** auditoría + diseño; sin implementación funcional.

## 1. Resultado ejecutivo

Usuarios V1 **ya está implementado en este repositorio**: backend ofrece
consulta, creación, edición de datos, cambio de estado, perfil, vigencia y
restablecimiento de contraseña; frontend ya tiene ruta, listado, filtros,
paginación y dialogs para esos flujos. No se deben volver a crear sus endpoints,
SP, modelos ni pantallas en F6B.

La auditoría confirma el contrato y el modelo actual contra código, SQL
versionado y metadatos READ-only de `ANEXO24_DEV`. No se leyeron filas personales,
contraseñas ni hashes; no se ejecutaron SP que devuelvan usuarios; no hubo
escrituras.

**Hallazgo de seguridad que requiere atención antes de validación runtime:**
`backend/.env` alimenta `APP_DB_USERNAME` a `bootRun`. La conexión de diagnóstico
con esas mismas credenciales a `ANEXO24_DEV` no fue `anexo24_app`; reportó
`IS_ROLEMEMBER('db_owner') = 1` e `IS_SRVROLEMEMBER('sysadmin') = 1`. En cambio,
`EXECUTE AS USER='anexo24_app'` confirmó EXECUTE específico de los diez SP de
usuario/auth y SELECT directo denegado en `UsuarioApp`, `PerfilApp`,
`PerfilActividad` y `Actividad`. F6A no cambia secretos/configuración. Antes de
F6B debe verificarse y corregirse de forma controlada la identidad efectiva del
DataSource de aplicación; no ejecutar commands con la identidad privilegiada.

## 2. Evidencia y alcance

Fuentes revisadas:

- `backend/src/main/java/.../administration/users/**` y `security/**`.
- `backend/src/test/java/.../administration/users/**` y `security/**`.
- `frontend/src/app/features/administration/users/**`, rutas y sidebar.
- `infra/sql/02-app-schema.sql`, `04-app-runtime-permissions.sql` y los diez
  SP `APP24_*USUARIO*` versionados.
- `docs/02-analisis/auditoria-sistema-actual.md`,
  `docs/02-analisis/datos-requeridos-aplicacion.md` y
  `docs/03-diseno/mapeo-administracion.md`.
- Metadatos de `ANEXO24_DEV` consultados con SQLCMD: columnas, tipos, defaults,
  índices, FK, parámetros/dependencias SP y permisos. Se usó impersonation
  únicamente para evaluar permisos.

No se hizo validación HTTP autenticada ni comprobación de interfaz en navegador
como parte de F6A. SQLCMD de metadatos no demuestra por sí solo la identidad del
proceso Spring; el mapeo de `APP_DB_*` a `app.datasource.*` sí está definido en
`application-local.yml` y `DataSourceConfig`.

## 3. Legado frente al producto actual

| Dato/comportamiento | Evidencia histórica | Estado en app24/código actual | Decisión de diseño |
|---|---|---|---|
| Clave, nombre, correo, perfil, vigencia | Documentados como campos de administración legacy. El legacy auditó límites observados distintos a los nuevos contratos. | `clave` 30, `nombre` 120, `correo` 150, `vigencia DATE`, `perfil_id`. | Los contratos físicos/API actuales son fuente para V1; no copiar límites legacy sin requerimiento y migración explícitos. |
| Confirmación/verificación de correo | Aparece en inventario de datos legacy; no hay semántica de verificación confirmada en el backend nuevo. | Sin campo, token, endpoint ni flujo de verificación. | Fuera del V1 actual. Aclarar requisito antes de proponer persistencia. |
| Indicador Usuario SAT | Reportado por la auditoría funcional legacy; asociado históricamente a privilegios amplios. | Sin columna/flag en `UsuarioApp`; permisos derivan de Perfil → PerfilActividad → Actividad. | No introducir identidad/permiso especial basado en clave o nombre. Revisar mínimo privilegio; no replicar perfil legacy privilegiado automáticamente. |
| Bloqueo/lockout | Reportado como concepto legacy. | Sin campo de bloqueo, contador de intentos, lock duration o endpoint. Login fallido sólo registra evento. | No equiparar `INACTIVO` con bloqueo temporal. Lockout requiere requisito y diseño separado. |
| Contraseña visible/precargada | Legacy audit confirmó password en texto en DOM al editar; defecto crítico. | DTOs administrativos no contienen hash; alta/reset aceptan contraseña nueva y la codifican en backend. | Nunca leer, devolver ni precargar contraseña almacenada. |
| Borrado | Legacy incluía cancelar usuario; su cancelación tenía un error de UI. | No existe DELETE; baja lógica con `estado=INACTIVO`; FK conserva referencias históricas. | No agregar borrado físico. |

## 4. Capacidades existentes

| Capacidad | Existe | Capa/evidencia |
|---|---|---|
| Autenticar cuenta y cargar permisos | Sí | `LoginController`, `LoginService`, `UsuarioRepository`, `UsuarioJdbcAdapter`; SP de autenticación. |
| Listar, filtrar y paginar usuarios | Sí | `GET /api/v1/administracion/usuarios`; use case, port y adapter JDBC. |
| Consultar usuario | Sí | `GET /api/v1/administracion/usuarios/{id}`; DTO no incluye hash/permisos. |
| Crear usuario | Sí | `POST`; contraseña inicial requerida, hash en backend, cuenta `ACTIVO`, perfil activo obligatorio. |
| Editar nombre/correo | Sí | `PUT /{id}`; `clave` permanece inmutable. |
| Activar/inactivar | Sí | `PATCH /{id}/estado`; guardrails de auto-inactivación y último administrador efectivo. |
| Cambiar perfil | Sí | `PATCH /{id}/perfil`; perfil destino debe existir/estar activo; guardrails de auto-cambio y último administrador efectivo. |
| Cambiar vigencia | Sí | `PATCH /{id}/vigencia`; fecha o `null`; guarda último administrador efectivo. |
| Bloquear/desbloquear | No | No hay representación física, SP ni endpoint. |
| Restablecer contraseña administrativamente | Sí, Fase 3C | `POST /{id}/password`; BCrypt y SP separado. No es un reset token/self-service. |
| Recuperación por email, historial/MFA/forzar cambio | No | Sin modelo ni flujo; documentado fuera del contrato actual. |
| Usuario SAT como flag | No | Sin campo ni caso especial en modelo nuevo. |
| DELETE físico | No | Intencionalmente fuera; integridad referencial e historial preservados. |

## 5. Modelo SQL LIVE — `ANEXO24_DEV`

Metadatos LIVE confirmaron los siguientes objetos/columnas e índices. En tipos
`varchar(n)`, `max_length` reportado por SQL Server es `n` bytes. Hash se
menciona sólo como columna: ningún valor fue seleccionado ni registrado.

| Tabla.campo | Tipo SQL LIVE | Null/default | Clave/relación | Uso confirmado/inferido |
|---|---|---|---|---|
| `UsuarioApp.id` | `BIGINT` | NOT NULL, IDENTITY | PK | Identificador técnico. |
| `UsuarioApp.clave` | `VARCHAR(30)` | NOT NULL | UNIQUE | Login/subject; no editable por contrato actual. |
| `UsuarioApp.nombre` | `VARCHAR(120)` | NOT NULL | — | Nombre mostrado/auditado. |
| `UsuarioApp.correo` | `VARCHAR(150)` | NOT NULL | UNIQUE | Correo administrativo; no hay verificación asociada. |
| `UsuarioApp.password_hash` | `VARCHAR(100)` | NOT NULL | — | Credencial interna. Algoritmo bcrypt confirmado por `BCryptPasswordEncoder` en código, no inferido por longitud. |
| `UsuarioApp.estado` | `VARCHAR(20)` | NOT NULL, default `ACTIVO` | — | Catálogo de aplicación/SP: `ACTIVO`/`INACTIVO`; DDL no tiene CHECK. |
| `UsuarioApp.vigencia` | `DATE` | NULL | — | `NULL` significa sin vencimiento. |
| `UsuarioApp.perfil_id` | `BIGINT` | NOT NULL | FK → `PerfilApp.id` | Un usuario pertenece a un perfil. |
| `PerfilApp.id` | `BIGINT` | NOT NULL, IDENTITY | PK | Identificador de perfil. |
| `PerfilApp.nombre` | `VARCHAR(80)` | NOT NULL | UNIQUE | Nombre de perfil. |
| `PerfilApp.estado` | `VARCHAR(20)` | NOT NULL, default `ACTIVO` | — | Estado textual. |
| `PerfilActividad.perfil_id` | `BIGINT` | NOT NULL | PK compuesta + FK → `PerfilApp.id` | Relación N:M. |
| `PerfilActividad.actividad_id` | `BIGINT` | NOT NULL | PK compuesta + FK → `Actividad.id` | Relación N:M. |
| `Actividad.id` | `BIGINT` | NOT NULL, IDENTITY | PK | Identificador de actividad. |
| `Actividad.clave` | `VARCHAR(60)` | NOT NULL | UNIQUE | Authority estable consumida por backend. |
| `Actividad.nombre` | `VARCHAR(120)` | NOT NULL | — | Etiqueta descriptiva. |
| `Actividad.recurso` | `VARCHAR(120)` | NOT NULL | — | Recurso. |
| `Actividad.accion` | `VARCHAR(40)` | NOT NULL | — | Acción. |

Cardinalidades confirmadas por FK/PK: `UsuarioApp N:1 PerfilApp`; `PerfilApp N:M
Actividad` mediante `PerfilActividad`. Además, FKs entrantes LIVE
`BitacoraEvento.usuario_id → UsuarioApp.id` y
`CargaFacturacion.usuario_id → UsuarioApp.id` impiden borrado físico referenciado.
Índices LIVE de `UsuarioApp`: PK por `id`, unique por `clave` y unique por
`correo`; no hay índice independiente en `perfil_id`. `PerfilActividad` tiene
PK compuesta `(perfil_id, actividad_id)`. No existen columnas físicas
`usuario_sat`, `bloqueado`, `correo_verificado`, `intentos_fallidos`, fechas de
alta/edición ni responsable de asignación en estas tablas. Las fechas actoría
no se deben inventar.

## 6. Stored Procedures existentes

Clasificación basada en propósito/código versionado. Los metadatos LIVE
confirmaron los diez nombres de usuario/auth, tipos y parámetros; result sets
se describen desde sus scripts versionados. SQL Server confirmó dependencias a
las tablas `app24` indicadas por los SP.

| SP | Tipo/uso | Parámetros LIVE | Result set versionado | Runtime EXECUTE |
|---|---|---|---|---|
| `APP24_Q_USUARIO_POR_CLAVE` | AUTH / READ | `@Clave VARCHAR(30)` IN | `id, clave, nombre, correo, password_hash, estado, vigencia, perfil_id`; hash sólo para el componente auth. | Sí |
| `APP24_Q_USUARIO_ACCESO` | AUTH / READ | `@UsuarioId BIGINT` IN | `perfil_id, perfil_estado, permiso`; LEFT JOIN conserva perfil activo con cero actividades. | Sí |
| `APP24_Q_USUARIOS_LISTAR` | ADMIN / READ | `@Clave VARCHAR(30)`, `@Nombre VARCHAR(120)`, `@Correo VARCHAR(150)`, `@Estado VARCHAR(20)`, `@PerfilId BIGINT`, `@Pagina INT`, `@Tamano INT` IN; `@Total BIGINT` OUT | `id, clave, nombre, correo, estado, vigencia, perfil_id, perfil_nombre`; no hash. | Sí |
| `APP24_Q_USUARIO_OBTENER` | ADMIN / READ | `@UsuarioId BIGINT` IN | Los ocho campos de proyección administrativa, sin hash. | Sí |
| `APP24_C_USUARIO_CREAR` | ADMIN / WRITE | clave, nombre, correo, `@PasswordHash VARCHAR(100)`, vigencia, perfilId IN; `@NuevoUsuarioId BIGINT` OUT | Sin result set de datos; ID por OUT. Estado inicial `ACTIVO`. | Sí |
| `APP24_C_USUARIO_ACTUALIZAR_DATOS` | ADMIN / WRITE | usuarioId, nombre, correo IN | Sin result set contractual. | Sí |
| `APP24_C_USUARIO_CAMBIAR_ESTADO` | ADMIN / WRITE | usuarioId, nuevoEstado, actorId, fechaActual IN | Sin result set contractual. | Sí |
| `APP24_C_USUARIO_CAMBIAR_PERFIL` | ADMIN / WRITE | usuarioId, perfilId, actorId, fechaActual IN | Sin result set contractual. | Sí |
| `APP24_C_USUARIO_CAMBIAR_VIGENCIA` | ADMIN / WRITE | usuarioId, vigencia, actorId, fechaActual IN | Sin result set contractual. | Sí |
| `APP24_C_USUARIO_RESTABLECER_PASSWORD` | ADMIN / WRITE; Fase 3C | usuarioId, `@PasswordHash VARCHAR(100)` IN | Sin result set contractual. | Sí |
| `APP24_C_BITACORA_REGISTRAR` | Audit / WRITE compartido | SP versionado, invocado por servicio de bitácora | ID/evento gestionado por adapter; no forma parte de DTO de usuario. | Sí |

Los SP funcionales usan `SET NOCOUNT ON`; los commands incorporan transacción,
validación y errores de dominio SQL; commands de estado/perfil/vigencia emplean
locking y preservan invariantes. No se encontraron SP para bloqueo, verificación
de correo, Usuario SAT ni borrado físico. No se propone duplicar SP existentes.

### Grants LIVE comprobados

- `anexo24_app` pertenece a `app24_runtime`.
- Bajo `EXECUTE AS USER='anexo24_app'`, `HAS_PERMS_BY_NAME(..., 'EXECUTE') = 1`
  para los diez SP de usuario/auth listados arriba.
- Bajo la misma impersonation, permiso `SELECT` directo = 0 para
  `UsuarioApp`, `PerfilApp`, `PerfilActividad` y `Actividad`.
- Estos resultados verifican permisos del principal runtime, **no** identidad
  efectiva de la conexión local `APP_DB_*` usada por SQLCMD/`bootRun`.

## 7. Backend, endpoints y autenticación actual

### Endpoints existentes (no son propuestas nuevas)

| Método | Path | Controller | Authority | Use case | SP |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | `LoginController` | Público | `LoginService.login` | `APP24_Q_USUARIO_POR_CLAVE`, `APP24_Q_USUARIO_ACCESO`, `APP24_C_BITACORA_REGISTRAR` |
| `GET` | `/api/v1/administracion/usuarios` | `UsuarioAdministracionController` | `USUARIOS_ADMINISTRAR` | `ListarUsuariosUseCase` | `APP24_Q_USUARIOS_LISTAR` |
| `GET` | `/api/v1/administracion/usuarios/{id}` | idem | `USUARIOS_ADMINISTRAR` | `ObtenerUsuarioUseCase` | `APP24_Q_USUARIO_OBTENER` |
| `POST` | `/api/v1/administracion/usuarios` | idem | `USUARIOS_ADMINISTRAR` | `CrearUsuarioUseCase` | `APP24_C_USUARIO_CREAR`, luego lectura de detalle; bitácora compartida |
| `PUT` | `/api/v1/administracion/usuarios/{id}` | idem | `USUARIOS_ADMINISTRAR` | `ActualizarUsuarioUseCase` | `APP24_C_USUARIO_ACTUALIZAR_DATOS` |
| `PATCH` | `/api/v1/administracion/usuarios/{id}/estado` | idem | `USUARIOS_ADMINISTRAR` | `CambiarEstadoUsuarioUseCase` | `APP24_C_USUARIO_CAMBIAR_ESTADO` |
| `PATCH` | `/api/v1/administracion/usuarios/{id}/perfil` | idem | `USUARIOS_ADMINISTRAR` | `CambiarPerfilUsuarioUseCase` | `APP24_C_USUARIO_CAMBIAR_PERFIL` |
| `PATCH` | `/api/v1/administracion/usuarios/{id}/vigencia` | idem | `USUARIOS_ADMINISTRAR` | `CambiarVigenciaUsuarioUseCase` | `APP24_C_USUARIO_CAMBIAR_VIGENCIA` |
| `POST` | `/api/v1/administracion/usuarios/{id}/password` | idem | `USUARIOS_ADMINISTRAR` | `RestablecerPasswordUsuarioUseCase` | `APP24_C_USUARIO_RESTABLECER_PASSWORD` |
| `GET` | `/api/v1/administracion/perfiles` | `PerfilAdministracionController` | `USUARIOS_ADMINISTRAR` **o** `PERFILES_ADMINISTRAR` | listado paginado de perfiles | `APP24_Q_PERFILES_LISTAR` |

Los GET devuelven 200 (más 400 para filtros inválidos; detalle puede dar 404);
creación devuelve 201; edición/cambios devuelven 200; reset devuelve 204 sin
body. Duplicados/conflictos usan 409; auth devuelve 401; falta de authority,
403; fallos de datos traducidos según el contrato global.

### Request/response existentes

- Listado: filtros opcionales `clave`, `nombre`, `correo`, `estado`, `perfilId`,
  `pagina` (base 1), `tamano` (1–100); response `items,total,pagina,tamano`.
- Crear: `{ clave, nombre, correo, password, vigencia, perfilId }`; clave 1–30,
  nombre 1–120, correo email 1–150, password requerido y sujeto a política de
  aplicación, perfilId positivo; vigencia opcional.
- Editar: `{ nombre, correo }`; no incluye clave ni contraseña.
- Estado: `{ estado: ACTIVO | INACTIVO }`.
- Perfil: `{ perfilId }`.
- Vigencia: `{ vigencia: fecha | null }`.
- Restablecer: `{ password }`; 204 sin respuesta de secreto.
- Proyección de usuario: `id, clave, nombre, correo, estado, vigencia, perfilId,
  perfilNombre`; no hash ni permisos.

### Flujo de login

`POST /api/v1/auth/login` recibe `LoginRequest { clave, password }`.
`LoginService` ejecuta este orden:

1. `UsuarioRepository.findByClave` → `APP24_Q_USUARIO_POR_CLAVE` lee datos de
   usuario y hash en infraestructura; el hash no pasa al DTO.
2. `UsuarioApp.estaActiva()` valida estado `ACTIVO` y vigencia inclusiva (hoy o
   futura; `null` no vence).
3. `PasswordEncoder.matches` verifica contraseña contra `BCryptPasswordEncoder`.
4. `findAccesoByUsuario` → `APP24_Q_USUARIO_ACCESO` obtiene estado de perfil y
   permisos; perfil inactivo rechaza login con error genérico.
5. Bitácora registra `LOGIN_OK` o `LOGIN_FALLIDO`; login fallido no revela si
   usuario, estado, vigencia, password o perfil causó rechazo.
6. `JwtTokenService` emite JWT HS256 con `sub=clave`, `uid`, `auth`, `iat`, `exp`.
   `JwtAuthFilter` valida firma/expiración/claims, toma authorities de `auth` y
   deja request anónima ante token inválido.

API stateless; password reset y cambio de estado/perfil no revocan JWT ya
emitidos. Las authorities de tokens existentes pueden seguir vigentes hasta la
expiración configurada por ambiente. No existe cookie/session ni endpoint de
refresh/session actual identificado en este flujo.

## 8. RBAC

- `/usuarios` frontend: `permissionGuard` + `data.permission =
  'USUARIOS_ADMINISTRAR'`; sidebar filtra por la misma authority.
- Todos los endpoints `/administracion/usuarios/**`, incluidos comandos de
  password, usan `@PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")`.
- `GET /administracion/perfiles` es excepción de lectura para
  `USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR`; comandos de perfiles requieren
  exclusivamente `PERFILES_ADMINISTRAR`.
- Actividades/permissions de perfiles requieren `PERFILES_ADMINISTRAR`;
  `ACTIVIDADES_ADMINISTRAR` permanece reservada.
- Frontend mejora UX; enforcement real es Spring Security y `@PreAuthorize`.

## 9. Guardrails observados

| Regla | Estado | Evidencia |
|---|---|---|
| Perfil requerido para crear; debe existir y estar `ACTIVO` | EXISTENTE | `APP24_C_USUARIO_CREAR`. |
| Perfil destino debe existir y estar `ACTIVO` | EXISTENTE | `APP24_C_USUARIO_CAMBIAR_PERFIL`. |
| Estado limitado a `ACTIVO`/`INACTIVO` | EXISTENTE | Application/use case y SP; no hay CHECK en tabla. |
| `clave` única; correo único | EXISTENTE | índices UNIQUE LIVE y commands que traducen duplicados. |
| Auto-inactivación prohibida | EXISTENTE | `APP24_C_USUARIO_CAMBIAR_ESTADO`. |
| Auto-cambio de perfil prohibido | EXISTENTE | `APP24_C_USUARIO_CAMBIAR_PERFIL`. |
| Mantener al menos un admin efectivo | EXISTENTE | SP de estado, perfil y vigencia; cuenta activa/vigente, perfil activo y authorities `USUARIOS_ADMINISTRAR` + `PERFILES_ADMINISTRAR`. |
| No-op de estado/perfil/vigencia/nombre-correo | EXISTENTE | Use cases evitan command/actor/bitácora cuando estado no cambió; tests existentes. |
| Perfil activo en login | EXISTENTE | `UsuarioAcceso.perfilActivo()` y `LoginService`. |
| Vigencia vencida impide login | EXISTENTE | `UsuarioApp.estaActiva()`. |
| Bloqueo temporal, intentos y auto-unlock | FALTANTE | Sin columnas/SP/política. No confundir con estado INACTIVO. |
| Verificación de correo | FALTANTE | Sin campo ni flujo. |
| Proteger Usuario SAT por identidad | FALTANTE / NO REQUERIDO EN MODELO NUEVO | No existe flag/caso especial; permisos se asignan por perfil/actividad. |
| Impedir que el usuario cambie su propio nombre/correo | No existe como guardrail demostrado | `PUT` no aplica regla self; no se agrega sin requisito explícito. |
| DELETE físico | EXCLUIDO | No hay endpoint/SP; baja lógica conserva referencias y auditoría. |

## 10. Bitácora

Acciones actuales de administración:

- `USUARIO_CREADO`
- `USUARIO_ACTUALIZADO`
- `USUARIO_ESTADO_CAMBIADO`
- `USUARIO_PERFIL_CAMBIADO`
- `USUARIO_VIGENCIA_CAMBIADA`
- `USUARIO_PASSWORD_RESTABLECIDA`

Autenticación registra `LOGIN_OK` y `LOGIN_FALLIDO`. Commands usan
`AuthenticatedUserContext` para actor y `correlationId`; escritura de negocio y
evento se ejecutan bajo `appTransactionManager`. El detalle observado usa IDs,
perfil/estado/vigencia de forma acotada.

Prohibido registrar password plaintext, hash viejo/nuevo, JWT, `Authorization`,
request completo o secretos. Ningún evento es necesario para un no-op.
Eventos futuros de bloqueo/verificación sólo se diseñarán si esas funciones son
aprobadas.

## 11. Frontend actual

La feature ya existe en `frontend/src/app/features/administration/users/`:

- Dominio: `UserAdministration`, `UserPage`, comandos y `UserRepository`.
- Application: use cases de buscar/obtener/crear/actualizar/estado/perfil/vigencia/
  reset.
- Infrastructure: DTO, mapper, `UserApiService`, `HttpUserRepository`.
- Presentation: `UserListPage`; dialogs create/edit/profile/expiration/password.
- Ruta lazy `/usuarios`, auth guard de layout + `permissionGuard`, entrada de
  sidebar condicionada por `USUARIOS_ADMINISTRAR`.
- Angular Material, forms reactivos, paginación/filtros, loading/empty/error,
  confirmación de estado y refresh visual tras respuestas exitosas. Tabla para
  escritorio y tarjetas/acciones para móvil.
- Componentes consumen use cases/puertos; HTTP vive en `UserApiService` y adapter.
- Alta pide password y confirmación; campos password usan `type=password`,
  `autocomplete=new-password`, y se limpian al cerrar/destruir. Reset es dialog
  separado; su toggle sólo muestra la contraseña nueva que el operador está
  escribiendo, nunca recupera ni muestra la contraseña almacenada.

No hay página detalle separada: el listado abre dialogs para operaciones. No
mostrar hash/password existente ni añadir campo `bloqueado`/`usuario SAT` sin
contrato nuevo.

## 12. Riesgos y gaps demostrados

### Bloqueante previo a F6B runtime/commands: identidad de DataSource local

`application-local.yml` vincula `APP_DB_USERNAME` a `app.datasource.username`;
`build.gradle.kts` inyecta `.env` a `bootRun`. Con credenciales locales
`APP_DB_*`, SQLCMD a `ANEXO24_DEV` reportó identidad distinta de
`anexo24_app`, `db_owner=1`, `sysadmin=1`. No se revelan credenciales ni nombre
principal. Esto es evidencia de la configuración local auditada, no prueba de
identidad de producción.

Acción futura: provisionar/usar la identidad de mínimo privilegio definida
(`anexo24_app`/`app24_runtime`) para el datasource app24 de desarrollo; verificar
`DB_NAME`, principal y permisos desde el mismo pool Spring antes de cualquier
prueba funcional con commands. No resolver dando nuevos grants, usando dbo ni
leyendo tablas directamente.

### Riesgo estático de `toString()` de secretos

`CrearUsuarioRequest` es record que incluye `password` y no redefine
`toString()`; `UsuarioApp` record contiene `passwordHash` y tampoco lo redefine.
El `toString()` generado incluiría esos campos si alguien registra el objeto.
No se encontró llamada de logging de esos objetos durante esta revisión: no se
afirma exposición actual. Como hardening futuro, redactar/evitar `toString()`
y añadir prueba que verifique que plaintext/hash no aparecen en representaciones
loggables. `RestablecerPasswordUsuarioRequest` sí redacted explícitamente.

### Límites de requisitos legacy

No hay especificación actual aprobada para verificación de correo, lockout ni
Usuario SAT. Son decisiones de producto/seguridad, no gaps para copiar de forma
automática del sistema legacy. Requieren fase de decisión aparte si son
necesarios.

## 13. Contratos/SP: reutilización y propuestas

| Acción/necesidad | Reutiliza existente | Nuevo SP | Motivo |
|---|---|---:|---|
| Login por clave + lectura de acceso | `APP24_Q_USUARIO_POR_CLAVE`, `APP24_Q_USUARIO_ACCESO` | 0 | Contrato vigente y grantado. |
| Listado / detalle | `APP24_Q_USUARIOS_LISTAR`, `APP24_Q_USUARIO_OBTENER` | 0 | Filtros/página y detalle sin hash ya existen. |
| Crear, editar, estado, perfil, vigencia | Cinco `APP24_C_USUARIO_*` correspondientes | 0 | Commands atómicos y guardrails existentes. |
| Reset administrativo | `APP24_C_USUARIO_RESTABLECER_PASSWORD` | 0 para Fase 3C | Mantener como flujo separado; no incluir en edición general. |
| Bitácora | `APP24_C_BITACORA_REGISTRAR` | 0 | Servicio/puerto compartido. |
| Bloqueo/lockout | Ninguno | **No proponer todavía** | Requiere definición de estado, umbral, expiración, desbloqueo, mensajes y efectos en JWT. |
| Verificación de correo | Ninguno | **No proponer todavía** | Semántica/token/caducidad/entrega y transición de cuenta no definidos. |
| Usuario SAT | Ninguno | **No proponer todavía** | No hay requisito nuevo confirmado; evitar privilegio especial. |

No crear `APP24_Q_USUARIO_OBTENER`, listar ni commands paralelos: ya existen.

## 14. HTTP canónico para el alcance actual

Los endpoints de la sección 7 son el contrato canónico existente; no se propone
su duplicación. Todos los commands y consultas admin requieren
`USUARIOS_ADMINISTRAR`, salvo `GET /perfiles` que admite también
`PERFILES_ADMINISTRAR` como lectura. Password reset permanece
`POST /api/v1/administracion/usuarios/{id}/password` y no se integra a `PUT`.

Si se aprueban futuras funciones no cubiertas, diseñar endpoint + authority +
SP + bitácora antes de implementar; no agregar `PATCH /bloqueo` ni `/correo`
por analogía con el legado.

## 15. Tests existentes y estrategia

### Evidencia de tests ya versionados

Backend incluye pruebas de controller/validación, use cases, adapters y auth:
`UsuarioAdministracionControllerTest`, `RestablecerPasswordUsuarioControllerWebTest`,
`CrearUsuarioRequestValidationTest`, tests de `Crear/Actualizar/CambiarEstado/
CambiarPerfil/CambiarVigencia/RestablecerPasswordUsuarioUseCase`, policy,
`UsuarioConsultaJdbcAdapterTest`, `UsuarioComandoJdbcAdapterTest`,
`UsuarioJdbcAdapterTest`, `LoginServiceTest`, `LoginControllerTest` y
`JwtAuthFilterTest`.

Frontend tiene tests de `user-list.page`, create/edit/profile/expiration/password
dialogs, API service y mapper; `app.routes.spec.ts` comprueba authority/ruta.
Los tests de adapter actuales prueban binding/mapping con infraestructura de
prueba; no equivalen a validación autenticada LIVE del flujo completo.

No se ejecutó suite durante F6A: sólo se modifica documentación y esta fase no
es un cierre funcional.

### Gate obligatorio para cualquier extensión futura

- **SQL:** happy path, duplicados, inexistente, perfil inactivo, guardrails,
  concurrencia/rollback y permisos `EXECUTE AS`; nunca DML contra usuarios reales.
- **Backend:** métodos/status/validación, 401/403 por authority, no-op sin
  commands/bitácora, hash sólo a SP, redacción de secretos y traducción de
  duplicados/errores.
- **Frontend:** ruta/RBAC, página/paginación/filtros, loading/empty/error, cambios
  confirmados sólo tras éxito, pending/disabled, errores 409/403 y no envío de
  password en requests no-password.
- **Runtime:** login de desarrollo real → JWT no expuesto → Spring Security →
  API → SP bajo identidad least-privilege → JSON → Angular Network. Validar
  primero `/usuarios` y catálogo perfiles; commands sólo con fixtures sintéticos
  controlados fuera de LIVE real. Registrar statuses/correlationId sin secretos.

## 16. F6B recomendada

**No volver a implementar Usuarios V1.** F6B debe ser una fase de
validación/hardening del vertical existente y cierre de requisitos:

1. Resolver/verificar la identidad least-privilege de `app.datasource` en entorno
   de desarrollo antes de ejecutar backend autenticado.
2. Validar runtime de GET listado/detalle y catálogo perfil con cuenta autorizada;
   luego reproducir flows mutables sólo en ambiente/fixture sintético controlado.
3. Cerrar riesgo estático de `toString()` de `CrearUsuarioRequest` y
   `UsuarioApp`, con tests de no exposición, si se aprueba como cambio de seguridad.
4. Decidir formalmente si negocio necesita correo verificado, bloqueo o flag
   Usuario SAT. Si no hay aprobación, mantenerlos fuera.
5. Sólo implementar cualquier gap confirmado que no esté ya cubierto por los
   endpoints/SP actuales; entregar evidencia HTTP/Angular y pruebas.

Fase 3C de restablecimiento administrativo ya está implementada; no reabrirla ni
mezclarla con un rediseño de Users V1 sin requerimiento explícito.

## 17. Cambios y seguridad de esta fase

Esta auditoría sólo crea/actualiza documentación. Estado esperado y verificado
antes de commit:

- Java productivo: 0 cambios.
- Frontend productivo: 0 cambios.
- Tests funcionales: 0 cambios.
- SQL/SP/grants versionados: 0 cambios.
- LIVE writes/DDL/grants: 0.
- Secretos/passwords/hashes/JWT en documentación: 0.
- Deploy/merge/PR/review automático: 0.

## 18. Seguimiento F6B — resultado parcial

**Estado:** `F6B_RUNTIME_IDENTITY_BLOCKED`; no declarar runtime-ready.

Git al inicio: feature `feature/administration-users-v1`, HEAD
`039c61412f540ac118cdc3d89e23bc087052c207`, árbol limpio; `dev` y
`origin/dev` en `de4fe8b44bdafe81aa74711e4588b7da2307e0fd`. Stashes requeridos
siguen presentes.

Configuración verificada sin leer valores secretos:

- `application-local.yml`: `app.datasource.*` usa `APP_DB_URL`,
  `APP_DB_USERNAME`, `APP_DB_PASSWORD`, sin defaults.
- `DataSourceConfig.appDataSource()` construye ese pool; adapters de app24
  reciben `appJdbcTemplate` enlazado a ese datasource.
- `build.gradle.kts`: `bootRun` inyecta claves de `backend/.env`; el archivo
  existe y está ignorado por Git. Sólo se inspeccionaron nombres de variables.
- F6A había comprobado con las credenciales locales de diagnóstico una
  identidad privilegiada (`db_owner` y `sysadmin`). No se inició Spring ni se
  atribuye a esta fase una observación independiente desde su pool.

Por riesgo de usar esa identidad privilegiada, no se arrancó backend, no se
obtuvo sesión/token, no se ejecutaron consultas HTTP autenticadas ni commands, y
no se alteró ninguna fila. Identidad Spring efectiva aún no queda comprobada
mediante conexión del pool; no se demostró principal runtime least-privilege.
No cambiar credenciales, grants ni roles en esta fase. Reanudar sólo cuando
estén disponibles credenciales autorizadas de runtime y pueda verificarse el
principal desde el pool Spring.

Hardening puntual: `CrearUsuarioRequest.toString()` redacta password y
`UsuarioApp.toString()` redacta hash. Se agregaron pruebas para asegurar que
valores ficticios no aparezcan en ambas representaciones. No se encontraron
llamadas actuales que registren esos objetos; el cambio evita exposición
accidental futura. No modifica contrato HTTP ni persistencia.

Validación runtime queda pendiente. No se inició backend ni se probaron auth,
HTTP o Angular Network por el bloqueo de identidad.

Suites ejecutadas sin conectar a LIVE:

- Backend `./gradlew clean test` y `./gradlew build`: PASS; 370 tests,
  0 failures, 0 errors, 0 skipped.
- Frontend `pnpm lint`, `pnpm test --watch=false`, `pnpm build`: PASS;
  60 tests, 0 failures. Build conserva warnings de budgets.
- Los dos tests de redacción se incluyeron en backend clean test.

No declarar `USERS_ADMINISTRATION_V1_RUNTIME_READY` hasta validar identidad
least-privilege, auth real, endpoints y Angular Network. Las suites PASS no
sustituyen ese gate runtime.

## 19. Seguimiento F6C — identidad runtime no coincide

**Resultado:** `F6C_RUNTIME_IDENTITY_MISMATCH`; detener gate antes de permisos,
SP READ y validación funcional.

Con `.env` local sin leer/imprimir valores secretos, se ejecutó `bootRun` con el
profile normal y puerto diagnóstico alternativo `18081` porque `8080` ya estaba
ocupado. Instrumentación temporal obtuvo una conexión de `appDataSource` y
registró únicamente metadata JDBC/SQL:

- `DatabaseMetaData.getUserName()`: `opdatos`.
- `USER_NAME()`: `dbo`.
- `SUSER_SNAME()`: `opdatos`.
- `Connection.getCatalog()`: `ANEXO24_DEV`.

La identidad no coincide con `anexo24_app`. Runner abortó inmediatamente tras
esta comparación; `bootRun` terminó con código 1 por el mismatch. No evaluó
memberships/permisos, no ejecutó SP READ ni WRITE, no consultó endpoints/health y
no se intentó auth. El proceso que ya ocupaba `8080` se dejó intacto. La lectura
de metadata desde el pool confirma conexión a `ANEXO24_DEV`, pero no least
privilege.

No se modificó `.env`, credenciales, roles, grants, SQL ni código permanente.
Se eliminaron runner y helper Python temporales. LIVE writes = 0; SP commands =
0. Git permanece sin secretos.

La evidencia versionada define el principal previsto como SQL login y database
user `anexo24_app`, miembro de `app24_runtime`; grants son EXECUTE por objeto.
La configuración efectiva reportada por la conexión Spring sigue resolviendo a
`opdatos`/`dbo`; no inferir causa (valor local, override o endpoint SQL) sin
inspección autorizada adicional. Operador debe verificar localmente qué valor
usa `APP_DB_USERNAME` y que URL apunte al servidor/database esperados; no
compartir password. Reanudar en la obtención metadata del pool después de
corregir la configuración local autorizada.

`RUNTIME_DATASOURCE_LEAST_PRIVILEGE_READY` no alcanzado. Los tests de regresión
F6B se ejecutaron antes de esta reanudación; esta ejecución no repitió suites,
pues no dejó cambios productivos ni pudo superar identidad.

## 20. Seguimiento F6C.1 — username local corregido; password requerido

**Resultado:** `F6C_RUNTIME_PASSWORD_REQUIRED`; detener antes de permisos y SP.

Gate Git inicial PASS: branch `feature/administration-users-v1`, HEAD
`a916122468b5631d66a551cc4f93c926467798f0`, `dev`/`origin/dev` en
`de4fe8b44bdafe81aa74711e4588b7da2307e0fd`, árbol limpio y stashes intactos.

Inspección segura de `backend/.env` encontró una sola ocurrencia de
`APP_DB_USERNAME`, con valor `opdatos`; una sola `APP_DB_URL`, host/instancia
`10.110.110.2\\SERVERSAPBO_DEV`, `databaseName=ANEXO24_DEV`. URL no contenía
usuario, password, `integratedSecurity` ni `authentication`. El parser
`loadDotEnv()` gana por última ocurrencia, por lo que valor efectivo era
`opdatos`. `DB_USERNAME=opdatos` corresponde a datasource primario y se dejó
intacto. `APP_DATASOURCE_USERNAME`, `SPRING_APPLICATION_JSON`, `JAVA_TOOL_OPTIONS`,
`_JAVA_OPTIONS` y `GRADLE_OPTS` no definían overrides relevantes en el entorno
inspeccionado; no se proporcionaron argumentos Spring de username. No se
inspeccionaron valores de passwords ni opciones completas.

Se corrigió sólo `APP_DB_USERNAME=anexo24_app` en `.env` local, preservando el
password configurado sin leerlo/modificarlo. Reparseo confirmó una única
ocurrencia y valor efectivo `anexo24_app`; Git sigue ignorando `.env`.

Con instrumentación temporal y `bootRun` normal en puerto diagnóstico `18081`,
SQL Server rechazó autenticación: `Login failed for user 'anexo24_app'`. El
runner no obtuvo conexión JDBC; por tanto identidad JDBC, database user/login,
role, privilegios y grants no se pudieron verificar desde el pool en este
intento. No hubo fallback a `opdatos`, prueba de password, auth HTTP, health,
SP READ/WRITE ni commands. Runner y helper temporal se eliminaron. LIVE writes
= 0.

No se determinó si rechazo se debe a password local que no corresponde a la
cuenta, login SQL deshabilitado u otra condición del servidor; no especular ni
intentar otras credenciales. Operador debe configurar localmente password
autorizado vigente para login `anexo24_app`, sin compartirlo. Luego reanudar
F6C en la conexión metadata desde `appDataSource`. No cambiar grants/roles ni
usar cuenta administrativa.

`.env.example` conserva `DB_USERNAME=anexo24_app` y
`APP_DB_USERNAME=anexo24_app`. No se cambió el principal primario: la evidencia
de F6C sólo establece el runtime least-privilege para `ANEXO24_DEV`, no el
principal autorizado de CALE_IMMEX; `DB_USERNAME` queda como gap documental a
confirmar aparte.

## 21. Seguimiento F6C.2 — auditoría metadata de login runtime

**Clasificación:** `RUNTIME_PASSWORD_CREDENTIAL_REQUIRED`.

Gate Git PASS: branch `feature/administration-users-v1`, HEAD
`94df3da6deb52fc3e007386e65d6ab6b2e6ff929`, `dev`/`origin/dev` en
`de4fe8b44bdafe81aa74711e4588b7da2307e0fd`, árbol limpio y ambos stashes
requeridos intactos.

Con conexión administrativa autenticada separadamente, sólo para consultas de
metadata/seguridad, login `anexo24_app` existe como `SQL_LOGIN`, está habilitado,
y tiene default database `master`. `LOGINPROPERTY` indica `IsLocked=0`,
`IsExpired=0`, `IsMustChange=0`; `BadPasswordCount=2`. No se consultó ni expuso
hash de password. La metadata no apunta a cuenta bloqueada, disabled, expirada
o con cambio obligatorio; SQL Server rechazó el intento de autenticación
anterior del pool Spring.

En `ANEXO24_DEV`, database user `anexo24_app` existe y su SID coincide con el
server login. Membership: `app24_runtime=1`, `db_owner=0`, `db_datareader=0`,
`db_datawriter=0`; `sysadmin=0`. No se observaron memberships en otros server
roles aparte de `public`, ni permisos explícitos `CONTROL SERVER`/`ALTER ANY
LOGIN`/`IMPERSONATE ANY LOGIN` para el login. No se observaron permisos amplios
`CONTROL`/`EXECUTE` en database ni `EXECUTE` en schema `app24` para el user/role.

`HAS_PERMS_BY_NAME` bajo `EXECUTE AS USER='anexo24_app'` confirmó ausencia de
acceso directo en tablas funcionales comprobadas: SELECT/INSERT/UPDATE/DELETE
sobre `app24.UsuarioApp`; SELECT sobre `PerfilApp`, `PerfilActividad` y
`Actividad`; SELECT/INSERT sobre `BitacoraEvento`. Los EXECUTE específicos
requeridos por el gate están presentes para los 12 SP de Auth, Usuarios,
Perfiles y bitácora (`APP24_Q_USUARIO_POR_CLAVE`,
`APP24_Q_USUARIO_ACCESO`, `APP24_Q_USUARIOS_LISTAR`,
`APP24_Q_USUARIO_OBTENER`, `APP24_C_USUARIO_CREAR`,
`APP24_C_USUARIO_ACTUALIZAR_DATOS`, `APP24_C_USUARIO_CAMBIAR_ESTADO`,
`APP24_C_USUARIO_CAMBIAR_PERFIL`, `APP24_C_USUARIO_CAMBIAR_VIGENCIA`,
`APP24_C_USUARIO_RESTABLECER_PASSWORD`, `APP24_Q_PERFILES_LISTAR`,
`APP24_C_BITACORA_REGISTRAR`). Sesiones activas actuales para login
`anexo24_app`: 0; esto no determina consumidores históricos.

La conexión administrativa integrada de Windows falló como
`GRUPOCALE\\opdatos`; la metadata se obtuvo después mediante login SQL
administrativo local, sólo en consultas de lectura. No se ejecutaron SP ni
consultas de negocio bajo `anexo24_app`; no hubo writes, DDL, cambios de grants,
roles o password.

La metadata confirma login, mapping, membership y permisos esperados, pero no
prueba que el password local configurado autentique. La autenticación previa
falló. Clasificar como `RUNTIME_PASSWORD_CREDENTIAL_REQUIRED`: operador debe
obtener la credencial vigente autorizada del responsable SQL Server y
configurarla localmente en `APP_DB_PASSWORD`; no compartirla en chat/reporte.
No rotar password ni probar alternativas en esta fase. Al disponer de
credencial autorizada, reanudar desde `appDataSource` identity check.

## 22. Seguimiento F6C — runtime real validado en el entorno actual

**Resultado funcional:** `RUNTIME_DATASOURCE_VALIDATED`.

Esta sección supersede las expectativas de identidad runtime descritas en los
seguimientos F6C anteriores para el entorno actual. El operador confirmó que el
principal autorizado y utilizado actualmente es `opdatos`; `anexo24_app` y
`app24_runtime` corresponden al modelo de least privilege versionado/propuesto,
pero no son la identidad runtime actual de este entorno. No se migró ni modificó
ningún login, password, rol o grant.

Evidencia manual recibida desde el `appDataSource` real:

- JDBC principal: `opdatos`.
- Database user: `dbo`.
- Login: `opdatos`.
- Catalog: `ANEXO24_DEV`.
- Conexión funcional: PASS.
- `APP24_Q_USUARIOS_LISTAR`: PASS, count `1`.
- `APP24_Q_PERFILES_LISTAR`: PASS, count `2`.
- Filas personales incluidas en evidencia: 0.

Observación de seguridad de la misma sesión:

- `app24_runtime=0`.
- `db_owner=1`, `db_datareader=1`, `db_datawriter=1`, `sysadmin=1`.
- `privileged_runtime_principal=true`.

Interpretación: el runtime funcional y los dos READ smoke tests pasan. La base
de datos **no aplica least privilege** al principal runtime actual; queda como
`KNOWN_SECURITY_RISK` / deuda de hardening, no como fallo funcional ni como
vulnerabilidad explotada. La identidad actual `opdatos` no se cambia en F6C. Una
futura migración a una cuenta dedicada con EXECUTE específico requiere decisión
y provisión explícitas.

SP-FIRST sigue siendo una regla de arquitectura independiente de los privilegios
del login: la aplicación conserva el consumo mediante Stored Procedures y no se
autoriza acceso funcional directo a tablas. En esta fase no se ejecutaron SP
WRITE, commands, DML, DDL ni cambios de grants/roles.

Instrumentación temporal `F6cRuntimeGateRunner` retirada después de la evidencia;
no forma parte del producto ni debe versionarse.

### Backlog de seguridad independiente

- `SECURITY_RUNTIME_LEAST_PRIVILEGE`: evaluar y planificar principal dedicado
  con permisos mínimos de EXECUTE; no cambiar permisos ni identidad como parte
  de F6C.
- `SECURITY_HARDENING_DEFAULT_USER_AUTOCONFIG`: revisar por qué durante
  `bootRun` aparece la autoconfiguración de `UserDetailsService`, un
  `InMemoryUserDetailsManager` y una credencial generada de desarrollo, pese al
  esquema JWT/stateless existente. La credencial observada no se registra ni se
  reutiliza. No se modificó `SecurityConfig` en F6C.

## 23. F6D.2 — smoke HTTP autenticado read-only de Usuarios

**Resultado:** `USERS_ADMINISTRATION_V1_AUTH_RUNTIME_VALIDATED`.

Con una cuenta de prueba autorizada, manejada exclusivamente en memoria y sin
registrar credencial, JWT, cabecera Authorization ni datos personales, el flujo
HTTP local confirmó:

- Login `200`, JWT recibido y usuario autenticado; la sesión incluyó
  `USUARIOS_ADMINISTRAR`, `BITACORA_CONSULTAR` y `OPERACIONES_CONSULTAR`.
- `GET /api/v1/administracion/usuarios` `200`: total 1, página 1, tamaño 20.
- Detalle del único usuario listado `200`, con los campos administrativos
  esperados; el ID inexistente devolvió `404`, código
  `RECURSO_NO_ENCONTRADO` y correlation ID.
- `GET /api/v1/administracion/perfiles` `200`: total 2.
- Filtros `clave`, `nombre`, `correo`, `estado`, `perfilId`, `pagina` y
  `tamano` devolvieron `200`; un filtro imposible devolvió `200` con 0 items.
- Listado y detalle no contienen propiedades `password`, `passwordHash` ni
  `password_hash`.
- Listado sin token y con token inválido devolvieron `401`.

No se ejecutaron commands: POST/PUT/PATCH/reset = 0 y LIVE writes = 0
(`LIVE_COMMAND_E2E_SKIPPED_NO_SAFE_CLEANUP`). La cobertura de commands se
mantiene en las suites ya aprobadas. No se realizaron DDL, DML, cambios de SP,
roles, grants ni logins.

Como verificación complementaria, Bitácora autenticada con rango UTC obligatorio
respondió `200` y sus filtros `modulo`/`resultado` funcionaron; su listado no
presentó nombres de propiedades sensibles. Bitácora sin token y con token
inválido devolvieron `401`.

La validación visual de Angular sigue siendo QA manual complementario; no fue
necesaria para el gate HTTP/API. Los pendientes de seguridad y Unicode se
mantienen sin cambios.
