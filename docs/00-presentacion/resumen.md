# Resumen ejecutivo para inicio de desarrollo

## Decisión solicitada

Autorizar la fase de construcción de la nueva aplicación Anexo 24 con un enfoque de integración controlada: se moderniza la interfaz y la lógica de aplicación sin cambiar la estructura de Módulo C ni sus procedimientos almacenados sin una autorización técnica y de negocio.

## Situación actual demostrada

La auditoría funcional realizada el 03 de septiembre de 2026 comprobó que el sistema vigente contiene catálogos de materiales (1,109 registros), productos (2,630), estructuras (aproximadamente 4,981 en una consulta amplia), consultas de entradas/salidas/consumo/activo fijo, cinco reportes, facturación por XLS/XLSX y gestión de usuarios, perfiles y 52 actividades. La evidencia proviene de pruebas controladas con datos sintéticos que fueron eliminados al finalizar.

## Riesgos que justifican la modernización

| Prioridad | Riesgo observado | Impacto | Respuesta propuesta |
|---|---|---|---|
| Crítica | Contraseña visible como texto y presente en DOM al editar. | Exposición de credenciales. | Hash Argon2id/bcrypt, formularios `password`, no retornar secretos. |
| Crítica | Usuario SAT con permisos administrativos amplios. | Cambios no autorizados. | Permisos por actividad, validación en backend y revisión periódica. |
| Alta | Cinco reportes fallan con un mensaje genérico. | Soporte lento y operación bloqueada. | Error con `correlationId`, logs técnicos y pruebas por reporte. |
| Alta | Carga de facturación no explica plantilla ni errores. | Reproceso y datos inconsistentes. | Plantilla versionada, vista previa y errores por archivo/fila/columna. |

## Alcance de la primera versión desarrollable

1. Autenticación, usuarios, perfiles y permisos en API.
2. Consultas de materiales, productos, estructuras y operaciones visibles, con filtros y paginación.
3. Reportes y exportación con diagnóstico rastreable.
4. Carga de facturación validada antes de guardar.
5. Bitácora inmutable de operaciones críticas.
6. Adaptador de integración con Módulo C y pruebas de comparación controlada.

## Información que se requiere de la empresa antes de conectar producción

- Acceso de solo lectura a una copia o ambiente controlado de Módulo C.
- Inventario de tablas, vistas, procedimientos, funciones y permisos.
- Diccionario/plantilla oficial de facturación y reglas de negocio de saldos, descargos y retornos.
- Responsable funcional que apruebe reglas, catálogos válidos y resultados de reportes.
- Criterios de datos personales, retención de bitácora, respaldo y ambiente de pruebas.

## Indicadores para la aceptación de la primera versión

- Ninguna contraseña se presenta, registra o transmite sin protección.
- Cada endpoint crítico exige permiso específico.
- Una muestra controlada devuelve resultados equivalentes entre Módulo C y la API nueva.
- Las cargas inválidas muestran ubicación y regla incumplida.
- Todo error operacional devuelve `correlationId` y conserva evidencia en bitácora/logs.
