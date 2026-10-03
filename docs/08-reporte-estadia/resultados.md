# Resultados

Métricas consolidadas con evidencia verificable al cierre técnico V1
(`dev` = `fb77be0`, tag `v1.0.0-rc1`). No se afirma reemplazo total del sistema
legacy.

## Cobertura de paridad legacy (79 capacidades)

| Estado | Conteo |
|---|---:|
| `IMPLEMENTED_EQUIVALENT` | 5 |
| `IMPLEMENTED_REDESIGNED` | 26 |
| `PARTIAL` | 12 |
| `MISSING` | 6 |
| `BLOCKED_BUSINESS` | 5 |
| `CONSOLIDATE` | 6 |
| `UNKNOWN` | 19 |
| **Total** | **79** |

Recalculado fila por fila desde
`docs/01-requerimientos/matriz-paridad-legacy-v1.md`; corrige agregados previos
(11/20) que no reflejaban `LEGACY-041` parcial.

## Módulos modernos entregados

- 19 rutas funcionales + wildcard, con runtime verificado y auditoría E2E.
- 11 reportes consolidados: Entradas, Salidas, Materiales utilizados,
  Bitácora, Compulsa, Rectificaciones, Vencimientos, Dirigidos, Análisis de
  descargas, Operaciones bloqueadas y F4 (CTM/desperdicio).
- 4 flujos de carga: pedimentos (con confirmación autoritativa), materiales y
  productos (staging), facturación (staging).
- Administración completa: usuarios, perfiles, permisos, actividades y
  bitácora.

## Datasets observados en LIVE (sólo lectura, ambiente DEV)

| Consulta | Filas observadas |
|---|---:|
| Materiales | 2 |
| Productos | 204 |
| Estructuras | 0 |
| Entradas | 1 |
| Salidas | 2,693 |
| Materiales utilizados | 3,105 |
| Activos fijos | 1 |
| Compulsa / Rectificaciones | 662 / 662 |
| Análisis de descargas | 3,866 |
| Bitácora (app24) | 632 |
| Vencimientos, Dirigidos, Operaciones bloqueadas, F4 | 0 |

## Pruebas y calidad

| Métrica | Valor |
|---|---|
| Frontend tests | 131/131 PASS |
| Lint / build frontend | PASS (warning de presupuesto de bundle pre-existente) |
| Backend `test build` (incluye ITs SQL) | PASS |
| SP-FIRST | violations = 0 |
| Runtime permission gate | CALE 24/24 · APP 35/35 · repo scan 0 findings |
| Auditoría E2E | API 25/25 esperado (23×200, 401e, 400e, 0×5xx); responsive 40/40 |
| Hallazgos E2E abiertos | P0 = 0 · P1 = 0 · P2 = 0 · P3 = 2 (diferidos) |
| CI de `dev` | run `37124761648` SUCCESS (post-integración documental) |

## SQL y despliegue

```text
CALE_IMMEX.dbo.SP        required=24 present=24 missing=0
ANEXO24_DEV.app24.SP     required=35 present=35 missing=0
ANEXO24_DEV.app24.TABLE  required=18 present=18 missing=0
```

- Objetos verificados por metadata read-only (`sys.objects`), sin DDL.
- Empaquetado product-like verificado (Docker/compose/nginx) con smoke
  estructural; sin despliegue a producción.

## Capacidades autoritativas

- **Confirmación autoritativa:** pedimentos (tipo 1 y 2) — implementada,
  probada con transacción, idempotencia y rollback.
- **Materiales, productos y facturación:** staging validado; confirmación
  pendiente de decisión de negocio.

## Límites explícitos

- No se reemplazó el 100 % del sistema legacy.
- No hay evidencia de uso productivo real: verificación en DEV/local con datos
  LIVE de sólo lectura y staging sintético.
- Saldos, descargos/PEPS, confirmación de facturación y KPIs de dashboard no
  están implementados por falta de decisión/contrato.
- `FUN-E2E-001` y `FUN-E2E-004` (P3) permanecen documentados sin corrección.
