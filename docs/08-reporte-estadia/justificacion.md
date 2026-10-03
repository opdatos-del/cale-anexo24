# Justificación

La modernización mejora la confidencialidad de credenciales, reduce privilegios
excesivos, convierte fallas genéricas en incidentes rastreables y permite cargas
con validación entendible. Mantener la integración con la base operativa
existente protege la continuidad operativa mientras se desacopla la nueva
interfaz de detalles físicos no autorizados.

En términos del proyecto, la justificación se materializa así:

- **Riesgo de seguridad:** autenticación con JWT, autorización por permisos en
  backend y en rutas, y auditoría de accesos en bitácora.
- **Confiabilidad de la información:** consultas a `CALE_IMMEX` exclusivamente
  vía procedimientos almacenados read-only versionados (SP-FIRST), sin SQL
  funcional embebido en Java.
- **Operación de cargas:** validación temprana, errores por archivo/hoja/fila/
  columna, hash anticuplicados y staging aislado; la confirmación autoritativa
  sólo existe donde hay contrato y caso de aceptación (pedimentos).
- **Cumplimiento y mantenimiento:** documentación de paridad por capacidad,
  gates automatizados (SP-FIRST, permisos, pruebas, build) y cierre técnico con
  inventario de despliegue verificado.

El proyecto no reemplaza al sistema legacy en su totalidad: prioriza las
capacidades con contrato suficiente y deja explícitas las decisiones y
evidencias pendientes.
