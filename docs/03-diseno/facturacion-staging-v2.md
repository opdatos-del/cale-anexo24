# Facturación V2: layout y staging durable

## Alcance

Esta fase cierra el contrato estructural del archivo `Layout_Facturas.xlsx`,
la configuración activa en `app24.ConfiguracionPlantilla` y la recuperación de
filas normalizadas después de reiniciar el backend. No ejecuta ni simula la
confirmación hacia CALE_IMMEX.

Estados de esta fase:

- `FACTURACION_LAYOUT_STRUCTURE_VALIDATED`: PASS.
- `FACTURACION_APP_STAGING_DURABLE`: PASS una vez aplicada la migración
  `05-facturacion-staging-v2.sql` en `ANEXO24_DEV`.
- `FACTURACION_CONFIRM_BLOCKED_LEGACY_EXECUTION`: ACTIVE.

## Layout validado

- Hoja: `FACTURAS`, una invariancia del dominio de Facturación.
- `ConfiguracionPlantilla` define versión, extensión, columnas, obligatoriedad y tipos.
- Columnas: Documento, Fecha, Almacen, Observaciones, Descarga, Tipo, Linea,
  Clave, Lote, Cantidad, Unidad, Dirigido y Cliente.
- Obligatorias: Documento, Fecha, Descarga, Tipo, Linea, Clave, Cantidad y
  Unidad.
- Opcionales: Almacen, Observaciones, Lote, Dirigido y Cliente.

La estructura validada no demuestra todavía el pipeline de guardado legacy.

El parser construye cada fila en el orden canónico de la plantilla buscando cada
encabezado por nombre normalizado; no depende del orden físico del XLSX. Las
columnas opcionales ausentes se persisten como cadena vacía y las columnas
adicionales desconocidas generan `COLUMNA_NO_CONFIGURADA` sin entrar a
`datos_json`. Las fechas válidas se normalizan a `yyyy-MM-dd` y las cantidades
a decimal positivo mediante `BigDecimal.toPlainString()`.
`CARGAFACTURASENPSALIDAS` requiere campos monetarios, fracción y país que no
forman parte del layout auditado (`NOT_PROVEN_COMPATIBLE_WITH_INTFACTURACION`).
`CARGA_FACTURAS` tampoco tiene correspondencia 1:1 demostrada. El pipeline
autoritativo continúa `UNRESOLVED`.

## Persistencia app24

`app24.CargaFacturacionFila` guarda una fila normalizada por registro, con
`hoja`, número de fila y `datos_json` limitado a las 13 claves del layout.
Existe FK hacia `CargaFacturacion`, validación `ISJSON` e índice único por
`(carga_id, fila)`. El workbook binario no se guarda en SQL ni en filesystem
permanente.

La creación se realiza mediante `APP24_C_FACTURACION_CARGA_CREAR` dentro del
transaction manager `appTransactionManager`; carga, filas, errores y evento de
bitácora forman una unidad app24. La plantilla se obtiene sólo mediante
`APP24_Q_FACTURACION_PLANTILLA_ACTIVA`; si no hay configuración activa se
rechaza la operación con HTTP 409 y `FACTURACION_PLANTILLA_NO_CONFIGURADA`,
incluyendo `correlationId`. No existe fallback provisional. La hoja `FACTURAS`
es una invariancia del dominio; la configuración DB define versión, extensión,
columnas, obligatoriedad y tipos.

## API

- `GET /api/v1/facturacion/plantilla`: contrato activo, columnas, tipos y
  obligatoriedad; sin plantilla responde HTTP 409 estructurado.
- `GET /api/v1/facturacion/plantilla/archivo`: XLSX sin filas de ejemplo;
  sin plantilla responde el mismo HTTP 409 estructurado.
- `POST /api/v1/facturacion/cargas`: valida y persiste staging app24.
- `GET /api/v1/facturacion/cargas/{id}?pagina=1&tamano=100`: recupera metadata,
  filas y errores desde DB, no desde memoria.

Todos requieren `FACTURACION_CARGAR` salvo las respuestas de autenticación
estándar. No existe endpoint de confirmación en esta fase.
