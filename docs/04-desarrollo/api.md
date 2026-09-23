# API REST

**Base:** `/api/v1` · **Formato:** JSON UTF-8 · **Seguridad:** Bearer token y
autorización por permiso · **Errores:** `{ code, message, correlationId, details? }`.

| Método | Ruta | Función | Permiso |
|---|---|---|---|
| POST | `/auth/login` | Inicia sesión. | Público con protección antiabuso |
| GET | `/catalogos/materiales` | Lista materiales paginados. | `MATERIALES_CONSULTAR` |
| GET | `/catalogos/productos` | Lista productos paginados. | `PRODUCTOS_CONSULTAR` |
| GET | `/operaciones/{tipo}` | Consulta entradas, salidas, consumo o activo fijo por rango. | Permiso del módulo |
| GET | `/reportes/{tipo}` | Genera datos de reporte por rango. | `REPORTES_GENERAR` |
| GET | `/reportes/{tipo}/exportacion` | Exporta reporte con resultados. | `REPORTES_EXPORTAR` |
| POST | `/facturacion/cargas` | Valida archivo y crea lote no persistido. | `FACTURACION_CARGAR` |
| POST | `/facturacion/cargas/{id}/confirmar` | Guarda lote validado. | `FACTURACION_GUARDAR` |
| GET | `/bitacora` | Consulta read-only paginada con rango UTC obligatorio (`desde`/`hasta`), filtros `usuarioId`, `modulo`, `resultado`, `correlationId` y orden `fecha DESC, id DESC`. | `BITACORA_CONSULTAR` |
| GET | `/administracion/usuarios` | Lista usuarios paginados. | `USUARIOS_ADMINISTRAR` |
| GET | `/administracion/usuarios/{id}` | Obtiene el detalle sin secretos de un usuario. | `USUARIOS_ADMINISTRAR` |
| POST | `/administracion/usuarios` | Crea un usuario activo, registra auditoría y devuelve `201 Created`. | `USUARIOS_ADMINISTRAR` |
| PUT | `/administracion/usuarios/{id}` | Modifica exclusivamente `nombre` y `correo`; registra auditoría. | `USUARIOS_ADMINISTRAR` |
| PATCH | `/administracion/usuarios/{id}/estado` | Cambia el estado del usuario. | `USUARIOS_ADMINISTRAR` |
| PATCH | `/administracion/usuarios/{id}/perfil` | Cambia el perfil asignado. | `USUARIOS_ADMINISTRAR` |
| PATCH | `/administracion/usuarios/{id}/vigencia` | Cambia la vigencia o la elimina. | `USUARIOS_ADMINISTRAR` |
| POST | `/administracion/usuarios/{id}/password` | Restablece la contraseña y responde `204 No Content`. | `USUARIOS_ADMINISTRAR` |

## Administración de usuarios — Fase 3A

Las rutas de usuarios están protegidas por `USUARIOS_ADMINISTRAR`.
Fases 3A y 3B están implementadas e integradas en DEV mediante commands SP
atómicos. Fase 3C está implementada y validada LIVE en su rama. Runtime está
READY: `app24_runtime` provisionado, membership correcta, 12 EXECUTE específicos,
cero grants directos de tablas y ownership chain compatible.

### `GET /administracion/usuarios`

Lista paginada con filtros `clave` (exacto), `nombre` (parcial con comodines
escapados), `correo` (exacto), `estado` (`ACTIVO` o `INACTIVO`) y `perfilId`
(exacto). El orden es `clave ASC, id ASC`.

### `GET /administracion/usuarios/{id}`

Devuelve los datos administrativos y el perfil asignado. Nunca expone
`password`, `passwordHash` ni otros secretos.

### `POST /administracion/usuarios`

Recibe `clave`, `nombre`, `correo`, `password`, `perfilId` y opcionalmente
`vigencia`. El usuario se crea en estado `ACTIVO`, con un perfil existente y
activo; `clave` y `correo` son únicos.

La contraseña debe tener **entre 10 y 50 caracteres**, e incluir al menos una
**mayúscula**, un **número** y un **carácter especial**. Se persiste únicamente
su hash BCrypt; la contraseña y el hash nunca se devuelven ni se registran en
la bitácora.

### `PUT /administracion/usuarios/{id}`

Solo admite la actualización de `nombre` y `correo`. La clave, el estado, el
perfil, la vigencia y la contraseña no se modifican por esta ruta ni forman
parte de Fase 3A.

Los commands de creación y edición invocan Stored Procedures app24; el usuario
y evento de bitácora se escriben en una única transacción mediante
`appTransactionManager`. La contraseña en claro nunca llega a SQL: sólo BCrypt.

## Administración de usuarios — Fase 3B

Fase 3B está implementada en repositorio y sus commands fueron validados con
pruebas LIVE sintéticas reversibles. Las mutaciones usan `appTransactionManager`
con aislamiento `SERIALIZABLE`; el command SQL preserva al menos un administrador
efectivo: usuario `ACTIVO`,
vigencia `null` o `>= LocalDate.now()` de la JVM, perfil `ACTIVO` y permisos
`USUARIOS_ADMINISTRAR` y `PERFILES_ADMINISTRAR`. No se permite la
auto-inactivación ni el cambio real del perfil propio; la vigencia propia se
permite, sujeta a ese guardrail. Los JWT ya emitidos no se revocan y conservan
sus autoridades hasta expirar.

### `PATCH /administracion/usuarios/{id}/estado`

Recibe `{ "estado": "ACTIVO" | "INACTIVO" }`. Cambia únicamente el estado;
rechaza la auto-inactivación y cualquier resultado que incumpla el guardrail
administrativo global.

### `PATCH /administracion/usuarios/{id}/perfil`

Recibe `{ "perfilId": number }`. El perfil debe existir y estar `ACTIVO`.
Rechaza un cambio real del perfil del propio actor y cualquier resultado que
incumpla el guardrail administrativo global.

### `PATCH /administracion/usuarios/{id}/vigencia`

Recibe `{ "vigencia": "YYYY-MM-DD" }` o `{ "vigencia": null }`. `null`
elimina el vencimiento; una fecha es válida si es igual o posterior a
`LocalDate.now()` de la JVM. La modificación de la vigencia propia se permite,
pero debe conservar el guardrail administrativo global.

## Administración de usuarios — Fase 3C

### `POST /administracion/usuarios/{id}/password`

Recibe `{ "password": "..." }` y responde `204 No Content`. Requiere
`USUARIOS_ADMINISTRAR`; permite reset propio, de usuario inactivo o vencido. La
política compartida exige 10..50 caracteres, una mayúscula, un número y un
carácter especial. BCrypt se ejecuta en Java y sólo `password_hash` llega a
`APP24_C_USUARIO_RESTABLECER_PASSWORD`.

El update y `USUARIO_PASSWORD_RESTABLECIDA` comparten `appTransactionManager`;
si falla Bitácora, todo revierte. El detalle contiene únicamente
`usuarioObjetivoId`. No cambia estado, vigencia, perfil, clave o permisos; no
revoca JWT existentes ni agrega `must_change_password` o historial.

## Bitácora de Administración

Las acciones disponibles incluyen `LOGIN_OK`, `LOGIN_FALLIDO`,
`USUARIO_CREADO`, `USUARIO_ACTUALIZADO`, `USUARIO_ESTADO_CAMBIADO`,
`USUARIO_PERFIL_CAMBIADO`, `USUARIO_VIGENCIA_CAMBIADA` y
`USUARIO_PASSWORD_RESTABLECIDA`. El detalle de los eventos no contiene
contraseñas, hashes, JWT ni cabeceras de autorización.

## Recursos no expuestos

No se documentan endpoints de perfiles, actividades ni PerfilActividad porque
no forman parte del contrato implementado de administración de usuarios.

Consumidor frontend de `GET /bitacora`: pantalla read-only en
`frontend/src/app/features/administration/audit-log` (ruta `/bitacora`, permiso
`BITACORA_CONSULTAR`). No hay consumidor frontend de escritura administrativa.
