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
| Estructuras | Consulta de BOM | SP PROPIO | `dbo.APP24_Q_ESTRUCTURAS_LISTAR` | QUERY | Sí | No | CONFIRMADO — APP24 QUERY READ-ONLY |
| Estructuras | Consulta de detalle legacy | SP LEGACY | `dbo.PR_INFORME_ESTRUCTURAS` | REPORT/QUERY | Sí | No | CONFIRMADO como referencia read-only; sin paginación y `@DESDE/@HASTA` sin efecto |
| Estructuras | Consulta de detalle legacy | VIEW LEGACY | `dbo.v_Estructuras` | REPORT/QUERY | Sí | No | CONFIRMADO como referencia read-only; sin filtros/IDs/paginación; ORDER BY no contractual |
| Estructuras | Construcción de BOM | SP | `dbo.CREAESTRUCTURAS` | PROCESS | Sí | Sí | CONFIRMADO; mutable, no usar para GET |
| Estructuras | Preparación 1 a 1 | SP | `dbo.ESTRUCTURAS1A1` | PROCESS | Sí | Sí | CONFIRMADO; mutable y llama `CREAESTRUCTURAS` |
| Entradas/importaciones | Carga de pedimentos | SP LEGACY | `dbo.CARGAPEDIMENTOS` | IMPORT/PROCESS | Sí | Sí | CONFIRMADO; mutable, auditado; no usar para GET |
| Entradas/importaciones | Consulta paginada de entradas/líneas | SP PROPIO | `dbo.APP24_Q_ENTRADAS_LISTAR` | QUERY | Sí | No | CONFIRMADO — APP24 QUERY READ-ONLY; `PR_INFORME_IMPORTACIONES` y `v_Importaciones` quedan como referencias legacy |
| Entradas/importaciones | Validación de pedimento | SP LEGACY | `dbo.VALIDAPEDIMENTO` | VALIDATION/PROCESS | Sí | Sí | CONFIRMADO por definición/dependencias; no usar para GET |
| Salidas/exportaciones | Carga de facturas en salidas | SP | `dbo.CARGAFACTURASENPSALIDAS` | IMPORT/PROCESS | Sí | Sí | CONFIRMADO; proceso mutable auditado estáticamente; no usar para GET |
| Salidas/exportaciones | Informe de exportaciones | SP LEGACY | `dbo.PR_INFORME_EXPORTACIONES` | REPORT/QUERY | Sí | No | CONFIRMADO; read-only ejecutado de forma controlada; 49 columnas, sin paginación ni total; referencia, no contrato HTTP |
| Salidas/exportaciones | Consulta de exportaciones legacy | VIEW LEGACY | `dbo.v_Exportaciones` | REPORT/QUERY | Sí | No | CONFIRMADO; read-only, 44 columnas, sin filtros parametrizados, paginación ni total; referencia, no contrato HTTP |
| Salidas/exportaciones | Consulta paginada de salidas/líneas V1 | SP PROPIO | `dbo.APP24_Q_SALIDAS_LISTAR` | QUERY | Sí | No | CONFIRMADO — APP24 QUERY READ-ONLY; contrato HTTP implementado; `PR_INFORME_EXPORTACIONES` y `v_Exportaciones` quedan como referencias legacy |
| Salidas/exportaciones | Descarga PEPS | SP | `dbo.DESCARGASALIDAPEPS` | PROCESS | Sí | Sí | CONFIRMADO; proceso mutable auditado estáticamente; no ejecutar desde GET |
| Materiales utilizados | Consulta paginada V1 por asignación histórica | SP PROPIO | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR`; fuente `dbo.DESCARGA` + `PARTIDAS` + `IMPORTACIONES` + `PSALIDAS` + `SALIDAS`; referencias `dbo.v_descarga` y `dbo.V_INFORMEDESCARGAS` | QUERY | Sí | No | CONFIRMADO — APP24 QUERY READ-ONLY; contrato HTTP implementado y validado; no usa reportes legacy como API |
| Materiales utilizados | Generación/explosión de descarga | SP | `dbo.DESCARGASALIDAPEPS` / `dbo.SALDOS` | PROCESS/CALCULATION | Sí | Sí | CONFIRMADO como proceso mutable; no usar para GET |
| Descargos | Consulta histórica de asignaciones | CUBIERTO POR MATERIALES UTILIZADOS | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR`; fuente `dbo.DESCARGA` | QUERY | Sí | No | CONFIRMADO — no crear GET Descargos independiente V1; duplicaría histórico ya expuesto |
| Descargos | Descargo / reproceso general | SP LEGACY | `dbo.DESCARGATSALIDA1` / `dbo.DESCARGASALIDAPEPS` | PROCESS/CALCULATION | Sí | Sí | CONFIRMADO; mutable, futuro command de alto riesgo; no ejecutar desde GET |
| Descargos | Descargo / reproceso por fecha | SP LEGACY | `dbo.DESCARGATSALIDAFECHA` / `dbo.DESCARGAXFECHA51` | PROCESS/CALCULATION | Sí | Sí | CONFIRMADO; mutable, fuera de V1; definición actual, locking y rollback pendientes |
| Saldos | Saldo persistido por partida | TABLE | `dbo.PARTIDAS.Saldo` | QUERY | Sí | No | CONFIRMADO; valor mutable/cached por cálculo legacy; fórmula, corte y contrato API pendientes |
| Saldos | Cálculo operativo | SP | `dbo.SALDOS` | CALCULATION | Sí | Sí | CONFIRMADO; inserta `DESCARGA`, actualiza `PARTIDAS.Saldo` e inserta `TRAZO`; no usar para GET |
| Saldos | Cálculo por familia | SP | `dbo.SALDOS_FAMILIA` | CALCULATION | Sí | Sí | CONFIRMADO; variante mutable; no usar para GET |
| Saldos | Cálculos CTM, dirigido y legado | SP | `dbo.SALDOSCTM` / `dbo.SALDOSDIRIGIDOS` / `dbo.SALDOS2` | CALCULATION | Sí | Sí | CONFIRMADO; flujos especializados/legacy; excluidos de consulta común |
| Saldos | Informe legacy de referencia | SP LEGACY | `dbo.PR_INFORME_SALDOS` | REPORT/QUERY | Sí | No | CONFIRMADO read-only por definición/metadata previa; 37 columnas, sin contrato HTTP/paginación/total; revalidación TLS pendiente |
| Saldos | Referencias de informe | VIEW LEGACY | `dbo.v_saldos` / `dbo.v_saldosdesp` | REPORT/QUERY | Sí | No | CONFIRMADO read-only; granularidad, filtros y contrato API pendientes |
| Saldos | Consulta HTTP V1 | NO IMPLEMENTAR | — | — | — | — | DECISIÓN V1 — no cerrar endpoint hasta reconciliar fórmula, corte, categoría y universo funcional |
| Reportes | Informe extendido de importaciones | SP LEGACY / VIEW LEGACY | `dbo.PR_INFORME_IMPORTACIONES` / `dbo.v_Importaciones` | REPORT/QUERY | Sí | No | CONFIRMADO read-only; 76/70 columnas, sin contrato HTTP; cubierto por Operaciones, no crear reporte V1 duplicado |
| Reportes | Informe extendido de exportaciones | SP LEGACY / VIEW LEGACY | `dbo.PR_INFORME_EXPORTACIONES` / `dbo.v_Exportaciones` | REPORT/QUERY | Sí | No | CONFIRMADO read-only; 49/44 columnas, sin contrato HTTP; cubierto por Operaciones, no crear reporte V1 duplicado |
| Reportes | Informe de estructuras | SP LEGACY / VIEW LEGACY | `dbo.PR_INFORME_ESTRUCTURAS` / `dbo.v_Estructuras` | REPORT/QUERY | Sí | No | CONFIRMADO read-only; detalle BOM sin contrato HTTP; cubierto por Catálogos, no crear reporte V1 duplicado |
| Reportes | Conciliación de materiales utilizados | VIEW LEGACY | `dbo.v_descarga` / `dbo.V_INFORMEDESCARGAS` | REPORT/QUERY | Sí | No | CONFIRMADO read-only; mezcla saldo/valores/conciliación; histórico ya cubierto por Materiales Utilizados |
| Bitácora | Consulta de eventos propios de aplicación | APP DATABASE | `ANEXO24_DEV.app24.BitacoraEvento` | QUERY | Sí | No | IMPLEMENTADO EN REPOSITORIO; PENDIENTE DE VALIDACIÓN RUNTIME REMOTA — `GET /api/v1/bitacora` con `BITACORA_CONSULTAR`, rango UTC, filtros exactos, paginación y orden estable; fuente propia, no Módulo C |
| Bitácora | Registro interno de eventos críticos | APP DATABASE | `ANEXO24_DEV.app24.BitacoraEvento` | AUDIT/COMMAND | Sí | Sí | IMPLEMENTADO EN REPOSITORIO — writer interno append-only y eventos `LOGIN_OK`/`LOGIN_FALLIDO` conectados desde Login; no endpoint público. Permisos mínimos `app24_runtime` pendientes de despliegue; 401/403 y demás eventos/transaccionalidad pendientes por command |
| Bitácora | Historial Web Forms / CALE_IMMEX | PENDIENTE | Fuente física legacy no demostrada | — | — | — | PENDIENTE DE REVALIDACIÓN TLS; no mezclar con bitácora propia ni exponer como GET |
| Reportes | Catálogo HTTP V1 | NO IMPLEMENTAR | — | — | — | — | DECISIÓN V1 — no hay reporte independiente aprobado; consultas existentes cubren listados operacionales |
| Reportes | Concentrado de saldos | SP | `dbo.INFORME_CONCENTRADOSALDOS` | REPORT/PROCESS | Sí | Sí | CONFIRMADO; llena `CONCENTRADOSALDOS`; no usar para GET |
| Activo fijo | Consulta paginada de partidas de importación marcadas activas V1 | SP PROPIO | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR`; fuente `dbo.Partidas` + `dbo.Importaciones`; referencia `dbo.v_g5` | QUERY | Sí | No | CONFIRMADO — APP24 QUERY READ-ONLY; contrato HTTP implementado y validado; no usar `dbo.ActivoFijo` ni procesos G5/G6 como API |
| Facturación | Flujo A: carga desde `TFACTURA` | SP LEGACY | `dbo.CARGA_FACTURAS` | IMPORT/PROCESS | Sí | Sí | CONFIRMADO por snapshot de definición; sin parámetros; usa `TFACTURA`, `TERRORFACTURA`, `PRODUCTOS`, `CLIENTES`, `FACTURA` y referencias a salidas/detalles; no ejecutar |
| Facturación | Flujo B: cargar líneas de salida | SP LEGACY | `dbo.CARGAFACTURASENPSALIDAS` | IMPORT/PROCESS | Sí | Sí | CONFIRMADO por definición snapshot; `@PEDIMENTO`, `@FACTURA`; valida `CARGAFACTURA` y escribe `PSALIDAS`/`ERRCARGAFACTURA`; no ejecutar |
| Facturación | Alta derivada de productos | SP LEGACY | `dbo.CREAPRODUCTOSCARGAFACTURA` | IMPORT | Sí | Sí | CONFIRMADO por definición snapshot; crea productos desde `CARGAFACTURA`; no reutilizar sin decisión funcional explícita |
| Facturación | Flujo `TmpFC` / facturas creadas | SP LEGACY | `dbo.INSTERTAFACTURASFC` | IMPORT | Sí | Sí | CONFIRMADO por snapshot de metadata; `TmpFC`, `TMPFCERROR`, `FacturasCreadas`; propósito y vigencia pendientes |
| Facturación | Vínculo especializado CTM | SP LEGACY | `dbo.LIGACTMFACTURA` | PROCESS | Sí | Sí | CONFIRMADO; usa `FACTURASCTM`; subdominio CTM fuera de V1 general |
| Facturación | API V1 de carga/confirmación | NO IMPLEMENTAR | — | — | — | — | DECISIÓN V1 — sin contrato; faltan plantilla oficial, granularidad, identidad, duplicados, transacción, aislamiento por lote y decisión de persistencia de validación |

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
