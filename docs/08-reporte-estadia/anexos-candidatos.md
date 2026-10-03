# Anexos candidatos — reporte de estadía

Inventario de referencias para el reporte final. Sólo referencias; no se copian
archivos ni datos empresariales aquí. Sin secretos.

| ANEXO | DESCRIPCIÓN | FUENTE | ESTADO | UTILIDAD EN REPORTE |
|---|---|---|---|---|
| A1 | Arquitectura hexagonal modular y reglas de dependencia | `docs/03-diseno/arquitectura.md`, `docs/04-arquitectura/` | Disponible | Marco de diseño de la solución |
| A2 | Modelo de datos / ER propuesto | `docs/03-diseno/modelo-datos.md`, `ERv1.png`–`ERv3.png` | Disponible | Sustento de diseño de BD (staging vs autoritativa) |
| A3 | Matriz de paridad legacy V1 (79 capacidades) | `docs/01-requerimientos/matriz-paridad-legacy-v1.md` | Disponible (conteos corregidos en cierre) | Resultados cuantitativos de cobertura |
| A4 | Enfoque SP-FIRST y auditoría de procedimientos | `docs/03-diseno/auditoria-stored-procedures.md`, `procedimientos-almacenados.md`, `scripts/check-inline-sql.py` | Disponible | Método de reutilización y control |
| A5 | Capturas UI (paridad visual) | Pendiente de recolección (pantallas descritas en `docs/03-diseno/prototipos/`) | GAP — sin captura versionada | Evidencia visual de la aplicación |
| A6 | Auditoría E2E interactiva V1 | `docs/05-pruebas/auditoria-funcional-e2e-v1.md` | Disponible | Pruebas funcionales de punta a punta |
| A7 | Suites de tests (backend/frontend) | `backend/src/test`, specs frontend; resumen en `docs/05-pruebas/pruebas-unitarias.md` | Disponible (131/131 frontend; ITs backend) | Verificación técnica |
| A8 | CI (workflow, gates y runs) | `.github/workflows/`, runs `37123546803` y `37124270634` | Disponible (capturas en GH) | Evidencia de integración continua |
| A9 | Ejemplos XLSX sintéticos (`E2E_*`) | Fixtures locales del sweep (temporales, borrados); estructura documentada en `docs/05-pruebas/pedimentos-staging-v1.md` y `catalog-imports-staging-v1.md` | GAP — regenerables con layout documentado | Casos de ejemplo para carga/validación |
| A10 | Staging en `ANEXO24_DEV/app24` | `docs/03-diseno/facturacion-staging-v2.md`, migraciones `infra/sql/migrations/` | Disponible (datos sintéticos retenidos para auditoría) | Arquitectura de staging seguro |
| A11 | Reportes V1 y exportaciones | `docs/05-pruebas/v1-technical-closure.md` §3/§6; `docs/03-diseno/mapeo-reportes*.md` | Disponible | Evidencia funcional de reportes |
| A12 | Evidencia de API (swagger/endpoints) | `ReportesController` y controllers; sweep API documentado en auditoría E2E | Disponible (documental; Swagger deshabilitado en prod) | Contratos de integración |
| A13 | Cronología de fases de desarrollo | `docs/08-reporte-estadia/evidencias-proyecto.md` (por fase) | Disponible | Trazabilidad del proceso |
| A14 | Cierre técnico y decisiones pendientes | `docs/05-pruebas/v1-technical-closure.md`, `docs/01-requerimientos/decisiones-alcance-v1.md` | Disponible | Alcance entregado y límites |
| A15 | Empaquetado/despliegue (Docker/compose) | `docs/05-pruebas/deployment-readiness-v1.md`, `docs/06-devops/` | Disponible | Preparación de entrega |

## Notas

- Secretos y credenciales: excluidos por diseño en todos los anexos candidatos.
- Los fixtures `E2E_*` no se versionan (contienen timestamps y son regenerables);
  el layout contractual está documentado en los mapeos y pruebas citadas.
- Capturas UI y evidencia de aceptación de operador son los dos gaps de anexo
  pendientes; ver §Cobertura documental en `evidencias-proyecto.md`.

## Checklist de capturas UI (prioridad A5)

Capturas a recolectar con datos no sensibles o estados vacíos/sintéticos. No se
versionan en esta fase; esta lista es el plan de recolección.

| # | Pantalla | Estado a capturar | Nota |
|---|---|---|---|
| 1 | `/login` | formulario vacío y validación visible | sin credenciales reales |
| 2 | `/dashboard` | vista con permisos de administrador | datos agregados no sensibles |
| 3 | `/materiales` | listado + filtro aplicado | dataset LIVE es reducido |
| 4 | `/productos` | listado con paginación | — |
| 5 | `/estructuras` | empty state | dataset vacío |
| 6 | `/catalogos` | catálogos auxiliares | — |
| 7 | `/catalogos/datos-generales` | ficha read-only | datos de empresa: usar captura autorizada o difuminar |
| 8 | `/catalogos/socios-comerciales` | listado | evitar datos sensibles de terceros |
| 9 | `/operaciones/entradas` | consulta con periodo | — |
| 10 | `/operaciones/salidas` | consulta con paginación | — |
| 11 | `/operaciones/materiales-utilizados` | consulta | — |
| 12 | `/operaciones/activos-fijos` | consulta | — |
| 13 | `/operaciones/pedimentos` | resultado `CON_ERRORES` de fixture sintético | nunca capturar confirmación real |
| 14 | `/reportes` | selector + un reporte generado | preferir F4 vacío o entradas |
| 15 | `/facturacion` | carga `VALIDADA` de fixture sintético | sin confirmación |
| 16 | `/usuarios` | listado | preferir usuario sintético |
| 17 | `/perfiles` | perfiles y permisos | — |
| 18 | `/bitacora` | listado con filtros | ocultar datos personales reales |

## Fixtures sintéticos requeridos (documentados, no versionados aún)

| Fixture | Contrato de referencia | Uso |
|---|---|---|
| Pedimento válido sintético | `docs/03-diseno/mapeo-carga-pedimentos.md` | captura de preview/errores; requiere material del catálogo para preview limpio |
| Pedimento con errores | mismo contrato | captura de errores por fila/columna |
| Material sintético | `docs/05-pruebas/catalog-imports-staging-v1.md` | captura `PREVISUALIZADA` |
| Producto sintético | mismo documento | captura `PREVISUALIZADA` |
| Facturación sintética (hoja `FACTURAS`) | plantilla activa del sistema | captura `VALIDADA` |

No recuperar los `E2E_*` temporales borrados; regenerar con nombres estables y
sin timestamps cuando se recolecten anexos.
