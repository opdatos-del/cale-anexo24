# Matriz de integración con Módulo C

Matriz inicial de fuentes SQL para la integración del backend con
`CALE_IMMEX`. Se actualiza conforme cada caso de uso sea auditado. La regla
vigente es **STORED PROCEDURE FIRST**:

1. consumir un SP legacy adecuado;
2. si no existe, consumir una view legacy adecuada;
3. si tampoco existe y el caso es estrictamente de lectura, crear un SP propio
   `APP24_Q_*` autorizado;
4. el backend consume siempre el objeto SQL mediante un adapter.

La matriz no implica que los procedimientos hayan sido ejecutados. Las
clasificaciones provienen de definiciones y metadata consultadas en solo
lectura.

| Módulo | Caso de uso | Fuente | Objeto SQL | Tipo | Lee | Escribe | Estado |
|---|---|---|---|---|---|---|---|
| Materiales | Consulta de catálogo | TABLE | `dbo.material` | QUERY | Sí | No | CONFIRMADO; patrón existente |
| Productos | Consulta de catálogo | SP PROPIO | `dbo.APP24_Q_PRODUCTOS_LISTAR` | QUERY | Sí | No | CONFIRMADO — APP24 QUERY READ-ONLY |
| Productos | Carga de catálogo | SP | `dbo.CARGA_PRODUCTOS` | IMPORT | Sí | Sí | CONFIRMADO; no usar para GET |
| Productos | Alta derivada por facturas | SP | `dbo.CARGA_FACTURAS` | IMPORT | Sí | Sí | CONFIRMADO; proceso mutable |
| Productos | Alta derivada por factura | SP | `dbo.CREAPRODUCTOSCARGAFACTURA` | IMPORT | Sí | Sí | CONFIRMADO; proceso mutable |
| Productos | Exportación de datos | SP | `dbo.SP_GENERA_TXT_COMPLETO` | EXPORT | Sí | Sí | CONFIRMADO; usa `bcp`/`xp_cmdshell`, no usar para GET |
| Productos | Campos dentro de informe de importaciones | SP | `dbo.PR_INFORME_IMPORTACIONES` | REPORT | Sí | No | CONFIRMADO; no es catálogo ni paginado |
| Productos | Campos dentro de informe de exportaciones | SP | `dbo.PR_INFORME_EXPORTACIONES` | REPORT | Sí | No | CONFIRMADO; no es catálogo ni paginado |
| Estructuras | Consulta de BOM | SP LEGACY CANDIDATO | `dbo.PR_INFORME_ESTRUCTURAS` | REPORT/QUERY | Sí | No | EN AUDITORÍA; detalle plano read-only, `@DESDE/@HASTA` sin efecto, sin paginación |
| Estructuras | Consulta de detalle | VIEW LEGACY | `dbo.v_Estructuras` | REPORT/QUERY | Sí | No | EN AUDITORÍA; referencia read-only, sin filtros/IDs/paginación; ORDER BY no contractual |
| Estructuras | Construcción de BOM | SP | `dbo.CREAESTRUCTURAS` | PROCESS | Sí | Sí | CONFIRMADO; mutable, no usar para GET |
| Estructuras | Preparación 1 a 1 | SP | `dbo.ESTRUCTURAS1A1` | PROCESS | Sí | Sí | CONFIRMADO; mutable y llama `CREAESTRUCTURAS` |
| Entradas/importaciones | Carga de pedimentos | SP | `dbo.CARGAPEDIMENTOS` | IMPORT | Sí | Sí | CONFIRMADO; pendiente de auditoría de módulo |
| Entradas/importaciones | Informe de importaciones | SP | `dbo.PR_INFORME_IMPORTACIONES` | REPORT | Sí | No | CONFIRMADO; pendiente de diseño de módulo |
| Salidas/exportaciones | Carga de facturas en salidas | SP | `dbo.CARGAFACTURASENPSALIDAS` | IMPORT/PROCESS | Sí | Sí | CONFIRMADO; pendiente de auditoría de módulo |
| Salidas/exportaciones | Informe de exportaciones | SP | `dbo.PR_INFORME_EXPORTACIONES` | REPORT | Sí | No | CONFIRMADO; pendiente de diseño de módulo |
| Salidas/exportaciones | Descarga PEPS | SP | `dbo.DESCARGASALIDAPEPS` | PROCESS | Sí | Sí | CONFIRMADO; pendiente de auditoría de módulo |
| Materiales utilizados | Explosión de estructura | SP | `dbo.DESCARGASALIDAPEPS` / `dbo.SALDOS` | PROCESS/CALCULATION | Sí | Sí | EN AUDITORÍA futura |
| Descargos | Descargo general | SP | `dbo.DESCARGATSALIDA1` | PROCESS | Sí | Sí | EN AUDITORÍA futura |
| Descargos | Descargo por fecha | SP | `dbo.DESCARGATSALIDAFECHA` | PROCESS | Sí | Sí | EN AUDITORÍA futura |
| Saldos | Cálculo operativo | SP | `dbo.SALDOS` | CALCULATION | Sí | Sí | EN AUDITORÍA |
| Saldos | Cálculo por familia | SP | `dbo.SALDOS_FAMILIA` | CALCULATION | Sí | Sí | EN AUDITORÍA |
| Saldos | Informe | SP | `dbo.PR_INFORME_SALDOS` | REPORT | Sí | No | EN AUDITORÍA |
| Reportes | Concentrado de saldos | SP | `dbo.INFORME_CONCENTRADOSALDOS` | REPORT | Sí | Sí | EN AUDITORÍA; llena tabla de concentración |
| Activo fijo | Consulta/carga | PENDIENTE | — | — | — | — | PENDIENTE DE AUDITAR |
| Facturación | Carga y procesamiento | PENDIENTE | — | — | — | — | PENDIENTE DE AUDITAR |

## Estado de Productos

Para la consulta del catálogo:

```text
Consulta de productos
    ↓
No existe SP legacy adecuado
    ↓
No existe View dedicada adecuada
    ↓
Autorización arquitectónica
    ↓
SP PROPIO APP24_Q_PRODUCTOS_LISTAR
    ↓
ProductoStoredProcedureAdapter
```

`dbo.APP24_Q_PRODUCTOS_LISTAR` encapsula únicamente la lectura de
`dbo.productos`, con filtro por código/nombre/fracción, paginación y `@Total`
como parámetro de salida. No reemplaza lógica de negocio legacy ni modifica
ningún SP existente. La evidencia detallada está en
[`mapeo-productos.md`](mapeo-productos.md).

## Dependencias relevantes confirmadas

```text
ESTRUCTURAS1A1
    ↓
CREAESTRUCTURAS
    ├── productos
    ├── estructuras
    ├── productomaterial
    ├── material
    ├── VALIDUNIT / FACTOR / EXISTEFACTOR
    └── errorCartaMateriales

DESCARGASALIDAPEPS / DESCARGATSALIDA1
    ├── GETPRODUCTSTRUCT
    └── SALDOS
          ├── productomaterial
          ├── material
          ├── partidas
          └── descarga
```

Las dependencias representan referencias observadas en SQL y no sustituyen
foreign keys. No se confirmaron FKs DDL entre Producto, Estructura,
Productomaterial y Material.
