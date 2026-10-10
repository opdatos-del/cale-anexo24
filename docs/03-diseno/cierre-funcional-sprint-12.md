# Cierre funcional Sprint 12 — discovery y alcance final V1

## Resultado

Sprint 12 no reabre capacidades previamente congeladas sin evidencia nueva. Se
implementó únicamente la subcapacidad demostrable de Inventario inicial de
Anexo 30. No se crearon tablas, índices, FK ni se ejecutaron procesos mutables.

```text
SPRINT = FINAL_FUNCTIONAL_CLOSURE_12
SUBAGENTS_AVAILABLE = NO
DISCOVERY = SEQUENTIAL
CALE_IMMEX_NEW_TABLES = 0
ANEXO24_DEV_NEW_TABLES = 0
ALTERED_TABLES = 0
NEW_FKS = 0
NEW_INDEXES = 0
CALE_IMMEX_WRITES = 0
LIVE_CALE_MUTABLE_EXECUTIONS = 0
LIVE_CALE_DDL = 0
```

## Discovery LIVE

Conexión read-only realizada contra `CALE_IMMEX` con `sqlcmd`. Se consultaron
`sys.objects`, `sys.tables`, `sys.columns`, `sys.sql_modules`,
`sys.sql_expression_dependencies`, `OBJECT_DEFINITION` y agregados de filas.
No se imprimieron valores fiscales completos.

### Datos generales — LEGACY-001

- `dbo.DatosGenerales` existe; cardinalidad observada: 1 fila.
- No existe PK/índice único que fuerce singleton.
- Fuente read-only y mapping UI siguen confirmados.
- Búsqueda de `UPDATE/INSERT/DELETE DatosGenerales` no encontró writer visible;
  `SP_GENERA_TXT_COMPLETO` aparece como referencia de proceso, no como contrato
  de mantenimiento de ficha.
- No existe contrato demostrado de campos editables, validación, permiso de
  guardado, concurrencia ni writer legacy.

```text
LEGACY_001 = PARTIAL_ACCEPTED_V1
GENERAL_DATA_EDIT_CONTRACT = BLOCKED_EVIDENCE
```

No se agrega `PUT`, no se inventa actualización directa.

### Clientes/proveedores — LEGACY-056

Definiciones LIVE completas auditadas:

- `dbo.CARGACLIENTES`: `INSERT` + `TRUNCATE`; no `UPDATE`; inserta sólo si no
  existe la clave en `clientes` ni en los errores del lote.
- `dbo.CARGAPROVEEDORES`: `INSERT` + `TRUNCATE`; no `UPDATE`; inserta sólo si no
  existe la clave en `Proveedores` ni en los errores del lote.
- Ambos procesos son mutables y no fueron ejecutados.

La implementación moderna de confirmación preserva claves existentes y confirma
altas válidas nuevas. Por tanto no falta una actualización: el comportamiento
legacy demostrado es INSERT-only.

```text
LEGACY_CLIENT_EXISTING_KEY_BEHAVIOR = INSERT_ONLY
LEGACY_PROVIDER_EXISTING_KEY_BEHAVIOR = INSERT_ONLY
CURRENT_IMPLEMENTATION_MATCHES_LEGACY = YES
LEGACY_056 = IMPLEMENTED_REDESIGNED
```

### Anexo 30 — LEGACY-073

Objetos LIVE confirmados:

- `dbo.INVENTARIOINICIAL`: VIEW read-only, 7 columnas, definición completa.
- Dependencias: `dbo.v_saldos` y `dbo.importaciones`.
- `dbo.v_saldos`: VIEW read-only; proyecta saldos persistidos desde
  `Importaciones`/`partidas`, sin DML propio.
- Snapshot actual A31 observado con 0 filas en tablas A31 auditadas.
- `dbo.A31_SALDOS`: procedimiento mutable con `INSERT` en `A31_DESCARGAS` y
  `UPDATE` sobre `A31_ENTRADAS.SALDO`; no se ejecutó.

`INVENTARIOINICIAL` tiene grano explícito por `documento`, fecha y fracción, y
proyección estable: patente, número de pedimento, clave de sección aduanera,
fecha de selección, fracción, valor comercial histórico e indicador de activo
fijo. Se implementó consulta read-only paginada y exportación XLSX; filtro sólo
sobre columnas proyectadas.

```text
ANEXO30_INITIAL_INVENTORY = IMPLEMENTED_READ_ONLY_LAST_SNAPSHOT
ANEXO30_BALANCES = BLOCKED_BUSINESS
ANEXO30_EXPIRATIONS = BLOCKED_EVIDENCE
ANEXO30_MUTABLE_SP_EXECUTIONS = 0
LEGACY_073 = PARTIAL_ACCEPTED_V1
```

Las lecturas previas de entradas, fracciones, descargas y comparativa permanecen
read-only. No se afirma actualidad: el snapshot LIVE actual está vacío.

## SP y permisos

Nuevo SP necesario porque no existía query reusable para la vista:

```text
dbo.APP24_Q_ANEXO30_INVENTARIO_INICIAL_LISTAR
```

Contrato: filtro textual opcional, página 1-based, tamaño 1..100, total OUTPUT,
proyección explícita y orden determinista. Runtime CALE cambia sólo por este
SP:

```text
RUNTIME_CALE_SP_BEFORE = 38
RUNTIME_CALE_SP_AFTER = 39
RUNTIME_CALE_SP_DELTA = +1
```

No se otorgaron permisos de tabla, schema, `CONTROL`, `db_owner` ni
`db_datareader`.

## Capacidades congeladas sin implementación

- Cambios de régimen y regularizaciones: `BLOCKED_EVIDENCE`; fuente, mapping,
  discriminador y contrato read-only no demostrados.
- Saldos fiscales operativos y motor de descargos: `BLOCKED_BUSINESS`; fórmula,
  corte, autorización, rollback e idempotencia pendientes; candidatos mutables.
- Facturación confirmation: `BLOCKED_BUSINESS`; forensic legacy incompleto.
- CTM/submaquila, scrap, ajuste anual, generación Anexo 30, worksheet/TXT y
  servicios/órdenes/procesos: `BLOCKED_BUSINESS` o `BLOCKED_EVIDENCE` según
  ausencia de regla o fuente. No se inventan endpoints.

## Counters de seguridad

```text
INLINE_BUSINESS_SQL_JAVA = 0
LIVE_CALE_METADATA_READS = 38 read-only batches
LIVE_CALE_DATA_READS = aggregate counts only
LIVE_CALE_READ_ONLY_SP_EXECUTIONS = 0
LIVE_CALE_MUTABLE_EXECUTIONS = 0
LIVE_CALE_WRITES = 0
LIVE_CALE_DDL = 0
```

## P0 final

```text
PEDIMENT_VALIDATION_V1 = PARTIAL_ACCEPTED_V1
  blocker: PED-005/PED-006 requieren INVENTARIO no disponible como contrato legacy.
SALDOS_OPERATIONAL = BLOCKED_BUSINESS
  blocker: fórmula, granularidad y corte fiscal no aprobados.
AUTO_DISCHARGE = BLOCKED_BUSINESS
  blocker: algoritmo PEPS, autorización, rollback e idempotencia no aprobados.
BILLING_CONFIRMATION = BLOCKED_BUSINESS
  blocker: entrypoint, mapping, atomicidad, retry y side effects legacy no cerrados.
```

Estado de cierre funcional: `IMPLEMENTATION_COMPLETE = YES` para trabajo
implementable demostrado; `READY_FOR_QA_HARDENING = YES`;
`READY_FOR_FINAL_PARITY_AUDIT = NO` hasta completar QA y auditoría final.
