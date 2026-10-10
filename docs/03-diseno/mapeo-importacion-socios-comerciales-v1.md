# Mapeo de importación de socios comerciales V1

## Resultado de la primera auditoría

La consulta de Clientes, Proveedores y Agentes ya tiene contratos read-only
separados. La importación/actualización es otra capacidad: los procedimientos
legacy son mutables y sus stages son globales. En esta primera pasada no se
crean tablas, endpoints, permisos ni parser nuevo porque no existe evidencia
suficiente de layout y reglas de carga autoritativas.

```text
CLIENT_IMPORT_CONTRACT = UNKNOWN
PROVIDER_IMPORT_CONTRACT = UNKNOWN
AGENT_IMPORT_CONTRACT = UNKNOWN
CLIENT_STAGING_V1 = NOT_IMPLEMENTED
PROVIDER_STAGING_V1 = NOT_IMPLEMENTED
AGENT_STAGING_V1 = NOT_IMPLEMENTED
BUSINESS_PARTY_IMPORTS_V1 = DISCOVERY_ONLY
BUSINESS_PARTY_IMPORTS_DISCOVERY = COMPLETE
BUSINESS_PARTY_IMPORTS_IMPLEMENTATION = BLOCKED_BY_CONTRACT
```

## Clientes

| Objeto | Evidencia disponible | Clasificación |
|---|---|---|
| `CARGACLIENTES` | Procedimiento legacy de carga/validación; usa inserts y staging | `WRITE/MIXED`; no ejecutar |
| `TMPCLIENTES` | Stage global mencionado por la definición/documentación | Sin aislamiento demostrado por usuario, lote, archivo o hash |
| `ECARGACLIENTES` | Stage/error auxiliar referido por `CARGACLIENTES` | Columnas, tipos y reglas de importación no cerrados en el contrato versionado |
| `dbo.clientes` | Maestro read-only: 5 filas observadas, clave funcional `Clave` | Destino mutable; no escribir |

No se confirmó layout oficial, Excel descargable, columnas obligatorias,
longitudes completas, estrategia alta/actualización, regla de RFC, regla de
duplicados ni contrato de errores de carga.

## Proveedores

| Objeto | Evidencia disponible | Clasificación |
|---|---|---|
| `CARGAPROVEEDORES` | Procedimiento legacy de carga/validación; usa inserts y staging | `WRITE/MIXED`; no ejecutar |
| `TMPPROVEEDORES` | Stage global mencionado por la definición/documentación | Sin aislamiento demostrado por usuario, lote, archivo o hash |
| `ECARGAPROVEEDORES` | Stage/error auxiliar referido por `CARGAPROVEEDORES` | Columnas, tipos y reglas de importación no cerrados en el contrato versionado |
| `dbo.Proveedores` | Maestro read-only: 5 filas observadas, clave funcional `Clave` | Destino mutable; no escribir |

No se confirmó layout oficial, columnas obligatorias, longitudes completas,
reglas de alta/actualización, RFC, duplicados ni errores persistibles.

## Agentes aduanales

| Objeto | Evidencia disponible | Clasificación |
|---|---|---|
| `CARGAAgentes` | Procedimiento legacy de carga/validación; usa inserts y staging | `WRITE/MIXED`; no ejecutar |
| `TMPagentes` | Stage global mencionado por la definición/documentación | Sin aislamiento demostrado por usuario, lote, archivo o hash |
| `ECARGAagentes` | Stage/error auxiliar referido por `CARGAAgentes` | Layout y reglas no cerrados |
| `dbo.agentes` | Maestro read-only: 2 filas observadas, clave funcional `Clave` | Destino mutable; no escribir |

`AGENTS_IMPORT_CONTRACT` permanece `UNKNOWN`; no bloquea la clasificación de
Clientes y Proveedores, pero tampoco alcanza para crear staging parcial.

## Layouts y reglas

| Catálogo | Layout | Reglas demostradas para importación | Decisión |
|---|---|---|---|
| Clientes | `UNKNOWN` | No suficientes | No implementar |
| Proveedores | `UNKNOWN` | No suficientes | No implementar |
| Agentes | `UNKNOWN` | No suficientes | No implementar |

Los documentos existentes sólo demuestran el maestro read-only y la existencia
de procesos mutables. No se convierten nombres de columnas del maestro en un
layout de carga inventado.

## Arquitectura preparada, sin implementación

Cuando el contrato sea confirmado, la solución deberá reutilizar el patrón
existente de importaciones de catálogos:

```text
upload → parser explícito por catálogo → validación → staging app24 → errores → preview
```

El staging futuro deberá aislar carga, hash, usuario y `correlationId`, y usar
SP versionados en `ANEXO24_DEV`. No se reutilizarán `TMPCLIENTES`,
`TMPPROVEEDORES` ni `TMPagentes` para preview concurrente.

No se implementan todavía:

- `CargaCliente` / `CargaClienteFila` / `ErrorCargaCliente`;
- `CargaProveedor` / `CargaProveedorFila` / `ErrorCargaProveedor`;
- tablas equivalentes para Agentes;
- `CLIENTES_CARGAR`, `PROVEEDORES_CARGAR` o `AGENTES_CARGAR`;
- confirmación hacia `dbo.clientes`, `dbo.Proveedores` o `dbo.agentes`.

## Evidencia necesaria para reabrir implementación

### Clientes

- layout oficial o layout legacy inequívoco;
- lista completa de columnas;
- obligatoriedad por columna;
- tipos y longitudes;
- regla de alta versus actualización;
- regla de duplicados e idempotencia;
- validación fiscal/RFC;
- contrato de errores;
- caso de aceptación reproducible.

### Proveedores

- layout oficial o layout legacy inequívoco;
- lista completa de columnas;
- obligatoriedad por columna;
- tipos y longitudes;
- regla de alta versus actualización;
- regla de duplicados e idempotencia;
- validación fiscal/RFC;
- contrato de errores;
- caso de aceptación reproducible.

### Agentes aduanales

- layout oficial o layout legacy inequívoco;
- lista completa de columnas;
- obligatoriedad por columna;
- tipos y longitudes;
- regla de alta versus actualización;
- regla de duplicados e idempotencia;
- validación fiscal/RFC;
- patente y sus reglas, si aplica;
- contrato de errores;
- caso de aceptación reproducible.

La ausencia actual de estos elementos es un bloqueo contractual/técnico de
implementación, no `BLOCKED_BUSINESS`: todavía no existe una decisión de negocio
concreta pendiente.

## Efectos y seguridad

```text
CARGACLIENTES executed = 0
CARGAPROVEEDORES executed = 0
CARGAAgentes executed = 0
CALE_IMMEX writes = 0
inline Java SQL = 0
```

La consulta read-only existente permanece bajo `CATALOGOS_AUX_CONSULTAR`. No se
reutiliza ese permiso para una futura escritura o staging de carga.

## Reauditoría Sprint 12 — clientes y proveedores

La primera auditoría anterior conserva el estado histórico de discovery. La
verificación LIVE de Sprint 12 cerró el comportamiento de claves existentes:

- `dbo.CARGACLIENTES` contiene `INSERT` y `TRUNCATE`, pero no `UPDATE`; usa
  `WHERE NOT EXISTS` contra la clave existente y contra errores del lote.
- `dbo.CARGAPROVEEDORES` contiene `INSERT` y `TRUNCATE`, pero no `UPDATE`; usa
  `WHERE NOT EXISTS` con la misma semántica de preservación.
- Ambos procedimientos son mutables y no se ejecutaron durante la auditoría.
- La implementación moderna confirma altas válidas nuevas y preserva claves ya
  existentes; coincide con el comportamiento legacy demostrado.

```text
LEGACY_CLIENT_EXISTING_KEY_BEHAVIOR = INSERT_ONLY
LEGACY_PROVIDER_EXISTING_KEY_BEHAVIOR = INSERT_ONLY
CURRENT_IMPLEMENTATION_MATCHES_LEGACY = YES
LEGACY_056 = IMPLEMENTED_REDESIGNED
CLIENT_PROVIDER_UPDATE_FEATURE = NOT_A_LEGACY_REQUIREMENT
CALE_IMMEX_WRITES = 0
MUTABLE_LEGACY_EXECUTIONS = 0
```

La capacidad se describe como importación preservando claves existentes, no como
actualización de registros. Las secciones iniciales `UNKNOWN` documentan el
estado previo a esta reauditoría y no representan el estado final V1.
