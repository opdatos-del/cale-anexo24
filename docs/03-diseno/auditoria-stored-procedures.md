# Auditoría global de Stored Procedures y SQL inline — SP-1C

## Alcance y método

Auditoría y migración ejecutadas desde `feature/backend-sp-app24-commands`, con metadata LIVE de SQL Server. Se usó la configuración local existente de `backend/.env`, sin imprimir credenciales ni modificarla. Las consultas metadata usaron `sys.procedures`, `sys.schemas`, `sys.parameters`, `sys.types`, `sys.sql_modules`, `sys.sql_expression_dependencies`, ownership y roles.

SP-1C autorizó únicamente los seis commands propios nuevos en `ANEXO24_DEV`. No se ejecutó ningún SP legacy ni de `CALE_IMMEX`. Las pruebas mutables usaron valores sintéticos y rollback; filas persistidas: 0. Los seis objetos nuevos sí fueron creados/alterados LIVE. No se aplicó `04-app-runtime-permissions.sql`.

El inventario histórico `docs/03-diseno/procedimientos-almacenados.md` fue generado el 15/09/2026 07:13 y declara **88 SP de CALE_IMMEX**. Su tabla contiene 89 nombres, debido a la entrada diferenciada `PROC_HISTORIADESCARGASALIDA;1`; por ello se conserva 88 como cifra declarada, pero las comparaciones por nombre usan el conjunto histórico disponible.

## Resultado de conexión LIVE

| Base | Conexión | Schemas con SP | Total LIVE | Evidencia |
|---|---:|---|---:|---|
| `CALE_IMMEX` | OK | `dbo` | **95** | `sys.procedures` |
| `ANEXO24_DEV` | OK | `app24` | **11 después de SP-1C** (5 después de SP-1B) | `sys.procedures` |

La conexión usó la configuración local ya existente, que contiene `trustServerCertificate=true`. No se agregó ningún bypass ni se modificó configuración versionada. La decisión de esta fase es de auditoría; la configuración TLS deberá revisarse antes de automatizar el acceso en otros entornos.

### Diferencia CALE_IMMEX histórico vs LIVE

| Comparación | Resultado |
|---|---:|
| Total histórico declarado | 88 |
| Nombres en tabla histórica versionada | 89 |
| Total LIVE | 95 |
| Nombres LIVE ausentes del conjunto histórico | 6 |
| Nombres históricos ausentes en LIVE | 0 |
| Diferencia numérica contra cifra declarada 88 | +7 |

Los seis nombres LIVE que no aparecen en el inventario histórico son:

- `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR`
- `dbo.APP24_Q_ENTRADAS_LISTAR`
- `dbo.APP24_Q_ESTRUCTURAS_LISTAR`
- `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR`
- `dbo.APP24_Q_PRODUCTOS_LISTAR`
- `dbo.APP24_Q_SALIDAS_LISTAR`

Cuatro procedimientos tienen `modify_date` posterior a `create_date`: `APP24_Q_PRODUCTOS_LISTAR`, `APP24_Q_ESTRUCTURAS_LISTAR`, `APP24_Q_ENTRADAS_LISTAR` y `APP24_Q_SALIDAS_LISTAR`. No es posible comparar definición histórica completa porque el documento versionado no conserva `modify_date` ni snapshot íntegro comparable para esos objetos. No se marca una modificación funcional concreta sin esa evidencia.

## Inventario de SQL inline funcional

La auditoría del backend identificó SQL funcional inline en los siguientes adapters. Cada operación queda clasificada como `MIGRAR_A_SP`; las decisiones de esta matriz se basan en metadata LIVE. SP-1B migró lecturas; SP-1C migró commands y writer de Bitácora.

| Dominio | Adapter | Datasource | Operación inline | Objetos | Clasificación | Decisión LIVE |
|---|---|---|---|---|---|---|
| Materiales | `MaterialJdbcAdapter` | CALE_IMMEX | llamada SP para count + listado paginado | `dbo.material` vía `dbo.APP24_Q_MATERIALES_LISTAR` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Auth | `UsuarioJdbcAdapter` | ANEXO24_DEV | usuario por clave; acceso/permisos vía SP | `UsuarioApp`, `PerfilApp`, `PerfilActividad`, `Actividad` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Usuarios read | `UsuarioConsultaJdbcAdapter` | ANEXO24_DEV | lista, count, detalle vía SP | `UsuarioApp`, `PerfilApp` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Usuarios commands | `UsuarioComandoJdbcAdapter` | ANEXO24_DEV | commands atómicos vía SP | `UsuarioApp`, `PerfilApp` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Fase 3B | `UsuarioComandoJdbcAdapter` | ANEXO24_DEV | estado, perfil, vigencia y guardrail vía SP | `UsuarioApp`, `PerfilApp`, `PerfilActividad`, `Actividad` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Perfil referencia | eliminado | ANEXO24_DEV | prevalidación absorbida por commands | `PerfilApp` | `NO_FUNCIONAL` | `EXCEPCION_TECNICA` |
| Bitácora write | `BitacoraJdbcAdapter` | ANEXO24_DEV | registrar evento vía SP | `BitacoraEvento` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Bitácora read | `BitacoraConsultaJdbcAdapter` | ANEXO24_DEV | lista, filtros, count vía SP | `BitacoraEvento`, `UsuarioApp` | `YA_USA_SP` | `REUTILIZAR / IMPLEMENTADO` |
| Salud | `SystemStatusController` | ambos | `SELECT 1` | — | `EXCEPCION_TECNICA` | `EXCEPCION_TECNICA` |

`SELECT 1` del health check es excepción técnica explícita, no deuda funcional.

## Matriz principal de necesidades

`CONFIRMADO` significa que la conclusión se basa en definición y metadata LIVE. Cuando no hay SP útil, `CREAR` queda como propuesta; todavía no se crea ningún objeto.

| Dominio / necesidad | Base | SP encontrado LIVE | Parámetros / result set | Mutabilidad | Propiedad | Cumple contrato | Decisión | Acción siguiente |
|---|---|---|---|---|---|---|---|---|
| Materiales: listar, filtrar, total, paginar, ordenar `clave, materialkey` | CALE_IMMEX | `dbo.APP24_Q_MATERIALES_LISTAR` | `@Filtro varchar(250)`, `@Pagina int`, `@Tamano int`, `@Total bigint OUTPUT`; 10 columnas | `READ_ONLY` | `APP24_PROPIO` | Sí | `REUTILIZAR / IMPLEMENTADO` | SP creado, versionado y desplegado LIVE en SP-1A; adapter migrado |
| `CARGA_MATERIALES` | CALE_IMMEX | `dbo.CARGA_MATERIALES` | Sin parámetros; sin result set contractual | `MIXED` / proceso | `LEGACY_COMPARTIDO` | No | `PENDIENTE` | No reutilizar para REST; no modificar legacy |
| Auth: usuario por clave | ANEXO24_DEV | `app24.APP24_Q_USUARIO_POR_CLAVE` | `@Clave varchar(30)`; 8 columnas, incluye `password_hash` sólo infraestructura | `READ_ONLY` | `APP24_PROPIO` | Sí | `REUTILIZAR / IMPLEMENTADO` | Adapter migrado; hash nunca se expone en API/logs |
| Auth: perfil, estado y permisos | ANEXO24_DEV | `app24.APP24_Q_USUARIO_ACCESO` | `@UsuarioId bigint`; `perfil_id`, `perfil_estado`, `permiso` | `READ_ONLY` | `APP24_PROPIO` | Sí | `REUTILIZAR / IMPLEMENTADO` | Adapter migrado; perfil inactivo no se filtra; cero permisos conserva fila |
| Usuarios read: lista + total | ANEXO24_DEV | `app24.APP24_Q_USUARIOS_LISTAR` | 8 parámetros, `@Total bigint OUTPUT`; 8 columnas | `READ_ONLY` | `APP24_PROPIO` | Sí | `REUTILIZAR / IMPLEMENTADO` | Adapter migrado; filtros literales y paginación en SP |
| Usuarios read: detalle por ID | ANEXO24_DEV | `app24.APP24_Q_USUARIO_OBTENER` | `@UsuarioId bigint`; 8 columnas | `READ_ONLY` | `APP24_PROPIO` | Sí | `REUTILIZAR / IMPLEMENTADO` | Adapter migrado; no devuelve `password_hash` |
| Fase 3A: duplicados + creación | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_C_USUARIO_CREAR` atómico, con validaciones y resultado controlado |
| Fase 3A: nombre/correo | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_C_USUARIO_ACTUALIZAR_DATOS` |
| Fase 3B: cambiar estado | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Command atómico con guardrail interno |
| Fase 3B: cambiar perfil | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Command atómico con validación de perfil y guardrail interno |
| Fase 3B: cambiar vigencia | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Command atómico con guardrail interno |
| Fase 3B: administrador efectivo | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Incluir consulta en cada command atómico o rutina interna común |
| Perfil referencia: estado por ID | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `PENDIENTE` | `DEFERIDO_A_SP_1C`: sólo prevalidación de commands; validación futura dentro de commands atómicos |
| Bitácora: insertar evento | ANEXO24_DEV | Ninguno; no hay SP en ningún schema | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_C_BITACORA_REGISTRAR` |
| Bitácora: lista, filtros y total | ANEXO24_DEV | `app24.APP24_Q_BITACORA_LISTAR` | 9 parámetros, `@Total bigint OUTPUT`; 9 columnas | `READ_ONLY` | `APP24_PROPIO` | Sí | `REUTILIZAR / IMPLEMENTADO` | Adapter read migrado; rango inclusivo y orden fecha/id descendente |

## Procedimientos relacionados con materiales

La búsqueda por definición, no sólo por nombre, encontró estas referencias a `material` o al catálogo/materiales. No todos son candidatos de catálogo:

| Procedimientos LIVE | Clasificación observada | Evaluación |
|---|---|---|
| `APP24_Q_ESTRUCTURAS_LISTAR` | `QUERY`, `READ_ONLY` | Consulta estructuras; referencia `productos`, `estructuras`, `productomaterial` y `material`. No devuelve contrato de catálogo material. |
| `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | `QUERY`, `READ_ONLY` | Consulta descargas/material utilizado; no lista `dbo.material` ni cubre catálogo. |
| `CARGA_MATERIALES` | `PROCESS`, `MIXED` | Borra/inserta/actualiza tablas de carga, material y factores. No ejecutar ni reutilizar para GET. |
| `CARGA_ESTRUCTURADESENSAMBLE`, `CARGAACTAS`, `CARGAPEDIMENTOS`, `CREAESTRUCTURAS` | `PROCESS`, `MIXED` | Procesos mutables legacy; no cumplen listado de catálogo. |
| `DESCARGASALIDAPEPS`, `DESCARGASCTMF`, `DESCARGATSALIDA1`, `DESCARGATSALIDAFECHA`, `DESCARGAXFECHA51` | `PROCESS` / `REPORT`, `MIXED` | Descargas/procesos; no cumplen catálogo y no se ejecutaron. |
| `ESTRUCTURAS1A1`, `PR_INFORME_ESTRUCTURAS`, `PR_INFORME_IMPORTACIONES`, `PR_INFORME_SALDOS`, `SALDOS`, `SALDOS_FAMILIA`, `SALDOSCTM`, `SP_DESENSAMBLE`, `SP_GENERA_TXT_COMPLETO`, `SP_MensajesInicio`, `Trazo_report`, `VALIDA_I_DETALLENP` | `PROCESS` / `REPORT`, `MIXED` | Referencias indirectas o funcionales a materiales; contratos incompatibles. |

**Decisión Materiales SP-1A: `REUTILIZAR / IMPLEMENTADO dbo.APP24_Q_MATERIALES_LISTAR`.** `CARGA_MATERIALES` sigue siendo carga mutable y no fue modificada. El nuevo SP cubre filtro por clave/descripción/fracción, `COUNT_BIG`, paginación BIGINT, orden `clave, materialkey` y las diez columnas del contrato. Metadata y pruebas LIVE confirmaron `QUERY / READ_ONLY / CONFIRMADO / APP24_PROPIO`.

## SP-1A — Materiales implementado

SP versionado en `infra/sql/procedures/queries/APP24_Q_MATERIALES_LISTAR.sql` y desplegado en CALE_IMMEX mediante `CREATE OR ALTER`.

Metadata LIVE posterior al despliegue:

| Elemento | Resultado |
|---|---|
| Schema/nombre | `dbo.APP24_Q_MATERIALES_LISTAR` |
| `modify_date` | 2026-09-22 13:33:26 |
| Parámetros | `@Filtro varchar(250)`, `@Pagina int`, `@Tamano int`, `@Total bigint OUTPUT` |
| Result set | `materialkey`, `clave`, `descripcion`, `fraccion`, `unidad`, `unidadt`, `tipomaterial`, `tipo`, `FactorUM`, `IGIE` |
| Mutabilidad | `READ_ONLY` |
| Clasificación | `QUERY / READ_ONLY / CONFIRMADO / APP24_PROPIO` |

Tipos LIVE de `dbo.material` usados para contrato: `materialkey numeric(18,0)`, `clave varchar(50)`, `descripcion varchar(250)`, `fraccion char(10)`, `unidad char(5)`, `unidadt char(5)`, `tipomaterial char(100)`, `tipo char(5)`, `FactorUM float`, `IGIE bigint`.

Pruebas LIVE read-only, sin volcar filas:

| Caso | Resultado |
|---|---|
| Sin filtro, página 1/tamaño 20 | PASS; 2 filas, total 2, 10 columnas |
| Filtro por clave existente | PASS; 1 fila, total 1 |
| Filtro por descripción existente | PASS; 1 fila, total 1 |
| Filtro sin coincidencias | PASS; 0 filas, total 0 |
| Página posterior | PASS; 0 filas, total 2 |
| Página muy alta | PASS; 0 filas, total 2 |
| Página inválida (`0`) | PASS; error SQL controlado |
| Tamaño inválido (`101`) | PASS; error SQL controlado |
| Orden `clave ASC, materialkey ASC` | PASS |

El filtro conserva semántica actual de `LIKE '%filtro%'`; no se introdujo escape de `%` o `_`. `MaterialJdbcAdapter` ya no contiene SQL funcional inline: sólo invoca el procedimiento, registra `@Total` y mapea las diez columnas.

No se agregó grant: no existe archivo de permisos runtime para CALE_IMMEX en este repositorio y el despliegue usó la identidad local existente. La futura cuenta runtime deberá recibir únicamente `GRANT EXECUTE ON OBJECT::dbo.APP24_Q_MATERIALES_LISTAR` cuando corresponda, sin ampliar permisos sobre `dbo.material`.

## SP-1B — Auth, usuarios read y bitácora read implementados

Antes de SP-1B, `ANEXO24_DEV` tenía 0 procedimientos. Después del despliegue LIVE tiene exactamente 5 procedimientos, todos en schema `app24`, todos `QUERY / READ_ONLY / CONFIRMADO / APP24_PROPIO`:

| Procedimiento | Parámetros | Result set | Estado LIVE |
|---|---|---|---|
| `app24.APP24_Q_USUARIO_POR_CLAVE` | `@Clave varchar(30)` | 8 columnas, incluye `password_hash` | PASS |
| `app24.APP24_Q_USUARIO_ACCESO` | `@UsuarioId bigint` | 3 columnas | PASS |
| `app24.APP24_Q_USUARIOS_LISTAR` | 7 entradas + `@Total bigint OUTPUT` | 8 columnas | PASS |
| `app24.APP24_Q_USUARIO_OBTENER` | `@UsuarioId bigint` | 8 columnas | PASS |
| `app24.APP24_Q_BITACORA_LISTAR` | 8 entradas + `@Total bigint OUTPUT` | 9 columnas | PASS |

Scripts versionados en `infra/sql/procedures/queries/`. Las definiciones sólo contienen lectura, validación y paginación; no contienen DML, DDL ni `EXEC` mutable. `password_hash` no se imprime ni se devuelve mediante DTO/API.

Metadata LIVE de tablas validada antes del diseño: 26 columnas en `UsuarioApp`, `PerfilApp`, `PerfilActividad`, `Actividad` y `BitacoraEvento`; PKs y únicos confirmados para IDs, claves de actividad, nombre de perfil, clave/correo de usuario; índices de fecha y correlación de bitácora presentes. Tipos relevantes coinciden con contratos (`UsuarioApp.clave varchar(30)`, nombre 120, correo 150, hash 100, IDs bigint; `BitacoraEvento.fecha datetime2(7)`).

Pruebas LIVE controladas PASS: usuario existente/inexistente, acceso existente/inexistente, perfil sin permisos conservando fila, perfil inactivo sin filtro SQL, listado sin filtro, clave exacta, filtros acumulados, detalle existente/inexistente, bitácora con rango válido e identificador inexistente, errores de página/tamaño/rango. Base LIVE tenía 1 usuario y 0 eventos de bitácora; sólo se reportaron métricas, nunca filas sensibles. No había caso seguro con comodines en nombre para validar contra fila real; el escape literal queda implementado en definición y cubierto por contrato del SP.

Decisiones restantes:

- Fase 3A: `CREAR` commands atómicos para creación y actualización básica.
- Fase 3B: `CREAR`; no hay commands existentes ni query de guardrail.
- Perfil referencia: `PENDIENTE`, `DEFERIDO_A_SP_1C`; adapter sólo se usa como prevalidación de commands.
- Bitácora write: `CREAR` `app24.APP24_C_BITACORA_REGISTRAR` en SP-1C.

## Decisión de arquitectura Fase 3B

La recomendación es la opción **B: tres commands SP atómicos con guardrail interno**, no tres `UPDATE` separados más una consulta posterior desde Java.

Propuesta:

- `app24.APP24_C_USUARIO_CAMBIAR_ESTADO`
- `app24.APP24_C_USUARIO_CAMBIAR_PERFIL`
- `app24.APP24_C_USUARIO_CAMBIAR_VIGENCIA`

Cada command deberá encapsular validación, mutación, comprobación de administrador efectivo y `COMMIT`/`ROLLBACK` controlado dentro de la misma unidad transaccional. La comprobación debe exigir usuario activo, vigencia efectiva, perfil activo y ambos permisos `USUARIOS_ADMINISTRAR` y `PERFILES_ADMINISTRAR`. Debe conservar auto-inactivación y auto-cambio de perfil como reglas de negocio de Fase 3B.

Esto reduce la ventana entre `UPDATE` y guardrail, preserva atomicidad bajo concurrencia y evita que un futuro consumidor omita la consulta posterior. La integración Java deberá conservar contratos de aplicación y traducir códigos/errores controlados; esta fase no implementa ese cambio.

## APP24_Q_* conocidos verificados LIVE

Todos existen LIVE en `dbo`, no en `app24`. Son `APP24_PROPIO` por nomenclatura, scripts versionados y evidencia del proyecto. Las definiciones inspeccionadas sólo contienen lecturas, validaciones de parámetros, paginación y output `@Total`; se clasifican `QUERY / READ_ONLY / CONFIRMADO`.

| SP | LIVE | Creado | Modificado | Parámetros principales | Result set |
|---|---|---|---|---|---:|
| `dbo.APP24_Q_PRODUCTOS_LISTAR` | EXISTE | 2026-09-19 10:20:45 | 2026-09-19 11:17:53 | `@Filtro varchar(250)`, `@Pagina int`, `@Tamano int`, `@Total bigint OUTPUT` | 6 |
| `dbo.APP24_Q_ESTRUCTURAS_LISTAR` | EXISTE | 2026-09-19 12:22:55 | 2026-09-19 12:38:30 | `@Producto varchar(50)`, `@Material varchar(50)`, paginación, total | 15 |
| `dbo.APP24_Q_ENTRADAS_LISTAR` | EXISTE | 2026-09-19 13:33:08 | 2026-09-21 06:26:22 | rango fecha, filtros pedimento/fracción/parte, paginación, total | 10 |
| `dbo.APP24_Q_SALIDAS_LISTAR` | EXISTE | 2026-09-21 08:27:27 | 2026-09-21 08:41:03 | rango fecha, filtros pedimento/fracción/parte, paginación, total | 9 |
| `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | EXISTE | 2026-09-21 12:05:23 | 2026-09-21 12:05:23 | rango fecha, material/producto/salida, paginación, total | 17 |
| `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` | EXISTE | 2026-09-21 13:36:40 | 2026-09-21 13:36:40 | rango fecha, pedimento/parte/serie/marca/modelo, paginación, total | 13 |
| `app24.APP24_Q_PERFILES_LISTAR` | EXISTE LIVE | Fase 5A | Fase 5A | `@Nombre` parcial escapado, `@Estado`, `@Pagina`, `@Tamano`, `@Total OUTPUT` | 4 |

Los seis SP fueron inspeccionados por metadata, parámetros, definición y result set. No se ejecutaron en esta fase. Las seis decisiones son `REUTILIZAR` para sus respectivos adapters ya versionados; no aplican a Materiales catálogo ni Administración.

## Clasificación y propiedad

- Los seis `APP24_Q_*` conocidos: `QUERY`, `READ_ONLY`, `CONFIRMADO`, `APP24_PROPIO`.
- `CARGA_MATERIALES` y demás procesos legacy con `INSERT`, `UPDATE` o `DELETE`: `PROCESS`, `MIXED`, `CONFIRMADO`, `LEGACY_COMPARTIDO`.
- SP legacy sin contrato suficiente para un endpoint actual: no se reutilizan; cualquier modificación requeriría `REQUIERE_APROBACION`.
- No se detectaron candidatos LIVE para `ADAPTAR` que cubran las necesidades de Administración o catálogo de materiales.
- Auth, Usuarios read y Bitácora read: cinco SP propios creados/desplegados LIVE y consumidos por adapters; decisión `REUTILIZAR / IMPLEMENTADO`.
- Fase 5A incorpora `app24.APP24_Q_PERFILES_LISTAR`: `QUERY / READ_ONLY / APP24_PROPIO`; soporta filtros de nombre parcial con comodines escapados y estado, paginación y total de perfiles.
- Perfil referencia fue eliminado en SP-1C; validación absorbida por commands atómicos.

## Permisos futuros

La migración posterior deberá reemplazar grants directos de tablas por permisos mínimos:

```sql
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_POR_CLAVE TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_ACCESO TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIOS_LISTAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_BITACORA_LISTAR TO app24_runtime;
```

**HISTÓRICO / RESUELTO:** los cinco grants read-only y seis grants command
quedaron versionados en `infra/sql/04-app-runtime-permissions.sql`. En SP-1C
no se aplicaron LIVE porque runtime membership seguía pendiente y ownership chain
aún no estaba re-auditada. SP-1D confirmó ownership compatible y SP-1E aplicó el
script dos veces; estado final `RUNTIME_READY`, EXECUTE-only e idempotencia PASS.

## SP-1C — Commands app24 implementados

- Conexiones LIVE: exitosas para ambas bases.
- SPs WRITE/MIXED/UNKNOWN ejecutados: **0**.
- SPs READ_ONLY ejecutados: cinco SP de SP-1B y `dbo.APP24_Q_MATERIALES_LISTAR`.
- Commands nuevos ejecutados sólo en pruebas sintéticas reversibles; SPs legacy y `CALE_IMMEX` no ejecutados.
- Datos persistidos: **0**; SPs legacy modificados: **0**; comandos propios creados LIVE: **6**.
- Backend migrado en Materiales, Auth/Usuarios/Bitácora read y commands administrativos; PerfilReferencia eliminado.
- Frontend no modificado; permisos versionados, grants LIVE pendientes por ownership chain incompatible y membership no confirmado.
### Stored Procedures LIVE

ANEXO24_DEV pasó de 5 a 11 SP. Se crearon exactamente:

- `app24.APP24_C_USUARIO_CREAR`
- `app24.APP24_C_USUARIO_ACTUALIZAR_DATOS`
- `app24.APP24_C_USUARIO_CAMBIAR_ESTADO`
- `app24.APP24_C_USUARIO_CAMBIAR_PERFIL`
- `app24.APP24_C_USUARIO_CAMBIAR_VIGENCIA`
- `app24.APP24_C_BITACORA_REGISTRAR`

Todos son `COMMAND / WRITE / CONFIRMADO / APP24_PROPIO`. Los commands sensibles aceptan transacción exterior Spring o inician/cierran una propia; usan `SET XACT_ABORT ON`, `UPDLOCK` y `HOLDLOCK`. Guardrail exige usuario activo, vigencia efectiva, perfil activo y permisos `USUARIOS_ADMINISTRAR` y `PERFILES_ADMINISTRAR`.

### Errores controlados

| Código | Contrato |
|---:|---|
| 51101 | `USUARIO_NO_EXISTE` |
| 51102 | `PERFIL_NO_EXISTE` |
| 51103 | `PERFIL_INACTIVO` |
| 51104 | `RECURSO_DUPLICADO` |
| 51105 | `AUTO_INACTIVACION_NO_PERMITIDA` |
| 51106 | `AUTO_CAMBIO_PERFIL_NO_PERMITIDO` |
| 51107 | `SIN_ADMINISTRADOR_EFECTIVO` |
| 51108 | `PARAMETRO_INVALIDO` |
| 51150 | `FILAS_AFECTADAS_INCONSISTENTES` |

`UsuarioComandoJdbcAdapter` traduce estos códigos y `2601/2627`; errores desconocidos conservan `DataAccessException`. `PerfilReferenciaRepository`, adapter y test exclusivo fueron eliminados. `UsuarioComandoRepository` ya no expone `exists*` ni rowcounts.

### Validación LIVE reversible

| Caso | Resultado |
|---|---|
| Conteo antes/después | PASS; 5 → 11 |
| Crear sintético y obtener ID | PASS |
| Actualizar nombre/correo | PASS |
| Bitácora sintética y `EventoId` | PASS |
| Vigencia pasada permitida | PASS |
| Duplicado | PASS; 51104 |
| Perfil inexistente | PASS; 51102 |
| Auto-inactivación | PASS; 51105 |
| Auto-cambio de perfil | PASS; 51106 |
| Rollback y ausencia posterior | PASS; filas persistidas 0 |

Identity pudo avanzar pese al rollback; no se ejecutó `DBCC CHECKIDENT`. No había dataset seguro para forzar perfil alternativo inactivo ni último administrador; esas mutaciones no se ejecutaron LIVE.

### Permisos y ownership

**HISTÓRICO / RESUELTO:** SP-1C dejó seis `GRANT EXECUTE` versionados y
conservó temporalmente permisos directos mientras ownership y membership estaban
pendientes. SP-1D confirmó ownership compatible y SP-1E aplicó el script dos
veces: rol `app24_runtime`, membership correcta, REVOKE explícito y EXECUTE-only.

### Estado final SP-1C

- SQL funcional inline de negocio en `UsuarioComandoJdbcAdapter`: **0**.
- SQL funcional inline de negocio en `BitacoraJdbcAdapter`: **0**.
- `PerfilReferenciaJdbcAdapter` y `PerfilReferenciaRepository`: eliminados.
- `SystemStatusController` conserva únicamente `SELECT 1` como `EXCEPCION_TECNICA`.
- CALE_IMMEX, Materiales, frontend y API externa sin cambios.

## SP-1D — cierre de auditoría y release gate

Auditoría LIVE read-only ejecutada el 2026-09-23 desde `feature/backend-sp-closure`, sin `CREATE`, `ALTER`, DML ni ejecución de comandos mutables.

## SP-1E — provisionamiento runtime least-privilege

`04-app-runtime-permissions.sql` se ejecutó en `ANEXO24_DEV` el 2026-09-23. La identidad lógica fue `opdatos`/`dbo`; no se expusieron credenciales. La ejecución creó `app24_runtime`, agregó `anexo24_app`, removió `db_datareader` y `db_datawriter`, revocó EXECUTE global y aplicó los 11 grants EXECUTE por objeto. Segunda ejecución: PASS, sin diferencias.

Impersonation legítima como `anexo24_app` confirmó: EXECUTE efectivo en queries/commands, SELECT/INSERT/UPDATE/DELETE directos sobre tablas objetivo = 0, SELECT directo `TOP (0)` = DENIED y query read-only = PASS. Esto valida ownership chain efectiva. No se ejecutaron commands ni se modificaron filas de negocio.

| Control | Resultado |
|---|---|
| `CALE_IMMEX` SP total | 96 |
| APP24 query propios en `dbo` | 7 presentes |
| `ANEXO24_DEV` SP total | 11 |
| APP24 queries/commands esperados | 11 presentes |
| Drift scripts app24 vs `sys.sql_modules` | 0; MATCH tras normalizar `USE`, `GO`, whitespace, `CREATE OR ALTER` y comentarios |
| SQL funcional inline de negocio | 0 |
| Excepción técnica | `SystemStatusController` → `SELECT 1` |
| Error code JDBC LIVE | `SQLServerException.getErrorCode() = 51102` confirmado |
| Ownership efectivo | `dbo` en schema `app24`, tablas y SPs; chain compatible |
| Runtime previo | `RUNTIME_PARTIAL` |
| Runtime SP-1E | `RUNTIME_READY`: rol dedicado, membership, EXECUTE específico y sin grants directos de tablas |

`BitacoraEvento.fecha` se verificó directamente con `sys.columns`: `datetime2`, `scale=3`, no existe drift respecto a `02-app-schema.sql`. El reporte anterior que indicaba 7 correspondía a la escala del tipo base, no a la escala efectiva de la columna.

`infra/sql/04-app-runtime-permissions.sql` se aplicó dos veces en `ANEXO24_DEV` con la identidad administrativa `opdatos`; ambas ejecuciones pasaron. La primera creó `app24_runtime`, retiró memberships `db_datareader`/`db_datawriter`, revocó EXECUTE global y concedió los 11 EXECUTE específicos existentes en SP-1E. La segunda confirmó idempotencia. Impersonation de `anexo24_app` confirmó EXECUTE efectivo, SELECT directo denegado y ejecución de query read-only mediante ownership chain.

La matriz de escenarios está en `docs/04-desarrollo/matriz-regresion-sp.md`. SP-1B tenía 316 tests; SP-1C terminó con 284; SP-1D cerró con **294 tests**, 0 fallos, 0 errores y 0 omitidos, agregando cobertura explícita de códigos SQL sin perseguir un número artificial. SP-1E cerró el gate runtime; no se modificaron datos de negocio. INT-1 integró la cadena en `dev` mediante fast-forward.

## Fase 3C — restablecimiento administrativo de contraseña

`app24.APP24_C_USUARIO_RESTABLECER_PASSWORD(@UsuarioId BIGINT,
@PasswordHash VARCHAR(100))` quedó versionado y desplegado LIVE. Actualiza sólo
`app24.UsuarioApp.password_hash`; valida parámetros con `51108`, inexistencia con
`51101` y rowcount con `51150`. Java valida política compartida, genera BCrypt y
nunca envía plaintext a SQL.

ANEXO24_DEV pasó de 11 a 12 SP app24. Prueba LIVE creó usuario sintético dentro
de transacción, confirmó booleanamente cambio de hash y ejecutó rollback; filas
sintéticas finales: 0. Prueba de usuario inexistente devolvió `51101`. Identity
pudo avanzar por el rollback; no se ejecutó `DBCC CHECKIDENT`.

**HISTÓRICO — Fase 3C:** `infra/sql/04-app-runtime-permissions.sql` se reaplicó y dejó 12 grants EXECUTE específicos,
cero grants directos de tablas y sin memberships `db_datareader`/`db_datawriter`.
Impersonation de `anexo24_app` confirmó `HAS_PERMS_BY_NAME` EXECUTE = 1 para el
nuevo SP y UPDATE directo sobre `UsuarioApp` = 0. CALE_IMMEX no fue modificado.

## Fases 5A, 5B y 5C — perfiles y permisos

F5A implementó `app24.APP24_Q_PERFILES_LISTAR`, el catálogo paginado de perfiles:
recibe filtro `nombre` parcial con comodines escapados, `estado`, `pagina` y
`tamano`; devuelve `id`, `nombre`, `estado`, `cantidadPermisos` y el total por
parámetro OUTPUT. Se clasifica `QUERY / READ_ONLY`: no realiza DML ni genera
bitácora.

F5B cerró el diseño. F5C implementó y validó LIVE seis SP adicionales:

```text
APP24_Q_ACTIVIDADES_LISTAR
APP24_Q_PERFIL_PERMISOS_LISTAR
APP24_C_PERFIL_CREAR
APP24_C_PERFIL_ACTUALIZAR_NOMBRE
APP24_C_PERFIL_CAMBIAR_ESTADO
APP24_C_PERFIL_REEMPLAZAR_PERMISOS
```

Los dos queries proveen el catálogo de actividades y los permisos de un perfil.
Los cuatro commands aplican `SET XACT_ABORT ON`; las mutaciones críticas usan
bloqueos `UPDLOCK`/`HOLDLOCK` y preservan el guardrail de al menos un administrador
efectivo. Cada command y su evento seguro de Bitácora comparten la transacción
`appTransactionManager` con aislamiento `SERIALIZABLE`.

Los endpoints implementados son `POST /administracion/perfiles`, `PUT
/administracion/perfiles/{id}`, `PATCH /administracion/perfiles/{id}/estado`,
`GET /administracion/perfiles/{id}/permisos`, `PUT
/administracion/perfiles/{id}/permisos` y `GET /administracion/actividades`;
todos requieren exclusivamente `PERFILES_ADMINISTRAR`. El listado de F5A conserva
la lectura con `USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR`.

LIVE F5C: la transacción sintética se revirtió y no dejó filas persistentes. No
se ejecutó la acción destructiva que dejaría sin el último administrador efectivo;
no se declara validación LIVE de ese caso. Runtime concede 19 `EXECUTE`
específicos sobre los 19 SP `app24` y conserva cero grants directos sobre tablas.
