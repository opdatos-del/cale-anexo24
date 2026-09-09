# Pruebas de integración

| ID | Integración | Resultado esperado |
|---|---|---|
| PI-001 | API - Módulo C | Consulta parametrizada devuelve DTO consistente con muestra controlada. |
| PI-002 | API - procedimiento de reporte | Error técnico se registra con correlación y no expone detalles. |
| PI-003 | Seguridad - permisos | Cada endpoint rechaza permisos insuficientes. |
| PI-004 | Carga - persistencia | Fallo intermedio revierte transacción; éxito es idempotente por hash. |
