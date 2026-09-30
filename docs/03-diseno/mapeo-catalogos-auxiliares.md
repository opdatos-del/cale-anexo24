# Mapeo de catálogos auxiliares V1

## 1. Alcance

Esta feature implementa únicamente consultas `READ_ONLY` de catálogos auxiliares
cuya fuente, clave y semántica fueron demostradas en el repositorio y en
`CALE_IMMEX`:

- unidades;
- tipos de material;
- almacenes;
- categorías.

No implementa proveedores, clientes, agentes aduanales, submaquilas, datos
generales, consultas guardadas ni un catálogo independiente de divisiones. La
evidencia de `DIVISION`/`ENTIDAD` no demuestra que exista un catálogo canónico
separado de `almacen`.

No se agregaron comandos de alta, edición o eliminación.

## 2. Evidencia de descubrimiento

La auditoría se apoyó en:

- `infra/sql/01-seed-cale-immex-test.sql` para nombres de tablas, columnas y
  relaciones de datos controlados;
- referencias de `unidad`, `TipoMaterial`, `categorias` y `ALMACENKEY` en los
  mapeos legacy;
- consultas read-only contra `CALE_IMMEX`, ejecutadas con la identidad
  autorizada configurada localmente;
- el patrón existente de Materiales y Productos: controller → caso de uso →
  puerto → adapter JDBC → procedimiento versionado.

### Resultado read-only y reconciliación de CALE_IMMEX

La ejecución LIVE sin filtro (`Pagina = 1`, `Tamano = 100`) reconcilió cada
procedimiento contra el conteo de su tabla fuente. `Rows` es el número de filas
recibidas por el result set y `Claves inválidas` cuenta claves nulas o vacías.

| Catálogo | Fuente | Source count | SP total | Rows | Claves inválidas | Resultado |
|---|---|---:|---:|---:|---:|---|
| Unidades | `dbo.unidad` | 7 | 7 | 7 | 0 | `PASS` |
| Tipos de material | `dbo.TipoMaterial` | 4 | 4 | 4 | 0 | `PASS` |
| Almacenes | `dbo.almacen` | 3 | 3 | 3 | 0 | `PASS` |
| Categorías | `dbo.categorias` | 3 | 3 | 3 | 0 | `PASS` |

Las proyecciones LIVE conservaron las claves y descripciones esperadas: unidad
(`CVE_UNIDAD`, `NOMBRE`, `ALIAS`), tipo (`TipoMaterial`), almacén
(`ALMACENKEY`, `ALMACEN`, `DESCRIPCION`) y categoría (`categoria`,
`descripcion`, `Dias_Validos`, `meses`). No hubo exclusiones adicionales por
`WHERE`, `TRIM`, `NULL` o estado. `SP_SOURCE_RECONCILIATION = PASS`.

| Fuente | Filas observadas | Clave | Descripción / etiqueta | Relaciones observadas | Contrato |
|---|---:|---|---|---|---|
| `dbo.unidad` | 7 | `CVE_UNIDAD` | `NOMBRE`, `ALIAS` | `material.unidad`, `material.unidadt`, `productos.UNIDAD`, `productos.UNIDADT`, factores | `CONTRACT_CONFIRMED` |
| `dbo.TipoMaterial` | 4 | `TipoMaterial` | La propia columna `TipoMaterial` | `material.tipomaterial` | `CONTRACT_CONFIRMED` |
| `dbo.almacen` | 3 | `ALMACENKEY` | `ALMACEN`, `DESCRIPCION` | `material.ALMACENKEY`, `productos.ALMACENKEY`, proveedores e importaciones | `CONTRACT_CONFIRMED` |
| `dbo.categorias` | 3 | `categoria` | `descripcion`, `Dias_Validos`, `meses` | `partidas.Categoria` e informes/procesos documentados | `CONTRACT_CONFIRMED` |
| `DIVISION`/`ENTIDAD` | no aplica | no demostrado | no demostrado | campos observados en cargas y datos generales | `CONTRACT_UNKNOWN` |

La consulta no detectó una fuente canónica independiente para `DIVISION`; por
eso la API expone `almacenes`, no `divisiones`. La capacidad de almacenes está
implementada y la de división independiente permanece `UNKNOWN`.

## 3. Contratos API

Todos los endpoints requieren autenticación y el permiso único
`CATALOGOS_AUX_CONSULTAR`:

| Endpoint | Fuente | Procedimiento | Filtro | Paginación |
|---|---|---|---|---|
| `GET /api/v1/catalogos/unidades` | `dbo.unidad` | `dbo.APP24_Q_UNIDADES_LISTAR` | clave, nombre o alias | base 1, 1–100 |
| `GET /api/v1/catalogos/tipos-material` | `dbo.TipoMaterial` | `dbo.APP24_Q_TIPOS_MATERIAL_LISTAR` | nombre | base 1, 1–100 |
| `GET /api/v1/catalogos/almacenes` | `dbo.almacen` | `dbo.APP24_Q_ALMACENES_LISTAR` | id, clave o descripción | base 1, 1–100 |
| `GET /api/v1/catalogos/categorias` | `dbo.categorias` | `dbo.APP24_Q_CATEGORIAS_LISTAR` | clave o descripción | base 1, 1–100 |

La respuesta conserva el wrapper común `items`, `total`, `pagina` y `tamano`.
Los filtros vacíos se normalizan a `NULL`. No hay SQL de negocio inline en Java.

## 4. Stored procedures

Se versionaron y aplicaron en `CALE_IMMEX` únicamente estos procedimientos de
consulta; su creación/actualización fue el único DDL ejecutado sobre esa base:

- `APP24_Q_UNIDADES_LISTAR`;
- `APP24_Q_TIPOS_MATERIAL_LISTAR`;
- `APP24_Q_ALMACENES_LISTAR`;
- `APP24_Q_CATEGORIAS_LISTAR`.

Cada procedimiento valida la paginación, calcula `COUNT_BIG`, aplica filtro
opcional y devuelve una proyección estable con `ORDER BY` determinista. La
reconciliación sin filtro devolvió `total` y filas iguales a la fuente para los
cuatro procedimientos.

Semántica de ejecución registrada:

- `CALE_IMMEX` data writes (`INSERT`/`UPDATE`/`DELETE`): `0`;
- `CALE_IMMEX` table DDL: `0`;
- `CALE_IMMEX` SP DDL: `4` (`CREATE OR ALTER PROCEDURE` read-only);
- SP legacy mutables ejecutados: `0`;
- SQL de negocio inline en Java: `0`.

La migración `infra/sql/migrations/08-catalogos-aux-permission.sql` fue auditada
estáticamente y aplicada una vez a `ANEXO24_DEV` (`APP_MIGRATION_08_APPLIED =
PASS`). Usa `IF NOT EXISTS` para la actividad y un `INSERT ... SELECT` con
`NOT EXISTS` para asignaciones; no contiene `DELETE`, passwords, usuarios nuevos
ni referencias a `CALE_IMMEX`. La validación LIVE dejó una actividad
`CATALOGOS_AUX_CONSULTAR`, una asignación a `ADMINISTRADOR`, una a `CONSULTA` y
cero duplicados.

El permiso también está en la semilla versionada y coincide exactamente entre
backend, guard de ruta, sidebar, tests y migración: `CATALOGOS_AUX_CONSULTAR`.

## 5. Backend

Cada contrato tiene su propio módulo bajo
`backend/src/main/java/com/jovycandy/anexo24/catalogs/auxiliary/` con:

- modelo y puerto de dominio;
- caso de uso de consulta;
- DTO y controller explícitos;
- adapter JDBC que sólo invoca su SP versionado.

La validación de paginación se comparte como regla técnica pequeña; no existe un
`GenericCatalogController` ni un motor universal de catálogos.

## 6. Frontend

La ruta lazy `/catalogos` se protege con `CATALOGOS_AUX_CONSULTAR` y se presenta
en una única superficie agrupada:

- selector de Unidades, Tipos de material, Almacenes y Categorías;
- tabla responsive con columnas propias de cada contrato;
- búsqueda, paginación y actualización;
- loading state, empty state, error recuperable y retry;
- sin acciones de escritura.

El sidebar agrega una sola entrada `Catálogos auxiliares` cuando el permiso está
presente. No se alteraron `/materiales`, `/productos` ni `/estructuras`.

## 7. Impacto de paridad

Con la reconciliación LIVE y los tests de seguridad cerrados, la matriz registra:

- `LEGACY-005` Tipos de material → `IMPLEMENTED_REDESIGNED`;
- `LEGACY-006` Unidades → `IMPLEMENTED_REDESIGNED`;
- `LEGACY-007` Categorías → `IMPLEMENTED_REDESIGNED`;
- `LEGACY-008` Divisiones/almacenes → `PARTIAL`: almacenes implementados en
  `/catalogos/almacenes`; división independiente permanece `UNKNOWN`.

Los conteos globales quedan en 5 `IMPLEMENTED_EQUIVALENT`, 17
`IMPLEMENTED_REDESIGNED`, 4 `PARTIAL`, 13 `MISSING`, 5 `BLOCKED_BUSINESS`, 6
`CONSOLIDATE` y 29 `UNKNOWN` sobre 79 capacidades. Esto no declara paridad
funcional completa.

## 8. Fuera de alcance

Permanecen sin implementación en esta feature:

- proveedores;
- clientes;
- agentes aduanales;
- submaquilas;
- datos generales;
- consultas guardadas;
- divisiones como catálogo independiente;
- cargas de archivos y cualquier proceso mutable;
- Saldos, descargos, Facturación confirmation, Dashboard, Ajuste anual y Anexo 30.
