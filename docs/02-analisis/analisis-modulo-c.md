# Análisis de Módulo C

## Base de conocimiento

Módulo C es una base SQL Server utilizada por Anexo 24. La integración es de solo lectura estructural hasta recibir autorización expresa; no se modifican tablas, vistas, procedimientos ni reglas actuales.

## Inventario técnico autorizado

La primera actividad técnica será un inventario reproducible de `sys.tables`, `sys.views`, `sys.procedures`, `sys.sql_modules`, llaves, índices, triggers, jobs y dependencias. Cada objeto se registrará con esquema, propósito, parámetros/columnas, permisos, consumidor, tablas impactadas y riesgo. El resultado será el anexo técnico de integración, no una suposición documental.

## Mapa funcional a confirmar

| Dominio visible | Contrato de integración esperado |
|---|---|
| Materiales, productos y estructuras | Consultas paginadas y filtradas sin escritura directa. |
| Entradas, salidas, consumo, activo fijo y saldos | Operaciones por rango de fecha y filtros; resultados consistentes con el sistema vigente. |
| Pedimentos, importaciones y retornos | Lectura de relaciones y reglas fiscales existentes. |
| Usuarios, perfiles y actividades | El nuevo esquema de seguridad no debe sustituir ni alterar datos sin decisión autorizada. |
| Facturación | Validación y registro transaccional detrás de un adaptador; plantilla oficial como artefacto de configuración. |

## Criterio de aceptación de integración

Cada consulta del sistema nuevo deberá quedar trazada como pantalla → endpoint → servicio → procedimiento/vista/tablas, con prueba de lectura y comparación de resultados contra una muestra controlada. Cualquier diferencia funcional se tratará como decisión de negocio, no como corrección silenciosa.
