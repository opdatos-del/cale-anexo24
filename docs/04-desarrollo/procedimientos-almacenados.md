# Procedimientos almacenados

No se documentan nombres ficticios: la auditoría confirmó que existen procedimientos en Módulo C, pero no expuso sus identificadores ni parámetros. El inventario técnico autorizado llenará este catálogo desde SQL Server y será la fuente de verdad.

| Dominio | Uso esperado | Protección |
|---|---|---|
| Catálogos | Listados y filtros de materiales, productos y estructuras | Solo lectura, parámetros tipados y paginación. |
| Operaciones | Entradas, salidas, consumo, activo fijo y saldos | Rango de fechas obligatorio cuando aplique y resultado mapeado a DTO. |
| Reportes | Consolidación y exportación | Correlación de error y límite de volumen. |
| Facturación | Validación/registro autorizado | Transacción, idempotencia por hash y bitácora. |

Todo procedimiento se invoca parametrizado desde el adaptador; no se concatena SQL ni se otorgan permisos de modificación más allá de los autorizados.
