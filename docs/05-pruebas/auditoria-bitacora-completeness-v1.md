# AUDITLOG_COMPLETENESS_V1_AUDIT

## Alcance y método

Auditoría only de consistencia de Bitácora sobre la aplicación integrada en
`dev` (`3eb4e02293bfef90e4e0e61cf7e96be0cb78db4d`). No se modificaron Java,
SQL, frontend, enums ni procedimientos.

La evidencia LIVE se obtuvo con consultas agregadas `SELECT` contra
`ANEXO24_DEV` y con un backend efímero en puerto `18081`. No se imprimieron
filas, usuarios, detalles, tokens ni credenciales. Para el smoke autenticado se usó un token efímero de auditoría en memoria;
no se ejecutó el endpoint de login porque no se proporcionó una contraseña
autorizada y no se adivinó ninguna.

## Static enums

- `actions`: 14.
- `modules`: 7.
- `results`: 2.
- `RAW_ACTION_STRINGS_OUTSIDE_CONTROLLED_BOUNDARY`: **0**.
- Los dos literales SQL de facturación (`CARGA_VALIDADA` y
  `CARGA_CON_ERRORES`) pertenecen al boundary controlado de
  `APP24_C_FACTURACION_CARGA_CREAR` y se envían al SP de Bitácora.

## Producers

### Java producers

Se localizaron 12 sitios productores Java, todos usando `BitacoraAccion`,
`BitacoraModulo` y `BitacoraResultado` como enums:

- `LoginService`: `LOGIN_OK` y `LOGIN_FALLIDO`.
- `CrearUsuarioUseCase`: `USUARIO_CREADO`.
- `ActualizarUsuarioUseCase`: `USUARIO_ACTUALIZADO`.
- `CambiarEstadoUsuarioUseCase`: `USUARIO_ESTADO_CAMBIADO`.
- `CambiarPerfilUsuarioUseCase`: `USUARIO_PERFIL_CAMBIADO`.
- `CambiarVigenciaUsuarioUseCase`: `USUARIO_VIGENCIA_CAMBIADA`.
- `RestablecerPasswordUsuarioUseCase`: `USUARIO_PASSWORD_RESTABLECIDA`.
- `CrearPerfilUseCase`: `PERFIL_CREADO`.
- `ActualizarNombrePerfilUseCase`: `PERFIL_ACTUALIZADO`.
- `CambiarEstadoPerfilUseCase`: `PERFIL_ESTADO_CAMBIADO`.
- `ReemplazarPermisosPerfilUseCase`: `PERFIL_PERMISOS_CAMBIADOS`.

Los productores Java pasan `correlationId` al evento y escriben sólo después de
la operación funcional exitosa, excepto las rutas controladas de login fallido,
que usan resultado `FALLO`.

### SQL producers

- `APP24_C_BITACORA_REGISTRAR`: único procedimiento que inserta en
  `app24.BitacoraEvento`; valida módulo, acción y resultado no nulos/blancos.
- `APP24_C_FACTURACION_CARGA_CREAR`: productor funcional que selecciona la
  acción según el estado de la carga y llama al SP anterior con módulo
  `FACTURACION`, resultado y `@CorrelationId`.
- `APP24_Q_BITACORA_LISTAR`: sólo consulta; no escribe.
- No se encontraron escrituras directas fuera del SP de registro.

`unknown producers`: **0**.

## LIVE domain

- `total events`: **615**
- `date min`: `2026-09-22T14:57:51.904`
- `date max`: `2026-09-29T16:32:14.332`

### Distinct actions

| Acción | Count | Enum |
|---|---:|---|
| `CARGA_CON_ERRORES` | 1 | válida |
| `CARGA_VALIDADA` | 2 | válida |
| `LOGIN_FALLIDO` | 5 | válida |
| `LOGIN_OK` | 477 | válida |
| `PERFIL_ACTUALIZADO` | 14 | válida |
| `PERFIL_CREADO` | 2 | válida |
| `PERFIL_ESTADO_CAMBIADO` | 6 | válida |
| `PERFIL_PERMISOS_CAMBIADOS` | 7 | válida |
| `USUARIO_ACTUALIZADO` | 10 | válida |
| `USUARIO_CREADO` | 1 | válida |
| `USUARIO_ESTADO_CAMBIADO` | 10 | válida |
| `USUARIO_PASSWORD_RESTABLECIDA` | 60 | válida |
| `USUARIO_PERFIL_CAMBIADO` | 10 | válida |
| `USUARIO_VIGENCIA_CAMBIADA` | 10 | válida |

- `unknown actions`: **0**.

### Distinct modules

| Módulo | Count | Enum |
|---|---:|---|
| `ADMINISTRACION` | 130 | válido |
| `FACTURACION` | 3 | válido |
| `SEGURIDAD` | 482 | válido |

- `unknown modules`: **0**.
- `CATALOGOS`, `OPERACIONES`, `REPORTES` y `SISTEMA` están definidos pero no
  tienen eventos LIVE en el rango consultado.

### Distinct results

| Resultado | Count | Enum |
|---|---:|---|
| `EXITO` | 609 | válido |
| `FALLO` | 6 | válido |

- `unknown results`: **0**.

### Null / blank contract

| Campo | Null/blank count | Clasificación |
|---|---:|---|
| acción | 0 | dominio válido |
| módulo | 0 | dominio válido |
| resultado | 0 | dominio válido |
| fecha | 0 | dominio válido |
| `correlationId` | 0 | metadata presente en LIVE actual |

El esquema y el modelo permiten `correlationId` opcional históricamente; su
nulabilidad no se considera bug histórico.

## Exhaustive pagination

- `range`: desde `2026-09-22T00:00:00Z` hasta `2026-09-29T23:59:59.999Z`.
- `page size`: 100, máximo permitido por la API.
- `pages`: **7**.
- `rows scanned`: **615**.
- `API total`: **615**.
- `unique IDs`: **615**.
- `duplicates`: **0**.
- `HTTP 500`: **0**.
- `enum errors`: **0**.
- `order`: estable y determinista conforme al contrato: `fecha DESC, id DESC`.

El mapper real usa `BitacoraModulo.valueOf`, `BitacoraAccion.valueOf` y
`BitacoraResultado.valueOf`; ninguna fila produjo error de parseo.

## Filters

| Filtro | HTTP |
|---|---:|
| fecha/rango completo | 200 |
| `usuarioId` | 200 |
| `modulo=ADMINISTRACION` | 200 |
| `modulo=FACTURACION` | 200 |
| `modulo=SEGURIDAD` | 200 |
| `resultado=EXITO` | 200 |
| módulo inválido | 400 |

El controller y el SP actuales no exponen un filtro `accion`; no se inventó ni
se probó un parámetro fuera del contrato real.

## Reports

- `report total`: **615**.
- `report pages`: **7**.
- `report rows`: **615**.
- `matches auditlog`: **YES**.
- `HTTP 500`: **0**.
- `XLSX`: **200**.
- MIME XLSX: **correcto**.
- estructura ZIP/XLSX: **válida**.
- workbook y sheet: **presentes**.
- headers: **9**.
- rows: **615**.
- empty export: **204**.

La exportación respeta el límite contractual de 10,000 filas y el sanitizador
estático protege valores que comienzan con `=`, `+`, `-` o `@`.

## Event coverage

| Action | Producer | Seen LIVE | Parseable | Notes |
|---|---|---:|---|---|
| `LOGIN_OK` | `LoginService.login` | 477 | YES | éxito de login |
| `LOGIN_FALLIDO` | `LoginService.registrarLoginFallido` | 5 | YES | credenciales/cuenta inválida |
| `USUARIO_CREADO` | `CrearUsuarioUseCase` | 1 | YES | administración |
| `USUARIO_ACTUALIZADO` | `ActualizarUsuarioUseCase` | 10 | YES | administración |
| `USUARIO_ESTADO_CAMBIADO` | `CambiarEstadoUsuarioUseCase` | 10 | YES | administración |
| `USUARIO_PERFIL_CAMBIADO` | `CambiarPerfilUsuarioUseCase` | 10 | YES | administración |
| `USUARIO_VIGENCIA_CAMBIADA` | `CambiarVigenciaUsuarioUseCase` | 10 | YES | administración |
| `USUARIO_PASSWORD_RESTABLECIDA` | `RestablecerPasswordUsuarioUseCase` | 60 | YES | detalle sin password |
| `PERFIL_CREADO` | `CrearPerfilUseCase` | 2 | YES | administración |
| `PERFIL_ACTUALIZADO` | `ActualizarNombrePerfilUseCase` | 14 | YES | administración |
| `PERFIL_ESTADO_CAMBIADO` | `CambiarEstadoPerfilUseCase` | 6 | YES | administración |
| `PERFIL_PERMISOS_CAMBIADOS` | `ReemplazarPermisosPerfilUseCase` | 7 | YES | administración |
| `CARGA_VALIDADA` | `APP24_C_FACTURACION_CARGA_CREAR` | 2 | YES | estado `PREVISUALIZADA` |
| `CARGA_CON_ERRORES` | `APP24_C_FACTURACION_CARGA_CREAR` | 1 | YES | demás estados |

No se exige cobertura LIVE de cada enum porque en esta ejecución las 14
acciones sí aparecieron, pero la regla de aceptación es que todo valor LIVE sea
parseable y que todo productor actual use un enum válido.

## Correlation

- `current producers pass correlationId`: **YES**.
- `live with correlation`: **615**.
- `live without correlation`: **0**.
- `historical caveat`: el contrato permite `NULL` para compatibilidad histórica.

## Privacy

- `sensitive detail producers`: **0**.
- `LIVE pattern counts`:
  - `password=`: 0
  - `Bearer`: 0
  - `jwt=`: 0
  - `secret=`: 0
- Los detalles observados por los productores contienen identificadores y
  metadatos operativos minimizados; no se devolvieron contenidos LIVE en esta
  bitácora.

## Frontend

Smoke focal con navegador headless y backend efímero:

- `screen`: **PASS**
- `filters`: **PASS**
- `pagination`: **PASS**
- `empty state`: **PASS**
- `console errors`: **0**
- `HTTP 500`: **0**

La UI usa el mismo contrato real `/api/v1/bitacora`; el frontend no expone un
filtro de acción, consistente con el controller actual.

## Authentication

- solicitud sin token: **401**.
- token sin permiso: **403**.
- `LOGIN_FLOW_REEXECUTED`: **NO**.
- login endpoint: no ejecutado; ya existe evidencia LIVE de `LOGIN_OK` y
  `LOGIN_FALLIDO`, ambos parseables, y no se usó una contraseña no
  proporcionada.

## Findings

No se identificaron hallazgos `AUDITLOG-001` o posteriores.

| ID | Severity | Layer | Finding | Impact | Suggested fix |
|---|---|---|---|---|---|
| — | — | — | Sin hallazgos | — | No fix requerido |

## Database

- `SELECT only`: **YES**.
- `DML`: **0**.
- `DDL`: **0**.
- `CALE_IMMEX writes`: **0**.
- No se ejecutaron migraciones ni procedimientos de escritura durante la
  auditoría.
- La tabla y procedimientos versionados fueron inspeccionados estáticamente;
  no se modificaron.

## Security

- `credentials persisted`: **NO**.
- `JWT persisted`: **NO**.
- `sensitive details exposed`: **NO**.
- El token efímero de auditoría se mantuvo en memoria y nunca se imprimió ni
  se escribió en disco.

## Git

- `branch`: `feature/auditlog-completeness-v1`.
- `HEAD`: `3eb4e02293bfef90e4e0e61cf7e96be0cb78db4d`.
- `working tree`: sólo el reporte nuevo sin stage y la modificación ajena.
- `unrelated bitacora`: `docs/03-diseno/mapeo-bitacora.md` preservada,
  modificada, unstaged y no descartada.
- `stashes`: 2 intactos.
- `commit`: no realizado.
- `push`: no realizado.

## Release readiness relation

- `AUDIT_LOG_COMPLETENESS`: **PASS**.
- `RELEASE_PROD_CONFIG_INTEGRATED`: **PASS**.
- `SCOPE_DECISION_REQUIRED`: **YES** para Saldos, confirmación de Facturación y
  Dashboard V1.

## Decision

- `ENUM_PRODUCER_CONSISTENCY = PASS`
- `LIVE_ENUM_COMPATIBILITY = PASS`
- `EXHAUSTIVE_PAGINATION = PASS`
- `REPORT_EXPORT_CONSISTENCY = PASS`
- `SENSITIVE_DETAIL_SCAN = PASS`
- `AUDIT_LOG_COMPLETENESS = PASS`
- `READY_FOR_AUDITLOG_FIX_PHASE = NO` — no se identificó una corrección
  funcional pendiente en esta auditoría.

**NO COMMIT.**
**NO PUSH.**
