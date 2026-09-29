# Ambientes

## Configuración obligatoria de producción

El perfil `prod` requiere variables de entorno sin valores por defecto:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: datasource primario de CALE_IMMEX.
- `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`: datasource complementario
  `app24`/ANEXO24.
- `JWT_SECRET`: secreto de firma HS256. Debe tener al menos 32 bytes de material
  UTF-8 para ser compatible con `Keys.hmacShaKeyFor(...)`.
- `JWT_EXPIRATION_MINUTES`: vigencia del token en minutos.
- `APP_CORS_ALLOWED_ORIGINS`: patrones de origen permitidos por CORS.

Los valores se proporcionan sólo al proceso de ejecución mediante el mecanismo
de secretos del ambiente. No se versionan, imprimen ni se sustituyen por valores
de desarrollo. La separación de ambos datasources es intencional: el primario
atiende CALE_IMMEX y el secundario atiende el esquema complementario.

`SECURITY_RUNTIME_LEAST_PRIVILEGE` continúa pendiente: validar la cuenta SQL y
sus permisos para producción requiere una decisión de seguridad separada.

| Ambiente | Propósito | Datos | Control |
|---|---|---|---|
| Desarrollo | Construcción y pruebas locales | Sintéticos o copia anonimizada autorizada | Secretos locales fuera de Git. |
| Pruebas | Integración y aceptación | Datos controlados no productivos | Acceso restringido, bitácora y respaldo. |
| Producción | Operación autorizada | Información real | HTTPS, mínimo privilegio, monitoreo, respaldo y cambio aprobado. |

Las URL y credenciales se administran como configuración de cada ambiente, nunca dentro del repositorio.
