# Objetivos

## Objetivo general

Desarrollar e implementar una aplicación web que modernice el control
automatizado de inventarios de comercio exterior conforme al Anexo 24,
integrándose de forma segura con la base operativa existente (`CALE_IMMEX`) sin
crear un modelo operativo paralelo.

## Objetivos específicos

1. Documentar la operación actual y sus riesgos mediante auditoría funcional y
   técnica read-only.
2. Diseñar seguridad por permisos, trazabilidad y manejo de errores útil.
3. Implementar consultas, reportes y carga validada sin alterar la base
   autoritativa sin autorización.
4. Establecer pruebas, compilación, despliegue y documentación reproducibles.
5. Determinar, con evidencia, qué capacidades legacy quedan cubiertas, cuáles
   requieren decisión de negocio y cuáles dependen de evidencia externa.

## Estado de cumplimiento (cierre técnico V1)

| Objetivo | Estado | Evidencia |
|---|---|---|
| 1. Auditoría funcional y técnica | Cumplido | `docs/03-diseno/mapeo-*.md`, `docs/01-requerimientos/matriz-paridad-legacy-v1.md` (79 capacidades) |
| 2. Seguridad, trazabilidad y errores | Cumplido | JWT/RBAC, bitácora `app24`, `correlationId`; auditoría E2E RBAC 13/13 |
| 3. Consultas, reportes y cargas | Cumplido en el alcance implementado | 11 reportes, 4 flujos de staging, confirmación autoritativa de pedimentos |
| 4. Pruebas, compilación y despliegue | Cumplido | CI verde, 131/131 frontend, ITs backend, contenedores product-like |
| 5. Determinación de cobertura | Cumplido | Matriz recalculada: 5 equivalentes + 26 rediseñadas + 12 parciales + 6 faltantes + 5 bloqueadas + 6 consolidadas + 19 desconocidas |

La aceptación formal por parte de la empresa (`OPERATOR_ACCEPTANCE_PENDING`) y
las decisiones de alcance pendientes se registran en
`docs/01-requerimientos/decisiones-alcance-v1.md`.
