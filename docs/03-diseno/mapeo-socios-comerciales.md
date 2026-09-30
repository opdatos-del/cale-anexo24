# Mapeo de socios comerciales V1

## Alcance

Consulta read-only paginada de clientes, proveedores y agentes aduanales. No se ejecutan procesos legacy de carga ni se escribe en `CALE_IMMEX`.

## Contratos confirmados

| Catálogo | Fuente | Filas LIVE | Clave | Etiqueta | Campos auxiliares | Calidad observada |
|---|---|---:|---|---|---|---|
| Clientes | `dbo.clientes` | 5 | `Clave` | `Nombre` | `Idfiscal`, `Pais`, `Correo` | 0 claves/nombres/fiscales vacíos; 0 claves duplicadas |
| Proveedores | `dbo.Proveedores` | 5 | `Clave` | `Nombre` | `Idfiscal`, `Pais`, `Correo` | 0 claves/nombres/fiscales vacíos; 0 claves duplicadas |
| Agentes aduanales | `dbo.agentes` | 2 | `Clave` | `Nombre` | `Rfc`, `Patente`, `AgenciaAduanal` | 0 claves/nombres/RFC vacíos; 0 claves duplicadas |

Las tablas no declaran PK/FK para estos maestros. La clave funcional observada es `Clave`; la V1 no modifica esquema legacy.

## Procedimientos nuevos

- `dbo.APP24_Q_CLIENTES_LISTAR`
- `dbo.APP24_Q_PROVEEDORES_LISTAR`
- `dbo.APP24_Q_AGENTES_ADUANALES_LISTAR`

Aceptan `Filtro`, `Pagina`, `Tamano` y `Total OUTPUT`; límite 100 filas. Java no contiene SQL funcional inline.

## Pipelines legacy

| Procedimiento | Clasificación | Evidencia | Ejecución |
|---|---|---|---|
| `CARGACLIENTES` | WRITE/MIXED | 4 tokens INSERT; usa staging, errores y maestro | No ejecutado |
| `CARGAPROVEEDORES` | WRITE/MIXED | 4 tokens INSERT; usa staging, errores y maestro | No ejecutado |
| `CARGAAgentes` | WRITE/MIXED | 5 tokens INSERT; usa staging, errores y maestro | No ejecutado |
| `CARGA_SUBMAQUILA` | MIXED | 2 tokens INSERT; usa `TMPSUBMAQUILA`, productos, salidas y partidas | No ejecutado |

`TMPCLIENTES`, `TMPPROVEEDORES` y `TMPagentes`, junto con sus tablas de error, no tienen usuario, lote, archivo ni hash. Son staging global y no apto para preview concurrente moderno. Por falta de layout autoritativo y reglas cerradas, no se implementa import staging en esta fase.

## Submaquila

`TMPSUBMAQUILA`, `RelacionSubmaquila`, `Encabezadotransubmaquila` y `Detalletransubmaquila` existen, pero estaban vacías y modelan proceso/transferencia, no un maestro canónico de submaquiladores. `SUBMAQUILA_READ = UNKNOWN`; no se expone endpoint ni tab.

## Relaciones observadas

Módulos SQL confirman uso de clientes en facturación/exportaciones y proveedores en importaciones/informes. No se observaron FK declaradas; estas relaciones son lógicas y no se inventan constraints.

## Seguridad

Los tres catálogos reutilizan `CATALOGOS_AUX_CONSULTAR`: mismo propósito read-only y misma superficie agrupada de catálogos. No se modifica RBAC.
