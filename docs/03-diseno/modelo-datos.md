# Modelo de datos propuesto

El modelo es **lógico y complementario**: no afirma ni reemplaza el modelo interno de Módulo C. Las entidades de comercio exterior se consumen mediante vistas o procedimientos autorizados; el esquema `anexo24_app` conserva configuración y trazabilidad propias de la aplicación.

## Entidades complementarias

| Entidad | Campos principales | Relación |
|---|---|---|
| UsuarioApp | id, clave, nombre, correo, password_hash, estado, vigencia | N:1 PerfilApp; registra BitacoraEvento. |
| PerfilApp | id, nombre, estado | N:M Actividad mediante PerfilActividad. |
| Actividad | id, clave, nombre, recurso, acción | Define permisos de API. |
| BitacoraEvento | id, usuario_id, fecha, módulo, acción, detalle, correlacion_id, resultado | N:1 UsuarioApp; inmutable. |
| CargaFacturacion | id, archivo, hash, fecha, usuario_id, estado, total/registros válidos/inválidos | N:1 UsuarioApp; 1:N ErrorCarga. |
| ErrorCarga | id, carga_id, hoja, fila, columna, valor, regla, mensaje | N:1 CargaFacturacion. |
| ConfiguracionPlantilla | id, nombre, versión, extensión, columnas_json, activa | Controla la plantilla oficial sin código duro. |

## Integración con Módulo C

Se representan adaptadores de lectura para Material, Producto, Estructura, Entrada, Salida, MaterialUtilizado, ActivoFijo, Pedimento, ImportacionTemporal, Retorno y Saldo. Sus claves físicas, tablas y cardinalidades se incorporarán solamente al recibir el inventario técnico autorizado. Las consultas parametrizadas y procedimientos evitan concatenación SQL.

## Reglas de integridad

- Claves UUID o `bigint` consistentes por esquema; índices en claves foráneas, fechas, estado y correlación.
- `password_hash` usa Argon2id o bcrypt; nunca se guarda contraseña reversible.
- Bitácora y errores conservan fecha UTC, usuario y resultado; no guardan secretos ni contenido completo de archivos.
- CargaFacturacion usa hash de archivo para detectar duplicados y estado `VALIDANDO`, `RECHAZADA`, `LISTA`, `GUARDADA` o `FALLIDA`.
