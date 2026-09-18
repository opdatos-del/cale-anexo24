# Matriz de integración con Módulo C

Matriz inicial de fuentes SQL para la integración del backend con
`CALE_IMMEX`. Se actualiza conforme cada caso de uso sea auditado. La regla
vigente es **STORED PROCEDURE FIRST**:

1. consumir un SP adecuado;
2. si no existe, consumir una view adecuada;
3. si tampoco existe, detener implementación y pedir aprobación para SQL directo.

La matriz no implica que los procedimientos hayan sido ejecutados. Las
clasificaciones provienen de definiciones y metadata consultadas en solo
lectura.

| Módulo | Caso de uso | Fuente | Objeto SQL | Tipo | Lee | Escribe | Estado |
|---|---|---|---|---|---|---|---|
| Materiales | Consulta de catálogo | TABLE | `dbo.material` | QUERY | Sí | No | CONFIRMADO; patrón existente |
| Productos | Consulta de catálogo | PENDIENTE | — | — | — | — | EN AUDITORÍA; sin SP/View de catálogo confirmado |
| Productos | Carga de catálogo | SP | `dbo.CARGA_PRODUCTOS` | IMPORT | Sí | Sí | CONFIRMADO; no usar para GET |
| Productos | Alta derivada por facturas | SP | `dbo.CARGA_FACTURAS` | IMPORT | Sí | Sí | CONFIRMADO; proceso mutable |
| Productos | Alta derivada por factura | SP | `dbo.CREAPRODUCTOSCARGAFACTURA` | IMPORT | Sí | Sí | CONFIRMADO; proceso mutable |
| Productos | Exportación de datos | SP | `dbo.SP_GENERA_TXT_COMPLETO` | EXPORT | Sí | Sí | CONFIRMADO; usa `bcp`/`xp_cmdshell`, no usar para GET |
| Productos | Campos dentro de informe de importaciones | SP | `dbo.PR_INFORME_IMPORTACIONES` | REPORT | Sí | No | CONFIRMADO; no es catálogo ni paginado |
| Productos | Campos dentro de informe de exportaciones | SP | `dbo.PR_INFORME_EXPORTACIONES` | REPORT | Sí | No | CONFIRMADO; no es catálogo ni paginado |
| Estructuras | Consulta de BOM | VIEW/SP | `dbo.v_Estructuras` / `dbo.PR_INFORME_ESTRUCTURAS` | REPORT | Sí | No | CONFIRMADO como fuente operativa; no implementar ahora |
| Estructuras | Construcción de BOM | SP | `dbo.CREAESTRUCTURAS` | PROCESS | Sí | Sí | CONFIRMADO |
| Estructuras | Preparación 1 a 1 | SP | `dbo.ESTRUCTURAS1A1` | PROCESS | Sí | Sí | CONFIRMADO; llama `CREAESTRUCTURAS` |
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

Para el caso prioritario:

```text
Consulta de productos
    ↓
No hay SP de consulta de catálogo confirmado
    ↓
No hay View canónica adecuada confirmada
    ↓
STOP
    ↓
Requiere aprobación para SQL directo sobre dbo.productos
```

La tabla `dbo.productos` es canónica como dato persistente, pero todavía no es
una fuente autorizada para implementar un adapter productivo. La decisión y la
evidencia detallada están en [`mapeo-productos.md`](mapeo-productos.md).

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
