# Fase 5B — Auditoría y diseño de perfiles y permisos

## 1. Alcance y estado

Este documento cierra el diseño V1 previo a implementar Administración de
Perfiles y Permisos. No añade endpoints, commands, Stored Procedures ni cambios
de datos. Las rutas descritas como **propuestas** no están implementadas.

Fuentes auditadas: DDL/seed/permisos runtime versionados, paquetes backend de
Administración, Seguridad y Bitácora, y metadata/datos agregados **LIVE** de
`ANEXO24_DEV.app24`.

La auditoría LIVE usó exclusivamente `SELECT` sobre tablas `app24` y catálogos
`sys.*`. No se ejecutaron SP, DML, DDL, `GRANT`, `REVOKE` ni consultas contra
`CALE_IMMEX`.

## 2. Modelo confirmado

| Entidad | Campos / restricciones relevantes | Relación |
|---|---|---|
| `PerfilApp` | `id BIGINT IDENTITY` PK, `nombre VARCHAR(80) NOT NULL UNIQUE`, `estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'` | Un perfil tiene muchos usuarios y muchas actividades asignadas. |
| `Actividad` | `id BIGINT IDENTITY` PK, `clave VARCHAR(60) NOT NULL UNIQUE`, `nombre VARCHAR(120)`, `recurso VARCHAR(120)`, `accion VARCHAR(40)` | Catálogo técnico de autoridades API. |
| `PerfilActividad` | PK compuesta `(perfil_id, actividad_id)` | Tabla puente PerfilApp N:M Actividad. |
| `UsuarioApp` | `perfil_id BIGINT NOT NULL` | UsuarioApp N:1 PerfilApp. |

FKs LIVE confirmadas: `UsuarioApp.perfil_id → PerfilApp.id`,
`PerfilActividad.perfil_id → PerfilApp.id` y
`PerfilActividad.actividad_id → Actividad.id`. Todas usan `NO_ACTION` para
`DELETE` y `UPDATE`.

No existe usuario sin perfil. Las PK/UNIQUE LIVE confirman que no puede haber
asignación PerfilActividad duplicada, perfil con nombre duplicado ni actividad
con clave duplicada. El DDL no declara `CHECK` para estados: V1 sólo admite
`ACTIVO` e `INACTIVO` y la validación debe vivir en application/command y SP.

## 3. Dataset LIVE auditado

| Métrica | Resultado |
|---|---:|
| Perfiles | 2 |
| Actividades | 12 |
| PerfilActividad | 16 |
| Usuarios | 1 |
| Perfiles sin usuarios | 1 |
| Perfiles sin permisos | 0 |
| Actividades sin perfiles | 0 |
| Duplicados lógicos de perfil/actividad/asignación | 0 / 0 / 0 |

| Perfil | Estado | Usuarios | Permisos |
|---|---|---:|---:|
| `ADMINISTRADOR` | `ACTIVO` | 1 | 12 |
| `CONSULTA` | `ACTIVO` | 0 | 4 |

El dataset coincide con el seed versionado. `ADMINISTRADOR` tiene las 12
actividades del catálogo. `CONSULTA` tiene exactamente:
`MATERIALES_CONSULTAR`, `PRODUCTOS_CONSULTAR`,
`ESTRUCTURAS_CONSULTAR` y `OPERACIONES_CONSULTAR`.

Las 12 actividades LIVE coinciden por clave con `03-app-seed-security.sql`:

```text
MATERIALES_CONSULTAR       PRODUCTOS_CONSULTAR
ESTRUCTURAS_CONSULTAR      OPERACIONES_CONSULTAR
REPORTES_GENERAR           REPORTES_EXPORTAR
FACTURACION_CARGAR         FACTURACION_GUARDAR
BITACORA_CONSULTAR         USUARIOS_ADMINISTRAR
PERFILES_ADMINISTRAR       ACTIVIDADES_ADMINISTRAR
```

No se detectó actividad faltante, extra ni duplicada. `ACTIVIDADES_ADMINISTRAR`
permanece reservado: no habilita CRUD de Actividad en V1.

## 4. Autenticación y permisos efectivos confirmados

Flujo vigente:

```text
Login
  → UsuarioApp por clave
  → estado/vigencia de UsuarioApp
  → BCrypt
  → APP24_Q_USUARIO_ACCESO
  → PerfilApp.estado explícito
  → PerfilActividad + Actividad.clave
  → JWT { subject, uid, auth }
```

`APP24_Q_USUARIO_ACCESO` devuelve perfil y permisos sin filtrar `PerfilApp.estado`.
`LoginService` rechaza de forma genérica un perfil ausente o `INACTIVO`.
Un perfil `ACTIVO` sin actividades sí puede autenticarse y recibe un JWT con
`auth` vacío; no se debe inferir inactividad de una lista vacía.

`JwtAuthFilter` construye authorities exclusivamente desde el claim `auth`. No
reconsulta usuario, perfil ni asignaciones en cada request. Por tanto, un cambio
de perfil, estado o permisos no revoca JWT ya emitidos: V1 acepta consistencia
eventual hasta la expiración configurada del token.

## 5. Administrador efectivo

Definición vigente y obligatoria para Fase 5B:

```text
UsuarioApp.estado = ACTIVO
AND (vigencia IS NULL OR vigencia >= fecha actual de la JVM)
AND PerfilApp.estado = ACTIVO
AND perfil tiene USUARIOS_ADMINISTRAR
AND perfil tiene PERFILES_ADMINISTRAR
```

No depende de nombre de perfil ni de ID. En particular, `ADMINISTRADOR` no es
una identidad de seguridad; su capacidad se deriva de actividades asignadas.

Los commands de Usuario ya preservan esta condición mediante transacción
`SERIALIZABLE`, `UPDLOCK` y `HOLDLOCK` en SQL. Fase 5B debe conservar el mismo
nivel en toda operación que reduzca dicha capacidad.

## 6. Stored Procedures y runtime actuales

`ANEXO24_DEV` tiene 13 SP en `app24`. Los candidatos relacionados son:

| SP | Clasificación | Mutabilidad | Evidencia / reutilización |
|---|---|---|---|
| `APP24_Q_PERFILES_LISTAR` | QUERY | READ_ONLY | Confirmado. Lista perfiles con total, filtros nombre/estado, paginación y cantidad de permisos. Se reutiliza sin cambio. |
| `APP24_Q_USUARIO_ACCESO` | QUERY | READ_ONLY | Confirmado. Obtiene estado de perfil y autoridades efectivas para login. No es catálogo ni editor de permisos. |
| `APP24_C_USUARIO_CAMBIAR_PERFIL` | COMMAND | WRITE | Confirmado. Cambia PerfilApp de UsuarioApp; valida perfil activo y guardrail. No modifica PerfilApp ni PerfilActividad. |

Metadata LIVE confirma parámetros y dependencias esperadas. Los dos queries
referencian PerfilApp/PerfilActividad y, cuando corresponde, Actividad/UsuarioApp;
el command de usuario referencia las cuatro tablas y contiene `UPDLOCK`,
`HOLDLOCK` y evaluación de administrador efectivo.

No existe SP para listar Actividad, consultar permisos de un perfil, crear/editar
perfil, cambiar su estado ni reemplazar PerfilActividad. Esos casos requieren
SP propios; no se reutilizan commands de Usuario.

Runtime LIVE: `app24_runtime` existe, `anexo24_app` pertenece al rol, hay 13
`GRANT EXECUTE` específicos, y grants directos de tablas `app24` son 0. La futura
fase debe agregar únicamente EXECUTE específico a cada SP nuevo y mantener cero
`SELECT`/`INSERT`/`UPDATE`/`DELETE` directos.

La compatibilidad LIVE es 160, por lo que un parámetro JSON para el conjunto de
IDs puede parsearse con `OPENJSON`. La implementación debe mantener un único
contrato interno para ese transporte, validarlo estrictamente y no construir SQL
inline.

## 7. Decisiones V1

### 7.1 PerfilApp

| Operación | Decisión |
|---|---|
| Crear | Sí. Nace `ACTIVO`, sin permisos implícitos. |
| Editar | Sí, únicamente `nombre`; `id`, estado y permisos no se editan en esta ruta. |
| Estado | Sí. `ACTIVO` / `INACTIVO`; inactivar bloquea login futuro de sus usuarios, sin reasignarlos ni borrar permisos. |
| Permisos | Sí. Reemplazo total atómico del conjunto PerfilActividad. |
| Delete físico | No. FKs, trazabilidad y baja lógica mediante `INACTIVO`. |

Nombre: obligatorio, `trim`, no blank, máximo 80, único. El duplicado responde
409 `RECURSO_DUPLICADO`; no se exponen constraints SQL.

Un perfil nuevo inicia con cero permisos. Esto es válido: sólo adquiere
capacidades tras el reemplazo explícito de permisos. Un perfil inactivo puede
editarse y configurarse: estado controla acceso, no preparación administrativa.

### 7.2 Actividad

Actividad es catálogo técnico controlado por código/seed. V1 sólo expone lectura;
no tendrá POST, PUT, PATCH ni DELETE. `ACTIVIDADES_ADMINISTRAR` queda
**RESERVADO / SIN USO V1**. Asignar actividades a perfiles y consultar el
catálogo se autoriza con `PERFILES_ADMINISTRAR`.

### 7.3 Reemplazo de permisos

El request representa el conjunto final, no un delta:

```json
{ "actividadIds": [1, 2, 3] }
```

`actividadIds: []` es válido para un perfil ordinario y deja el perfil activo sin
permisos. Cada ID debe ser entero positivo, único y existente. IDs repetidos se
rechazan con 400 `VALIDACION_INVALIDA`; no se silencian ni se normalizan. Un ID
inexistente responde 404 `RECURSO_NO_ENCONTRADO`.

El command debe bloquear el perfil objetivo, validar el conjunto completo,
reemplazar asignaciones y evaluar guardrail antes de commit. La operación y su
evento de Bitácora comparten `appTransactionManager`; no hay estado intermedio
observable de permisos parcialmente reemplazados.

### 7.4 Guardrail de perfiles

Deben ejecutar guardrail SQL atómico:

- `PATCH /perfiles/{id}/estado` al pasar a `INACTIVO`;
- `PUT /perfiles/{id}/permisos` cuando conjunto final puede retirar cualquiera
de las dos autoridades administrativas;
- cualquier command posterior que reduzca capacidad efectiva.

Crear, renombrar y activar un perfil no reducen capacidad y no requieren ese
control global.

Recomendación V1 para inactivar el perfil propio del actor: **permitirlo sólo si
el resultado final conserva otro administrador efectivo**. No hay regla vigente
que exija prohibición absoluta de auto-inactivación de perfil; el riesgo relevante
es quedar sin administración efectiva y ya tiene guardrail objetivo. El actor
puede conservar su JWT actual hasta expiración; esta es la consistencia eventual
V1 documentada, no una revocación inmediata.

El mismo criterio se aplica cuando un actor modifica permisos de su propio
perfil: no se prohíbe por identidad, pero la operación falla con 409
`ESTADO_INCOMPATIBLE` si el resultado deja cero administradores efectivos.

## 8. Contrato HTTP propuesto para implementación posterior

Todos los endpoints siguientes requieren exclusivamente:

```java
@PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
```

La excepción ya implementada se conserva sólo para catálogo de Usuarios:
`GET /api/v1/administracion/perfiles` usa
`hasAnyAuthority('USUARIOS_ADMINISTRAR','PERFILES_ADMINISTRAR')`. Ningún command
de perfiles recibe `USUARIOS_ADMINISTRAR`.

| Método/ruta | Request / response | Resultado |
|---|---|---|
| `POST /api/v1/administracion/perfiles` | `{ "nombre": "..." }` → perfil `id,nombre,estado,cantidadPermisos` | `201`; crea ACTIVO sin permisos. |
| `PUT /api/v1/administracion/perfiles/{id}` | `{ "nombre": "..." }` → perfil | `200`; sólo nombre. |
| `PATCH /api/v1/administracion/perfiles/{id}/estado` | `{ "estado": "ACTIVO" | "INACTIVO" }` → perfil | `200`; aplica guardrail al inactivar. |
| `GET /api/v1/administracion/perfiles/{id}/permisos` | `{ "perfilId": n, "permisos": [{ "id", "clave", "nombre", "recurso", "accion" }] }` | `200`; orden `clave ASC, id ASC`. |
| `PUT /api/v1/administracion/perfiles/{id}/permisos` | `{ "actividadIds": [...] }` → misma respuesta de permisos | `200`; reemplazo total atómico. |
| `GET /api/v1/administracion/actividades` | lista `{ id, clave, nombre, recurso, accion }` | `200`; orden `clave ASC, id ASC`, sin paginación V1. |

`GET /actividades` también requiere `PERFILES_ADMINISTRAR`. No necesita paginación
mientras sea catálogo técnico pequeño y versionado (12 filas LIVE); se revisará
si deja de serlo.

Errores comunes: 400 validación/formato, 401 sin autenticación, 403 sin permiso,
404 perfil/actividad inexistente, 409 nombre duplicado o `ESTADO_INCOMPATIBLE`
por guardrail, y 503 dependencia. Los SP pueden reutilizar códigos ya tratados:
51102 (recurso ausente), 51104 (duplicado), 51107 (sin administrador efectivo),
51108 (parámetro inválido) y 51150 (inconsistencia interna). Nunca filtrar SQL,
constraints ni detalle técnico.

## 9. Diseño de persistencia para fase de implementación

SP nuevos propuestos, todos en `app24` y versionados antes de desplegar:

```text
APP24_Q_ACTIVIDADES_LISTAR
APP24_Q_PERFIL_PERMISOS_LISTAR
APP24_C_PERFIL_CREAR
APP24_C_PERFIL_ACTUALIZAR_NOMBRE
APP24_C_PERFIL_CAMBIAR_ESTADO
APP24_C_PERFIL_REEMPLAZAR_PERMISOS
```

Los adapters sólo invocarán `{call app24...}` con `appJdbcTemplate`. Los commands
usarían `@Transactional(transactionManager = "appTransactionManager",
isolation = Isolation.SERIALIZABLE)` y los SP usarán `SET XACT_ABORT ON`,
`UPDLOCK`/`HOLDLOCK` en perfiles/asignaciones/actividades/usuarios necesarios para
el guardrail. No habrá SQL funcional inline.

Después de despliegue y verificación LIVE, `04-app-runtime-permissions.sql` debe
sumar exactamente seis `GRANT EXECUTE ON OBJECT::app24... TO app24_runtime` y
conservar los 13 existentes, sin permisos directos de tabla.

## 10. Bitácora y frontend posteriores

Al implementar commands, agregar al enum de Bitácora y registrar, en la misma
transacción, sólo eventos exitosos seguros:

```text
PERFIL_CREADO
PERFIL_ACTUALIZADO
PERFIL_ESTADO_CAMBIADO
PERFIL_PERMISOS_CAMBIADOS
```

Detalle permitido: IDs, estado anterior/nuevo, conteo o IDs de actividades si
caben en el límite. Prohibido: JWT, headers Authorization, credenciales, SQL,
stack traces y request completo.

El frontend posterior tendrá ruta/pantalla de Perfiles sólo cuando se implemente
este contrato. Usará IDs reales de actividades y perfiles, no nombres ni IDs
hardcodeados. La infraestructura read-only actual de perfiles permanece como
base reutilizable.

## 11. Gates para fase de implementación

1. discovery LIVE de SP antes de crear cada objeto;
2. scripts versionados y metadata LIVE semánticamente coincidentes;
3. tests unitarios de validación, RBAC, errores, matriz y guardrails;
4. pruebas de concurrencia/rollback del reemplazo e inactivación;
5. grants runtime específicos, sin acceso directo a tablas;
6. bitácora en transacción con command;
7. validación read-only de metadata y permisos tras despliegue;
8. frontend sólo después de contratos backend integrados.
