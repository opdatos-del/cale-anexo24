# Matriz de permisos propuesta

Los nombres de perfiles se validarán con la empresa; la matriz define capacidades, no personas. Los permisos se validan en backend y el menú solo refleja la autorización efectiva.

| Capacidad | Administrador de aplicación | Operador comercio exterior | Consulta/auditoría |
|---|---:|---:|---:|
| Consultar catálogos | Sí | Sí | Sí |
| Consultar operaciones y saldos | Sí | Sí | Sí, si se autoriza |
| Generar/exportar reportes | Sí | Sí | Sí, si se autoriza |
| Cargar y confirmar facturación | Sí | Sí, según actividad | No |
| Consultar bitácora | Sí | Solo sus operaciones, si se autoriza | Sí |
| Administrar usuarios | Sí | No | No |
| Administrar perfiles y actividades | Sí | No | No |
| Cambiar configuración de plantilla | Sí | No | No |

La separación propuesta corrige la observación de que el perfil Usuario SAT contaba con capacidades administrativas completas. Toda asignación o revocación genera evento de bitácora.
