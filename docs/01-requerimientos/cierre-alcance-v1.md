# Cierre de alcance V1 — paquete de congelación

Este documento consolida la auditoría de cierre posterior a la integración de la
confirmación autoritativa de pedimentos en `dev`. Es **documental**: no modifica
Java, Angular, SQL, procedimientos ni migraciones, y no ejecuta procesos
mutables.

Fuentes auditadas: `matriz-paridad-legacy-v1.md`,
`decisiones-alcance-v1.md`, `evidencias-proyecto.md`,
`mapeo-facturacion.md`, `mapeo-descargos.md`, `mapeo-saldos.md` y la evidencia
CI de `dev` (`a64434a`, run `37014148994`).

`V1_COMPLETE` y `V1_SCOPE_FROZEN` permanecen en `NO` hasta la respuesta de
negocio y la aprobación formal.

---

## 1. Métricas verificadas (recalculadas desde archivo)

Conteos recalculados fila por fila en `matriz-paridad-legacy-v1.md`
(79 filas, sin valores asumidos).

| Métrica | Valor |
|---|---:|
| `LEGACY_CAPABILITIES_TOTAL` | 79 |
| `IMPLEMENTED_EQUIVALENT` | 5 |
| `IMPLEMENTED_REDESIGNED` | 26 |
| `PARTIAL` | 11 |
| `MISSING` | 6 |
| `BLOCKED_BUSINESS` | 5 |
| `CONSOLIDATE` | 6 |
| `UNKNOWN` | 20 |

| Prioridad | Total | Implementadas | Parciales | Faltantes | Bloqueadas | Consolidadas | Desconocidas |
|---|---:|---:|---:|---:|---:|---:|---:|
| P0 | 18 | 13 (4 equiv + 9 rediseñadas) | 2 | 0 | 3 | 0 | 0 |
| P1 | 40 | 14 (1 equiv + 13 rediseñadas) | 7 | 5 | 2 | 3 | 9 |
| P2 | 20 | 4 | 2 | 1 | 0 | 3 | 10 |
| P3 | 1 | 0 | 0 | 0 | 0 | 0 | 1 |

Métricas P0 (matriz):

- `P0_TOTAL = 18`
- `P0_IMPLEMENTED = 13`
- `P0_PARTIAL = 2`
- `P0_BLOCKED_BUSINESS = 3`
- `P0_MISSING = 0`
- `P0_NON_MATRIX_BLOCKED = 18 - 3 = 15`
- `P0_NON_MATRIX_BLOCKED_COMPLETION = 13/15 = 86.7 %`

Métricas operativas P0:

- `P0_PARTIAL_REQUIRING_BUSINESS_DECISION = 2` (`LEGACY-017`, `LEGACY-058`)
- `P0_ACTIONABLE_WITHOUT_BUSINESS_DECISION = 13`
- `P0_ACTIONABLE_IMPLEMENTED = 13`
- `P0_ACTIONABLE_COMPLETION = 13/13 = 100 %`

Explicación: `P0_ACTIONABLE_COMPLETION = 100 %` **no** significa
`V1_COMPLETE = YES`. Significa únicamente que no queda capacidad P0 funcional
implementable sin una decisión o contrato externo. Las cinco capacidades P0
restantes (`LEGACY-017`, `021`, `029`, `038`, `058`) requieren decisión de
negocio; las dos parciales (`017`, `058`) ya cuentan con staging y validación
entregados.

---

## A. Capacidades ya cerradas

Cerradas y probadas con evidencia CI real (`dev`, run `37014148994`):

- Importación autoritativa de pedimentos (`LEGACY-057`): staging → validación →
  confirmación `IMPORTACIONES`/`PARTIDAS`, transacción única, idempotencia,
  `ALREADY_CONFIRMED`, `DUPLICATE_OPERATION`, rollback total.
- Carga/staging de pedimentos (`LEGACY-016`) y exportaciones (`LEGACY-019`):
  carga, parser, validación, preview, errores, hash, RBAC y confirmación
  (`SALIDAS`/`PSALIDAS`/`DIRIGIDO` condicional).
- Consulta read-only: materiales, productos, estructuras, entradas, salidas,
  materiales utilizados, activos fijos, catálogos auxiliares y socios
  comerciales.
- Reportes V1 con exportación XLSX (entradas, salidas, materiales utilizados,
  bitácora, análisis de descargas, vencimientos parciales, compulsa y
  rectificaciones parciales).
- Administración: login, usuarios, perfiles, permisos, bitácora.
- Carga y validación de facturación hasta staging durable (`LEGACY-058`,
  parcial por diseño: sin confirmación operativa).
- Staging y preview de importación de materiales y productos (`LEGACY-054/055`,
  parcial por diseño: sin confirmación a catálogo).

---

## B. P0 restante

Cinco capacidades P0 sin cierre. Ninguna tiene trabajo técnico interno
pendiente: todas requieren respuesta de negocio o fuente externa.

| ID | Capacidad | Estado | Evidencia | Blocker | Acción restante | Tipo |
|---|---|---|---|---|---|---|
| LEGACY-017 | Validación de pedimentos (PED-005/PED-006) | PARTIAL | `matriz-paridad-legacy-v1.md`; PED-001..004 y PED-007 implementadas | `INVENTARIO` no existe en LIVE; falta fuente/regla de inventario | Definir fuente y regla de inventario para PED-005/006 y luego implementar | BUSINESS_DECISION_REQUIRED |
| LEGACY-021 | Consulta de saldos con semántica fiscal | BLOCKED_BUSINESS | `decisiones-alcance-v1.md` §1; `mapeo-saldos.md` | Fórmula, fuentes, granularidad y corte pendientes | Responder preguntas de Saldos; caso de aceptación | BUSINESS_DECISION_REQUIRED |
| LEGACY-029 | Motor de descargos / PEPS | BLOCKED_BUSINESS | `matriz-paridad-legacy-v1.md`; `mapeo-descargos.md` §12 | Algoritmo, autorización, rollback e idempotencia pendientes | Decidir alcance V1 del motor y reglas | BUSINESS_DECISION_REQUIRED |
| LEGACY-038 | Reporte de saldos | BLOCKED_BUSINESS | Misma decisión que LEGACY-021 | Depende de la definición de Saldos | Reutiliza la respuesta de Saldos | BUSINESS_DECISION_REQUIRED |
| LEGACY-058 | Confirmación operativa de facturación | PARTIAL | `decisiones-alcance-v1.md` §2; `mapeo-facturacion.md` | Pipeline autoritativo y side effects pendientes de negocio | Responder preguntas de Facturación; caso de aceptación | BUSINESS_DECISION_REQUIRED |

Resumen:

- `P0_BUSINESS_DECISION_REMAINING = 5` (todos `BUSINESS_DECISION_REQUIRED`)
- `P0_TECHNICAL_REMAINING_WITH_CURRENT_CONTRACTS = 0`

---

## C. Decisiones de negocio pendientes

| Tema | Estado actual | Documento de detalle |
|---|---|---|
| `SALDOS` | `PENDING_BUSINESS` | `decisiones-alcance-v1.md` §1 |
| `FACTURACION_CONFIRM` | `PENDING_BUSINESS` | `decisiones-alcance-v1.md` §2 |
| `DASHBOARD_V1` | `PENDING_BUSINESS` | `decisiones-alcance-v1.md` §3 |
| Descargos / PEPS | `BLOCKED_BUSINESS` (sin paquete de preguntas propio) | `mapeo-descargos.md` §12-13 |

Brecha detectada: Descargos/PEPS está bloqueado en la matriz pero no tiene
sección de decisión equivalente a las otras tres. El paquete de la sección 9
añade las preguntas mínimas.

---

## D. P1 propuesto

`P1_TOTAL = 40`; `P1_UNFINISHED = 26`. `MATRIX_STATE` (columna Estado de la
matriz) y `RECOMMENDED_DISPOSITION` son cosas distintas: una fila puede estar
`PARTIAL`, `MISSING`, `UNKNOWN` o `CONSOLIDATE` en la matriz y su disposición
recomendada ser `BUSINESS_DECISION_REQUIRED`. Esta fase no reclasifica estados
de matriz; la disposición de revisión se expresa únicamente como
`BUSINESS_DECISION_REQUIRED`.

| Disposición recomendada | IDs | Cantidad |
|---|---|---:|
| IMPORTANT_FOR_V1 | — (ninguno con evidencia de obligatoriedad) | 0 |
| BUSINESS_DECISION_REQUIRED | 001, 025, 026, 028, 030, 042, 050, 052, 054, 055, 056, 060, 077 | 13 |
| POST_V1_CANDIDATE | 031, 032, 046, 049 | 4 |
| UNKNOWN_NEEDS_AUDIT | 023, 024, 064, 065, 069, 070, 071, 072, 073 | 9 |

Detalle:

| ID | Capacidad | MATRIX_STATE | RECOMMENDED_DISPOSITION | Nota |
|---|---|---|---|---|
| LEGACY-001 | Datos generales — edición/mantenimiento | PARTIAL | BUSINESS_DECISION_REQUIRED | Confirmar si V1 exige mantenimiento |
| LEGACY-025 | Actas de destrucción (operación) | MISSING | BUSINESS_DECISION_REQUIRED | Layout y reglas sin cerrar |
| LEGACY-026 | Transferencias de submaquila | MISSING | BUSINESS_DECISION_REQUIRED | Contrato y reglas fiscales |
| LEGACY-028 | CTM (consulta/proceso) | MISSING | BUSINESS_DECISION_REQUIRED | Flujo especializado |
| LEGACY-030 | Descargo dirigido | BLOCKED_BUSINESS | BUSINESS_DECISION_REQUIRED | Regla dirigida y separación de funciones |
| LEGACY-042 | Consolidado de saldos | BLOCKED_BUSINESS | BUSINESS_DECISION_REQUIRED | Depende de Saldos |
| LEGACY-050 | Reporte de permisos | CONSOLIDATE | BUSINESS_DECISION_REQUIRED | Confirmar consolidación en Perfiles |
| LEGACY-052 | Reporte de activo fijo | CONSOLIDATE | BUSINESS_DECISION_REQUIRED | Confirmar consolidación |
| LEGACY-054 | Importación de materiales (confirmación) | PARTIAL | BUSINESS_DECISION_REQUIRED | Falta pipeline de confirmación a catálogo |
| LEGACY-055 | Importación de productos (confirmación) | PARTIAL | BUSINESS_DECISION_REQUIRED | Falta pipeline de confirmación a catálogo |
| LEGACY-056 | Importación clientes/proveedores | MISSING | BUSINESS_DECISION_REQUIRED | Efectos compartidos con facturación |
| LEGACY-060 | Importación de actas de destrucción | MISSING | BUSINESS_DECISION_REQUIRED | Layout y reglas |
| LEGACY-077 | Pantalla separada de actividades | CONSOLIDATE | BUSINESS_DECISION_REQUIRED | Confirmar si negocio exige pantalla propia |
| LEGACY-031 | Descargo bloqueado (resolver) | PARTIAL | POST_V1_CANDIDATE | Read-only existente; resolver fuera de V1 |
| LEGACY-032 | Análisis de descarga (faltantes/trazo) | PARTIAL | POST_V1_CANDIDATE | Extensión read-only factible |
| LEGACY-046 | Vencimientos (alcance completo) | PARTIAL | POST_V1_CANDIDATE | Ampliación read-only factible |
| LEGACY-049 | Dirigidos (generación/PEPS) | PARTIAL | POST_V1_CANDIDATE | Consulta existente; generación fuera |
| LEGACY-023 | Cambios de régimen | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Auditoría funcional previa |
| LEGACY-024 | Regularizaciones | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Auditoría funcional previa |
| LEGACY-064 | Carga ajuste anual | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Layouts y reglas |
| LEGACY-065 | Informe ajuste anual | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Definición de resultado |
| LEGACY-069 | Informe Anexo 30 | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Reglas regulatorias |
| LEGACY-070 | Parámetros Anexo 30 | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Semántica de filtros |
| LEGACY-071 | Worksheet y errores Anexo 30 | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Flujo y fuente |
| LEGACY-072 | TXT y Excel Anexo 30 | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Formato y seguridad |
| LEGACY-073 | Revisión Anexo 30 | UNKNOWN | UNKNOWN_NEEDS_AUDIT | Reglas y aceptación |

No se asume que todo P1 deba implementarse en V1.

---

## E. POST-V1 propuesto

- Motor de descargos automático y dirigido (`LEGACY-029`, `LEGACY-030`), PEPS
  y saldos mutables: sólo tras decisión de negocio y caso de aceptación.
- Confirmación operativa de facturación (`LEGACY-058`) y confirmación de
  importaciones de catálogo (`LEGACY-054/055`): mismo patrón que pedimentos.
- Extensiones read-only de descargos: resolución de bloqueados, faltantes y
  trazo, vencimientos completo, compulsa detallada, rectificaciones detalladas.
- Importaciones especiales: actas de destrucción, transferencias de submaquila,
  CTM, clientes/proveedores, servicios, órdenes de fabricación, procesos.
- Ajuste anual y Anexo 30: requieren auditoría funcional y regulatoria previa.
- CRUD de catálogos auxiliares y datos generales, si negocio lo confirma.
- Confirmación de facturación e importaciones de catálogo comparten el patrón
  ya probado: staging → validación → command autoritativo → API → UI.

---

## F. UNKNOWN que requiere auditoría adicional

`UNKNOWN_TOTAL = 20`.

Distribución exacta:

- `UNKNOWN_AFFECTS_V1_CONFIRMED = 0`
- `UNKNOWN_POST_V1 = 10`: `LEGACY-014`, `LEGACY-041`, `LEGACY-048`,
  `LEGACY-059`, `LEGACY-061`, `LEGACY-062`, `LEGACY-063`, `LEGACY-066`,
  `LEGACY-067`, `LEGACY-068`.
- `UNKNOWN_UNDECIDED = 10`: `LEGACY-012`, `LEGACY-023`, `LEGACY-024`,
  `LEGACY-064`, `LEGACY-065`, `LEGACY-069`, `LEGACY-070`, `LEGACY-071`,
  `LEGACY-072`, `LEGACY-073`.

| Grupo | IDs | Disposición |
|---|---|---|
| Catálogos | 014 | POST_V1 |
| Catálogos | 012 | UNDECIDED |
| Operaciones especiales | 023, 024 | UNDECIDED |
| Reportes | 041, 048 | POST_V1 |
| Importaciones especiales | 059, 061, 062, 063 | POST_V1 |
| Ajuste anual | 066, 067, 068 | POST_V1 |
| Ajuste anual | 064, 065 | UNDECIDED |
| Anexo 30 | 069, 070, 071, 072, 073 | UNDECIDED |

Ningún UNKNOWN es requisito automático de V1; los UNDECIDED requieren respuesta
de negocio para decidir si entran a V1 o pasan a POST_V1.

---

## G. Criterios para `V1_COMPLETE = YES`

1. Respuestas de negocio registradas para Saldos, Facturación, Dashboard y
   Descargos/PEPS (sección 9).
2. Decisiones clasificadas `MUST_HAVE_V1` implementadas y probadas con caso de
   aceptación, o reclasificadas formalmente a `POST_V1`/`NOT_REQUIRED`.
3. `TARGET_LEAST_PRIVILEGE_MIGRATION` completado o aceptado explícitamente como
   pendiente controlado (sección 12).
4. CI verde en `dev` con SQL IT y concurrencia sin skips.
5. Sin blockers técnicos internos abiertos (`TECHNICAL_INTERNAL_BLOCKERS = 0`).
6. Evidencia del reporte de estadía completa (sección 13).

`V1_COMPLETE = NO` mientras 1-2 no se cumplan.

---

## 9. Paquete de decisiones para reunión con negocio

Máximo 15 preguntas. Cada respuesta debe clasificarse además como
`MUST_HAVE_V1`, `POST_V1` o `NOT_REQUIRED`; para `MUST_HAVE_V1` se debe
indicar fuente, regla y caso esperado.

### Facturación (LEGACY-058)

1. Después del preview aprobado, ¿qué paso o botón confirma y qué proceso
   autoritativo debe ejecutar (flujo A, B, C, CTM, adaptador nuevo u omitir)?
2. ¿Qué tablas o registros deben cambiar y en qué pantalla o reporte se
   verifica el resultado?
3. ¿Qué reglas aplican a productos o clientes faltantes durante la carga?
4. ¿Qué comportamiento se exige ante duplicados, reintentos de archivo
   corregido, errores parciales y rollback?
5. ¿Pueden proporcionar la plantilla autorizada vigente y un archivo
   anonimizado de una a tres filas con resultado esperado?

### Saldos (LEGACY-021 / LEGACY-038)

6. ¿Qué representa exactamente "Saldo" y cuál es la fila oficial de un reporte
   (partida, material, pedimento, fracción u otra)?
7. ¿Cuál es la fórmula y las fuentes autoritativas (saldo inicial, entradas,
   consumo, salidas, retornos, ajustes) y cuál es la fecha/corte?
8. ¿Qué unidades, conversiones y redondeo aplican, y cómo se tratan
   cancelaciones, mermas, desperdicios y activos fijos?
9. ¿Cuál es la fuente oficial autorizada y pueden entregarse dos o tres casos
   anonimizados para reconciliación?

### Descargos / PEPS (LEGACY-029 / LEGACY-030)

10. ¿El motor de descargos (automático y dirigido) es `MUST_HAVE_V1`,
    `POST_V1` o `NOT_REQUIRED`?
11. Si aplica: ¿cuál es el algoritmo oficial (PEPS u otro), la idempotencia y
    el rollback esperados?
12. ¿Requiere separación de funciones o aprobación dual, y quién autoriza el
    caso de aceptación?

### Dashboard (LEGACY-DASHBOARD)

13. ¿El dashboard actual (saludo, accesos por permiso, avisos, estado y total
    de materiales) es suficiente para V1? Si sí, se cierra como
    `OUT_OF_SCOPE_V1` para ampliaciones.
14. Si no: ¿qué métricas candidatas son `MUST_HAVE_V1`, `POST_V1` o
    `NOT_REQUIRED`?
15. Para cada métrica `MUST_HAVE_V1`: ¿fórmula, fuente, periodo, permiso,
    frecuencia y comportamiento sin datos?

---

## 10. MISSING (6) — detalle

`MISSING_TOTAL = 6`; `MISSING_P0 = 0`, `MISSING_P1 = 5`, `MISSING_P2 = 1`.
Ninguna se implementa en esta fase.

| ID | Capacidad | Prio | Evidencia | Alcance probable | Relevancia | Disposición recomendada |
|---|---|---|---|---|---|---|
| LEGACY-025 | Actas de destrucción (operación) | P1 | `CARGAACTAS`, `DescargaDesp` | Carga y efectos de actas | Fiscal/destrucción | BUSINESS_DECISION_REQUIRED → POST_V1 |
| LEGACY-026 | Transferencias de submaquila | P1 | `CARGA_SUBMAQUILA`, CTM | Transferencia fiscal entre submaquilas | Fiscal | BUSINESS_DECISION_REQUIRED → POST_V1 |
| LEGACY-027 | Constancias | P2 | `CARGACONSTANCIAS` | Carga de constancias | Media | POST_V1 |
| LEGACY-028 | CTM (consulta/proceso) | P1 | `CTMDESCARGA`, `SALDOSCTM` | Consulta y proceso CTM | Fiscal | BUSINESS_DECISION_REQUIRED → POST_V1 |
| LEGACY-056 | Importar clientes/proveedores | P1 | `CARGA_FACTURAS` y catálogos | Alta/actualización de socios | Productividad | BUSINESS_DECISION_REQUIRED (comparte efectos con facturación) |
| LEGACY-060 | Importar actas de destrucción | P1 | `CARGAACTAS` | Carga de actas | Fiscal | BUSINESS_DECISION_REQUIRED → POST_V1 |

---

## 11. Separación read-only vs motor mutable (Saldos y Descargos)

- Read-only existente y probado: materiales utilizados (filas físicas de
  `DESCARGA`), análisis de descargas, operaciones bloqueadas (snapshot),
  dirigidos (líneas marcadas), vencimientos (subconjunto de desperdicio),
  compulsa y rectificaciones (resúmenes).
- Motor mutable NO ejecutado y fuera de V1: `DESCARGATSALIDA*`,
  `DESCARGASALIDAPEPS`, `DESCARGAXFECHA51`, `SALDOS*`, `DESCDIRIGIDA`,
  `SALDOSDIRIGIDOS`, `INFORME_CONCENTRADOSALDOS`.
- `PARTIDAS.Saldo` es valor persistido; su recálculo pertenece al motor.
- No se infiere PEPS, saldo fiscal, descargo automático ni reprocesamiento.

---

## 12. Hardening técnico (fase independiente)

No es paridad funcional; lista para planificar por separado:

| Tema | Estado |
|---|---|
| Runtime least privilege (`anexo24_app` en lugar de `opdatos`/`dbo`) | `TARGET_LEAST_PRIVILEGE_MIGRATION = PENDING` |
| Revisión de grants por conexión (CALE_IMMEX / ANEXO24_DEV) | Pendiente; sin GRANT improvisado |
| Staging / prod promotion (`dev` → `staging` → `main`) | CI por rama existe; promoción pendiente |
| Deployment checklist (migrations idempotentes, DDL verificado) | Parcial: usado para pedimentos; falta formalizar para releases |
| Backups / rollback operativo | Pendiente de definir |
| Observabilidad (logs, correlación, métricas de command) | `correlationId` reutilizado; métricas pendientes |

---

## 13. Reporte de estadía — preparación

Capítulos con evidencia suficiente (`docs/08-reporte-estadia/evidencias-proyecto.md`):

- Método: auditoría funcional legacy, análisis de SP y call graph, arquitectura
  hexagonal, staging aislado, SP-FIRST con gate automatizado, Testcontainers.
- Resultados: cobertura 61/61 (import) y 45/45 (export); SQL IT 15/15; 
  concurrencia 2/2; frontend 113 tests; matriz HTTP 401/403/200/404/409/422;
  rollback 0 escrituras parciales; cargas mixtas PASS; SP-FIRST 0 violations;
  DDL LIVE aplicado con 0 ejecuciones de command y 0 escrituras de negocio.

Evidencia cerrada:

- CI final de `dev`: run `37014148994` @ `a64434a` — SUCCESS
  (sp-first-gate, backend, frontend), registrado en `evidencias-proyecto.md`.
- Paridad vigente: 5 equivalentes + 26 rediseñadas + 11 parciales + 6 faltantes
  + 5 bloqueadas + 6 consolidadas + 20 desconocidas = 79.
- Inconsistencia «paridad sin cambios» de `evidencias-proyecto.md` corregida en
  el commit `1fa2e3b`.

Pendientes reales:

- anexos todavía no producidos;
- redacción final de capítulos del reporte;
- decisiones empresariales necesarias para congelar alcance.

Estado de preparación del reporte:

- `REPORT_EVIDENCE_CORE = READY`
- `REPORT_FINAL_WRITING = PENDING`
- `ANNEXES = PENDING`
- `BUSINESS_DECISIONS = PENDING`

No se declara el reporte final terminado.

Anexos candidatos:

- diagrama del pipeline final staging → validación → confirmación autoritativa;
- diagrama de la transacción del command y orden global de locks;
- matriz de errores SQL → HTTP;
- matriz 401/403/200/404/409/422;
- captura de la UI de confirmación;
- prueba de concurrencia `MAX+1` vs lock de tabla;
- listado de tests SQL IT con resultado CI.

---

## 14. Estado de control

- `PARITY_TOTAL = 79` (recalculado, coincide con expectativa).
- `P0_BUSINESS_DECISION_REMAINING = 5` (todos `BUSINESS_DECISION_REQUIRED`).
- `P0_TECHNICAL_REMAINING_WITH_CURRENT_CONTRACTS = 0`.
- `P0_ACTIONABLE_COMPLETION = 100 %` (13/13; no implica `V1_COMPLETE`).
- `MISSING_TOTAL = 6` (0 P0).
- `UNKNOWN_TOTAL = 20`; `UNKNOWN_AFFECTS_V1_CONFIRMED = 0`;
  `UNKNOWN_UNDECIDED = 10`; `UNKNOWN_POST_V1 = 10`.
- `SALDOS = PENDING_BUSINESS`.
- `FACTURACION_CONFIRM = PENDING_BUSINESS`.
- `DASHBOARD_V1 = PENDING_BUSINESS`.
- Descargos/PEPS: `BLOCKED_BUSINESS` sin paquete de preguntas previo; añadido
  en la sección 9.
- `TECHNICAL_INTERNAL_BLOCKERS = 0`.
- `BUSINESS_DECISION_REQUIRED = YES`.
- `SCOPE_DECISION_REQUIRED = YES`.
- `V1_SCOPE_FROZEN = NO`.
- `V1_COMPLETE = NO`.
- `READY_TO_FREEZE_SCOPE = YES` (el paquete está listo para revisión; el
  congelamiento requiere la respuesta de negocio).
- Esta rama es documental: no modifica Java, Angular, SQL, SP ni migraciones.

## 15. Interpretación

El desarrollo funcional P0 debe pausarse en los puntos sin contrato empresarial
para evitar inferir reglas fiscales. Esto no bloquea trabajos independientes
de: least privilege, deployment hardening, observabilidad, pruebas E2E,
documentación y preparación de staging.
