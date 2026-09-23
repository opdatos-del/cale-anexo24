# Matriz de regresión SP-1D

Estado de auditoría: 2026-09-23. SP-1E runtime cerrado desde `feature/backend-sp-runtime`.

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

SP-1B tenía 316 tests y SP-1C dejó 284. La reducción provino de eliminar pruebas de SQL inline, preconsultas `exists*`, rowcounts y `PerfilReferencia`, no de eliminar contratos externos. SP-1D agrega cobertura explícita de códigos SQL controlados; no se persigue recuperar artificialmente el número histórico 316.

## Límites LIVE

No se fabricaron perfiles alternativos, perfil inactivo ni segundo administrador. Por ello `51103` y `51107` sensibles a dataset se validan por definición SQL, locking, transacción y unit tests. No se marcaron escenarios funcionales sin cobertura equivalente.

## Gate runtime SP-1E

| Control | Estado | Evidencia |
|---|---|---|
| `app24_runtime` provisionado | PASS | LIVE metadata |
| `anexo24_app` miembro del rol | PASS | LIVE role membership |
| `db_datareader` / `db_datawriter` | PASS removidos | LIVE role membership |
| EXECUTE específico | PASS, 11/11 | LIVE permissions |
| grants directos de tablas | PASS, 0 | LIVE permissions + `HAS_PERMS_BY_NAME` |
| query read-only como runtime | PASS | impersonation `anexo24_app` |
| SELECT directo de tabla | DENIED | `TOP (0)` bajo impersonation |
| segunda ejecución 04 | PASS | idempotencia confirmada |
