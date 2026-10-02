# Runtime least privilege — auditoría y diseño

Fase de **auditoría y diseño**. No se crean logins, usuarios, roles, GRANT ni
DENY; no se altera el runtime; no se ejecuta DDL ni SP mutables. Toda la
evidencia LIVE es metadata read-only (`sys.*`, `OBJECT_DEFINITION`).

Estado del diseño: `LEAST_PRIVILEGE_DESIGN_READY = YES` (con un spike de
verificación pendiente para el mecanismo cross-database, sección 9).

---

## 1. Estado actual

Identidad runtime auditada:

| Elemento | Valor LIVE |
|---|---|
| Login runtime actual | `opdatos` (SQL_LOGIN, miembro de `sysadmin`) |
| `CALE_IMMEX` | user = `dbo` (mapea al login `opdatos`) |
| `ANEXO24_DEV` | user = `dbo` (mapea al login `opdatos`) |
| Login `anexo24_app` | **existe** a nivel servidor (SQL_LOGIN, habilitado, sin roles de servidor) |
| User `anexo24_app` en `CALE_IMMEX` | **no existe** |
| User `anexo24_app` en `ANEXO24_DEV` | existe; miembro de rol `app24_runtime`; GRANT `CONNECT` |
| Permisos actuales de `app24_runtime` | 35 GRANT `EXECUTE` por objeto (todos los SP `app24`), sin permisos de tabla |
| `opdatos` | rol servidor `sysadmin` |

Propiedades de seguridad LIVE (ambas DB):

```text
TRUSTWORTHY        = OFF (CALE_IMMEX y ANEXO24_DEV)
DB_CHAINING        = OFF (ambas)
containment        = NONE
execute-as modules = 0
firmas (cert)      = 0
ownership          = CALE_IMMEX: schema dbo → owner dbo (509 objetos)
                     ANEXO24_DEV: schema app24 → owner dbo (126 objetos)
```

Consecuencias:

- El runtime actual corre con `sysadmin`/`dbo`: puede leer, escribir, alterar y
  borrar cualquier objeto de ambas bases. No es least privilege.
- Todo el ownership es `dbo` en ambas DB: el **ownership chaining intra-DB**
  cubre las referencias SP → tabla/vista/función dentro de la misma base
  (módulo y objeto con el mismo owner), sin necesidad de permisos de tabla
  para el caller.
- `DB_CHAINING = OFF` y `TRUSTWORTHY = OFF`: **no** hay chaining
  cross-database. El único flujo cross-db (sección 5) hoy funciona únicamente
  porque el caller es `dbo`/`sysadmin`.

## 2. Riesgos

| # | Riesgo | Severidad |
|---|---|---|
| R1 | El runtime puede ejecutar DDL/DML arbitrario si la app fuese comprometida (sysadmin) | Crítico |
| R2 | Un bug podría borrar o alterar tablas legacy sin barrera de permisos | Crítico |
| R3 | El login `anexo24_app` existe pero no puede usarse aún en `CALE_IMMEX` (sin user) | Alto |
| R4 | El flujo cross-db de pedimentos fallará bajo least privilege si no se elige mecanismo (sin chaining/firmas/EXECUTE AS) | Alto |
| R5 | Deriva repo↔LIVE: los GRANT de `04-app-runtime-permissions.sql` en repo cubren menos SP que LIVE; el archivo dejó de ser fuente de verdad | Medio |
| R6 | Dependencia colgante `nodo.value` en `APP24_Q_PEDIMENTO_VALIDAR_REGLAS` (posible falso positivo de parser XML); verificar en fase de implementación | Bajo |

## 3. Inventario Java → SQL Server

Acceso a datos desde Java (auditado en `backend/src/main/java`):

- **2 datasources** (`DataSourceConfig`): `jdbcTemplate` → `CALE_IMMEX`
  (Módulo C, primary) y `appJdbcTemplate` → `ANEXO24_DEV` (esquema `app24`).
- **36 adapters JDBC** con `JdbcTemplate.call` + `connection.prepareCall`
  (`{call ...}`), sin SQL inline (SP-FIRST: `violations = 0`).
- **1 excepción técnica**: `SystemStatusController.checkDatabase` ejecuta
  `SELECT 1` en ambos datasources (excepción autorizada
  `TECHNICAL_SQL_EXCEPTION_001`).
- **58 entry points únicos** consumidos: 23 en `CALE_IMMEX.dbo` y 35 en
  `ANEXO24_DEV.app24`. Ninguno usa SQL dinámico.

Adapters por datasource:

```text
CALE_IMMEX (jdbcTemplate) — 23 adapters
  ConfirmacionPedimentoJdbcAdapter, PedimentoReglasJdbcAdapter,
  MaterialJdbcAdapter, ProductoStoredProcedureAdapter,
  EstructuraStoredProcedureAdapter, EntradaStoredProcedureAdapter,
  SalidaStoredProcedureAdapter, ActivoFijoStoredProcedureAdapter,
  MaterialUtilizadoStoredProcedureAdapter, AnalisisDescargaStoredProcedureAdapter,
  CompulsaStoredProcedureAdapter, OperacionBloqueadaStoredProcedureAdapter,
  OperacionDirigidaStoredProcedureAdapter, RectificacionStoredProcedureAdapter,
  VencimientoStoredProcedureAdapter, CategoriaJdbcAdapter,
  TipoMaterialJdbcAdapter, UnidadJdbcAdapter, AlmacenJdbcAdapter,
  ClienteJdbcAdapter, AgenteAduanalJdbcAdapter, ProveedorJdbcAdapter,
  DatosGeneralesJdbcAdapter

ANEXO24_DEV (appJdbcTemplate) — 13 adapters
  UsuarioJdbcAdapter, UsuarioConsultaJdbcAdapter, UsuarioComandoJdbcAdapter,
  PerfilConsultaJdbcAdapter, PerfilComandoJdbcAdapter,
  PerfilPermisosConsultaJdbcAdapter, ActividadConsultaJdbcAdapter,
  BitacoraConsultaJdbcAdapter, BitacoraJdbcAdapter,
  CargaFacturacionJdbcAdapter, PlantillaFacturacionJdbcAdapter,
  CatalogImportJdbcAdapter, CargaPedimentoJdbcAdapter

Ambos: SystemStatusController (SELECT 1, técnico)
```

## 4. Matriz DB / SP

Leyenda: R = read-only; W = escribe (por sí o por dependencia); X = cross-db;
grado de permiso propuesto = `EXECUTE` por objeto al rol runtime de esa DB.

### 4.1 `CALE_IMMEX` (23 entry points → rol propuesto `cale_immex_runtime`)

| SP (`dbo.`) | Java consumer | R/W | Cross-db | Dinámico | Grant |
|---|---|---|---|---|---|
| `APP24_Q_ACTIVOS_FIJOS_LISTAR` | ActivoFijoStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_AGENTES_ADUANALES_LISTAR` | AgenteAduanalJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_ALMACENES_LISTAR` | AlmacenJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_ANALISIS_DESCARGAS_LISTAR` | AnalisisDescargaStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_CATEGORIAS_LISTAR` | CategoriaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_CLIENTES_LISTAR` | ClienteJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_COMPULSA_LISTAR` | CompulsaStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_DATOS_GENERALES_OBTENER` | DatosGeneralesJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_DIRIGIDOS_LISTAR` | OperacionDirigidaStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_ENTRADAS_LISTAR` | EntradaStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_ESTRUCTURAS_LISTAR` | EstructuraStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_MATERIALES_LISTAR` | MaterialJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | MaterialUtilizadoStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR` | OperacionBloqueadaStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_PEDIMENTO_VALIDAR_REGLAS` | PedimentoReglasJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_PRODUCTOS_LISTAR` | ProductoStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_PROVEEDORES_LISTAR` | ProveedorJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_RECTIFICACIONES_LISTAR` | RectificacionStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_SALIDAS_LISTAR` | SalidaStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_Q_TIPOS_MATERIAL_LISTAR` | TipoMaterialJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_UNIDADES_LISTAR` | UnidadJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_VENCIMIENTOS_LISTAR` | VencimientoStoredProcedureAdapter | R | N | N | EXECUTE |
| `APP24_C_PEDIMENTO_CONFIRMAR` | ConfirmacionPedimentoJdbcAdapter | W | **Y** | N | EXECUTE |

Dependencias transitivas (ver sección 5): el command ejecuta
`ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR` y escribe en
`CargaPedimento`, `CargaPedimentoFila` y `ErrorCargaPedimento`. El resto de
entry points sólo leen tablas/vistas/funciones `dbo` de la misma DB (50
referencias de tabla + 2 de `DBO` + funciones `FACTOR`, `VALIDUNIT`,
`ISVALIDUNIT`, `getProductStruct`), todas del owner `dbo`.

### 4.2 `ANEXO24_DEV` (35 entry points → rol existente `app24_runtime`)

| SP (`app24.`) | Java consumer | R/W | Cross-db | Dinámico | Grant |
|---|---|---|---|---|---|
| `APP24_Q_USUARIO_POR_CLAVE` | UsuarioJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_USUARIO_ACCESO` | UsuarioJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_USUARIOS_LISTAR` | UsuarioConsultaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_USUARIO_OBTENER` | UsuarioConsultaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_USUARIO_CREAR` | UsuarioComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_USUARIO_ACTUALIZAR_DATOS` | UsuarioComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_USUARIO_CAMBIAR_ESTADO` | UsuarioComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_USUARIO_CAMBIAR_PERFIL` | UsuarioComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_USUARIO_CAMBIAR_VIGENCIA` | UsuarioComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_USUARIO_RESTABLECER_PASSWORD` | UsuarioComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_Q_PERFILES_LISTAR` | PerfilConsultaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_PERFIL_PERMISOS_LISTAR` | PerfilPermisosConsultaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_PERFIL_CREAR` | PerfilComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_PERFIL_ACTUALIZAR_NOMBRE` | PerfilComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_PERFIL_CAMBIAR_ESTADO` | PerfilComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_C_PERFIL_REEMPLAZAR_PERMISOS` | PerfilComandoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_Q_ACTIVIDADES_LISTAR` | ActividadConsultaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_BITACORA_LISTAR` | BitacoraConsultaJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_BITACORA_REGISTRAR` | BitacoraJdbcAdapter | W | N | N | EXECUTE |
| `APP24_Q_PEDIMENTO_CARGA_POR_HASH` | CargaPedimentoJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_PEDIMENTO_CARGA_OBTENER` | CargaPedimentoJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_PEDIMENTO_CARGA_ERRORES` | CargaPedimentoJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_PEDIMENTO_CARGA_CREAR` | CargaPedimentoJdbcAdapter | W | N | N | EXECUTE |
| `APP24_Q_FACTURACION_CARGA_POR_HASH` | CargaFacturacionJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_FACTURACION_CARGA_OBTENER` | CargaFacturacionJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_FACTURACION_CARGA_CREAR` | CargaFacturacionJdbcAdapter | W | N | N | EXECUTE |
| `APP24_Q_FACTURACION_PLANTILLA_ACTIVA` | PlantillaFacturacionJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH` | CatalogImportJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER` | CatalogImportJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES` | CatalogImportJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_CATALOGO_MATERIAL_CARGA_CREAR` | CatalogImportJdbcAdapter | W | N | N | EXECUTE |
| `APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH` | CatalogImportJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER` | CatalogImportJdbcAdapter | R | N | N | EXECUTE |
| `APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES` | CatalogImportJdbcAdapter | R | N | N | EXECUTE |
| `APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR` | CatalogImportJdbcAdapter | W | N | N | EXECUTE |

Todas las dependencias transitivas de estos 35 SP apuntan a tablas
`ANEXO24_DEV.app24` (84 referencias distintas) del owner `dbo`; sin cross-db,
sin SQL dinámico. Las cinco familias `..._CARGA_CREAR` ejecutan además
`APP24_C_BITACORA_REGISTRAR` (misma DB, mismo owner).

## 5. Call graph cross-database

Grafo completo del producto: **un solo flujo cross-db**.

```text
CALE_IMMEX.dbo.APP24_C_PEDIMENTO_CONFIRMAR
  ├─ EXEC → ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR   (cross-db, SP)
  │            └─ INSERT ANEXO24_DEV.app24.BitacoraEvento
  ├─ UPDATE/INSERT → ANEXO24_DEV.app24.CargaPedimento      (cross-db, tabla)
  ├─ SELECT        → ANEXO24_DEV.app24.CargaPedimentoFila  (cross-db, tabla)
  ├─ SELECT        → ANEXO24_DEV.app24.ErrorCargaPedimento (cross-db, tabla)
  └─ intra-db (ownership chaining dbo):
       IMPORTACIONES → PARTIDAS, SALIDAS → PSALIDAS, DIRIGIDO,
       MATERIAL, PRODUCTOS, FACTOR(), VALIDUNIT()
```

Aristas cross-db totales: 4 (1 EXEC + 3 tablas). Sin ellas, el resto del
producto es intra-DB con owner `dbo` uniforme → cobertura por ownership
chaining.

`execute_as` en módulos: ninguno. Firmas: ninguna. `DB_CHAINING`: OFF.
`TRUSTWORTHY`: OFF. `containment`: NONE.

## 6. Diseño propuesto del principal

```text
Login (servidor):  anexo24_app        [YA EXISTE en LIVE]
  Uso exclusivo: conexión de la aplicación (dos datasources)
  Server roles:  ninguno (solo CONNECT SQL implícito de public+login)
  Password:      gestionado por operación (rotación fuera de este documento)

Users:
  CALE_IMMEX.anexo24_app     [A CREAR en implementación]
  ANEXO24_DEV.anexo24_app    [YA EXISTE]

Roles propios:
  CALE_IMMEX.cale_immex_runtime   [A CREAR]  → member: anexo24_app
  ANEXO24_DEV.app24_runtime       [YA EXISTE] → member: anexo24_app
```

Roles propios elegidos por DB para (a) agrupar los 23 + 35 EXECUTE, (b)
permitir futuras reasignaciones sin tocar al user, (c) NO reutilizar roles
fijos. No se usa `db_owner`, `db_datareader` ni `db_datawriter`.

## 7. Permisos propuestos

### Servidor

| Permiso | Principal | Razón |
|---|---|---|
| `CONNECT SQL` | `anexo24_app` | inherente al login; único privilegio de servidor |

Sin `sysadmin`, `securityadmin`, `serveradmin`, `dbcreator`, `CONTROL SERVER`.

### CALE_IMMEX

| Objeto | Permiso | A |
|---|---|---|
| 23 SP de la tabla 4.1 | `EXECUTE` por objeto | `cale_immex_runtime` |

### ANEXO24_DEV

| Objeto | Permiso | A |
|---|---|---|
| 35 SP de la tabla 4.2 | `EXECUTE` por objeto | `app24_runtime` |
| Mecanismo cross-db (sección 9): `APP24_C_BITACORA_REGISTRAR` + `CargaPedimento`, `CargaPedimentoFila`, `ErrorCargaPedimento` | `EXECUTE` / `SELECT`+`INSERT`+`UPDATE` | **principal de contexto del command**, nunca el caller runtime |

`DIRECT_TABLE_PERMISSIONS (runtime) = 0` en ambas DB.
`SCHEMA-level EXECUTE = NO` (schema `app24` contiene commands sensibles y
schema `dbo` contiene 509 objetos legacy; prohibido grant de schema).

## 8. Permisos explícitamente prohibidos

- `db_owner`, `db_datareader`, `db_datawriter`, `db_ddladmin` para runtime.
- `GRANT EXECUTE ON SCHEMA::...` en cualquiera de las dos DB.
- Cualquier `SELECT`/`INSERT`/`UPDATE`/`DELETE` directo a tablas para el
  runtime.
- `ALTER`, `CREATE`, `DROP`, `CONTROL`, `TAKE OWNERSHIP`, `VIEW DEFINITION`.
- `TRUSTWORTHY ON`, `DB_CHAINING ON` como atajo cross-db.
- `WITH EXECUTE AS OWNER` salvo evaluación explícita (implica `dbo`).
- Reutilización de `operaciones_consultar` o permisos funcionales para
  autorización de objetos SQL.

## 9. Estrategia cross-database (CALE → ANEXO24_DEV)

Evaluación de alternativas sin `db_owner`:

| Opción | Cómo | Evaluación |
|---|---|---|
| A. Mismo login mapeado en ambas DB + permisos directos al caller | GRANT EXECUTE/DML a `anexo24_app` en `ANEXO24_DEV` sobre bitácora y 3 tablas | Simple, pero da DML directo al caller si otras vías lo alcanzan; sube superficie; **no preferida** |
| B. GRANT EXECUTE en entry-points | Ya propuesto para la parte intra-DB | Necesario pero insuficiente cross-db |
| C. Permisos explícitos en SP destino (bitácora) | GRANT EXECUTE al principal de contexto | Parte de la solución elegida |
| D. Ownership chaining | OFF; habilitarlo (`DB_CHAINING ON`) es atajo transversal | **Rechazado** |
| E. `EXECUTE AS` con usuario de contexto dedicado en la DB origen | `WITH EXECUTE AS 'svc_ctx'` en el command; el login del contexto tiene user en ambas DB con permisos mínimos | Alternativa válida; requiere login adicional y `IMPERSONATE` |
| F. Module signing con certificado | Firmar `APP24_C_PEDIMENTO_CONFIRMAR`; usuario desde certificado en `ANEXO24_DEV` con `EXECUTE` bitácora + DML 3 tablas | **Preferida**; sin login adicional; requiere verificación del comportamiento cross-db en el servidor del cliente |

**Decisión de diseño:** F (module signing) como principal; E como fallback.
Antes de implementar se requiere un **spike de verificación** en entorno
desechable (contenedor o DB de prueba): firmar el command, replicar el
certificado en `ANEXO24_DEV`, crear el cert-user con los permisos mínimos y
confirmar que un caller `anexo24_app` (sin permisos cross-db, sin chaining)
puede ejecutar el flujo completo. Si el servidor no honra el patrón
documentado de firma cross-db, se implementa E.

El caller runtime nunca recibe DML sobre las 3 tablas: sólo el principal de
contexto (cert-user) o el usuario de impersonación.

## 10. Identidad de migrations / deployment

Separación runtime vs DDL:

- `anexo24_app` **no** podrá `CREATE PROCEDURE`, `ALTER TABLE`, `CREATE USER`
  ni `GRANT`.
- Propuesta: identidad de deployment separada, p. ej. login `anexo24_deploy`
  (o el principal administrativo actual bajo proceso controlado) con permisos
  DDL en `CALE_IMMEX` y `ANEXO24_DEV`, usada únicamente por el flujo de
  migraciones (scripts `infra/sql` + despliegue de SP), nunca por la
  aplicación.
- Los scripts de migración ya son idempotentes y versionados; el deployment
  sigue siendo manual/controlado en esta etapa (sin cambio).
- No se crea ninguna identidad de deployment en esta fase.

## 11. Test plan (implementación futura)

### Positivas (login `anexo24_app`)

1. Conexión a ambas DB y `SELECT 1` (health check).
2. Catálogos: materiales, productos, estructuras, auxiliares, socios.
3. Operaciones: entradas, salidas, materiales utilizados, activos fijos.
4. Reportes: los 12 endpoints read-only.
5. Administración: usuarios, perfiles, actividades, permisos, bitácora.
6. Auth: login (`APP24_Q_USUARIO_POR_CLAVE`, `APP24_Q_USUARIO_ACCESO`).
7. Pedimentos: upload, preview, errores y **confirmación autoritativa
   completa** (flujo cross-db con el mecanismo elegido).

### Negativas (todas deben fallar con permiso denegado)

| Prueba | Resultado esperado |
|---|---|
| `SELECT` directo a tabla | DENIED |
| `INSERT` directo a tabla | DENIED |
| `UPDATE` directo a tabla | DENIED |
| `DELETE` directo a tabla | DENIED |
| `ALTER` objeto | DENIED |
| `CREATE` objeto | DENIED |
| `DROP` objeto | DENIED |
| `EXEC` SP no autorizado (p. ej. legacy `CARGAPEDIMENTOS` en CALE o cualquier SP fuera de las listas 4.1/4.2) | DENIED |
| `SELECT` desde `ANEXO24_DEV` en sesión CALE sin el command | DENIED |

Verificación de contexto: `SUSER_SNAME() = 'anexo24_app'` y `USER_NAME() =
'anexo24_app'` en ambas DB durante las pruebas positivas.

## 12. Rollback plan

Si la nueva identidad falla en validación:

1. Revertir variables de entorno de conexión (sin tocar YAML):
   `DB_USERNAME`/`DB_PASSWORD` y `APP_DB_USERNAME`/`APP_DB_PASSWORD` a la
   identidad anterior; reiniciar el backend.
2. Validaciones de rollback: health check, un endpoint por módulo, login y
   confirmación de pedimentos en entorno controlado.
3. Criterios de abort: cualquier prueba positiva falla, o cualquier negativa
   resulta permitida, o aparece una dependencia cross-db no inventariada.
4. Evidencia requerida antes de retirar la identidad anterior: matriz
   positiva/negativa PASS, log de confirmación con `usuario` y `correlacion`.
5. No se ejecuta rollback de DDL por este medio: los GRANT/roles nuevos son
   aditivos y no mutan datos.

## 13. Findings / blockers

| # | Finding | Estado |
|---|---|---|
| F1 | Login `anexo24_app` existe; **falta el user en `CALE_IMMEX`** — blocker para migrar runtime | Blocker de implementación |
| F2 | Deriva repo↔LIVE: `04-app-runtime-permissions.sql` no refleja los 35 GRANT LIVE (cubre menos); actualizar el archivo en la fase de implementación | Finding medio |
| F3 | Mecanismo cross-db sin verificar en el servidor destino (firma vs `EXECUTE AS`) | Spike obligatorio antes de implementar |
| F4 | `APP24_Q_PEDIMENTO_VALIDAR_REGLAS` registra dependencia colgante `nodo.value` (probable falso positivo de método XML); verificar y, si procede, corregir en fase posterior | Finding bajo |
| F5 | `RECTIFICACIONES`/`VENCIMIENTOS` resuelven sobre views sin dependencias de tabla registradas en `sys.sql_expression_dependencies`; el chaining las cubre igualmente (owner dbo); confirmar en implementación | Finding bajo |
| F6 | `opdatos` restante con `sysadmin` seguirá existiendo como identidad administrativa; no se elimina en esta fase | Aceptado |

`LIVE_CHANGES = 0`: esta fase no creó ni modificó ningún objeto LIVE.
