# Arquitectura de software

La solución propuesta usa Angular y TypeScript en el navegador; Java 21, Spring Boot y Spring Security en una API REST; y SQL Server Módulo C mediante un adaptador de persistencia. La separación encapsula procedimientos almacenados y evita acoplar la interfaz a SQL Server.

[Abrir diagrama de arquitectura](../../diagramas/arquitectura-anexo24.html)

## Responsabilidades

| Capa | Responsabilidad |
|---|---|
| Frontend | Navegación, formularios, validación de experiencia y visualización de errores. |
| API y seguridad | Autenticación, autorización por permiso, validación de solicitudes y contratos JSON. |
| Dominio | Reglas de negocio, casos de uso, reportes, carga y bitácora. |
| Adaptador Módulo C | Llamadas parametrizadas a vistas/procedimientos y mapeo a modelos de aplicación. |
| Datos | Módulo C sin cambios no autorizados y esquema complementario aislado para auditoría/configuración. |

La comunicación externa es HTTPS; los secretos se administran por ambiente y no se registran en logs. Las operaciones de carga y escritura usan transacciones y un identificador de correlación.

