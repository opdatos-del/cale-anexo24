# Casos de uso

## CU-001: Iniciar sesión

- **Actor:** Usuario autorizado.
- **Objetivo:** acceder solo a las funciones asignadas.
- **Precondición:** cuenta activa y vigente.
- **Flujo:** captura credenciales; API valida hash y estado; crea sesión/tokens seguros; carga permisos; muestra inicio.
- **Alternos:** credenciales inválidas, cuenta bloqueada o vencida devuelven mensaje seguro y evento de bitácora.
- **Resultado:** sesión autenticada con permisos mínimos.

## CU-030: Consultar operación

El usuario selecciona entradas, salidas, materiales utilizados o activo fijo e indica rango de fechas. La API valida permiso, rango y filtros; consulta Módulo C con parámetros; devuelve resultados paginados. Sin resultados devuelve colección vacía y no error.

## CU-040: Generar reporte

El usuario selecciona reporte y rango. La API valida parámetros, ejecuta la consulta consolidada y devuelve datos o archivo exportable. Cualquier fallo registra correlación y muestra un mensaje accionable sin revelar detalles internos.

## CU-050: Cargar facturación

El usuario selecciona XLS/XLSX; el sistema valida extensión, tamaño, plantilla y contenido; presenta errores localizables o vista previa; con confirmación guarda transaccionalmente y registra la bitácora.
