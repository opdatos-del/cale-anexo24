# Procesos actuales del sistema Anexo 24

## Acceso y autorización

El usuario inicia sesión, el sistema valida credenciales y presenta funciones según el perfil. La modernización conservará ese flujo, pero la autorización se validará en API para cada operación.

## Consultas y reportes

Catálogos presentan tablas filtrables. Entradas, salidas, materiales utilizados y activo fijo exigen rango de fechas antes de recuperar información. Los cinco reportes siguen el patrón fecha inicial + fecha final + generar + exportar; actualmente devuelven error genérico con rango válido. La nueva versión validará rango, paginará resultados y mostrará errores correlacionables.

## Gestión y carga

Usuarios, perfiles y actividades permiten administración controlada. Facturación acepta múltiples XLS/XLSX y valida antes de guardar; se propone conservar el flujo seleccionar → validar → vista previa → confirmar → guardar transaccionalmente → bitácora, con errores por archivo, hoja, fila y columna.
