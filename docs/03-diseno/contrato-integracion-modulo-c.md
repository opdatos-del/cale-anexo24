# Contrato de integración con Módulo C

## Principios técnicos

1. Cada acceso sale del módulo de negocio hacia un adaptador específico; Angular nunca se conecta a SQL Server.
2. Las consultas son parametrizadas y devuelven DTOs; no se concatena SQL.
3. El adaptador registra duración, resultado y `correlationId`, pero no secretos ni sentencias con datos sensibles.
4. Los objetos existentes se consumen con permisos mínimos y preferentemente mediante cuenta técnica diferenciada por ambiente.
5. Ninguna regla de saldo, descargo, retorno o pedimento se reimplementa hasta compararla con el comportamiento autorizado de Módulo C.

## Matriz de mapeo que se llenará con evidencia técnica

| Dominio | Endpoint API | Entrada | Salida mínima | Objeto Módulo C | Evidencia de equivalencia |
|---|---|---|---|---|---|
| Materiales | `GET /catalogos/materiales` | página, tamaño, filtros | lista, total, metadatos | Vista/procedimiento autorizado | Muestra comparada y aprobada. |
| Estructuras | `GET /estructuras` | producto, fechas | producto-material-cantidad-vigencia | Vista/procedimiento autorizado | Consulta amplia y por fecha. |
| Entradas/salidas | `GET /operaciones/{tipo}` | rango y filtros | movimientos paginados | Vista/procedimiento autorizado | Conteo y muestra por periodo. |
| Reportes | `GET /reportes/{tipo}` | rango y filtros | filas/exportación | Procedimiento autorizado | Resultado y manejo de excepción. |
| Saldos | `GET /saldos` | corte, filtros | saldo inicial/movimientos/final | Regla confirmada | Reconciliación aprobada. |
| Facturación | `POST /facturacion/cargas` | archivo y plantilla | diagnóstico o lote | Proceso autorizado | Prueba de rollback e idempotencia. |

## Checklist de acceso técnico

Registrar instancia, base, esquema, objetos, propietario, permisos de ejecución, parámetros, tipos de datos, resultados, dependencias, índices relevantes, transacciones, errores conocidos y datos de prueba. El inventario se realiza inicialmente en modo lectura y su resultado se anexa al expediente técnico.
