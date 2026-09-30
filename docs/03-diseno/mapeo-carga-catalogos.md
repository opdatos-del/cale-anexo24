# Mapeo de carga de catálogos V1

## 1. Alcance y frontera

Esta entrega cubre únicamente la ruta segura:

```text
archivo Excel → parser → validación estructural → staging app24 → errores → preview
```

Aplica a **Materiales** y **Productos**. No confirma altas ni modificaciones en
`CALE_IMMEX`, no ejecuta los procedimientos legacy de carga y no escribe en los
stages legacy durante la previsualización.

La capacidad funcional queda como `PARTIAL`: el staging, la validación y la
preview están disponibles; la confirmación autoritativa queda para una feature
separada.

## 2. Auditoría legacy read-only

La auditoría de metadata y conteos se realizó sin ejecutar comandos mutables:

| Objeto | Tipo | Columnas | Filas observadas | Clasificación/uso |
|---|---|---:|---:|---|
| `dbo.CargaMaterial` | USER_TABLE | 31 | 2 | staging legacy observado |
| `dbo.ECargaMaterial` | USER_TABLE | 4 | 0 | errores legacy observado |
| `dbo.FactoresMP` | USER_TABLE | 6 | 2 | factores asociados a material |
| `dbo.material` | USER_TABLE | 19 | 2 | destino legacy de confirmación |
| `dbo.tmpproductos` | USER_TABLE | 8 | 0 | staging legacy observado |
| `dbo.ECargaProducto` | USER_TABLE | 4 | 0 | errores legacy observado |
| `dbo.productos` | USER_TABLE | 11 | 204 | destino legacy de confirmación |
| `dbo.CARGA_MATERIALES` | PROCEDURE | — | — | `MIXED`; no ejecutado |
| `dbo.CARGA_PRODUCTOS` | PROCEDURE | — | — | `MIXED`; no ejecutado |

`CARGA_MATERIALES` puede insertar, actualizar y eliminar en `CargaMaterial`,
`ECargaMaterial`, `material` y `FactoresMP`, además de validar unidades y reglas
de clave/fracción. `CARGA_PRODUCTOS` puede insertar y eliminar en
`tmpproductos`, `ECargaProducto` y `productos`. Ninguno fue ejecutado.

El stage legacy es global y no demostró aislamiento por usuario, carga o archivo;
por eso V1 no lo reutiliza.

## 3. Contrato de layout

No se identificó un layout oficial versionado aprobado para esta feature. La
fuente V1 es:

```text
PEDIMENT_LAYOUT_SOURCE = LEGACY_STAGE_DERIVED
CATALOG_IMPORT_LAYOUT_CONTRACT = PARTIAL
```

Los encabezados aceptados por el parser se derivan de la metadata del stage y de
las reglas observadas en los procedimientos legacy. La equivalencia física
completa entre cada encabezado y todas las 31/8 columnas del stage requiere una
revisión posterior; no se presenta este contrato como plantilla oficial.

| Catálogo | Encabezados V1 confirmados | Reglas estructurales | Estado |
|---|---|---|---|
| Materiales | `ClaveMaterial`, `ClaveMaterialProveedor`, `DescripcionComercial`, `UnidadComercial`, `UnidadTarifa`, `Fraccion`, factores de unidad, `DIVISION`, `ENTIDAD`, `TIPOM`, `NumeroSerie`, `MARCA`, `MODELO` | clave material mínima, proveedor opcional, fracción de 8 caracteres, factores decimales positivos, duplicados | `PARTIAL` |
| Productos | `CVE_PRODUCTO`, `NOMBRE`, `UNIDAD`, `fraccion`, `DIVISION`, `CVE_PRODUCTO_CLIENTE`, `AUXILIAR` | clave y nombre mínimos, fracción de 8 caracteres, clave cliente opcional, duplicados | `PARTIAL` |

La validación de unidades, tipos de material, almacenes o categorías contra
`CALE_IMMEX` no se activa por inferencia. Sólo se agregará cuando exista un
mapping confirmado para cada columna.

## 4. Modelo nuevo aislado

La migración `infra/sql/migrations/10-catalog-imports-staging-v1.sql`, aplicada
únicamente en `ANEXO24_DEV`, crea:

- `app24.CargaCatalogoMaterial` y `CargaCatalogoMaterialFila`;
- `app24.ErrorCargaMaterial`;
- `app24.CargaCatalogoProducto` y `CargaCatalogoProductoFila`;
- `app24.ErrorCargaProducto`.

Cada carga conserva archivo, SHA-256 único, usuario, fecha, estado, totales,
versión de contrato y correlación. Cada fila queda aislada por `carga_id`; los
errores conservan fila, columna, código, mensaje y valor enmascarado.

Estados V1:

- `PREVISUALIZADA`;
- `CON_ERRORES`.

El procesamiento es síncrono. No se introducen estados artificiales ni existe
estado `CONFIRMADA` en esta feature.

## 5. SP-first

Los adapters Java sólo invocan SP versionados:

| Catálogo | Command | Queries |
|---|---|---|
| Materiales | `APP24_C_CATALOGO_MATERIAL_CARGA_CREAR` | `APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH`, `APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER`, `APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES` |
| Productos | `APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR` | `APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH`, `APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER`, `APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES` |

La reconciliación LIVE confirmó los ocho objetos en `ANEXO24_DEV`, con la
misma firma estructural que los archivos versionados. No existe SQL de negocio
inline en Java.

La migración genera únicamente objetos `app24`, índices, permisos y asignaciones
idempotentes. No contiene `DROP`, usuarios, contraseñas ni referencias a
`CALE_IMMEX`.

## 6. API, permisos y UI

Endpoints:

- `POST/GET /api/v1/catalogos/importaciones/materiales`;
- `GET /api/v1/catalogos/importaciones/materiales/{id}/errores`;
- `POST/GET /api/v1/catalogos/importaciones/productos`;
- `GET /api/v1/catalogos/importaciones/productos/{id}/errores`.

Permisos exactos:

- `MATERIALES_CARGAR`;
- `PRODUCTOS_CARGAR`.

La ruta moderna es `/catalogos/importaciones`, agrupada bajo Catálogos. La UI
permite seleccionar `.xls` y `.xlsx`, muestra loading, validación, totales,
preview, errores y retry. No ofrece botón de confirmación; informa que la
confirmación operativa aún no está habilitada. En mobile usa scroll controlado de
tabla y no carga todas las filas fuera de la página.

## 7. Seguridad y escrituras

- 401 sin autenticación y 403 sin el permiso específico están cubiertos por
  tests de controller.
- La autorización con cada permiso específico está cubierta por tests.
- El hash evita duplicar silenciosamente una carga activa.
- `CALE_IMMEX` no recibe escrituras.
- `CARGA_MATERIALES`, `CARGA_PRODUCTOS`, `CARGAMATERIAL`, `tmpproductos`,
  `MATERIAL` y `PRODUCTOS` no se escriben desde V1.
- La bitácora usa las acciones controladas existentes `CARGA_VALIDADA` y
  `CARGA_CON_ERRORES`.

## 8. Estado de paridad

- `LEGACY-054 Materiales`: `MISSING → PARTIAL`.
- `LEGACY-055 Productos`: `MISSING → PARTIAL`.

El cambio representa staging, parser, validación, errores, preview, hash, RBAC y
durabilidad. No representa confirmación de negocio.
