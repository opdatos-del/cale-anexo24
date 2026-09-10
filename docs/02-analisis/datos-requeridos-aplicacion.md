# Datos requeridos por la aplicación


## Datos de consulta por módulo

| Módulo | Datos mínimos a mostrar | Filtros/criterios | Estado de evidencia ||
|---|---|---|---|---|
| Datos generales | Razón social, RFC, registro IMMEX y domicilio fiscal. | Consulta única; guardar/cancelar visibles. | Observado. |
| Materiales | Fracción arancelaria, descripción, unidad TIGIE, UMC y número de parte. | Parte, descripción, fracción y paginación. | Observado; estado es propuesta opcional. |
| Productos | Fracción arancelaria, descripción, unidad, UMC y número de parte. | Parte, descripción, fracción y paginación. | Observado; estado es propuesta opcional.|
| Estructuras | Producto, material y vigencia de consulta. Cantidad, unidad de consumo y vigencia inicio/fin detallada. | Producto y rango de fechas. | Relación y filtros observados; cantidades y vigencias detalladas por confirmar técnicamente. |
| Entradas | Pedimento, clave de pedimento, fecha de entrada, fecha de pago, fracción, UMC, cantidad y número de parte. | Rango obligatorio y filtros observados. | Observado. |
| Salidas | Pedimento, fecha, clave, fracción, UMC, cantidad y número de parte. | Rango obligatorio y filtros observados. | Observado. |
| Material utilizado | Importación, exportación, material, producto, cantidad consumida, unidad y fecha. | Rango obligatorio. | Relación importación-exportación-consumo observada; campos internos por confirmar. |
| Activo fijo | Pedimento, descripción, marca, modelo, número de serie y fechas. | Rango de fechas y filtros observados. | Observado. |
| Saldos | Parte/material, unidad, saldo inicial, entradas, consumo, salidas/retornos, saldo final y fecha de corte. | Fecha de corte, parte y fracción. | Propuesto para reporte; fórmula y fuentes por confirmar. |
| Pedimentos, importaciones temporales y retornos | Identificador, clave, fechas, partidas, cantidades, unidad, estatus y relaciones de descargo. | Filtros definidos por la regla autorizada. | Requeridos por proyecto; campos físicos y reglas por confirmar.|

## Datos de facturación y carga de archivos

La auditoría observó que la pantalla acepta `.xls` y `.xlsx`, permite selección múltiple y contiene los botones **CARGAR**, **GUARDAR**, **LIMPIAR** y **DESCARGAR**. La tabla visible muestra estos datos, que deben formar el contrato mínimo de una plantilla inicial:

| Campo visible | Tipo propuesto | Regla inicial |
|---|---|---|
| Documento | Texto | Obligatorio; longitud y unicidad por confirmar. |
| Fecha | Fecha | Obligatoria; formato definido por plantilla. |
| Almacén | Texto/catálogo | Obligatorio si la regla de operación lo exige. |
| Observaciones | Texto | Opcional; límite por definir con negocio. |
| Descarga | Referencia o cantidad | Significado y regla por confirmar. |
| Tipo | Texto/catálogo | Catálogo autorizado por empresa. |
| Línea | Texto/catálogo | Regla por confirmar. |
| Clave | Texto | Identificador de negocio; validar catálogo/longitud. |
| Lote | Texto | Validar formato y posible trazabilidad. |
| Cantidad | Decimal | Mayor que cero; precisión y escala por confirmar. |
| Unidad | Texto/catálogo | Debe corresponder al catálogo autorizado. |
| Dirigido | Texto/catálogo | Regla y valores permitidos por confirmar. |
| Cliente | Texto/catálogo | Regla y valores permitidos por confirmar. |

La nueva aplicación debe validar extensión, tamaño, columnas, tipos, obligatoriedad, catálogos, duplicados y reglas de negocio antes de guardar. El resultado debe señalar archivo, hoja, fila, columna, valor enmascarado, código de regla y mensaje.

## Datos administrados por la aplicación nueva

| Dominio | Datos necesarios | Estado de evidencia | Uso |
|---|---|---|---|
| Usuario | Clave (máximo observado 35), nombre (máximo observado 100), correo, confirmación de correo, perfil, vigencia, indicador Usuario SAT, bloqueo, estado, hash de contraseña y fechas de auditoría. | Campos y límites principales observados; hash/estado/auditoría propuestos. | Acceso y responsabilidad de acciones. |
| Credencial | Hash de contraseña, fecha de cambio, intentos fallidos, último acceso y política aplicada. | Propuesto por seguridad; la interfaz actual expone contraseña como texto y debe corregirse. | Protección de cuenta y bloqueo controlado. |
| Perfil | Nombre, descripción, estado y fechas. | Nombre y asignación de actividades observados; campos administrativos propuestos. | Agrupar permisos. |
| Actividad | Clave, nombre, recurso, acción, estado. | Clave/nombre observados; recurso/acción propuestos para API. | Autorizar operaciones de backend. |
| PerfilActividad | Perfil, actividad, fecha de asignación y responsable. | Relación observada; trazabilidad propuesta. | Evitar permisos implícitos. |
| Bitácora | Usuario, fecha de ingreso, módulo, acción, detalle, fecha de acción, resultado y `correlationId`. | Primeros seis campos observados; resultado/correlación propuestos. | Soporte, trazabilidad y cumplimiento. |
| Carga | Archivo, hash SHA-256, usuario, fecha, estado, plantilla/versión, totales de filas y `correlationId`. | Propuesto. | Idempotencia y seguimiento. |
| ErrorCarga | Hoja, fila, columna, valor enmascarado, código de regla y mensaje. | Propuesto a partir de la deficiencia observada. | Corrección por el usuario. |
| PlantillaCarga | Nombre, versión, extensiones, columnas, tipos, obligatoriedad y catálogos permitidos. | Propuesto; la plantilla oficial no fue visible en auditoría. | Validación sin cambiar código. |

## Reglas de tipos y seguridad para construir tablas nuevas

- Usar `datetime2` en UTC para eventos, vigencias y auditoría; conservar la zona horaria en presentación.
- Usar `decimal(p,s)` para cantidades; `p` y `s` se cierran con la unidad y las reglas de Módulo C, no por suposición.
- Definir textos de negocio como `nvarchar`, con longitudes confirmadas por muestra técnica; no truncar silenciosamente.
- Guardar contraseñas únicamente como hash fuerte; nunca almacenar ni devolver contraseña, token o secreto.
- Indexar identificadores de negocio, fechas de consulta, estado, claves foráneas y `correlationId` según el patrón de acceso.
- Tratar RFC, datos comerciales, archivos y detalles de error conforme a la clasificación de información de la empresa; nunca copiar archivos completos o secretos a logs/bitácora.

## Cierre necesario antes de crear o mapear tablas

Antes de decidir una tabla física de comercio exterior se requiere: inventario de Módulo C, llaves primarias/foráneas, tipos, catálogos, procedimientos, muestra anonimizada, reglas de saldo/descargo/retorno y plantilla oficial de facturación. Con esa evidencia se transforma este documento en un diccionario técnico de tablas y un modelo ER definitivo.
