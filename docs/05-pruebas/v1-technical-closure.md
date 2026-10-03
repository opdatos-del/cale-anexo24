# Cierre técnico V1 — snapshot consolidado

Documento de consolidación técnica previo a una entrega V1. No implementa
capacidades nuevas, no ejecuta DDL, no modifica Java/Angular/SQL y no reabre
auditorías: recalcula el estado real desde el repositorio, la evidencia E2E y
metadata read-only LIVE.

## 1. SHA evaluado

```text
base dev       = 3c00e1908465cde700f7b7fe1e4166d2e3d8d555
CI dev         = run 37123546803 — SUCCESS (sp-first-gate, backend, frontend)
rama           = release/v1-technical-closure
working tree   = docs/03-diseno/mapeo-bitacora.md ajeno preservado; 2 stashes intactos
```

Integración previa: `feature/e2e-v1-fixes` (fd98364, 8b7a4fc, 3c00e19) entró a
`dev` por fast-forward; sin merge commit.

## 2. Alcance V1

Entregable V1 = aplicación moderna (Angular + Spring Boot) que cubre:

- consulta operativa y de catálogos sobre `CALE_IMMEX` (read-only vía SP);
- reportes consolidados read-only con exportación XLSX;
- administración y seguridad sobre `ANEXO24_DEV/app24`;
- importación autoritativa de pedimentos (staging → validación → confirmación);
- staging validado (sin confirmación) para catálogos y facturación;
- infraestructura de despliegue product-like (Docker/compose) preparada.

Fuera de V1 (decisión de negocio o evidencia pendiente): saldos, motor de
descargos/PEPS, confirmación de catálogos y facturación, ajuste anual, Anexo 30,
operaciones especiales e interfaces sin contrato.

## 3. Módulos implementados y superficie real

Rutas reales registradas en `app.routes.ts` (19 + wildcard). Estado por
evidencia: `IMPLEMENTED` (código en `dev`), `RUNTIME_VERIFIED` (API 200 en
ejecución real contra LIVE), `E2E_VERIFIED` (auditoría interactiva
`docs/05-pruebas/auditoria-funcional-e2e-v1.md`).

| Ruta | Implemented | Runtime | E2E |
|---|---|---|---|
| `/login` | YES | YES (401 controlado; JWT real) | YES (UI; flujo con credenciales reales `OPERATOR_ACCEPTANCE_PENDING`) |
| `/forbidden` | YES | YES | YES |
| `/dashboard` | YES | YES | YES |
| `/materiales` | YES | YES | YES |
| `/productos` | YES | YES | YES |
| `/estructuras` | YES | YES (0 filas) | YES |
| `/catalogos` | YES | YES | YES |
| `/catalogos/datos-generales` | YES | YES | YES |
| `/catalogos/socios-comerciales` | YES | YES | YES |
| `/catalogos/importaciones` | YES | YES (staging) | YES |
| `/operaciones/entradas` | YES | YES | YES |
| `/operaciones/salidas` | YES | YES | YES |
| `/operaciones/materiales-utilizados` | YES | YES | YES |
| `/operaciones/activos-fijos` | YES | YES | YES |
| `/operaciones/pedimentos` | YES | YES (staging + confirmación implementada) | YES (sin confirmar en E2E) |
| `/reportes` | YES | YES (11 tipos) | YES |
| `/facturacion` | YES | YES (staging) | YES |
| `/usuarios` | YES | YES | YES |
| `/perfiles` | YES | YES | YES |
| `/bitacora` | YES | YES | YES |
| wildcard `**` | YES | YES (→ `/dashboard`) | YES |

Regresión post-merge en `dev`: RBAC 4/4 y partidas UI sin sufijo `.0` (ver §9).

## 4. Matriz de paridad — snapshot recalculado

Recalculado fila por fila desde `matriz-paridad-legacy-v1.md` (parser, no
conteos históricos). Total `LEGACY_CAPABILITIES_TOTAL = 79`.

| Estado | Conteo |
|---|---:|
| `IMPLEMENTED_EQUIVALENT` | 5 |
| `IMPLEMENTED_REDESIGNED` | 26 |
| `PARTIAL` | 12 |
| `MISSING` | 6 |
| `BLOCKED_BUSINESS` | 5 |
| `CONSOLIDATE` | 6 |
| `UNKNOWN` | 19 |
| `NOT_REQUIRED` | 0 |

Corrección aplicada en esta fase: el resumen anterior de la matriz declaraba
`PARTIAL = 11` / `UNKNOWN = 20`; el recalculó da `PARTIAL = 12` / `UNKNOWN = 19`
porque `LEGACY-041` ya estaba en `PARTIAL` (F4) sin actualizar el agregado. Los
estados de las filas no se modificaron.

Distribución por prioridad:

| Prioridad | Total | Implementadas | Parciales | Faltantes | Bloqueadas | Consolidadas | Desconocidas |
|---|---:|---:|---:|---:|---:|---:|---:|
| P0 | 18 | 13 | 2 | 0 | 3 | 0 | 0 |
| P1 | 40 | 14 | 7 | 5 | 2 | 3 | 9 |
| P2 | 20 | 4 | 2 | 1 | 0 | 3 | 10 |
| P3 | 1 | 0 | 0 | 0 | 0 | 0 | 1 |

### P0 — estado V1

- `P0_TOTAL = 18`, `P0_IMPLEMENTED = 13` (4 equivalentes + 9 rediseñadas).
- `P0_PARTIAL = 2`: `LEGACY-017` (PED-005/006 dependen de `INVENTARIO`, ausente
  en LIVE) y `LEGACY-058` (facturación sin confirmación).
- `P0_BLOCKED_BUSINESS = 3`: `LEGACY-021`/`LEGACY-038` (saldos) y
  `LEGACY-029` (motor de descargos).
- `P0_TECHNICAL_ACTIONABLE_REMAINING = 0`.

### P1 — estado V1

- `P1_TOTAL = 40`; `P1_IMPLEMENTED = 14`; `P1_PARTIAL = 7`; `P1_MISSING = 5`;
  `P1_BLOCKED_BUSINESS = 2`; `P1_CONSOLIDATE = 3`; `P1_UNKNOWN = 9`.
- `P1_TECHNICAL_ACTIONABLE_REMAINING = 0`: todo lo pendiente cae en decisión de
  negocio, evidencia externa o POST_V1 (clasificación §8–§11).

## 5. Calidad técnica (checklist)

| Ítem | Estado | Evidencia |
|---|---|---|
| Backend tests + build | PASS | `gradlew test build` BUILD SUCCESSFUL; ITs SQL/Testcontainers incluidos |
| Frontend tests | PASS | 131/131 (31 archivos) |
| Lint | PASS | `pnpm lint` sin findings |
| Build | PASS | `pnpm build` OK (warning de presupuesto de bundle pre-existente, no funcional) |
| SP-FIRST | PASS | `SP_FIRST_GATE|PASS|violations=0` |
| Runtime permission gate | PASS | CALE 24/24 · APP 35/35 · verify 24/35 · repo scan 76 archivos / 0 findings |
| E2E interactivo | PASS | 20/21 pre-fix; hallazgo P1 corregido y verificado; post-fix 13/13 RBAC + 40/40 viewports |
| Responsive | PASS | 40/40 (tablet/mobile) sin overflow ni errores |
| RBAC | PASS | API 6/6 y browser 13/13 tras fix; regresión post-merge 4/4 |
| Uploads | PASS (staging) | Pedimentos, materiales, productos y facturación; confirmación autoritativa sólo pedimentos |
| Exportaciones XLSX | PASS | 4 operativos descargados y abiertos con `openpyxl`; F4 204 con dataset vacío |
| Deployment LIVE de SP | PASS | 24/24 CALE + 35/35 APP SP + 18/18 tablas `app24` presentes, 0 missing/0 extra (§7) |
| Health / readiness | PASS | `/actuator/health` protegido y respondiendo; `system/status` 200 (`moduleCDatabase`/`applicationDatabase` UP); healthcheck compose healthy |
| Logging / correlationId | PASS | Errores API devuelven `correlationId`; bitácora app24 registra eventos |
| Contenedores | PASS | `DEPLOYMENT_READINESS_V1` (build, runtime, proxy, compose `down` limpio); `AUTHENTICATED_PROXY_SMOKE = NOT_EXECUTED` histórico (re-ejecutable con el mecanismo JWT actual) |

## 6. Evidencia E2E (resumen)

Fuente: `docs/05-pruebas/auditoria-funcional-e2e-v1.md` (sweep completo +
fase de correcciones).

```text
API_CHECKS = 25/25 EXPECTED (200=23, 401e=1, 400e=1, 5xx=0)
rutas = 20/21 PASS pre-fix; 0 P0; P1=1 (corregido), P2=1 (corregido), P3=2 (diferidos)
reportes = 11/11 API 200; paginación con datos en 6; exports operativos OK
uploads = 3 flujos staging con hash/duplicados 409; 0 confirmaciones
RBAC = API 6/6; browser 13/13 tras fd98364
responsive = 40/40 sin overflow ni errores de consola/página
```

## 7. SQL / runtime — inventario de despliegue

Verificación read-only de metadata LIVE (`sys.objects`), sin DDL y sin ejecutar
objetos mutables:

```text
CALE_IMMEX.dbo.SP        required=24 present=24 missing=0 extra=0
ANEXO24_DEV.app24.SP     required=35 present=35 missing=0 extra=0
ANEXO24_DEV.app24.TABLE  required=18 present=18 missing=0 extra=0
```

- Los 24 SP de `CALE_IMMEX` son los entry points `dbo.APP24_Q_*` (incluye
  `APP24_Q_F4_LISTAR` desplegado para LEGACY-041).
- Los 35 SP `app24.*` y las 18 tablas de staging/administración viven en
  `ANEXO24_DEV` y están presentes.
- Identidad runtime vigente: `opdatos` (decisión de proyecto). El hardening
  `anexo24_app` (scripts 04–07) permanece `DEFERRED_SECURITY_HARDENING`, no se
  ejecuta LIVE y no bloquea V1.
- `cambio de BD`, `migración de datos` y `DDL en LIVE`: 0 en esta fase.

## 8. Pendientes técnicos (A — TECHNICAL_ACTIONABLE)

```text
P0: 0
P1: 0
P3 (diferidos explícitamente, no bloquean V1): 2
  FUN-E2E-001  padding CHAR legacy visible sólo en JSON de API (UI/XLSX limpios)
  FUN-E2E-004  500 en export de reportes textuales sin endpoint (no expuesto en UI)
```

No hay trabajo técnico P0/P1 pendiente bajo los contratos actuales.

## 9. Pendientes de negocio (B — BUSINESS_DECISION_REQUIRED)

Cuatro temas con paquete de preguntas en `docs/01-requerimientos/cierre-alcance-v1.md` §9:

| Tema | IDs | Preguntas clave |
|---|---|---|
| Saldos | 021, 038, 042 | fórmula, fuentes, grano, corte, unidades |
| Facturación — confirmación | 058 | pipeline autoritativo, tablas, duplicados, rollback |
| Descargos / PEPS | 029, 030 | algoritmo, idempotencia, aprobación, rollback |
| Dashboard V1 | — | ¿resumen actual suficiente o KPIs `MUST_HAVE_V1`? |

Ítems adicionales que requieren decisión (24 IDs):
`001, 017, 021, 029, 030, 038, 042, 043, 044, 045, 050, 052, 054, 055, 056,
058, 064, 065, 069, 070, 071, 072, 073, 077` (+ Dashboard, sin ID de matriz).
Incluye: confirmación de catálogos (054/055), importación de socios (056),
consolidados (043–045, 050, 052, 077), edición de datos generales (001),
PED-005/006 (017), ajuste anual (064/065) y Anexo 30 (069–073).
`BUSINESS_BLOCKERS = 4 temas / 25 ítems (24 IDs + Dashboard)`.

## 10. Pendientes por evidencia externa (C — EXTERNAL_EVIDENCE_REQUIRED)

Sin contrato/layout/pantalla utilizable; no se reauditan (una pasada ya hecha):

| ID | Capacidad | Estado |
|---|---|---|
| 048 | Scrap / desperdicio | `WAITING_EXTERNAL_CONTRACT` (matriz conserva `UNKNOWN`) |
| 060 | Actas de destrucción | `WAITING_EXTERNAL_CONTRACT` (matriz conserva `MISSING`) |
| 023 | Cambios de régimen | evidencia funcional + regla |
| 024 | Regularizaciones | evidencia funcional + regla |
| 025 | Actas de destrucción (operación) | layout + reglas (+ decisión) |
| 026 | Transferencias de submaquila | layout + reglas fiscales (+ decisión) |
| 027 | Constancias | layout y efectos |
| 028 | CTM (proceso) | contrato de flujo (+ decisión) |
| 059 | Servicios | fuente no localizada |
| 061 | Órdenes de fabricación | fuente no localizada |
| 062 | Procesos | fuente no localizada |
| 063 | CTM / carta de materiales | layout y proceso |

`EXTERNAL_CONTRACT_BLOCKERS = 12` (los marcados con “(+ decisión)” además
requieren respuesta de negocio; su clasificación primaria aquí es evidencia).

## 11. POST_V1 (D)

- `014` consultas guardadas.
- `031` resolución/desbloqueo de operaciones bloqueadas.
- `032` extensión de análisis de descarga (faltantes/trazo).
- `041` resto del subsistema CTM y HDE.
- `046` alcance completo de vencimientos.
- `049` generación dirigida/PEPS.
- `066, 067, 068` secciones restantes del ajuste anual.
- Confirmaciones autoritativas de catálogos y facturación comparten el patrón ya
  probado de pedimentos y se retoman sólo con decisión de negocio.

`POST_V1_ITEMS = 9` (IDs listados).

## 12. P3 conocidos (no bloquean V1)

- `FUN-E2E-001` — padding `char` legacy visible en JSON de API para compulsa,
  rectificaciones y análisis de descargas; UI recorta con formatter y los XLSX
  exportables no muestran padding (`openpyxl padded=0`).
- `FUN-E2E-004` — 500 `ERROR_INTERNO` en `GET /api/v1/reportes/{…}/exportacion`
  para los seis reportes textuales sin endpoint; la UI no expone esos paths.

## 13. Criterios de salida V1 (técnicos)

1. CI verde en `dev` — CUMPLIDO (run 37123546803).
2. Gates SP-FIRST y runtime permission — CUMPLIDOS.
3. Suite backend + ITs y suite frontend verdes — CUMPLIDO (131/131).
4. E2E interactivo sin hallazgos P0/P1/P2 abiertos — CUMPLIDO (P1/P2 corregidos
   y verificados; P3 diferidos y documentados).
5. Objetos SQL requeridos presentes LIVE — CUMPLIDO (77/77).
6. Cero trabajo técnico P0/P1 pendiente bajo contratos actuales — CUMPLIDO.
7. Decisiones de negocio y evidencia externa registradas como fuera de V1 —
   CUMPLIDO (no bloquean la entrega técnica).

## 14. Decisión final

```text
V1_TECHNICALLY_READY = YES
TECHNICAL_BLOCKERS = 0
BUSINESS_BLOCKERS = 4 temas / 25 ítems
EXTERNAL_CONTRACT_BLOCKERS = 12
POST_V1_ITEMS = 9
KNOWN_LOW_PRIORITY_ISSUES = 2 (FUN-E2E-001, FUN-E2E-004)
PROJECT_COMPLETE = NO
```

`V1_TECHNICALLY_READY = YES` significa: todo lo implementable bajo los
contratos actuales está cerrado, probado y desplegado. `PROJECT_COMPLETE = NO`
porque negocio aún tiene decisiones pendientes (Saldos, confirmación de
Facturación, Descargos/PEPS, Dashboard) y existen 12 capacidades esperando
contrato/evidencia externa.
