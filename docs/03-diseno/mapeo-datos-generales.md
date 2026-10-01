# Datos generales de la empresa — contrato Read V1

## Estado

- `GENERAL_DATA_READ_SCOPE = CONFIRMED_UI`
- `GENERAL_DATA_SOURCE_CONTRACT = CONFIRMED`
- `GENERAL_DATA_READ_CONTRACT = CONFIRMED`
- `GENERAL_DATA_WRITE_CONTRACT = UNKNOWN`
- `GENERAL_DATA_WRITE_V1 = NOT_IMPLEMENTED`
- `LEGACY-001 = PARTIAL`

La auditoría legacy observó una ficha única con Razón social, RFC, Registro
IMMEX y Domicilio fiscal. También mostró acciones Guardar y Cancelar, pero la
edición no se ejecutó por tratarse de datos empresariales únicos. Esta V1 sólo
consulta la fuente existente; no crea una tabla paralela ni implementa edición.

## Fuente canónica y cardinalidad

La fuente canónica demostrada es `dbo.DatosGenerales` en `CALE_IMMEX`.
No se usa `dbo.settings`: su metadata indica una tabla de configuración técnica
con 3 filas y columnas `settings`/`value`; no se consultaron ni expusieron sus
valores.

```text
COMPANY_GENERAL_DATA_SOURCE = dbo.DatosGenerales
SOURCE_ROWS = 1
ACTIVE_ROWS = NOT_APPLICABLE (no existe columna de estado/activo demostrada)
CANDIDATE_COMPANY_ROWS = 1
SOURCE_PK_OR_UNIQUE_INDEX = NONE
CARDINALITY_RULE = 0 o 1 fila; más de una fila provoca error, no selección arbitraria
```

La cardinalidad actual es singleton por conteo LIVE, no por una PK. El SP aplica
la regla y falla explícitamente si aparecen dos o más registros; el adapter también
rechaza una respuesta de más de una fila y nunca usa `TOP 1` ni `findFirst()`.

```text
GENERAL_DATA_SINGLETON_GUARD = PASS
CARDINALITY_0 = 204 / respuesta vacía
CARDINALITY_1 = entidad
CARDINALITY_GT_1 = error explícito
```

## Mapping

| Contrato UI | Fuente | Transformación V1 | Evidencia LIVE |
|---|---|---|---|
| `razonSocial` | `DatosGenerales.Denominacion` | `LTRIM/RTRIM`, vacío a `NULL` | presente; longitud 43 |
| `rfc` | `DatosGenerales.Rfc` | `LTRIM/RTRIM`, vacío a `NULL` | presente; longitud 12 |
| `registroImmex` | `DatosGenerales.RegistroIMMEX` | `LTRIM/RTRIM`, vacío a `NULL` | presente; longitud 12 |
| `domicilioFiscal` | `CalleNumero`, `CalleNumeroInterior`, `Colonia`, `Municipio`, `Entidad`, `Codigopostal` | componentes no vacíos unidos con `, ` | presente |

No se muestran `URLWS`, `BASEVUCEM`, correo, teléfono, vigencias, registros
alternativos ni otros campos de `DatosGenerales`: no forman parte del contrato
UI confirmado y pueden tener implicaciones operativas o sensibles.

```text
GENERAL_DATA_FIELD_WHITELIST = PASS
DTO_FIELDS = razonSocial, rfc, registroImmex, domicilioFiscal
SETTINGS = NOT_USED
```

## Contratos SQL

- SP read-only versionado: `dbo.APP24_Q_DATOS_GENERALES_OBTENER`.
- Sin parámetros y sin paginación: devuelve cero o una entidad.
- API: `GET /api/v1/catalogos/datos-generales`.
- Permiso reutilizado: `CATALOGOS_AUX_CONSULTAR`.
- La definición usa una proyección explícita; no hace `SELECT *`.
- La cardinalidad se valida con `COUNT_BIG(*)` antes de la proyección.
- El domicilio usa `LTRIM/RTRIM`, `NULLIF` y `CONCAT_WS`, por lo que los
  componentes `NULL` o blank se omiten sin separadores vacíos ni literales
  `null`/`undefined`.
- No existe PUT, PATCH ni POST en esta V1.

Referencias LIVE adicionales a `DatosGenerales` (`COMPARATIVADESCARGA31`,
`COMPARATIVADESCARGA31_DETALLE`, `SP_GENERA_TXT_COMPLETO` y `V_G6`) se mantienen
fuera del contrato de esta ficha. Son reportes/procesos de otros dominios; no se
reutilizan como fuente empresarial ni se ejecutan durante esta fase. No se
observó un writer directo de la tabla en el inventario de módulos; el write
contract permanece `UNKNOWN` porque la pantalla legacy de edición no fue
probada.

## Reconciliación LIVE

Sin imprimir valores empresariales:

```text
source rows = 1
SP rows = 1
source razon social = PRESENT / length 43
SP razon social = PRESENT / length 43
source RFC = PRESENT / length 12
SP RFC = PRESENT / length 12
source Registro IMMEX = PRESENT / length 12
SP Registro IMMEX = PRESENT / length 12
source domicilio = PRESENT
SP domicilio = PRESENT
STRUCTURAL_MATCH = PASS
```

La comparación valida cardinalidad, presencia y longitudes; no registra RFC,
razón social ni domicilio completos.

### Prueba sintética de domicilio

`infra/sql/tests/APP24_Q_DATOS_GENERALES_OBTENER.address.sql` ejecuta sólo una
expresión con una variable de tabla y valores sintéticos. Cubre caso completo,
sin interior, sin colonia, componentes `NULL` y componentes blank; no inserta en
`dbo.DatosGenerales` ni en ninguna tabla de `CALE_IMMEX`.

```text
GENERAL_DATA_ADDRESS_FORMATTING = PASS
CALE_IMMEX_WRITES = 0
```

## UI

- Ruta: `/catalogos/datos-generales`.
- Superficie: Catálogos, sin nuevo módulo lateral.
- Presenta ficha empresarial de sólo lectura.
- Estados: loading, success, empty (HTTP 204), error/retry y 403 mediante el
  guard y permiso existente. El cliente trata 204 como `null`, apaga loading y
  muestra empty state; no intenta parsear JSON ni deja spinner infinito.
- No muestra botones `Guardar`, `Cancelar`, `Editar` ni acciones mutables.

## Seguridad y exclusiones

- 401 sin autenticación.
- 403 sin `CATALOGOS_AUX_CONSULTAR`.
- 200 autorizado con la fila actual.
- Valores completos de la empresa no se imprimieron durante discovery,
  reconciliación ni logs de prueba.
- `settings` no se expone.
- No se ejecutaron procedimientos legacy mutables ni se escribieron datos de
  negocio en `CALE_IMMEX`.

## División / almacenes

El fallback de División no fue necesario: el contrato de Datos Generales quedó
confirmado. La auditoría posterior `feature/legacy-divisions-read-v1` demostró
que División se resuelve contra `dbo.almacen` (`dbo.ENTIDAD(@DIVISION)` devuelve
`ALMACENKEY`), por lo que `LEGACY-008` quedó `IMPLEMENTED_REDESIGNED` cubierto por
`/catalogos/almacenes`, sin cambios en esta feature.
