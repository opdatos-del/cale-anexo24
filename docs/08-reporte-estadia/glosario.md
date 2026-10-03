# Glosario

| Término | Definición |
|---|---|
| Anexo 24 | Marco de control automatizado de inventarios para operaciones de comercio exterior. |
| `CALE_IMMEX` | Base operativa autoritativa del sistema legacy; fuente única de datos funcionales. |
| `ANEXO24_DEV` / `app24` | Base y esquema complementarios de la aplicación: usuarios, perfiles, permisos, bitácora y staging. |
| Pedimento | Documento aduanero asociado a una operación de comercio exterior; en el proyecto se importa por archivo y se confirma de forma autoritativa. |
| UMC | Unidad de medida comercial observada en catálogos. |
| SP-FIRST | Regla del proyecto: reutilizar un procedimiento existente auditado antes de crear SQL nuevo; Java sólo invoca SP. |
| Staging | Persistencia intermedia aislada de una carga (archivo, filas normalizadas, errores, hash) antes de cualquier confirmación. |
| Confirmación autoritativa | Comando transaccional e idempotente que aplica una carga validada sobre la base operativa; sólo existe con contrato y caso de aceptación aprobados. |
| Hash / fingerprint | SHA-256 del archivo cargado que evita duplicados y permite rastrear reenvíos. |
| Bitácora | Registro de acciones, resultado y contexto de trazabilidad en `app24`. |
| CorrelationId | Identificador que relaciona solicitud, logs y error reportado. |
| JWT | Token firmado que representa la sesión y sus permisos ante el backend. |
| RBAC | Control de acceso basado en permisos por actividad, verificado en backend y en rutas del frontend. |
| Gate | Verificación automatizada de CI (SP-FIRST, permisos runtime, pruebas, build) que condiciona la integración. |
| Paridad legacy | Cobertura por capacidad del sistema anterior, clasificada con evidencia (equivalente, rediseñada, parcial, faltante, bloqueada, consolidada, desconocida). |
| RC1 | Punto de referencia congelado `v1.0.0-rc1` (no release productiva). |
| Identidad SQL runtime | Cuenta de aplicación con la que el backend accede a las bases; su endurecimiento a mínimo privilegio queda diferido como mejora futura. |
| Testcontainers | Infraestructura de pruebas que levanta SQL Server efímero para integration tests. |
