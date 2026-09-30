# Mapeo de carga de exportaciones V1

## Resultado de la auditoría

La carga de exportaciones no requiere un staging paralelo en V1. El staging
moderno de pedimentos representa filas de ambas ramas mediante el campo
`TipoOperacion`:

```text
Excel → ExcelPedimentoParser → validación batch → CargaPedimento/CargaPedimentoFila/ErrorCargaPedimento → preview
```

La evidencia legacy confirma que `dbo.CARGAPEDIMENTOS` procesa también
`TIPOOPERACION = 2` y, durante su flujo mutable, crea o actualiza salidas y
partidas de salida (`SALIDAS`/`PSALIDAS`). Ese paso operativo no se ejecuta en
esta feature.

## Semántica observada

| Valor | Semántica observable | Evidencia | Estado V1 |
|---:|---|---|---|
| `1` | Rama de importación/pedimento de entrada; la validación read-only evalúa material (`PED-003`) | `CARGAPEDIMENTOS`, `VALIDA_I_DETALLENP`, `APP24_Q_PEDIMENTO_VALIDAR_REGLAS` | Soportada en staging |
| `2` | Rama de exportación/salida directa; la validación read-only evalúa producto (`PED-004`) | `CARGAPEDIMENTOS`, `VALIDA_I_DETALLENP`, `mapeo-entradas.md` | Soportada en staging |

No se asigna semántica a otros valores. El parser los rechaza con
`OPERACION_NO_CONFIRMADA` porque el contrato V1 sólo documenta `1` y `2`.

## Reutilización comprobada

| Capacidad | Resultado |
|---|---|
| Header/layout | Común; se deriva de `CargaPedimentosIE` y `CAMPOS_CONFIRMADOS` |
| Filas | Comunes; `PedimentoFila` conserva `TipoOperacion` y campos de salida |
| Errores | Comunes; `PedimentoError` y códigos `PED-XXX` |
| Parser | Común; conserva `TipoOperacion = 2` sin descartar la fila |
| Validación | Común; reutiliza `PED-001`, `PED-002` y `PED-004` para operación 2 |
| Preview | Común en `/operaciones/pedimentos`; ahora comunica importación/exportación |
| Hash | Común; la guardia SHA-256 cubre cualquier archivo de staging |
| Permiso | `PEDIMENTOS_CARGAR`; no se crea `EXPORTACIONES_CARGAR` |

No se crean `CargaExportacion`, `CargaExportacionFila` ni
`ErrorCargaExportacion`.

## Límites

Esta V1 no confirma ni importa operaciones hacia `SALIDAS`, `PSALIDAS`,
`DESCARGA` o inventario. No ejecuta `CARGAPEDIMENTOS`, `INSERTAPEDIMENTO`,
`CARGA_ENCABEZADOS` ni otro procedimiento mutable. Por tanto:

```text
EXPORT_SEMANTICS = PARTIAL_CONFIRMED
PEDIMENT_STAGING_REUSABLE = YES
EXPORT_STAGING_V1 = PARTIAL
LEGACY-019 = PARTIAL
LEGACY-016 = PARTIAL
LEGACY-057 = MISSING
CALE_IMMEX_WRITES = 0
MUTABLE_SP_EXECUTED = 0
INLINE_JAVA_SQL = 0
```

La diferencia entre `LEGACY-019` y `LEGACY-057` se conserva: esta feature
reutiliza staging/preview para una rama de exportación; no implementa la
importación/confirmación operacional legacy.
