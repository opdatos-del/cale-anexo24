# Anexos — inventario actualizado (RC1)

Índice de anexos con archivo, capítulo relacionado y estado. Referencias
solamente; sin secretos ni datos empresariales. Las capturas de listados usan
datos sintéticos o estados vacíos; los fixtures son 100 % sintéticos.

## Capturas UI (A5.x) — `anexos/capturas/`

| ID | Nombre | Archivo | Capítulo relacionado | Estado |
|---|---|---|---|---|
| A5.1 | Login | `01-login.png` | Desarrollo (F1) | Sanitizada |
| A5.2 | Dashboard | `02-dashboard.png` | Resultados | Sanitizada (datos sintéticos) |
| A5.3 | Materiales | `03-materiales.png` | Resultados | Sanitizada (mocks sintéticos) |
| A5.4 | Productos | `04-productos.png` | Resultados | Sanitizada (mocks sintéticos) |
| A5.5 | Estructuras | `05-estructuras.png` | Resultados | Sanitizada (vacío real) |
| A5.6 | Catálogos auxiliares | `06-catalogos.png` | Resultados | Sanitizada (mocks vacíos) |
| A5.7 | Datos generales | `07-datos-generales.png` | Resultados | Sanitizada (sin registro) |
| A5.8 | Socios comerciales | `08-socios-comerciales.png` | Resultados | Sanitizada (mocks vacíos) |
| A5.9 | Importaciones de catálogos — inicial | `09a-importaciones-inicial.png` | Desarrollo (F10) | Sanitizada |
| A5.10 | Importaciones de catálogos — preview | `09b-importaciones-preview.png` | Desarrollo (F10) | Sanitizada (`PREVISUALIZADA` sintética) |
| A5.11 | Entradas | `10-entradas.png` | Resultados | Sanitizada (mock vacío) |
| A5.12 | Salidas | `11-salidas.png` | Resultados | Sanitizada (mock vacío) |
| A5.13 | Materiales utilizados | `12-materiales-utilizados.png` | Resultados | Sanitizada (mock vacío) |
| A5.14 | Activos fijos | `13-activos-fijos.png` | Resultados | Sanitizada (mock vacío) |
| A5.15 | Pedimentos — inicial | `14a-pedimentos-inicial.png` | Desarrollo (F8) | Sanitizada |
| A5.16 | Pedimentos — validación | `14b-pedimentos-validacion.png` | Desarrollo (F8/F9) | Sanitizada (fixture sintético) |
| A5.17 | Reportes (F4 vacío) | `15-reportes.png` | Resultados | Sanitizada |
| A5.18 | Facturación — inicial | `16a-facturacion-inicial.png` | Desarrollo (F11) | Sanitizada |
| A5.19 | Facturación — validada | `16b-facturacion-validada.png` | Desarrollo (F11) | Sanitizada (fixture sintético) |
| A5.20 | Usuarios | `17-usuarios.png` | Desarrollo (F6) | Sanitizada (mocks sintéticos) |
| A5.21 | Perfiles | `18-perfiles.png` | Desarrollo (F6) | Sanitizada (mock vacío) |
| A5.22 | Bitácora | `19-bitacora.png` | Desarrollo (F6) | Sanitizada (mock vacío) |

## Fixtures sintéticos (A9.x) — `anexos/fixtures/`

| ID | Nombre | Archivo | Uso | Estado |
|---|---|---|---|---|
| A9.1 | Pedimento sintético | `fixture-pedimento-sintetico.xlsx` | Validación (produce error estructurado `PED-003` esperado) | Validado |
| A9.2 | Materiales | `fixture-materiales.xlsx` | Importación de catálogo (preview) | Validado |
| A9.3 | Productos | `fixture-productos.xlsx` | Importación de catálogo (preview) | Validado |
| A9.4 | Facturación (hoja `FACTURAS`) | `fixture-facturacion.xlsx` | Carga validada | Validado |
| A9.5 | Validación inválida | `fixture-invalido-validacion.xlsx` | Errores de estructura | Validado |

## Otros anexos

| ID | Nombre | Fuente | Estado |
|---|---|---|---|
| A1 | Arquitectura | `docs/03-diseno/arquitectura.md`, `diagramas/arquitectura-anexo24.html` | Disponible |
| A2 | Modelo de datos / ER | `docs/03-diseno/ERv3.png` (propuesta) | Disponible; ERv1/v2 `NOT_NEEDED` |
| A3 | Matriz de paridad (79) | `docs/01-requerimientos/matriz-paridad-legacy-v1.md` | Disponible |
| A4 | SP-FIRST | `docs/03-diseno/auditoria-stored-procedures.md`, `docs/04-arquitectura/auditoria-sp-first.md` | Disponible |
| A6 | Auditoría E2E | `docs/05-pruebas/auditoria-funcional-e2e-v1.md` | Disponible |
| A7 | Tests | `docs/05-pruebas/pruebas-unitarias.md`; suites del repo | Disponible |
| A8 | CI | `.github/workflows/`; runs `37125329238` (dev) | Disponible |
| A10 | Staging | `docs/03-diseno/facturacion-staging-v2.md`; `infra/sql/migrations/` | Disponible |
| A11 | Reportes | `docs/05-pruebas/v1-technical-closure.md` §3/§6 | Disponible |
| A12 | Evidencia de API | Sweep API en auditoría E2E; controllers | Disponible |
| A13 | Cronología de fases | `docs/08-reporte-estadia/evidencias-proyecto.md` | Disponible |
| A14 | Cierre técnico | `docs/05-pruebas/v1-technical-closure.md`, `decisiones-alcance-v1.md` | Disponible |
| A15 | Empaquetado/despliegue | `docs/05-pruebas/deployment-readiness-v1.md` | Disponible |

## Diagramas — revisión de legibilidad (Parte L)

| Diagrama | Fuente | Estado | Nota |
|---|---|---|---|
| Arquitectura general | `diagramas/arquitectura-anexo24.html` | `USE` | Exportar a PNG/SVG para el DOCX |
| Modelo de datos | `docs/03-diseno/ERv3.png` | `USE` (propuesta) | No refleja tablas `app24`; señalado como propuesta |
| ERv1 / ERv2 | `docs/03-diseno/ERv1.png`, `ERv2.png` | `NOT_NEEDED` | Versiones históricas |
| Flujo de carga validada | `diagramas/carga-facturacion.html` | `USE` | Patrón de cargas del proyecto |
| Flujo de confirmación de pedimentos | — | `REGENERATE` | Sólo documentado en texto; generar si se requiere |
| Arquitectura hexagonal | — | `REGENERATE` | Descrita en `marco-teorico.md`; diagrama dedicado pendiente |

## Flags de la fase

```text
ANNEX_SCREENSHOTS_COMPLETE = YES (22 capturas sanitizadas)
SYNTHETIC_FIXTURES_COMPLETE = YES (5 fixtures validados)
PRIVACY_REVIEW = PASS (mocks sintéticos + muestreo visual de capturas de riesgo)
FINAL_BUSINESS_CONCLUSION_PENDING = YES
```
