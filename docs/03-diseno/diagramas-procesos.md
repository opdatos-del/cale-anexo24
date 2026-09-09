# Diagramas de procesos

## Carga de facturación

[Abrir diagrama de carga validada](../../diagramas/carga-facturacion.html)

El proceso evita el comportamiento actual de error genérico: primero comprueba extensión y tamaño, después la plantilla y los datos, permite revisar el resumen y solo registra tras confirmación. Los rechazos indican localización y regla incumplida; el registro es transaccional y deja bitácora.

## Consulta operativa

1. El usuario autorizado elige módulo y rango de fechas.
2. La API valida permisos y parámetros.
3. El adaptador ejecuta la consulta autorizada en Módulo C.
4. La aplicación entrega resultados paginados, filtros aplicados y correlación de la solicitud.
5. La exportación se habilita únicamente cuando existen resultados.

