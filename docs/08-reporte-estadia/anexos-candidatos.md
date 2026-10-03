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
