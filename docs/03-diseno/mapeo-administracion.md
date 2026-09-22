# Administración (usuarios, perfiles y permisos)

## 1. Objetivo y decisión arquitectónica

Esta auditoría documental define el estado del módulo Administración en el
repositorio: modelo de datos, dominio, autenticación, permisos runtime y
contratos candidatos V1. Fase 0: **solo documentación, sin código nuevo**.

**CONFIRMADO:** solo existe la lectura orientada a autenticación
(`findByClave`, `findPermisosByUsuario`) y el flujo de login/JWT. No hay
controllers, services ni adapters de escritura para administrar usuarios,
perfiles o actividades. Los permisos `USUARIOS_ADMINISTRAR`,
`PERFILES_ADMINISTRAR` y `ACTIVIDADES_ADMINISTRAR` existen solo como seed SQL;
no tienen backend ni frontend.

Esta segunda pasada **cierra las decisiones funcionales/técnicas V1** previas a
cualquier command administrativo. No contradice la evidencia confirmada: la
amplía. Todo lo marcado `DECISIÓN V1` es contrato de diseño, no capacidad
actual.

> **DECISIÓN V1.** El módulo Administración usa la base de aplicación
> `ANEXO24_DEV.app24`, no Módulo C. La regla STORED PROCEDURE FIRST no aplica
> aquí: no se crean `APP24_Q_*` para el esquema propio. La matriz de
> integración registra estas fuentes como `APP DATABASE`.

## 2. Fuentes de evidencia

| Fuente | Aporta | Estado |
|---|---|---|
| `infra/sql/02-app-schema.sql` | DDL de `app24` (PerfilApp, Actividad, PerfilActividad, UsuarioApp, BitacoraEvento, CargaFacturacion, ErrorCarga, ConfiguracionPlantilla) | **CONFIRMADO** para esquema versionado |
| `infra/sql/03-app-seed-security.sql` | Perfiles `ADMINISTRADOR`/`CONSULTA`, 12 actividades, asignaciones y usuario `admin` | **CONFIRMADO** |
| `infra/sql/04-app-runtime-permissions.sql` | Política mínima por objeto para `anexo24_app` vía rol `app24_runtime` | **IMPLEMENTADO EN REPOSITORIO**; **PENDIENTE DE DESPLIEGUE** |
| `administration/users/**` | Modelo `UsuarioApp`, puerto `UsuarioRepository`, `UsuarioJdbcAdapter` | **IMPLEMENTADO EN REPOSITORIO**; solo lectura de autenticación |
| `security/**` | Login, JWT, filtro, contexto de usuario, entry point y excepciones | **IMPLEMENTADO EN REPOSITORIO** |
| `auditlog/**` | Catálogo de módulos/acciones y writer append-only | **IMPLEMENTADO EN REPOSITORIO**; acciones de Administración pendientes |
| `shared/config/DataSourceConfig.java` | Dos orígenes: `primaryDataSource`/`jdbcTemplate` (Módulo C) y `appDataSource`/`appJdbcTemplate` (app24); sin transaction manager declarado | **CONFIRMADO**; hallazgo transaccional en §29 |
| `frontend/src/app/features/administration/**` | Solo `audit-log` implementado; `users`, `profiles`, `permissions` vacíos (`.gitkeep`) | **IMPLEMENTADO EN REPOSITORIO** (bitácora); **NO IMPLEMENTADO** (resto) |
| `docs/03-diseno/modelo-datos.md`, `docs/04-desarrollo/api.md` | Modelo lógico y contrato API documentado | **CONFIRMADO** como documentación; `api.md` describe rutas prospectivas de administración **NO implementadas** |

No se ejecutó SQL, no se consultó runtime remoto y no se debilitó TLS.

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
| `UsuarioRepository` | `findByClave(String)`, `findPermisosByUsuario(Long)` | **IMPLEMENTADO EN REPOSITORIO**; puerto mínimo de autenticación |
| `UsuarioJdbcAdapter` | `@Qualifier("appJdbcTemplate")`; SQL parametrizado contra `app24.UsuarioApp` | **IMPLEMENTADO EN REPOSITORIO**; usa base de aplicación, no Módulo C |

**CONFIRMADO:** el paquete `administration` contiene únicamente esos 3
archivos. No hay dominio para PerfilApp ni Actividad: se referencian solo por
SQL y seed.

## 5. Autenticación (`security/**`)

| Componente | Comportamiento | Conclusión |
|---|---|---|
| `LoginController` | `POST /api/v1/auth/login`, público; pasa correlationId normalizado | **IMPLEMENTADO EN REPOSITORIO** |
| `LoginRequest` | `clave` y `password` `@NotBlank` | **IMPLEMENTADO EN REPOSITORIO** |
| `LoginResponse` | `token`, `expiraEn`, `usuario`, `permisos` | **IMPLEMENTADO EN REPOSITORIO** |
| `LoginService` | `findByClave`; `estaActiva()` + `passwordEncoder.matches`; permisos; `LOGIN_OK`/`LOGIN_FALLIDO`; JWT con permisos | **IMPLEMENTADO EN REPOSITORIO** |
| `JwtTokenService` | HS256; claims `subject=clave`, `uid`, `auth`; expiración `${jwt.expiration-minutes}` | **IMPLEMENTADO EN REPOSITORIO** |
| `JwtAuthFilter` | Bearer; valida firma; `uid` numérico entero positivo obligatorio; falla → anónimo (fail-closed) | **IMPLEMENTADO EN REPOSITORIO** |
| `AuthenticatedUserPrincipal` / `AuthenticatedUserContext` | Identidad confiable desde contexto; `currentUser()` solo con principal esperado | **IMPLEMENTADO EN REPOSITORIO** |
| `SecurityConfig` | Stateless; `permitAll` login/health/info/swagger; resto `authenticated`; `@EnableMethodSecurity`; bcrypt | **IMPLEMENTADO EN REPOSITORIO** |
| `ApiAuthenticationEntryPoint` | 401 JSON con correlación | **IMPLEMENTADO EN REPOSITORIO** |
| `GlobalExceptionHandler` | 401 credenciales, 403 acceso, 401 auth, 400 validación/formato, 503 datos, 500 inesperado | **IMPLEMENTADO EN REPOSITORIO** |

**CONFIRMADO:** la autorización por endpoint usa `@PreAuthorize("hasAuthority('...')")`
— 9 controladores existentes (bitácora, materiales, productos, estructuras,
entradas, salidas, materiales utilizados, activos fijos). La autorización recae
enteramente en el claim `auth` del token.

## 6. HALLAZGO CRÍTICO — permisos sin validar `PerfilApp.estado`

`UsuarioJdbcAdapter.findPermisosByUsuario` ejecuta:

```sql
SELECT a.clave FROM app24.UsuarioApp u
JOIN app24.PerfilActividad pa ON pa.perfil_id = u.perfil_id
JOIN app24.Actividad a ON a.id = pa.actividad_id
WHERE u.id = ? AND u.estado = 'ACTIVO'
```

**CONFIRMADO (código):** filtra `UsuarioApp.estado = 'ACTIVO'` pero **no**
consulta `PerfilApp.estado`. `LoginService` valida `usuario.estaActiva()`
(usuario + vigencia), pero ningún punto valida que el **perfil asignado** esté
`ACTIVO`.

**Consecuencia:** un perfil `INACTIVO` sigue concediendo todos sus permisos al
usuario que lo tiene asignado (mientras el usuario esté ACTIVO y vigente). El
`PerfilApp.estado` quedó sin efecto operativo.

La decisión de diseño que cierra este hallazgo está en **§17 — Semántica de
perfil INACTIVO** (login debe rechazar, no emitir JWT vacío) y **§18 —
Sesiones JWT ya emitidas** (consistencia eventual; sin revocación inmediata en
V1). **NO implementado todavía.**

## 7. Bitácora y eventos de Administración

**CONFIRMADO:** `BitacoraModulo` incluye `ADMINISTRACION`; `BitacoraAccion`
solo define `LOGIN_OK`/`LOGIN_FALLIDO`.

**PENDIENTE:** acciones de Administración (catálogo V1 cerrado en **§28**)
se aprueban junto con la implementación. Writer append-only existente
(`RegistrarEventoBitacoraService` + `BitacoraJdbcAdapter`) está listo para
conectarse a los futuros commands con `AuthenticatedUserContext` como actor.

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

**NO IMPLEMENTADO:** los permisos `USUARIOS_ADMINISTRAR`,
`PERFILES_ADMINISTRAR` y `ACTIVIDADES_ADMINISTRAR` no protegen ningún endpoint
porque no existen endpoints de administración.

## 9. Permisos runtime de base de datos — estado actual

**CONFIRMADO (script):** `04-app-runtime-permissions.sql` concede a
`app24_runtime` (miembro `anexo24_app`):

| Objeto | Permiso |
|---|---|
| `app24.UsuarioApp` | `SELECT` |
| `app24.PerfilActividad` | `SELECT` |
| `app24.Actividad` | `SELECT` |
| `app24.BitacoraEvento` | `SELECT, INSERT` |

Retira `db_datareader`/`db_datawriter` y `REVOKE EXECUTE` global; `ALTER ROLE ...
DROP MEMBER` solo si la membership existe (idempotente).

**PENDIENTE DE DESPLIEGUE:** script no aplicado en servidor. La matriz de
permisos candidata para Administración futura está en **§31**.

## 10. Frontend

| Ruta | Estado |
|---|---|
| `features/administration/audit-log` | **IMPLEMENTADO EN REPOSITORIO** — ruta `/bitacora` lazy con `permissionGuard` + `data.permission: 'BITACORA_CONSULTAR'`; sidebar «Administración» → «Bitácora» (`manage_search`) |
| `features/administration/users` | **NO IMPLEMENTADO** — solo `.gitkeep` |
| `features/administration/profiles` | **NO IMPLEMENTADO** — solo `.gitkeep` |
| `features/administration/permissions` | **NO IMPLEMENTADO** — solo `.gitkeep` |

**CONFIRMADO:** `permissionGuard` redirige a `/forbidden` cuando el usuario no
tiene el permiso; `app.routes.ts` usa el patrón `canActivate + data.permission`
para todas las rutas protegidas. Los placeholders `users`/`profiles`/
`permissions` no tienen rutas ni menú: la estructura hexagonal de `audit-log`
es el patrón a replicar cuando se implementen.

## 11. Documentación declarada vs. realidad

`docs/04-desarrollo/api.md` (§ endpoints) declara: «Los endpoints de
administración siguen `/usuarios`, `/perfiles` y `/actividades»...».

**CONFIRMADO (divergencia):** esos endpoints **no existen** en el repositorio:
no hay controllers `/api/v1/usuarios|perfiles|actividades`. La línea es
documentación prospectiva. **DECISIÓN V1:** rutas reales bajo
`/api/v1/administracion/*` (§32); cuando se implementen, `api.md` deberá
corregirse para reflejar las rutas y permisos reales. **NO se modifica
`api.md` en esta fase.**

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
| RESTABLECER CONTRASEÑA | Sí | `POST /api/v1/administracion/usuarios/{id}/password` (§15) |
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
- **RESTABLECER CONTRASEÑA:** endpoint/command separado
  (`POST /{id}/password`), no la edición general (`PUT`), porque es acción
  sensible con evento de Bitácora propio.

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

**Estado: NO implementado todavía.** La corrección concreta toca
`findPermisosByUsuario` (JOIN a `PerfilApp` + `p.estado = 'ACTIVO'`) y
`LoginService` (rechazo temprano). Se pone en FASE 1 del roadmap (§38) como
hardening previo, junto con tests.

## 18. Sesiones JWT ya emitidas — DECISIÓN V1

**CONFIRMADO POR ARQUITECTURA ACTUAL (código):** el JWT incluye claims
`subject` (clave), `uid` y `auth` (lista de autoridades), y es **stateless**.
`JwtAuthFilter` valida firma, expiración y claims, pero **no vuelve a
consultar** `UsuarioApp.estado`, `vigencia`, `PerfilApp.estado` ni
`PerfilActividad` en cada request.

Por tanto, si después del login se inactiva usuario, vence la vigencia, cambia
el perfil, se revoca un permiso o se inactiva un perfil, **un JWT ya emitido
conserva sus `authorities` hasta expirar** (`jwt.expiration-minutes`, default
15 según `application-test.yml`; valor prod por `.env`). No afirmar
revocación inmediata.

**DECISIÓN V1:** aceptar **consistencia eventual hasta expiración**. El token
corto (15 min) limita la ventana; la alternativa de revocación inmediata
(denylist/JTI indexado, versión de token, re-validación por request) se marca
como **trabajo adicional, NO capacidad actual**, y solo si auditoría/negocio
la exigen.

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
(`findPermisosByUsuario`). **NO implementar permisos directos por usuario en
V1** — exigiría cambio DDL y complejiza la auditoría.

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

**Documentación de implementación:** la validación C necesita una consulta
adicional (existencia de al menos un usuario en las condiciones anteriores con
perfil ACTIVO y los dos permisos). Es un puerto/método nuevo de repositorio
(por ej. `existsConCapacidadAdministrativa()` o equivalente) y se ejecuta en la
misma transacción del command que inactiva un usuario/perfil. **PENDIENTE de
implementar** — no existe hoy.

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

## 29. Transaccionalidad — HALLAZGO CONFIRMADO

**CONFIRMADO (código, `shared/config/DataSourceConfig.java`):** existen los
beans:

```text
primaryDataSourceProperties, primaryDataSource   (@Primary — Módulo C / CALE_IMMEX)
jdbcTemplate                                     (Módulo C)
appDataSourceProperties, appDataSource           (ANEXO24_DEV / app24)
appJdbcTemplate                                  (app24)
```

**PERO NO existe** en `DataSourceConfig` — ni en el resto de
`backend/src/main/java` (grep completo) — un
`PlatformTransactionManager` / `DataSourceTransactionManager` /
`JdbcTransactionManager` / `TransactionTemplate` específico para
`appDataSource`. Cero apariciones de `@Transactional` en el backend.

**Por tanto:** NO está demostrado que un futuro `@Transactional` sobre commands
de Administración englobe correctamente `UPDATE/INSERT app24.*` **+**
`INSERT app24.BitacoraEvento`. Por auto-configuración de Spring Boot, el
transaction manager único se enlaza al DataSource `@Primary` (Módulo C); **no
asumir que el default apunta a `appDataSource`**.

**DECISIÓN V1:** antes del primer command administrativo mutable:
1. configurar un transaction manager explícito para `appDataSource`
   (`DataSourceTransactionManager` o `JdbcTransactionManager` sobre
   `appDataSource`, expuesto con `@Qualifier` propio);
2. usarlo en los commands de negocio **y** en el evento final de Bitácora (la
   operación de negocio y su evento en la misma transacción);
3. probar con tests la participación conjunta (rollback del evento si falla la
   operación y viceversa).

**NO implementar todavía.** Queda en FASE 1 del roadmap (§38).

## 30. Estrategia de persistencia app24 — DECISIÓN V1

Para tablas propias `ANEXO24_DEV.app24`:

- **JdbcTemplate parametrizado directo** (patrón ya usado por
  `UsuarioJdbcAdapter` / `BitacoraConsultaJdbcAdapter`);
- **transacciones explícitas** (transaction manager app24, §29);
- **grants mínimos por objeto** (§31).

**NO crear stored procedures solo por seguir SP-FIRST.** SP-FIRST queda
exclusivamente para `CALE_IMMEX` / Módulo C. Un SP en `app24` solo tendría
sentido si aparece lógica DB compleja con valor real demostrado. Evitar
sobreingeniería.

## 31. Runtime permissions — matriz futura candidata

Estado actual (§9) + **candidata V1 para Administración** (no modificar `04`
todavía):

| Objeto | Hoy | Administración futura |
|---|---|---|
| `UsuarioApp` | `SELECT` | `SELECT, INSERT, UPDATE` — **NO DELETE** |
| `PerfilApp` | — | `SELECT, INSERT, UPDATE` — **NO DELETE** |
| `PerfilActividad` | `SELECT` | `SELECT, INSERT, DELETE` |
| `Actividad` | `SELECT` | `SELECT` — **NO INSERT/UPDATE/DELETE** |
| `BitacoraEvento` | `SELECT, INSERT` | `SELECT, INSERT` (sin cambio) |

**Explicación:** `DELETE` en `PerfilActividad` es necesario para reemplazar
asignaciones (`PUT /{id}/permisos`, §24), pero **NO implica** DELETE de
`PerfilApp`, `UsuarioApp` ni `Actividad` — esos objetos nunca reciben DELETE
(§13, §26, §27). El despliegue de esta matriz va en FASE 7 (§38) con TLS
confiable.

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

**Documentado:** `docs/04-desarrollo/api.md` todavía usa rutas prospectivas de
raíz (`/usuarios`, `/perfiles`, `/actividades`) y **deberá corregirse al
implementar**. No se modifica `api.md` en esta fase.

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
  password no blank (política de complejidad PENDIENTE).

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
multi-propósito ambiguo. **NO implementar.**

## 34. Contratos — PERFILES (candidatos)

- **Permiso común:** `PERFILES_ADMINISTRAR`.

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

## 38. Roadmap de implementación recomendado

Secuencial — no comenzar FASE N+1 sin cerrar FASE N:

```text
FASE 1  Hardening previo:
        - PerfilApp.estado efectivo en auth (login + findPermisosByUsuario);
        - transaction manager explícito appDataSource + tests (§29);
        - handler 404/409 y convención de errores.

FASE 2  Usuarios read-only:
        - query/paginación con JOIN PerfilApp;
        - GET /administracion/usuarios (+ /{id}).

FASE 3  Usuarios commands:
        - crear, editar, estado, perfil, vigencia, reset password;
        - guardrails auto-bloqueo (§25);
        - Bitácora (§28); grants UsuarioApp (§31).

FASE 4  Frontend Usuarios.

FASE 5  Perfiles + consulta Actividades:
        - query; commands; reemplazo transaccional PerfilActividad;
        - Bitácora; grants PerfilApp/PerfilActividad (§31).

FASE 6  Frontend Perfiles/Permisos.

FASE 7  Runtime permissions / despliegue con TLS confiable
        (aplicar 04 + matriz §31, verificar runtime).

FASE 8  E2E administrativo controlado.
```

Ajustar únicamente si la evidencia de implementación justifica otro orden.

## 39. Riesgos y pendientes

1. **PerfilInactivo-concede-permisos (§6/§17):** corregido solo en FASE 1; hoy
   sigue siendo un hueco real.
2. JWT emitidos no revocables (consistencia eventual aceptada, §18) — revisar
   con auditoría si la ventana de 15 min no es aceptable.
3. Desplegar `04-app-runtime-permissions.sql` y verificar runtime con TLS
   confiable (FASE 7).
4. Confirmar timezone JVM en producción para semántica de `vigencia` (§19).
5. Política de complejidad de contraseñas y de reset (generada vs. provista)
   — PENDIENTE, no bloquea contrato.
6. Aprobar preventivamente `must_change_password`/historial/MFA si negocio los
   exige — FUERA DE V1 (§15).
7. Posible limpieza futura de `ACTIVIDADES_ADMINISTRAR` del seed (§23).
8. Corregir `api.md` (rutas y permisos reales) al implementar (§32).

## 40. Restricciones cumplidas

- Fase 0 documental: cero código nuevo, cero SQL remoto, cero bypass TLS.
- Solo se modificó `docs/03-diseno/mapeo-administracion.md` en esta segunda
  pasada; la matriz de integración se dejó **intacta** (las filas existentes
  de Administración siguen siendo correctas; ninguna decisión nueva las
  contradice).
- No se modificó `backend/**`, `frontend/**`, `infra/sql/**` ni
  `docs/04-desarrollo/api.md`.
- Sin PR, sin merge, sin code review automático.