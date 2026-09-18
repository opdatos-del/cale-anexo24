# Patrón de catálogo backend: Materiales

Este documento describe únicamente el patrón implementado por el catálogo de
Materiales. No define una abstracción genérica ni sustituye la investigación
necesaria para Productos y Estructuras.

## Flujo

```text
MaterialController
      ↓
ListarMaterialesUseCase
      ↓
MaterialRepository (puerto)
      ↓
MaterialJdbcAdapter
      ↓
dbo.material en CALE_IMMEX
```

## Responsabilidades

- **Controller:** expone `GET /api/v1/catalogos/materiales`, recibe parámetros
  HTTP, delega al caso de uso y convierte el modelo a `MaterialDto`. No contiene
  SQL ni reglas de negocio.
- **Use case:** valida la paginación y normaliza el filtro antes de invocar el
  puerto.
- **Repository port:** expresa la consulta paginada sin conocer JDBC ni SQL
  Server.
- **JDBC adapter:** implementa el puerto contra `dbo.material`, usa parámetros
  JDBC y transforma filas a objetos de dominio.
- **DTO:** define la representación pública de Materiales y permanece separado
  del modelo de dominio.

## Contrato de consulta

`pagina` es base 1 y debe ser mayor o igual que 1. `tamano` debe estar entre 1
 y 100 inclusive. Los valores inválidos producen HTTP 400; no se corrigen de
forma silenciosa.

El filtro se normaliza en application: `null` y valores blank se convierten en
`null`; los demás valores se recortan con `trim()` antes de llegar al adapter.

La paginación se ejecuta en SQL Server con `OFFSET/FETCH`, y la respuesta usa
`Pagina<T>`:

```json
{"items": [], "total": 0, "pagina": 1, "tamano": 20}
```

El orden actual permanece como `ORDER BY clave`. La auditoría disponible no
confirma que `materialkey` sea una clave única y estable para agregarlo como
segundo criterio. Debe validarse contra el esquema real antes de cambiarlo.

## Seguridad y errores

El endpoint requiere la autoridad `MATERIALES_CONSULTAR`. La autenticación y la
autorización se ejecutan en backend mediante JWT y `@PreAuthorize`.

Los errores se convierten al contrato `ApiError`, incluyendo `correlationId`:

- 400 para parámetros inválidos o tipos incorrectos.
- 401 cuando falta una autenticación válida.
- 403 cuando falta el permiso.
- 503 cuando falla el acceso a datos.

El adapter recibe explícitamente el `JdbcTemplate` de CALE_IMMEX mediante
`@Qualifier("jdbcTemplate")`; el datasource de app24 no se usa para esta
consulta.

## Testing

La consulta se prueba en tres niveles:

- el use case prueba validación de paginación y normalización del filtro;
- el adapter prueba parámetros, filtro, paginación, resultados vacíos y
  propagación de errores JDBC;
- la capa web/security verifica los contratos 200, 400, 401, 403 y 503 con
  MockMvc y usuarios/autorizaciones de prueba, sin depender de una base real.
