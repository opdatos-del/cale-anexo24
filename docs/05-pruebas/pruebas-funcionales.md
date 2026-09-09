# Pruebas funcionales

| ID | Escenario | Resultado esperado |
|---|---|---|
| PF-001 | Inicio de sesión válido e inválido | Acceso solo con credenciales válidas; error seguro y bitácora de intento. |
| PF-002 | Usuario sin permiso administrativo | API responde 403 y la interfaz no ofrece acción. |
| PF-003 | Consulta operativa sin rango | Mensaje específico de rango obligatorio. |
| PF-004 | Reporte con rango válido | Resultado, sin datos o error correlacionable; nunca error genérico. |
| PF-005 | Exportación sin resultados | Informa que no hay datos y no crea archivo vacío. |
| PF-006 | XLSX con columna inválida | Indica archivo, hoja, fila, columna y regla. |
| PF-007 | Carga válida confirmada | Registro transaccional y evento de bitácora. |
