# Reglas de negocio

| ID | Regla |
|---|---|
| RN-001 | Un usuario solo ejecuta actividades asignadas a su perfil; la API valida el permiso. |
| RN-002 | Las consultas operativas y reportes que dependan de periodo exigen fecha inicial y final válidas. |
| RN-003 | Sin resultados no se genera una exportación vacía; se informa la condición. |
| RN-004 | La contraseña no se muestra, devuelve ni registra; se almacena con hash. |
| RN-005 | Una carga solo se guarda después de validar archivo, plantilla y datos y recibir confirmación. |
| RN-006 | Cada error de carga identifica ubicación y regla, sin revelar información sensible. |
| RN-007 | Las operaciones críticas registran usuario, fecha, módulo, acción, resultado y correlación. |
| RN-008 | Módulo C se consulta o modifica únicamente por contratos autorizados. |
