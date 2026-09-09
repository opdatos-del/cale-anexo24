# Requerimientos funcionales

| ID | Requerimiento | Evidencia/decisión |
|---|---|---|
| RF-001 | Iniciar sesión y restringir módulos por permiso. | Observado en sistema actual; reforzado en API. |
| RF-003 | Administrar usuarios, perfiles y actividades. | CRUD auditado con datos sintéticos. |
| RF-010 | Consultar datos generales, materiales, productos y estructuras. | Catálogos observados. |
| RF-020 | Consultar entradas, salidas, consumo y activo fijo con filtros. | Operación visible; rango obligatorio. |
| RF-024 | Consultar pedimentos, importaciones, retornos y saldos por contratos autorizados. | Dominio requerido por proyecto; implementación contra inventario técnico. |
| RF-030 | Generar reportes de entradas, salidas, saldos, consumo y bitácora. | Cinco reportes auditados; se corrige error genérico. |
| RF-036 | Exportar cuando existan resultados. | Comportamiento observado. |
| RF-040 | Cargar XLS/XLSX con validación detallada y confirmación. | Carga observada y rediseñada. |
| RF-050 | Registrar y consultar bitácora. | Campos observados: usuario, fechas, módulo, acción y detalle. |
