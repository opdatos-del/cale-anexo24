# Consultas guardadas — rediseño seguro (LEGACY-014)

## Decisión

`dbo.consultas` fue inspeccionada en `CALE_IMMEX` sólo por metadata y agregados.
Existe con `NombreConsulta char(25)`, `Descripcion char(80)` y `Consulta char(150)`;
estaba vacía durante discovery, sin PK, índices únicos, FKs ni triggers. No se halló
un caller SQL con dependencia exacta. Su campo `Consulta` se clasifica como
almacenamiento de SQL crudo potencial; nunca se lee por runtime moderno, no se
migra y no se ejecuta.

`LEGACY_RAW_SQL_EXECUTION = NOT_REPLICATED_SECURITY_DEVIATION`.

## Arquitectura moderna

Los presets viven en `ANEXO24_DEV.app24.ConsultaGuardada`, no en `CALE_IMMEX`.
Cada fila pertenece a `UsuarioApp`; la unicidad `(usuario_id, alcance, nombre)`
y todos los SP incluyen el usuario autenticado. El API nunca recibe `usuario_id`.
Un ID ajeno responde `404`, sin revelar existencia.

Los únicos scopes son `REPORTES`, `ENTRADAS`, `SALIDAS`,
`MATERIALES_UTILIZADOS` y `ACTIVOS_FIJOS`. `criterios_json` debe ser objeto JSON,
con `ISJSON`, máximo 4000 caracteres y validación backend de allowlist, tipos,
fechas ISO y rango. Se rechazan llaves desconocidas, objetos anidados y arreglos.
No se guardan resultados, paginación, credenciales, tokens ni SQL.

## Persistencia y seguridad

`20-saved-queries-v1.sql` versiona tabla y los SP app24:

- `APP24_Q_CONSULTAS_GUARDADAS_LISTAR`
- `APP24_C_CONSULTA_GUARDADA_CREAR`
- `APP24_C_CONSULTA_GUARDADA_ACTUALIZAR`
- `APP24_C_CONSULTA_GUARDADA_ELIMINAR`

Son llamadas desde el adapter JDBC por `appJdbcTemplate`. No existe SQL inline de
negocio, SQL dinámico, endpoint `/execute`, ni grants sobre `CALE_IMMEX`.
`04-app-runtime-permissions.sql` contiene sólo grants `EXECUTE` por objeto para
la futura identidad runtime endurecida; no se aplicó ese script global.

Las acciones crear, actualizar y eliminar se registran en bitácora con ID y
scope, sin serializar ni registrar valores de criterios.

## API y UI

`/api/v1/consultas-guardadas` ofrece GET filtrable por alcance, POST, PUT y
DELETE sólo para usuario autenticado. El control reutilizable se integra en
Reportes, Entradas, Salidas, Materiales utilizados y Activos fijos. Aplicar un
preset valida toda la forma antes de mutar filtros; si es obsoleto se conserva el
estado actual y se informa incompatibilidad. Aplicar no otorga permisos ni ejecuta
consultas automáticamente en Reportes.

## Límites

Máximo 100 presets por usuario. No hubo migración de filas legacy. El rediseño
cubre filtros estructurados modernos, no reproducción de ejecución SQL legacy.
