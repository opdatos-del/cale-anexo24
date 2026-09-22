# Bitácora / Audit log

## 1. Objetivo y decisión arquitectónica

Esta auditoría define fuente canónica, modelo, seguridad y contratos candidatos
de la bitácora de la **nueva aplicación**. La fase posterior de hardening
versionó identidad JWT confiable y permisos SQL mínimos; no implementa writer,
endpoint, frontend, triggers ni eventos de prueba.

> **DECISIÓN V1 — opción B.** `ANEXO24_DEV.app24.BitacoraEvento` es la fuente
> canónica candidata para eventos funcionales y de seguridad de la aplicación
> nueva. El modelo permite cerrar una consulta HTTP read-only candidata, pero
> **no debe implementarse hasta ajustar la inmutabilidad por privilegios y
> diseñar el writer**. No existe fuente legacy canónica demostrada.

La bitácora legacy, si se demuestra, será histórica y separada. No se unifica
con `app24.BitacoraEvento` por nombre o apariencia de pantalla.

## 2. Fuentes de evidencia

| Fuente | Aporta | Estado |
|---|---|---|
| `infra/sql/02-app-schema.sql` | DDL, FK, default e índices de `app24.BitacoraEvento` | **CONFIRMADO** para esquema versionado |
| `infra/sql/00-bootstrap.sql` y `04-app-runtime-permissions.sql` | Bootstrap sin permisos globales y política mínima por objeto para `anexo24_app` | **IMPLEMENTADO EN REPOSITORIO**; **PENDIENTE DE DESPLIEGUE** en servidor |
| `infra/sql/03-app-seed-security.sql` | Permiso y asignación inicial de perfiles | **CONFIRMADO** |
| `CorrelationIdFilter`, filtros JWT, login, excepciones y adapter de usuarios | Propagación actual de identidad y correlación | **CONFIRMADO** por código |
| Auditoría Web Forms | Existió reporte Bitácora con fechas, pero falló al generar | **CONFIRMADO** para pantalla; no para fuente física |
| CALE_IMMEX actual | Objetos `BITACORA`, `HISTORIA`, `LOG`, `AUDIT`, grants, metadata y datos | **PENDIENTE DE REVALIDACIÓN**; TLS bloqueado sin bypass |

No se ejecutó SQL ni se debilitó TLS. No se ejecutó una pantalla legacy de
Bitácora durante esta fase.

## 3. Fuente legacy

**CONFIRMADO:** Web Forms expuso un reporte llamado Bitácora entre cinco
reportes; requería fechas y devolvió error genérico durante auditoría funcional.

**PENDIENTE:** tabla, view, SP, columnas, granularidad, usuario, acción,
resultado, retención y comportamiento read-only del reporte legacy. Búsqueda
documental sólo localizó objetos `HISTORIA*` de descargos y procesos mutables;
no demuestra una bitácora de usuario canónica.

**DECISIÓN V1:** no leer `CALE_IMMEX` para `GET /api/v1/bitacora`. Si después
se requiere consultar eventos históricos legacy, será un caso de uso separado,
con fuente, seguridad y contrato propios.

## 4. Fuente `app24` y modelo actual

Fuente candidata: `ANEXO24_DEV.app24.BitacoraEvento`.

| Campo/objeto | DDL actual | Conclusión |
|---|---|---|
| `id` | `BIGINT IDENTITY(1,1)`, PK | Identidad técnica y desempate estable candidato |
| `usuario_id` | `BIGINT NULL`, FK a `app24.UsuarioApp.id` | Actor autenticado o técnico/anónimo según política |
| `fecha` | `DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME()` | Fecha UTC del evento |
| `modulo` | `VARCHAR(80) NOT NULL` | Clasificación controlada por aplicación; no ruta HTTP |
| `accion` | `VARCHAR(40) NOT NULL` | Código controlado por aplicación; no texto libre |
| `detalle` | `VARCHAR(500) NULL` | Resumen seguro y limitado; no log técnico |
| `correlacion_id` | `VARCHAR(40) NULL` | Referencia de solicitud normalizada |
| `resultado` | `VARCHAR(20) NOT NULL` | Código controlado de resultado |
| `FK_BitacoraEvento_Usuario` | Sin `ON DELETE CASCADE` declarado | Protege relación por comportamiento `NO ACTION` predeterminado |
| Índices | `fecha DESC`; `correlacion_id` | No hay índices actuales por usuario/módulo/resultado ni desempate `(fecha, id)` |

No hay `CHECK` de catálogos, trigger, SP de escritura, `GRANT` específico de
sólo inserción ni mecanismo DDL que impida `UPDATE`/`DELETE` sobre esta tabla.

## 5. Inmutabilidad real

La palabra “inmutable” en comentario y documentos es una intención, no una
garantía actual:

- **CONFIRMADO:** no existe `BitacoraRepository`, `BitacoraService`, writer,
  controlador ni endpoint de Bitácora en backend.
- **CONFIRMADO:** no hay `UPDATE`/`DELETE`/trigger de Bitácora versionado.
- **IMPLEMENTADO EN REPOSITORIO; PENDIENTE DE DESPLIEGUE:**
  `00-bootstrap.sql` ya no asigna `db_datareader`, `db_datawriter` ni
  `EXECUTE` global. `04-app-runtime-permissions.sql` retira memberships
  heredados y concede a `app24_runtime` sólo `SELECT` para autenticación y
  `SELECT` + `INSERT` sobre Bitácora; no concede `UPDATE` ni `DELETE`.
- **CONFIRMADO:** `UsuarioJdbcAdapter` ya usa `appJdbcTemplate` contra
  `ANEXO24_DEV`; un futuro adaptador de Bitácora pertenece a este esquema,
  nunca a `CALE_IMMEX` ni a un `APP24_Q_*` legacy.

Estrategia mínima futura, antes de implementar:

1. arquitectura: puerto/repository interno con sólo `registrar` y `listar`;
   sin método de actualización/borrado;
2. API externa: `GET` únicamente; nunca `POST`, `PUT`, `PATCH` ni `DELETE`;
3. privilegios: retirar `db_datawriter` de la cuenta que no deba mutar todo el
   esquema y usar roles/cuentas de mínimo privilegio, con `INSERT`/`SELECT`
   estrictamente necesarios sobre Bitácora y sin `UPDATE`/`DELETE`;
4. evaluar después si una restricción DB adicional es necesaria.

No se aprueba blockchain, hash chain ni sobreingeniería equivalente. El cambio
de privilegios/DDL queda fuera de esta fase.

## 6. Usuario e identidad histórica

`usuario_id = NULL` es correcto sólo para eventos sin actor autenticado:

| Caso | `usuario_id` futuro |
|---|---|
| Login exitoso | ID real de `UsuarioApp` |
| Login fallido de usuario existente, tras decisión de riesgo | Puede ser ID real; nunca se expone al cliente |
| Login fallido de clave inexistente | `NULL` |
| Solicitud anónima, proceso técnico o evento de infraestructura aprobado | `NULL` o actor técnico identificado por política futura |
| Evento autenticado | ID obtenido de contexto confiable, nunca de body/query/frontend |

**IMPLEMENTADO EN REPOSITORIO; PENDIENTE DE DESPLIEGUE:** JWT incluye claim
`uid` y subject `clave`. `JwtAuthFilter` valida ambos tras verificar el token;
requiere `uid` numérico, entero y positivo, además de subject no vacío. Luego
construye `AuthenticatedUserPrincipal(userId, username)`, que implementa
`Principal`: `authentication.getPrincipal()` conserva identidad confiable y
`authentication.getName()` conserva username. Un token sin `uid`, con `uid`
inválido/no positivo o subject inválido queda anónimo. Writer futuro no debe
reparsear JWT ni aceptar `userId` cliente.

**PENDIENTE:** `usuario_id` más JOIN a `UsuarioApp` muestra clave/nombre
actuales, no snapshot histórico si esos atributos cambian. V1 puede presentar
`usuarioId` y, si requiere etiqueta, usuario actual con esa limitación visible;
un snapshot seguro de clave/nombre necesita decisión de cumplimiento antes de
cambiar esquema.

La FK sin cascade favorece conservar eventos e impedirá el borrado físico del
usuario con eventos asociados. `UsuarioApp.estado` ya existe: **DECISIÓN V1
candidata**, usar inactivación en Administración y no borrado físico. Falta
regla funcional de retención/anonimización de identidad.

## 7. Fecha y correlationId

`fecha` se persiste en UTC por `SYSUTCDATETIME()` y `DATETIME2(3)`.
**DECISIÓN V1:** representa fecha del evento; frontend convierte para zona local.
No se persistirá hora local ni segunda fecha sin evidencia.

“Fecha de ingreso” y “fecha de acción” de documentación previa no están
probadas como dos conceptos de la nueva aplicación. Un login es simplemente
un evento con su propia `fecha`.

`CorrelationIdFilter` acepta `X-Correlation-Id` sólo si cumple
`[A-Za-z0-9._-]{1,40}`; de otro modo genera UUID. Coloca valor normalizado en
atributo request `correlationId` y respuesta. UUID textual mide 36 caracteres;
por tanto cabe en `correlacion_id VARCHAR(40)`.

**CONTRATO INTERNO:** writer recibe el valor ya normalizado desde atributo de
request/contexto. No genera un segundo identificador. `GlobalExceptionHandler`
y `ApiAuthenticationEntryPoint` ya reutilizan atributo o aplican mismo límite
antes de fallback. Debe conservarse cadena:

```text
HTTP request → correlationId → respuesta/error y log técnico → BitacoraEvento
```

## 8. Módulo, acción y resultado

`modulo`, `accion` y `resultado` no deben recibir textos libres ni rutas HTTP.
No hay `CHECK` en DDL: controles deben vivir en código hasta que se apruebe un
catálogo persistente.

| Campo | Catálogo candidato | Estado |
|---|---|---|
| Módulo | `SEGURIDAD`, `CATALOGOS`, `OPERACIONES`, `ADMINISTRACION`, `FACTURACION`, `REPORTES`, `SISTEMA` | **DECISIÓN V1 candidata**; deriva de paquetes y dominios existentes/futuros |
| Acción | Código estable por caso crítico, por ejemplo `LOGIN_OK`, `LOGIN_FALLIDO`, `USUARIO_CREADO`, `USUARIO_ESTADO_CAMBIADO`, `PERMISOS_CAMBIADOS`, `CARGA_CONFIRMADA`, `PROCESO_EJECUTADO` | **PENDIENTE:** aprobar sólo al habilitar caso de uso; no registrar texto narrativo |
| Resultado | `EXITO`, `FALLO`; `DENEGADO` sólo para autorización autenticada si se aprueba | **DECISIÓN V1 candidata**; no usar variantes `OK`, `SUCCESS` o similares |

## 9. Detalle, privacidad y logs

`detalle` es resumen corto controlado, máximo 500 caracteres. No es JSON libre,
SQL, stack trace ni copia de solicitud. Si necesita estructura, usar formato
controlado/versionado en futura decisión, no datos arbitrarios.

| Permitido candidato | Enmascarar/minimizar | Prohibido |
|---|---|---|
| `cargaId`, ID actor/afectado, perfil, permiso, operación, conteos, módulo, resultado, correlación | RFC si negocio lo exige, identificadores comerciales, nombre de archivo, valores de error | password, hash de contraseña, JWT/refresh token, `Authorization`, credenciales DB, contenido XLS/XLSX, archivo completo, SQL, stack trace, payload completo, información comercial extensa |

**Logs técnicos** conservan stack trace, latencia e infraestructura bajo controles
operativos. **Bitácora** conserva quién, qué acción crítica, cuándo, resultado,
correlación y resumen seguro. Nunca se copian excepciones completas a
`BitacoraEvento`.

## 10. Política de eventos

Bitácora es audit log de seguridad/negocio crítico; no request log HTTP. Un GET
normal de catálogo u operación no se registra por defecto.

| Evento | Crítico | Registrar | Usuario | Resultado | Detalle seguro |
|---|---:|---|---|---|---|
| Login exitoso | Sí | Sí, cuando exista writer | ID autenticado | `EXITO` | `LOGIN_OK` sin token |
| Login fallido controlado | Sí | Sí, con protección antiabuso | ID sólo si se conoce con seguridad; si no `NULL` | `FALLO` | Sin password ni clave intentada en claro |
| 401 por token ausente/inválido o tráfico anónimo | No por defecto | No por evento | `NULL` | — | Log técnico/métricas con rate limit |
| 403 de usuario autenticado | Candidato | Sólo para operación sensible y con límite | ID autenticado | `DENEGADO` si se aprueba | Permiso/operación, no JWT |
| Consulta normal | No | No | — | — | — |
| Consulta de información sensible | PENDIENTE clasificación | PENDIENTE | ID autenticado | `EXITO`/`FALLO` | Criterio y objeto mínimo |
| Crear/desactivar usuario, vigencia o perfil | Sí | Sí al implementar | Actor JWT | `EXITO`/`FALLO` | ID afectado y cambio seguro |
| Asignar/revocar permisos | Sí | Sí al implementar | Actor JWT | `EXITO`/`FALLO` | Perfil/actividad afectada; nunca hash |
| Validar/confirmar facturación | Sí | Sí si Facturación se aprueba | Actor JWT | `EXITO`/`FALLO` | `cargaId`, conteos resumidos |
| Proceso fiscal mutable futuro | Sí | Sí antes/durante/final según diseño | Actor JWT | `EXITO`/`FALLO` | operación y parámetros no sensibles |
| Exportación futura | PENDIENTE | PENDIENTE clasificación | Actor JWT | `EXITO`/`FALLO` | tipo/rango/conteo, sin dataset |
| Error interno | No por defecto | No como copia de stack trace | Contextual | — | Log técnico con correlación |

Login no revela al cliente si la clave existe. La decisión de enlazar fallos a
un usuario existente requiere análisis de abuso y privacidad al implementar.

## 11. Fallo de writer y transaccionalidad

No aplica misma política a todo evento:

- **Comandos críticos mutables futuros:** evento final y operación deben quedar
  en misma transacción o usar patrón confiable equivalente. Si writer no puede
  registrar resultado crítico, la operación no debe declararse exitosa sin una
  decisión explícita de compensación/outbox.
- **Login:** puede registrar de forma independiente; si falla escritura, no
  debe emitir token sin al menos registrar alerta técnica y definir política de
  riesgo. Comportamiento exacto pendiente de diseño de autenticación.
- **Consultas no auditadas:** un fallo de writer no aplica.

No debe fallar silenciosamente. Persistir log técnico con `correlationId` es
mínimo cuando el writer tenga error, sin recursión infinita de auditoría.

## 12. Retención, volumen e índices

No existe política aprobada de retención, archivado, purga o anonimización.
No se infiere retención infinita ni se crea DELETE administrativo.

No registrar GETs comunes evita convertir la tabla en access log y reduce
volumen/índices. Medir volumen real y definir retención con seguridad,
cumplimiento e infraestructura antes de producción.

Índices actuales sirven para fecha y correlación individual. Si se aprueban
filtros V1/volumen real, evaluar posteriormente `(fecha DESC, id DESC)` para
orden estable y selectividad por usuario/módulo/resultado. No se propone DDL
concreto en esta fase.

## 13. Permiso y perfiles

**CONFIRMADO:** seed define `BITACORA_CONSULTAR`.

| Perfil seed | Permiso efectivo |
|---|---|
| `ADMINISTRADOR` | Sí: recibe todas las actividades existentes |
| `CONSULTA` | No: sólo materiales, productos, estructuras y operaciones |

La matriz propuesta menciona visibilidad potencial para operador/auditoría,
pero no es asignación de seed. No se modifica seguridad.

## 14. Contratos V1 candidatos

### 14.1 Writer interno — no implementado

```text
registrarEvento(
  usuarioIdConfiable | null,
  moduloControlado,
  accionControlada,
  resultadoControlado,
  detalleSeguro | null,
  correlationIdNormalizado
)
```

Sólo servicios internos lo invocan. No existe endpoint público que acepte un
evento arbitrario. Debe usar `appJdbcTemplate` y parámetros SQL; no toca
`CALE_IMMEX`.

### 14.2 Consulta HTTP — contrato cerrado como candidato, no implementado

```http
GET /api/v1/bitacora
Authorization: Bearer <token>
```

- **Permiso:** `BITACORA_CONSULTAR`.
- **Fuente:** `ANEXO24_DEV.app24.BitacoraEvento` con `LEFT JOIN` opcional a
  `UsuarioApp` sólo para etiqueta actual.
- **Rango:** `desde` y `hasta` juntos; ambos obligatorios para evitar lectura
  total. `desde <= hasta`. Límite máximo de periodo: **PENDIENTE**.
- **Filtros candidatos demostrados por prototipo:** `usuarioId`, `modulo`,
  `resultado`, `correlationId`; no se agregan otros sin caso de uso.
- **Paginación:** `pagina=1`, `tamano=20`, `tamano=1..100`.
- **Orden candidato:** `fecha DESC, id DESC`.
- **Response mínimo candidato:** `id`, `fecha`, `usuarioId`, etiqueta de
  usuario actual opcional, `modulo`, `accion`, `detalle`, `resultado`,
  `correlationId`.

La etiqueta de usuario no es snapshot histórico. GET nunca escribe ni
"regenera" eventos.

## 15. Riesgos y pendientes

1. Desplegar y verificar con acceso SQL confiable el hardening versionado:
   retiro de privilegios globales y aplicación de `app24_runtime`.
2. Diseñar writer interno que consuma `AuthenticatedUserPrincipal` desde
   `SecurityContext`, sin aceptar identidad del cliente.
3. Cerrar retención, archivado, anonimización, clasificación sensible,
   capacidad y límites de rango.
4. Confirmar fuente legacy con TLS confiable y distinguir su historial del app
   audit log.
5. Decidir snapshot histórico de identidad y política de inactivación/borrado
   de usuarios.
6. Aprobar catálogo de módulos/acciones/resultados y política de 403/exports.
7. Revisar consistencia transaccional con cada command mutable antes de crear
   dichos commands.
8. Evaluar índices con volumen y patrón real, no por suposición.

## 16. Restricciones cumplidas

- Hardening backend y SQL versionado: `AuthenticatedUserPrincipal`, validación
  fail-closed de `uid` y `04-app-runtime-permissions.sql`.
- Cero writer, endpoint, frontend, triggers o eventos implementados.
- Cero eventos insertados y cero SQL remoto ejecutado; despliegue de permisos
  sigue pendiente de acceso TLS confiable.
- Cero bypass TLS, procesos legacy, PR, merge o code review automático.
