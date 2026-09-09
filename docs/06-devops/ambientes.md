# Ambientes

| Ambiente | Propósito | Datos | Control |
|---|---|---|---|
| Desarrollo | Construcción y pruebas locales | Sintéticos o copia anonimizada autorizada | Secretos locales fuera de Git. |
| Pruebas | Integración y aceptación | Datos controlados no productivos | Acceso restringido, bitácora y respaldo. |
| Producción | Operación autorizada | Información real | HTTPS, mínimo privilegio, monitoreo, respaldo y cambio aprobado. |

Las URL y credenciales se administran como configuración de cada ambiente, nunca dentro del repositorio.
