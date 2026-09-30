# Pruebas de staging de importaciones de catálogos V1

## Alcance

Se valida únicamente:

```text
upload → parser → validación → staging app24 → errores → preview
```

La confirmación hacia `CALE_IMMEX` no está implementada ni se ejecuta en estas
pruebas.

## Legacy observado

La auditoría read-only clasificó `CARGA_MATERIALES` y `CARGA_PRODUCTOS` como
`MIXED`. Se observaron `CargaMaterial`, `ECargaMaterial`, `FactoresMP`,
`material`, `tmpproductos`, `ECargaProducto` y `productos`. Los procedimientos
legacy no se ejecutaron porque pueden mutar staging global, errores y catálogos
operativos.

## Staging nuevo

La migración `10-catalog-imports-staging-v1.sql` fue aplicada en
`ANEXO24_DEV`:

- seis tablas `app24` de carga, fila y error;
- ocho SP versionados (dos commands y seis queries);
- permisos `MATERIALES_CARGAR` y `PRODUCTOS_CARGAR`;
- asignaciones a `ADMINISTRADOR`.

La migración es idempotente para tablas, índices, permisos y asignaciones. No
crea usuarios, no contiene contraseñas, no usa `DROP` y no modifica
`CALE_IMMEX`.

## Pruebas automatizadas

Backend:

- parser de materiales `.xls` y `.xlsx` válido;
- parser de productos `.xls` y `.xlsx` válido;
- encabezado obligatorio ausente;
- decimal inválido;
- clave duplicada;
- adapter con command SP separado para cada catálogo y 11 parámetros;
- 401 sin autenticación;
- 403 sin permiso;
- acceso con `MATERIALES_CARGAR` y `PRODUCTOS_CARGAR`;
- endpoint de errores devuelve únicamente errores, no filas de preview;
- compatibilidad de acciones de bitácora mediante `BitacoraAccion.valueOf`.

Límites efectivos en el endpoint/parser: archivo máximo `10 MiB`, máximo
`2,000` filas, `40` columnas y `100,000` celdas. Los límites se aplican en
código, no sólo en documentación.

Backend final: `./gradlew.bat clean test` — `439/439 PASS`, `failed = 0`, `skipped = 0`; `./gradlew.bat build` — `PASS`.

Frontend:

- pestañas visibles según permisos;
- upload de materiales y productos;
- `.xls` y `.xlsx` anunciados y aceptados por la UI;
- loading, éxito, preview, error recuperable y extensión inválida;
- endpoint multipart de productos;
- ruta de Catálogos con permisos exactos;
- no se muestra confirmación operativa.

## Smoke LIVE transaccional

Se ejecutó contra `ANEXO24_DEV` con valores sintéticos identificables:

| Caso | Estado | Cargas | Filas | Errores | Bitácora |
|---|---|---:|---:|---:|---|
| Material | `PREVISUALIZADA` | 1 | 1 | 0 | `CARGA_VALIDADA / EXITO` |
| Producto | `CON_ERRORES` | 1 | 1 | 1 | `CARGA_CON_ERRORES / FALLO` |

Dentro de la transacción se verificaron:

- command de cada catálogo;
- consulta por hash con `exists = 1`;
- detalle mediante `APP24_Q_*_CARGA_OBTENER`;
- errores mediante `APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES`;
- estados, filas y errores coherentes;
- bitácora controlada por correlación sintética.

Después de `ROLLBACK` se verificó:

```text
material_cargas_after_rollback = 0
material_rows_after_rollback = 0
material_errors_after_rollback = 0
producto_cargas_after_rollback = 0
producto_rows_after_rollback = 0
producto_errors_after_rollback = 0
audit_events_after_rollback = 0
```

Por tanto:

```text
PERSISTENT_TEST_DATA = 0
CALE_IMMEX_WRITES = 0
LEGACY_MUTABLE_SP_EXECUTED = 0
```

## Runtime product-like y confirmación

`docker compose -f compose.prod.yml build` y el arranque product-like pasaron.
El backend quedó `healthy` sin puerto host y el frontend `healthy` en
`8088:8080`. La ruta SPA `/catalogos/importaciones` respondió `200` con la
aplicación Angular real y el fallback de Nginx. Los `POST` protegidos de
Materiales y Productos a través de `/api/` respondieron `401` sin token; los
headers `X-Content-Type-Options`, `Referrer-Policy` y `X-Frame-Options` se
mantuvieron presentes. El stack se bajó sin volúmenes y no quedaron
contenedores del compose.

No se ejecutó login/browser autenticado ni se fabricaron JWT o credenciales.
La seguridad queda demostrada por tests 401/403/autorizado, los `401` runtime y
el smoke SQL de staging. No existe endpoint `/confirmar` en esta V1.

Mensaje funcional de la UI:

> Previsualización validada. La confirmación operativa todavía no está habilitada.

## Paridad

- `LEGACY-054`: `MISSING → PARTIAL`.
- `LEGACY-055`: `MISSING → PARTIAL`.

El estado no se eleva a `IMPLEMENTED_REDESIGNED` porque la confirmación de
materiales/productos hacia las tablas operativas legacy sigue fuera de alcance.
