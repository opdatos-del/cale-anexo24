# Manual técnico

La solución integra Angular con una API Spring Boot protegida por Spring Security y un adaptador SQL Server para Módulo C. El adaptador centraliza procedimientos y consultas; el esquema complementario conserva permisos, bitácora y cargas sin alterar la base existente.

## Configuración y mantenimiento

Configurar por ambiente URL de API, cadena de SQL Server, secretos de autenticación, límites de carga, ubicación de exportaciones y nivel de logs. Los secretos se inyectan desde el gestor autorizado. Para incidentes, consultar `correlationId`, bitácora y logs; nunca recuperar contraseñas ni exponer SQL en mensajes de usuario.

## Operación segura

Realizar inventario de Módulo C en solo lectura, ejecutar pruebas con datos sintéticos, respaldar antes de cambios autorizados y revisar periódicamente permisos, fallos de reporte y rechazos de carga.
