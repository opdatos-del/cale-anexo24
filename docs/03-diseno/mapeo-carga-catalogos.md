# Mapeo de carga de catálogos V1

## 1. Alcance y frontera

Materiales cubre ruta controlada:

```text
archivo Excel → parser → staging app24 → preview → confirmación explícita
→ dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR → dbo.CARGA_MATERIALES
```

Productos permanecen en preview. La previsualización no toca `CALE_IMMEX`. La
confirmación de materiales usa locks de tabla, rechaza stage legacy ocupado,
delega reglas a `CARGA_MATERIALES` y no ejecuta SQL funcional desde Java.

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

Estados materiales:

- `PREVISUALIZADA`;
- `CON_ERRORES`;
- `CONFIRMADA`.

`13-material-confirmar-state-permission.sql` agrega `CONFIRMADA` y permiso
separado. El procesamiento es síncrono.

## 5. SP-first

Los adapters Java sólo invocan SP versionados:

| Catálogo | Command | Queries |
|---|---|---|
| Materiales | `APP24_C_CATALOGO_MATERIAL_CARGA_CREAR`; `dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR` → `dbo.CARGA_MATERIALES` | `APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH`, `APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER`, `APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES` |
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
- `POST /api/v1/catalogos/importaciones/materiales/{id}/confirmacion`;
- `GET /api/v1/catalogos/importaciones/materiales/{id}/errores`;
- `POST/GET /api/v1/catalogos/importaciones/productos`;
- `GET /api/v1/catalogos/importaciones/productos/{id}/errores`.

Permisos exactos:

- `MATERIALES_CARGAR`;
- `MATERIALES_CONFIRMAR`;
- `PRODUCTOS_CARGAR`.

La ruta moderna es `/catalogos/importaciones`, agrupada bajo Catálogos. La UI
permite seleccionar `.xls` y `.xlsx`, muestra loading, validación, totales,
preview, errores y retry. Una carga material `PREVISUALIZADA` muestra
confirmación explícita sólo con `MATERIALES_CONFIRMAR`, con aviso de efecto sobre
catálogo. En mobile usa scroll controlado de tabla y no carga todas las filas
fuera de la página.

## 7. Seguridad y escrituras

- 401 sin autenticación y 403 sin el permiso específico están cubiertos por
  tests de controller.
- La autorización con cada permiso específico está cubierta por tests.
- El hash evita duplicar silenciosamente una carga activa.
- La confirmación LIVE permanece pendiente de autorización; no se ejecutó write
  LIVE durante esta feature.
- Sólo wrapper técnico puede escribir `CargaMaterial`/`ECargaMaterial`; éste
  delega cambio autoritativo a `CARGA_MATERIALES`.
- Bitácora registra `MATERIAL_CARGA_CONFIRMADA` o
  `MATERIAL_CARGA_CONFIRMACION_ERROR` con carga, conteos y correlación.

## 8. Estado de paridad

- `LEGACY-054 Materiales`: `PARTIAL → IMPLEMENTED_REDESIGNED`.
- `LEGACY-055 Productos`: `MISSING → PARTIAL`.

Materiales incluye staging, parser, preview, RBAC separado, confirmación
controlada, rollback y bitácora; `LIVE_MUTATION_ACCEPTANCE = PENDING`. Productos
no representa confirmación de negocio.

## Seguimiento de implementación — clientes y proveedores

La superficie /catalogos/importaciones incorpora tabs de Clientes y Proveedores
con archivo XLS/XLSX, staging aislado, validación, preview, errores y confirmación
explícita protegida por CLIENTES_CARGAR/CLIENTES_CONFIRMAR y
PROVEEDORES_CARGAR/PROVEEDORES_CONFIRMAR.

Los comandos dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR y
dbo.APP24_C_PROVEEDOR_CARGA_CONFIRMAR confirman altas válidas nuevas desde sus
stages app24; las claves existentes se conservan sin actualización conforme al
contrato legacy INSERT-only. Los controladores, adapters, casos de uso, migrations
15/16, permisos y tests backend/frontend están versionados.

LEGACY_056 = PARTIAL
LEGACY_056_REMAINING_GAP = actualización de registros existentes no implementada
