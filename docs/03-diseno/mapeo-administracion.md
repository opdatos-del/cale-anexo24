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
| `frontend/src/app/features/administration/**` | Solo `audit-log` implementado; `users`, `profiles`, `permissions` vacíos (`.gitkeep`) | **IMPLEMENTADO EN REPOSITORIO** (bitácora); **NO IMPLEMENTADO** (resto) |
| `docs/03-diseno/modelo-datos.md`, `docs/04-desarrollo/api.md` | Modelo lógico y contrato API documentado | **CONFIRMADO** como documentación; `api.md` describe endpoints de administración **NO implementados** |

No se ejecutó SQL, no se consultó runtime remoto y no se debilitó TLS.

## 3. Modelo de datos `app24` (DDL versionado)

### 3.1 PerfilApp

| Campo | DDL | Conclusión |
|---|---|---|
| `id` | `BIGINT IDENTITY(1,1)` PK | Identidad técnica |
| `nombre` | `VARCHAR(80) NOT NULL UNIQUE` | Nombre canónico de perfil |
| `estado` | `VARCHAR(20) DEFAULT 'ACTIVO'` | **INFERIDO:** `ACTIVO`/`INACTIVO` como los demás estados; **PENDIENTE:** sin `CHECK`, sin catálogo documentado |

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
fallará por `NO ACTION`. **DECISIÓN V1:** inactivación, no borrado físico.

### 3.4 UsuarioApp

| Campo | DDL | Conclusión |
|---|---|---|
| `id` | `BIGINT IDENTITY(1,1)` PK | Identidad técnica |
| `clave` | `VARCHAR(30) NOT NULL UNIQUE` | Login idempotente |
| `correo` | `VARCHAR(150) NOT NULL UNIQUE` | Identidad correo |
| `password_hash` | `VARCHAR(100) NOT NULL` | bcrypt (60 chars, ADR-002); hash seed `$2b$10$...` compatible |
| `estado` | `VARCHAR(20) DEFAULT 'ACTIVO'` | **INFERIDO:** ACTIVO/INACTIVO; sin `CHECK` |
| `vigencia` | `DATE NULL` | Vencimiento opcional de cuenta |
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

**CONSECUENCIA V1 (diseño):** al implementar Administración, `findPermisosByUsuario`
debe unirse también a `PerfilApp` filtrando `p.estado = 'ACTIVO'`, o el modelo
de inactivación de perfiles no tendrá efecto. **PENDIENTE de decisión** si la
inactivación de perfil también debe invalidar sesiones JWT existentes (hoy el
token conserva `auth` hasta expirar).

## 7. Bitácora y eventos de Administración

**CONFIRMADO:** `BitacoraModulo` incluye `ADMINISTRACION`; `BitacoraAccion`
solo define `LOGIN_OK`/`LOGIN_FALLIDO`.

**PENDIENTE:** acciones de Administración (candidatas de
`mapeo-bitacora.md` §8: `USUARIO_CREADO`, `USUARIO_ESTADO_CAMBIADO`,
`PERMISOS_CAMBIADOS`, ...) se aprueban solo al habilitar el caso de uso. Writer
append-only existente (`RegistrarEventoBitacoraService` + `BitacoraJdbcAdapter`)
está listo para conectarse a los futuros commands con
`AuthenticatedUserContext` como actor.

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

## 9. Permisos runtime de base de datos

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

**PENDIENTE DE DESPLIEGUE:** script no aplicado en servidor. **PENDIENTE V1:**
cuando existan commands de Administración, `app24_runtime` necesitará
`INSERT/UPDATE` sobre `UsuarioApp`/`PerfilApp`/`PerfilActividad` (y
eventualmente `Actividad`) — decisión de privilegios mínimos por objeto antes
de implementar.

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
documentación prospectiva. **DECISIÓN V1:** no crear dichos endpoints en esta
fase; cuando se implementen, `api.md` deberá listarlos con su permiso y
contrato real.

## 12. Contratos V1 candidatos

### 12.1 Autenticación — IMPLEMENTADO EN REPOSITORIO

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "clave": "admin", "password": "..." }
```

Response: `{ token, expiraEn, usuario, permisos }`. Público; `@NotBlank` en
ambos campos; `CredencialesInvalidasException` → 401 genérico sin revelar si
existía la clave.

### 12.2 Administración — NO IMPLEMENTADO; candidato

| Recurso | Operación V1 candidata | Permiso | Fuente |
|---|---|---|---|
| `/api/v1/usuarios` | Listar (paginado), crear, estado/vigencia, perfil | `USUARIOS_ADMINISTRAR` | `app24.UsuarioApp` + `PerfilApp` |
| `/api/v1/perfiles` | Listar, crear, estado, asignación de actividades | `PERFILES_ADMINISTRAR` | `app24.PerfilApp` + `PerfilActividad` |
| `/api/v1/actividades` | Listar | `ACTIVIDADES_ADMINISTRAR` | `app24.Actividad` |

**PENDIENTE:** contrato HTTP, DTOs, validación de esquema, paginación, eventos
de bitácora por operación y privilegios `app24_runtime` para escritura. Ninguno
se implementa en esta fase.

## 13. Riesgos y pendientes

1. **Perfil inactivo concede permisos** (hallazgo §6): corregir consulta o
   modelo al implementar; validar política de sesiones JWT vigentes.
2. Desplegar `04-app-runtime-permissions.sql` y verificar runtime con TLS
   confiable (fuera de esta fase).
3. Definir modelo de administración: CRUD de usuarios (¿crear/activar/
   desactivar/cambiar vigencia?), perfil único por usuario, catálogo de
   estados sin `CHECK` en DDL.
4. Aprobar eventos de bitácora de Administración y conectarlos al writer con
   actor de `AuthenticatedUserContext`.
5. Ajustar `app24_runtime` con privilegios mínimos de escritura cuando existan
   commands.
6. Cerrar política 401/403 en bitácora.
7. API `api.md`: corregir la declaración prospectiva de `/usuarios`,
   `/perfiles`, `/actividades` cuando se implementen.

## 14. Restricciones cumplidas

- Fase 0 documental: cero código nuevo, cero SQL remoto, cero bypass TLS.
- Solo se agregó documentación: este mapeo y las filas de Administración en
  `matriz-integracion-modulo-c.md`.
- No se modificó `api.md` ni `modelo-datos.md` en esta fase.
- Sin PR, sin merge, sin code review automático.