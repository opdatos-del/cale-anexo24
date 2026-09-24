# Matriz de regresión SP-1D

Estado de auditoría: SP-1E runtime cerrado desde `feature/backend-sp-runtime`;
F5B diseño cerrado y F5C implementada/validada LIVE.

`UNIT` indica cobertura automatizada en backend; `LIVE` indica ejecución controlada contra `ANEXO24_DEV`. No se ejecutaron mutaciones en `CALE_IMMEX`. Los casos LIVE no ejecutables por dataset se mantienen cubiertos por contrato SQL y tests unitarios.

## Commands de usuarios

| Área | Escenario | Estado | Evidencia |
|---|---|---|---|
| Crear | válido, ID output y relectura | BOTH | `CrearUsuarioUseCaseTest`, prueba LIVE reversible |
| Crear | password inválido | UNIT | `CrearUsuarioUseCaseTest`, validación DTO |
| Crear | clave/correo duplicado | BOTH | `UsuarioComandoJdbcAdapterTest`, LIVE `51104` |
| Crear | perfil inexistente | BOTH | adapter/use case, LIVE `51102` |
| Crear | perfil inactivo | UNIT | contrato command y use case; dataset LIVE sin perfil inactivo |
| Crear | actor ausente | UNIT | `CrearUsuarioUseCaseTest` |
| Crear | bitácora y fallo de bitácora | UNIT | `CrearUsuarioUseCaseTest` |
| Crear | rollback transaccional | UNIT | `CrearUsuarioUseCaseTest`; LIVE create/update/event dentro de transacción con rollback |
| Crear | relectura | UNIT/LIVE | use case y prueba reversible |
| Actualizar | nombre, correo y ambos | UNIT | `ActualizarUsuarioUseCaseTest` |
| Actualizar | no-op | UNIT | `ActualizarUsuarioUseCaseTest` |
| Actualizar | usuario inexistente | UNIT | `ActualizarUsuarioUseCaseTest` |
| Actualizar | correo duplicado | UNIT/LIVE | adapter/use case, LIVE `51104` |
| Actualizar | actor ausente y fallo bitácora | UNIT | `ActualizarUsuarioUseCaseTest` |
| Estado | ACTIVO→INACTIVO / INACTIVO→ACTIVO | UNIT | `CambiarEstadoUsuarioUseCaseTest`; guardado por command SP |
| Estado | no-op, estado inválido, usuario inexistente | UNIT | `CambiarEstadoUsuarioUseCaseTest` |
| Estado | auto-inactivación | BOTH | test de contrato y LIVE `51105` |
| Estado | sin administrador efectivo | UNIT | contrato `51107`; LIVE no ejecutable sin fixture seguro |
| Estado | actor ausente, bitácora y rollback | UNIT | tests de use case; transacción app |
| Perfil | cambio válido, no-op, usuario inexistente | UNIT | `CambiarPerfilUsuarioUseCaseTest` |
| Perfil | perfil inexistente/inactivo | BOTH/UNIT | LIVE `51102`; `51103` unitario, sin perfil inactivo LIVE |
| Perfil | auto-cambio y sin administrador efectivo | BOTH/UNIT | LIVE `51106`; `51107` unitario |
| Perfil | actor ausente, bitácora y rollback | UNIT | tests de use case |
| Vigencia | fecha, `NULL`, fecha pasada y no-op | UNIT/LIVE | `CambiarVigenciaUsuarioUseCaseTest`; LIVE fecha pasada |
| Vigencia | usuario inexistente, sin administrador, actor ausente | UNIT | tests/contrato; `51107` LIVE no ejecutable por dataset |
| Vigencia | bitácora y rollback | UNIT | tests de use case |
| Password | válido; encoder recibe plaintext y repository sólo hash | UNIT/LIVE | `RestablecerPasswordUsuarioUseCaseTest`; reset sintético reversible |
| Password | id null/no positivo; password null/blank/corto/largo/sin mayúscula/número/especial | UNIT | `PasswordPolicyTest`, `RestablecerPasswordUsuarioUseCaseTest` |
| Password | usuario inexistente y errores 51101/51108/51150 | UNIT/LIVE | adapter; LIVE `51101` |
| Password | actor ausente, fallo command, bitácora segura y rollback | UNIT | tests de use case; transacción `appTransactionManager` |
| Password | HTTP 204/400/401/403/404 sin secretos en response | UNIT | `RestablecerPasswordUsuarioControllerWebTest` |

## Perfiles read-only — Fase 5A

| Área | Escenario | Estado | Evidencia |
|---|---|---|---|
| Listar perfiles | filtros `nombre` parcial con comodines escapados, `estado`, paginación y orden estable | PASS | LIVE read-only: sin filtros, ACTIVO/INACTIVO, `%` y `_`; unit tests backend |
| Listar perfiles | item `id`, `nombre`, `estado`, `cantidadPermisos`; sin DML ni bitácora | PASS | `APP24_Q_PERFILES_LISTAR` LIVE y tests de adapter/controller |
| Listar perfiles | autorización con `USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR`; escritura futura exclusiva de `PERFILES_ADMINISTRAR` | PASS | Tests controller/RBAC Fase 5A |

## Perfiles y permisos — Fases 5B/5C

| Área | Escenario | Estado | Evidencia |
|---|---|---|---|
| F5B | Diseño de contratos, RBAC, guardrail y persistencia | CERRADO | `auditoria-diseno-perfiles-permisos-fase-5b.md` |
| F5C | Seis SP de perfiles/permisos desplegados | LIVE | `APP24_Q_ACTIVIDADES_LISTAR`, `APP24_Q_PERFIL_PERMISOS_LISTAR`, `APP24_C_PERFIL_CREAR`, `APP24_C_PERFIL_ACTUALIZAR_NOMBRE`, `APP24_C_PERFIL_CAMBIAR_ESTADO` y `APP24_C_PERFIL_REEMPLAZAR_PERMISOS` |
| F5C | Endpoints de perfil, permisos y actividades; RBAC | LIVE | Escritura/consultas F5C con `PERFILES_ADMINISTRAR`; listado de perfiles conserva lectura con `USUARIOS_ADMINISTRAR` o `PERFILES_ADMINISTRAR` |
| F5C | Guardrail, locks, transacción y Bitácora | LIVE | Commands con `SERIALIZABLE`, `SET XACT_ABORT ON`, `UPDLOCK`/`HOLDLOCK`; negocio y evento comparten transacción |
| F5C | Operación sintética revertida | PASS | Rollback sin filas persistentes |
| F5C | Último administrador efectivo | NO EJECUTADO LIVE | No se realizó la operación destructiva por seguridad del dataset; no se infiere cobertura adicional |

## Bitácora

| Escenario | Estado | Evidencia |
|---|---|---|
| actor y actor `NULL` | UNIT/LIVE | `BitacoraJdbcAdapterTest`; LIVE sintético |
| detalle y correlación `NULL` | UNIT | `BitacoraJdbcAdapterTest` |
| `EventoId` válido | BOTH | adapter y LIVE reversible |
| `EventoId` nulo/cero | UNIT | `BitacoraJdbcAdapterTest` |
| fallo BD propagado | UNIT | adapter contract |
| rollback exterior | LIVE | evento sintético dentro de transacción revertida |

## Errores SQL

`UsuarioComandoJdbcAdapterTest` cubre explícitamente `51101`, `51102`, `51103`, `51104`, `51105`, `51106`, `51107`, `51108`, `51150`, `2601`, `2627` y propagación de error desconocido como `DataAccessException` original. Probe LIVE confirmó `SQLServerException.getErrorCode() = 51102`.

## Diferencia de conteo

SP-1B tenía 316 tests y SP-1C dejó 284. La reducción provino de eliminar pruebas de SQL inline, preconsultas `exists*`, rowcounts y `PerfilReferencia`, no de eliminar contratos externos. SP-1D cerró con 294 tests, 0 fallos, 0 errores y 0 omitidos, agregando cobertura explícita de códigos SQL controlados; no se persigue recuperar artificialmente el número histórico 316. INT-1 integró esta cobertura en `dev`.

## Límites LIVE

No se fabricaron perfiles alternativos, perfil inactivo ni segundo administrador. Por ello `51103` y `51107` sensibles a dataset se validan por definición SQL, locking, transacción y unit tests. No se marcaron escenarios funcionales sin cobertura equivalente.

## Gate runtime SP-1E

| Control | Estado | Evidencia |
|---|---|---|
| `app24_runtime` provisionado | PASS | LIVE metadata |
| `anexo24_app` miembro del rol | PASS | LIVE role membership |
| `db_datareader` / `db_datawriter` | PASS removidos | LIVE role membership |
| EXECUTE específico | 19 específicos | Runtime vigente; F5C agregó seis SP/grants de perfiles y permisos |
| grants directos de tablas | 0 | Runtime vigente; sin grants directos de tablas |
| query read-only como runtime | PASS | impersonation `anexo24_app` |
| SELECT directo de tabla | DENIED | `TOP (0)` bajo impersonation |
| segunda ejecución 04 | Histórico: PASS | idempotencia confirmada en SP-1E; F5C eleva el runtime vigente a 19 EXECUTE específicos |
