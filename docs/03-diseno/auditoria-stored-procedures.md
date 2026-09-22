# Auditoría global de Stored Procedures y SQL inline — SP-0D

## Alcance y método

Auditoría ejecutada desde `feature/backend-sp-discovery`, con metadata LIVE de SQL Server. Se usó la configuración local existente de `backend/.env`, sin imprimir credenciales ni modificarla. Las consultas realizadas fueron exclusivamente sobre `sys.procedures`, `sys.schemas`, `sys.parameters`, `sys.types`, `sys.sql_modules`, `sys.sql_expression_dependencies` y `sys.dm_exec_describe_first_result_set_for_object`.

No se ejecutaron Stored Procedures de negocio. No se ejecutó ningún SP `WRITE`, `MIXED` o `UNKNOWN`. No se modificaron datos, objetos ni permisos.

El inventario histórico `docs/03-diseno/procedimientos-almacenados.md` fue generado el 15/09/2026 07:13 y declara **88 SP de CALE_IMMEX**. Su tabla contiene 89 nombres, debido a la entrada diferenciada `PROC_HISTORIADESCARGASALIDA;1`; por ello se conserva 88 como cifra declarada, pero las comparaciones por nombre usan el conjunto histórico disponible.

## Resultado de conexión LIVE

| Base | Conexión | Schemas con SP | Total LIVE | Evidencia |
|---|---:|---|---:|---|
| `CALE_IMMEX` | OK | `dbo` | **95** | `sys.procedures` |
| `ANEXO24_DEV` | OK | ninguno | **0** | `sys.procedures` |

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

La auditoría del backend identificó SQL funcional inline en los siguientes adapters. Cada operación queda clasificada como `MIGRAR_A_SP`; las decisiones de esta matriz se basan en metadata LIVE.

| Dominio | Adapter | Datasource | Operación inline | Objetos | Clasificación | Decisión LIVE |
|---|---|---|---|---|---|---|
| Materiales | `MaterialJdbcAdapter` | CALE_IMMEX | `COUNT` + listado paginado | `dbo.material` | `MIGRAR_A_SP` | `CREAR` |
| Auth | `UsuarioJdbcAdapter` | ANEXO24_DEV | usuario por clave; acceso/permisos | `UsuarioApp`, `PerfilApp`, `PerfilActividad`, `Actividad` | `MIGRAR_A_SP` | `CREAR` |
| Usuarios read | `UsuarioConsultaJdbcAdapter` | ANEXO24_DEV | lista, count, detalle | `UsuarioApp`, `PerfilApp` | `MIGRAR_A_SP` | `CREAR` |
| Usuarios commands | `UsuarioComandoJdbcAdapter` | ANEXO24_DEV | duplicados, insert, datos básicos | `UsuarioApp`, `PerfilApp` | `MIGRAR_A_SP` | `CREAR` |
| Fase 3B | `UsuarioComandoJdbcAdapter` | ANEXO24_DEV | estado, perfil, vigencia, guardrail | `UsuarioApp`, `PerfilApp`, `PerfilActividad`, `Actividad` | `MIGRAR_A_SP` | `CREAR` |
| Perfil referencia | `PerfilReferenciaJdbcAdapter` | ANEXO24_DEV | estado por ID | `PerfilApp` | `MIGRAR_A_SP` | `CREAR` |
| Bitácora write | `BitacoraJdbcAdapter` | ANEXO24_DEV | insertar evento | `BitacoraEvento` | `MIGRAR_A_SP` | `CREAR` |
| Bitácora read | `BitacoraConsultaJdbcAdapter` | ANEXO24_DEV | lista, filtros, count | `BitacoraEvento`, `UsuarioApp` | `MIGRAR_A_SP` | `CREAR` |
| Salud | `SystemStatusController` | ambos | `SELECT 1` | — | `EXCEPCION_TECNICA` | `EXCEPCION_TECNICA` |

`SELECT 1` del health check es excepción técnica explícita, no deuda funcional.

## Matriz principal de necesidades

`CONFIRMADO` significa que la conclusión se basa en definición y metadata LIVE. Cuando no hay SP útil, `CREAR` queda como propuesta; todavía no se crea ningún objeto.

| Dominio / necesidad | Base | SP encontrado LIVE | Parámetros / result set | Mutabilidad | Propiedad | Cumple contrato | Decisión | Acción siguiente |
|---|---|---|---|---|---|---|---|---|
| Materiales: listar, filtrar, total, paginar, ordenar `clave, materialkey` | CALE_IMMEX | Ningún SP de catálogo | No aplica | No aplica | — | No | `CREAR` | Proponer `dbo.APP24_Q_MATERIALES_LISTAR` con filtro `clave/descripcion/fraccion`, total, offset y orden estable |
| `CARGA_MATERIALES` | CALE_IMMEX | `dbo.CARGA_MATERIALES` | Sin parámetros; sin result set contractual | `MIXED` / proceso | `LEGACY_COMPARTIDO` | No | `PENDIENTE` | No reutilizar para REST; no modificar legacy |
| Auth: usuario por clave | ANEXO24_DEV | Ninguno; total APP24_DEV = 0 | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_Q_USUARIO_POR_CLAVE` |
| Auth: perfil, estado y permisos | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_Q_USUARIO_ACCESO` con permisos efectivos |
| Usuarios read: lista + total | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_Q_USUARIOS_LISTAR` |
| Usuarios read: detalle por ID | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_Q_USUARIO_OBTENER` |
| Fase 3A: duplicados + creación | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_C_USUARIO_CREAR` atómico, con validaciones y resultado controlado |
| Fase 3A: nombre/correo | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_C_USUARIO_ACTUALIZAR_DATOS` |
| Fase 3B: cambiar estado | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Command atómico con guardrail interno |
| Fase 3B: cambiar perfil | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Command atómico con validación de perfil y guardrail interno |
| Fase 3B: cambiar vigencia | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Command atómico con guardrail interno |
| Fase 3B: administrador efectivo | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Incluir consulta en cada command atómico o rutina interna común |
| Perfil referencia: estado por ID | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_Q_PERFIL_ESTADO` sólo si no forma parte de un SP de usuarios |
| Bitácora: insertar evento | ANEXO24_DEV | Ninguno; no hay SP en ningún schema | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_C_BITACORA_REGISTRAR` |
| Bitácora: lista, filtros y total | ANEXO24_DEV | Ninguno | No aplica | No aplica | — | No | `CREAR` | Proponer `app24.APP24_Q_BITACORA_LISTAR` |

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

**Decisión Materiales: `CREAR dbo.APP24_Q_MATERIALES_LISTAR`.** `CARGA_MATERIALES` es una carga mutable sin parámetros ni contrato de result set. Los SP de estructuras/materiales utilizados tienen otro propósito, filtros y columnas. No existe LIVE un SP que cubra filtro por clave/descripción/fracción, total, paginación y orden `clave, materialkey` requerido por `MaterialJdbcAdapter`.

## Auth, usuarios, Fase 3A, Fase 3B, perfil y bitácora

`ANEXO24_DEV` no contiene procedimientos en `dbo`, `app24` ni otros schemas. Tampoco hay SP LIVE que referencien `UsuarioApp`, `PerfilApp`, `PerfilActividad`, `Actividad` o `BitacoraEvento`. Por tanto:

- Auth: `CREAR` `APP24_Q_USUARIO_POR_CLAVE` y `APP24_Q_USUARIO_ACCESO`.
- Usuarios read: `CREAR` list/detail; no hay alternativa que reutilizar o adaptar.
- Fase 3A: `CREAR` commands atómicos para creación y actualización básica; las validaciones de duplicidad y perfil deben encapsularse en el command de creación.
- Fase 3B: `CREAR`; no hay commands existentes ni query de guardrail.
- Perfil: `CREAR` o incluir referencia en contracts de usuario; no existe SP de detalle reutilizable.
- Bitácora: `CREAR` write y read; no existe alternativa LIVE.

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

Los seis SP fueron inspeccionados por metadata, parámetros, definición y result set. No se ejecutaron en esta fase. Las seis decisiones son `REUTILIZAR` para sus respectivos adapters ya versionados; no aplican a Materiales catálogo ni Administración.

## Clasificación y propiedad

- Los seis `APP24_Q_*` conocidos: `QUERY`, `READ_ONLY`, `CONFIRMADO`, `APP24_PROPIO`.
- `CARGA_MATERIALES` y demás procesos legacy con `INSERT`, `UPDATE` o `DELETE`: `PROCESS`, `MIXED`, `CONFIRMADO`, `LEGACY_COMPARTIDO`.
- SP legacy sin contrato suficiente para un endpoint actual: no se reutilizan; cualquier modificación requeriría `REQUIERE_APROBACION`.
- No se detectaron candidatos LIVE para `ADAPTAR` que cubran las necesidades de Administración o catálogo de materiales.
- No se marca ningún SP como `REUTILIZAR` para Auth, usuarios, Fase 3A, Fase 3B, perfil o bitácora.

## Permisos futuros

La migración posterior deberá reemplazar grants directos de tablas por permisos mínimos:

```sql
GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIO_POR_CLAVE TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_USUARIO_CREAR TO app24_runtime;
```

La lista final deberá incluir sólo SPs aprobados y creados para cada operación. No se modifica `infra/sql/04-app-runtime-permissions.sql` en SP-0D. No se conceden permisos nuevos en esta auditoría.

## Estado final SP-0D

- Conexiones LIVE: exitosas para ambas bases.
- SPs WRITE/MIXED/UNKNOWN ejecutados: **0**.
- SPs READ_ONLY ejecutados: **0**; metadata de result set obtenida sin ejecutar.
- Datos modificados: **0**.
- SPs creados o alterados: **0**.
- Backend, frontend e `infra/sql`: sin cambios.
- Siguiente fase: revisar/aprobar esta matriz antes de implementar cualquier SP.
