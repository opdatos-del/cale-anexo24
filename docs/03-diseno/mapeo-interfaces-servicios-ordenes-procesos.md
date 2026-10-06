# Interfaces especializadas - discovery SP-FIRST (Servicios, Ordenes de fabricacion, Procesos)

## Alcance y controles

Discovery read-only de LEGACY-059, LEGACY-061 y LEGACY-062. No se crean endpoint, permiso, UI, stored procedure ni migracion.

- LIVE writes = 0.
- LIVE mutable executions = 0.
- SQL de negocio inline en Java = 0.
- Nuevo query SP = 0.
- Nuevo business SP = 0.
- LIVE reads = 0 (sin credencial autorizada).

## Disponibilidad de evidencia LIVE

Sesion con falta de credenciales para CALE_IMMEX (autenticacion integrada opdatos sin permiso); no se ejecuta SELECT ni metadata.

```text
CURRENT_LIVE_REVALIDATION = NOT_AVAILABLE
VERSIONED_PRIOR_EVIDENCE = AVAILABLE
```

## Inventario versionado

### Procedimientos (docs/03-diseno/procedimientos-almacenados.md)

- Familia CARGA documentada: CARGA_SUBMAQUILA, CARGAACTAS, CARGA_MATERIALES, CARGA_PRODUCTOS, CARGA_FACTURAS, CARGACONSTANCIAS, CARGAPEDIMENTOS, CARGA_ENCABEZADOS, CARGA_ESTRUCTURADESENSAMBLE, CARGAAgentes, CARGACLIENTES, CARGAPROVEEDORES, CARGAFACTURASENPSALIDAS. Ningun CARGA_SERVICIOS, CARGA_ORDENES o CARGA_PROCESOS aparece.
- Familia PR_INFORME: cubre importaciones, exportaciones, estructuras y saldos. No hay PR_INFORME_SERVICIOS.
- Familia INSERT: pedimentos, dirigidos y facturaciones.
- Resto: descargos, saldos, compulsa, CTM, A31, compulsa DS y reportes; sin coincidencias con los tres IDs.

### Archivos fisicos

Sin archivos dedicados en backend/src/test/resources, docs o infra/sql ademas de los ya clasificados.

### Codigo y frontend

Busquedas sobre Servicio, ServicioImport, Orden Fabric, Order Manufact, Proceso, ProcessImport, UUID en backend/src/main y frontend/src/app devuelven solo coincidencias genericas (errores, traducciones, bitacora). No hay paquetes dedicados.

### Matriz y backlog

Los tres IDs siguen NOT_CAPTURED en la matriz y P2 en el backlog.
## LEGACY-059 - Servicios

| Campo | Evidencia / estado |
|---|---|
| Nombre funcional | UNKNOWN: solo mencionado en el backlog |
| Pantalla | UNKNOWN |
| Ruta ASPX | NOT_CAPTURED / UNKNOWN |
| Menu | UNKNOWN |
| Proposito visible | UNKNOWN |
| Grano | UNKNOWN |
| Filtros / rango / documento | UNKNOWN |
| Columnas / orden / acciones | UNKNOWN |
| Exportacion | UNKNOWN |
| Comportamiento vacio / con datos | UNKNOWN |
| Fuente SQL | NOT_FOUND: sin CARGA_SERVICIOS, PR_INFORME_SERVICIOS, dbo.servicios o V_SERVICIOS |
| SP legacy reutilizable | NONE |
| Stage / layout | NOT_FOUND |
| Writers | UNKNOWN |
| Dependencias | UNKNOWN |

```text
LEGACY-059_SCREEN_CONTRACT_CONFIRMED = NO
LEGACY-059_SOURCE_CONTRACT_CONFIRMED = NO
LEGACY-059_INPUT_OR_READ_CONTRACT_CONFIRMED = NO
LEGACY-059_GRAIN_CONFIRMED = NO
LEGACY-059_FILTER_CONTRACT_CONFIRMED = NO
LEGACY-059_MUTABLE_PRECONDITION_REQUIRED = UNKNOWN
LEGACY_059 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
```

La pista cualitativa (montos, impuestos, moneda/tipo de cambio, paises) no es contrato; cualquier implementacion inventaria reglas fiscales.
## LEGACY-061 - Ordenes de fabricacion

| Campo | Evidencia / estado |
|---|---|
| Nombre funcional | UNKNOWN: solo mencionado en el backlog |
| Pantalla | UNKNOWN |
| Ruta ASPX | NOT_CAPTURED / UNKNOWN |
| Menu | UNKNOWN |
| Proposito visible | UNKNOWN: dominio reusa campos semanticos de descarga/saldo ya excluidos |
| Grano | UNKNOWN |
| Filtros / rango / documento | UNKNOWN |
| Columnas / orden / acciones | UNKNOWN |
| Exportacion | UNKNOWN |
| Comportamiento vacio / con datos | UNKNOWN |
| Fuente SQL | NOT_FOUND: sin CARGA_ORDEN, PR_INFORME_ORDEN, dbo.ordenes o V_ORDEN |
| SP legacy reutilizable | NONE |
| Stage / layout | NOT_FOUND |
| Writers | UNKNOWN |
| Dependencias | UNKNOWN |

```text
LEGACY-061_SCREEN_CONTRACT_CONFIRMED = NO
LEGACY-061_SOURCE_CONTRACT_CONFIRMED = NO
LEGACY-061_INPUT_OR_READ_CONTRACT_CONFIRMED = NO
LEGACY-061_GRAIN_CONFIRMED = NO
LEGACY-061_FILTER_CONTRACT_CONFIRMED = NO
LEGACY-061_MUTABLE_PRECONDITION_REQUIRED = UNKNOWN
LEGACY_061 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
```
## LEGACY-062 - Procesos

| Campo | Evidencia / estado |
|---|---|
| Nombre funcional | UNKNOWN: solo mencionado como importar procesos |
| Pantalla | UNKNOWN |
| Ruta ASPX | NOT_CAPTURED / UNKNOWN |
| Menu | UNKNOWN |
| Proposito visible | UNKNOWN: pista cualitativa (UUID, archivo, usuario, estatus, tipo, fechas) sin respaldo SQL |
| Grano | UNKNOWN |
| Filtros / rango / documento | UNKNOWN |
| Columnas / orden / acciones | UNKNOWN |
| Exportacion | UNKNOWN |
| Comportamiento vacio / con datos | UNKNOWN |
| Fuente SQL | NOT_FOUND: sin CARGA_PROCESOS, PR_INFORME_PROCESOS, dbo.procesos o V_PROCESOS |
| SP legacy reutilizable | NONE |
| Stage / layout | NOT_FOUND |
| Writers | UNKNOWN |
| Dependencias | UNKNOWN |

```text
LEGACY-062_SCREEN_CONTRACT_CONFIRMED = NO
LEGACY-062_SOURCE_CONTRACT_CONFIRMED = NO
LEGACY-062_INPUT_OR_READ_CONTRACT_CONFIRMED = NO
LEGACY-062_GRAIN_CONFIRMED = NO
LEGACY-062_FILTER_CONTRACT_CONFIRMED = NO
LEGACY-062_MUTABLE_PRECONDITION_REQUIRED = UNKNOWN
LEGACY_062 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
```

La pista cualitativa (UUID, estatus, fechas) es estructural y no basta para declarar contrato. Sin layout, campos ni reglas no hay diseno implementable.
## Gate de implementacion por capacidad

| Requisito | LEGACY-059 | LEGACY-061 | LEGACY-062 |
|---|---|---|---|
| SCREEN_CONTRACT_CONFIRMED | NO | NO | NO |
| INPUT_OR_READ_CONTRACT_CONFIRMED | NO | NO | NO |
| SOURCE_CONTRACT_CONFIRMED | NO | NO | NO |
| GRAIN_CONFIRMED | NO | NO | NO |
| FILTER_CONTRACT_CONFIRMED | NO | NO | NO |
| VALIDATION_CONTRACT_CONFIRMED | NO | NO | NO |
| MUTABLE_PRECONDITION_REQUIRED = NO | UNKNOWN | UNKNOWN | UNKNOWN |
| Implementable ahora | NO | NO | NO |

### Blocker separado por capacidad

LEGACY-059: no se localiza fuente SQL, layout, stage, SP, view ni permiso.

LEGACY-061: no se localiza fuente SQL, layout, stage, SP, view ni permiso; el dominio reusa campos semanticos ya excluidos.

LEGACY-062: no se localiza fuente SQL, layout, stage, SP, view ni permiso; el patron de staging propio existe para facturacion y pedimentos pero no es reutilizable sin contrato.

Sin wrappers APP24_Q ni procedimientos de negocio: NEW_QUERY_SP = 0 y NEW_BUSINESS_SP = 0.
## Estado

LEGACY_059 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_061 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
LEGACY_062 = BLOCKED_NO_SOURCE_FOUND_IN_VERSIONED_EVIDENCE
NEW_QUERY_SP = 0
NEW_BUSINESS_SP = 0
INLINE_BUSINESS_SQL_JAVA = 0
LIVE_ACTIVATION_PENDING = YES
LIVE reads = 0
LIVE writes = 0
LIVE mutable executions = 0
