# Planteamiento del problema

El sistema de referencia concentra funciones relevantes para el Anexo 24, pero
presenta riesgos observables: exposición de credenciales en interfaz, permisos
administrativos amplios, reportes que fallan con error genérico y carga de
facturación sin guía ni diagnóstico detallado. La falta de separación moderna y
de trazabilidad técnica dificulta el mantenimiento y el soporte, y la
dependencia de procesos almacenados mutables sin contrato documentado eleva el
riesgo operativo.

La problemática se concreta en cuatro frentes:

1. **Seguridad y control de acceso:** credenciales y privilegios administrados
   sin separación de funciones; sin trazabilidad de cambios.
2. **Confiabilidad operativa:** consultas y reportes sin validación de
   contratos ni manejo de errores útil; cargas de archivos sin diagnóstico
   accionable.
3. **Trazabilidad y auditoría:** eventos sin correlación que dificultan
   investigar incidentes y demostrar cumplimiento.
4. **Evolución del sistema:** lógica y datos fuertemente acoplados a la
   interfaz legacy, con documentación insuficiente para cambios seguros.

Se requiere una solución web segura y verificable que modernice la operación
sin alterar el modelo autoritativo: consultas y reportes read-only sobre la base
existente, cargas con validación y staging aislado, administración con permisos
y bitácora, y una ruta controlada para confirmar operaciones sólo cuando exista
contrato y caso de aceptación aprobados.
