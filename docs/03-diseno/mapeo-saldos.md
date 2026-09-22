# Saldos — auditoría técnica, separación de cálculo y alcance V1

> **Estado:** auditoría cerrada para alcance V1. No existe contrato HTTP de
> Saldos aprobado todavía.
>
> **Evidencia:** definiciones y metadata read-only capturadas el 15/09/2026,
> snapshots read-only documentados principalmente el 21/09/2026 y mapeos ya
> versionados. La reconsulta actual de `CALE_IMMEX` quedó bloqueada antes de
> ejecutar SQL por certificado TLS no confiable. No se usó bypass de certificado,
> no se expusieron credenciales y no se ejecutó ningún proceso legacy.

## 1. Objetivo

Responder qué significa **saldo** en `CALE_IMMEX`, separando consulta segura de
cálculo/reproceso. Esta fase no crea SP `APP24_Q_*`, backend, frontend ni
modifica objetos SQL legacy.

Etiquetas usadas:

- **CONFIRMADO:** definición, metadata o medición read-only previa.
- **INFERIDO:** conclusión razonable que aún requiere prueba directa.
- **DECISIÓN V1:** límite deliberado de esta modernización.
- **PENDIENTE:** evidencia funcional/técnica no disponible.

## 2. Definición observada

**CONFIRMADO:** el modelo conserva `dbo.PARTIDAS.Saldo numeric(18,4)` como un
valor persistido por partida de importación. Los procesos de descargo/saldo
calculan asignaciones, insertan en `dbo.DESCARGA`, actualizan
`dbo.PARTIDAS.Saldo` e insertan trazabilidad en `dbo.TRAZO`.

```text
IMPORTACIONES
  └─ PARTIDAS (Cantidad, Saldo)
       └─ procesos de descargo/saldo
            ├─ DESCARGA (asignaciones históricas)
            └─ TRAZO (trazabilidad/faltantes del proceso)
```

**INFERIDO:** `PARTIDAS.Saldo` funciona como resultado materializado o cacheado
del cálculo legacy, no como un hecho inmutable suficiente para reconstruir el
histórico fiscal por sí solo.

Se distinguen estos conceptos, que no deben mezclarse:

| Concepto | Evidencia | Estado |
|---|---|---|
| Saldo persistido por partida | `PARTIDAS.Saldo` | CONFIRMADO |
| Cálculo/reproceso de saldo | familia `SALDOS*` y procesos de descargo | CONFIRMADO, mutable |
| Consumo/descarga histórico | filas físicas de `DESCARGA` | CONFIRMADO; ya expuesto por Materiales Utilizados |
| Informe de saldo legacy | `PR_INFORME_SALDOS`, `v_saldos`, `v_saldosdesp` | CONFIRMADO read-only; contrato API no cerrado |
| Concentrado de saldo | `INFORME_CONCENTRADOSALDOS` / `CONCENTRADOSALDOS` | CONFIRMADO, proceso mutable |
| Saldo fiscal a corte aprobado | fórmula, corte y universo funcional | PENDIENTE |

## 3. Separación estricta: query vs cálculo

### Consulta read-only

Candidatos observados:

| Objeto | Tipo | Efecto | Estado |
|---|---|---|---|
| `dbo.PR_INFORME_SALDOS` | REPORT/QUERY | READ ONLY | CONFIRMADO por definición/metadata previa |
| `dbo.v_saldos` | VIEW/REPORT | READ ONLY | CONFIRMADO por definición previa |
| `dbo.v_saldosdesp` | VIEW/REPORT | READ ONLY | CONFIRMADO por definición previa |

Estos objetos son referencia técnica; no se convirtieron automáticamente en
contrato HTTP porque faltan proyección mínima aprobada, filtros, orden,
paginación, total y conciliación funcional.

### Cálculo, descargo o mantenimiento

| Objeto | Clasificación | Efecto | Estado |
|---|---|---|---|
| `dbo.SALDOS` | CALCULATION | WRITE | CONFIRMADO |
| `dbo.SALDOS_FAMILIA` | CALCULATION | WRITE | CONFIRMADO |
| `dbo.SALDOS2` | CALCULATION | WRITE | CONFIRMADO |
| `dbo.SALDOSCTM` | CALCULATION | WRITE | CONFIRMADO |
| `dbo.SALDOSDIRIGIDOS` | CALCULATION | WRITE | CONFIRMADO por flujo dirigido |
| `dbo.DESCARGASALIDAPEPS` | PROCESS/CALCULATION | WRITE | CONFIRMADO |
| `dbo.DESCARGATSALIDA1` | PROCESS | WRITE | CONFIRMADO |
| `dbo.DESCARGATSALIDAFECHA` | PROCESS | WRITE | CONFIRMADO |
| `dbo.DESCARGAXFECHA51` | PROCESS/CALCULATION | WRITE | CONFIRMADO |
| `dbo.INFORME_CONCENTRADOSALDOS` | REPORT/PROCESS | WRITE | CONFIRMADO; llena `CONCENTRADOSALDOS` |

**DECISIÓN V1:** ningún `GET` ejecutará estos procesos, directa o
indirectamente. Tampoco se ejecutaron durante esta auditoría.

## 4. `dbo.PARTIDAS.Saldo`

| Aspecto | Evidencia | Estado |
|---|---|---|
| Tipo | `numeric(18,4)` nullable | CONFIRMADO |
| Relación base | Pertenece a partida enlazada lógicamente a `IMPORTACIONES` mediante `Importacionlink` | CONFIRMADO |
| Actualización | `SALDOS` lo actualiza al procesar descargas | CONFIRMADO |
| Cantidad de entrada | `PARTIDAS.Cantidad numeric(18,4)` | CONFIRMADO |
| Valor fiscal final/histórico | No demostrado | PENDIENTE |

El snapshot documentado de las dos partidas activas registró: 0 `NULL`, un
saldo cero, 0 negativos, mínimo `0.0000` y máximo `513541.3950`. Es una muestra
limitada de Activos Fijos, no una estadística general actual de `PARTIDAS`.

**DECISIÓN V1:** no presentar `PARTIDAS.Saldo` como saldo fiscal conciliado,
saldo histórico o saldo a una fecha sin cerrar regla y corte.

## 5. Reconciliación con `dbo.DESCARGA`

**CONFIRMADO:** `SALDOS` recibe por separado cantidades incorporada, merma y
desperdicio. Calcula porcentajes sobre:

```text
TOTAL = TOTINCORPORADO + TOTDESPERDICIADO + TOTMERMADO
```

Al persistir una descarga conserva, entre otros, `CantUtil`, `Merma`,
`Desperdicio`, sus cantidades tarifarias y unidades; además actualiza
`PARTIDAS.Saldo` e inserta `TRAZO`.

El snapshot histórico documentado de `DESCARGA` contiene 3,866 filas físicas:

| Dato | Valor documentado |
|---|---:|
| `Descargakey` distintos | 3,866 (1 a 3,866) |
| `(Pentradalink, Psalidalink, Clave)` distintos | 3,643 |
| Grupos duplicados legítimos | 223; máximo 2 filas por grupo |
| `CantUtil` | no nulo; mínimo 0.4234, máximo 14664.0892 |
| `Merma` | no nulo; mínimo 0, máximo 320.6320 |
| `Desperdicio` | nulo en 3,866 filas |
| Negativos en componentes observados | 0 |

**CONFIRMADO:** una fila física de `DESCARGA` no equivale a una fila de saldo.
Su granularidad es asignación histórica entrada → salida/material; está cubierta
por `GET /api/v1/operaciones/materiales-utilizados`.

**PENDIENTE:** no hay evidencia que autorice una fórmula universal como:

```text
PARTIDAS.Saldo = Cantidad - SUM(DESCARGA.CantUtil)
PARTIDAS.Saldo = Cantidad - SUM(CantUtil + Merma + Desperdicio)
```

Los flujos por familia, dirigido, CTM, desperdicio, A31, retornos y
normalizaciones especiales impiden inferirla. El seed de prueba muestra
comparaciones diagnósticas, no una aserción fiscal de igualdad.

## 6. Granularidad e identidad candidatas

**CONFIRMADO:** una partida de importación permite modelar una fila de saldo
actual porque contiene `Partidakey`, cantidad y saldo persistido. La relación
funcional es:

```text
IMPORTACIONES.Ipedimentokey = PARTIDAS.Importacionlink
```

**INFERIDO:** si se aprueba una consulta de saldo actual por partida, la
identidad técnica natural será `PARTIDAS.Partidakey`; no debe inventarse
`balanceId`.

**PENDIENTE:** no está definida la fila funcional de un informe fiscal de saldo:
puede ser partida, partida + categoría, material agregado, pedimento + material,
fracción o concentrado. Por eso no se fija todavía una respuesta, filtros ni
paginación.

## 7. Fechas y temporalidad

| Candidato | Papel demostrado | Estado |
|---|---|---|
| `IMPORTACIONES.Fecha` | Fecha de pago/importación usada por reportes y filtros | CONFIRMADO |
| `IMPORTACIONES.Fecha_ad` | Fecha de entrada | CONFIRMADO |
| `PARTIDAS.PFECHAIMPO` / `FECHAIMPO` | Fechas heredadas de partida; semántica de corte no cerrada | PENDIENTE |
| Fecha propia de saldo | No localizada en evidencia disponible | PENDIENTE |
| Fecha de ejecución del descargo | No localizada en `DESCARGA`; `SALIDAS.FECHADESCARGA` fue nula en 660/660 del snapshot | CONFIRMADO para snapshot |

**DECISIÓN V1:** no llamar «saldo histórico» ni «saldo a corte» a una lectura de
`PARTIDAS.Saldo`. Es saldo persistido actual al momento de consulta; las fechas
de importación sólo identificarían la entrada mientras no se demuestre un
snapshot o corte histórico.

## 8. `dbo.PR_INFORME_SALDOS`

| Aspecto | Evidencia |
|---|---|
| Parámetros | `@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)` |
| Fuentes documentadas | `Importaciones`, `partidas`, `categorias` |
| Resultado | 37 columnas confirmadas por `sys.dm_exec_describe_first_result_set_for_object` |
| Implementación interna | Tabla variable local; sin DML persistente ni `EXEC` mutable documentado |
| Clasificación | REPORT/QUERY, READ ONLY |
| Paginación, total y orden contractual | No documentados |

**CONFIRMADO:** puede ser ejecutado sólo en una futura validación read-only
controlada, tras recuperar acceso TLS confiable.

**DECISIÓN V1:** no reutilizarlo directamente como API. El inventario de 37
columnas, aliases, nullability, semántica del rango, uso real de `@documento`,
orden y equivalencia funcional deben volver a verificarse. Un reporte legacy sin
paginación ni total no es contrato HTTP.

## 9. Views de saldos

| View | Uso documentado | Limitación para API |
|---|---|---|
| `dbo.v_saldos` | Reporte read-only de saldo actual | No hay contrato de filtros, IDs, orden, total ni paginación documentado |
| `dbo.v_saldosdesp` | Reporte read-only de saldo/desperdicio | Mezcla subdominio especializado; granularidad/filtros pendientes |

**DECISIÓN V1:** no usar una view directamente como fuente de endpoint. Puede
servir como referencia de comparación una vez aprobada la semántica.

## 10. Familia `SALDOS*`

### `dbo.SALDOS`

**CONFIRMADO:** recibe material, cantidades de incorporación/desperdicio/merma,
links de salida, tipo, fecha de exportación, línea y vínculo a producto-material;
`@faltodescarga numeric(18,6)` es salida. Inserta `DESCARGA`, actualiza
`PARTIDAS.Saldo` e inserta `TRAZO`.

### `dbo.SALDOS_FAMILIA`

**CONFIRMADO:** misma firma documentada que `SALDOS` y variante de cálculo por
familia. Se clasifica WRITE. La diferencia algorítmica completa y sus sentencias
actuales requieren relectura de definición con conexión confiable.

### `dbo.SALDOS2`

**CONFIRMADO:** variante legacy con parámetros `float` y referencia a
`GENERADORES`; se clasifica WRITE. No normalizar ni unir sus links `float` en
una futura consulta sin medición de integralidad y rango.

### `dbo.SALDOSCTM` y `dbo.SALDOSDIRIGIDOS`

**CONFIRMADO:** son flujos especializados CTM y dirigido, respectivamente;
ambos se clasifican WRITE. `SALDOSCTM` referencia `DESCARGACTMF` y `TRAZOCTM`.
`DESCDIRIGIDA` deriva el flujo dirigido hacia `SALDOSDIRIGIDOS`.

**DECISIÓN V1:** ninguna variante especializada entra en una consulta común de
saldos hasta contrato propio.

## 11. Categorías y concentrados

### Categorías

**CONFIRMADO:** `PR_INFORME_SALDOS` usa `categorias`. La evidencia existente
asocia `categorias.meses` con cálculos legacy de vigencia, vencimiento y días
restantes.

**PENDIENTE:** confirmar código, nombre, cardinalidad, join y utilidad
funcional de categoría para saldo. No se añade como filtro/columna V1.

### Concentrados

**CONFIRMADO:** `dbo.INFORME_CONCENTRADOSALDOS(@DESDE date, @HASTA date)` llena
`dbo.CONCENTRADOSALDOS`; por ello es REPORT/PROCESS WRITE.

**DECISIÓN V1:** no ejecutar generador ni leer `CONCENTRADOSALDOS` como fuente
canónica. Debe auditarse como caso de uso propio: origen, vigencia, limpieza,
snapshot, concurrencia y autorización.

## 12. Relación con descargos, trazo y subtipos

```text
SALIDAS / PSALIDAS
  → GETPRODUCTSTRUCT
  → ESTRUCTURAS / PRODUCTOMATERIAL
  → DESCARGASALIDAPEPS o DESCARGATSALIDA*
  → SALDOS
      ├─ recrea/insert DESCARGA
      ├─ UPDATE PARTIDAS.Saldo
      └─ INSERT/recrea TRAZO
```

**CONFIRMADO:** `TRAZO` estaba vacío en el snapshot de 3,866 descargas; no es
vínculo obligatorio de la consulta histórica, pero participa en cálculo y
faltantes.

**CONFIRMADO:** CTM, dirigido, desperdicio, F4 y A31 mantienen objetos y/o
flujos diferenciados (`SALDOSCTM`, `SALDOSDIRIGIDOS`, `DESCARGA_CTMA`,
`DESCARGA_DESPERDICIO`, `A31_SALDOS`, entre otros).

**DECISIÓN V1:** excluirlos de cualquier contrato general de saldos.

## 13. Calidad de datos disponible

| Fuente/medición | Resultado documentado | Estado |
|---|---|---|
| `DESCARGA` | 3,866 filas; links a `PARTIDAS`, `IMPORTACIONES`, `PSALIDAS` y `SALIDAS` íntegros en 3,866/3,866 | CONFIRMADO en snapshot |
| Links legacy de `DESCARGA` | sin nulos ni valores no enteros documentados; relaciones lógicas, no FKs DDL | CONFIRMADO en snapshot |
| `TRAZO` | vacío; 3,866 descargas sin relación | CONFIRMADO en snapshot |
| Partidas activas | 2; métricas limitadas descritas en sección 4 | CONFIRMADO en snapshot |
| Conteos/valores globales actuales de `PARTIDAS.Saldo` | no revalidados | PENDIENTE |
| Nulos, blancos, ceros, negativos y extremos actuales de saldo | no revalidados | PENDIENTE |
| Reconciliación matemática actual | no demostrada | PENDIENTE |

No se tratarán datos de snapshot como medición actual hasta recuperar acceso
read-only con certificado confiable.

## 14. Pantalla web legacy

**CONFIRMADO:** auditoría web anterior observó reportes de entradas, salidas,
saldos, materiales utilizados y bitácora; requerían fechas y fallaron con error
genérico al generar. No se obtuvo sesión autorizada específica para identificar
filtros, columnas, totales, exportación o botones peligrosos de Saldos.

**PENDIENTE:** navegación read-only con sesión autorizada. No usar acciones de
calcular, actualizar, regenerar, reprocesar ni descarga PEPS.

## 15. Riesgos

1. **Fiscal/funcional:** saldo persistido no equivale aún a saldo fiscal
   reconciliado ni a saldo histórico a corte.
2. **Mutación:** `SALDOS*`, descargos y concentrados insertan, actualizan,
   borran o recrean datos.
3. **Concurrencia:** locking, transacciones, rollback, duración e idempotencia
   de reproceso no están verificados.
4. **Subtipos:** CTM, dirigido, desperdicio, F4 y A31 tienen reglas propias.
5. **Datos legacy:** existen links `float` en el circuito de descargo; no
   convertirlos ingenuamente.
6. **Tiempo:** reportes de vigencia pueden depender de `GETDATE()`; no son
   snapshots históricos reproducibles.
7. **Conectividad:** TLS impidió revalidar metadata y mediciones actuales. No
   se debilitará la validación de certificados para cerrar un contrato.

## 16. Decisión arquitectónica y contrato V1

### Decisión

**DECISIÓN V1: no implementar todavía un endpoint GET de Saldos.**

No se elige todavía entre `GET /api/v1/reportes/saldos` y
`GET /api/v1/operaciones/saldos`; ambas ubicaciones requieren una definición
funcional que hoy no está cerrada. La evidencia llama `PR_INFORME_SALDOS` a la
fuente legacy y el sistema web lo agrupaba como reporte, por lo que
`/api/v1/reportes/saldos` es el candidato semántico **si** se aprueba un informe
read-only posteriormente.

### Fuente recomendada futura

1. Revalidar `dbo.PR_INFORME_SALDOS` con metadata y ejecución read-only
   controlada.
2. Compararlo con `dbo.v_saldos` y `dbo.v_saldosdesp` sólo como referencias.
3. Si ninguno entrega contrato mínimo seguro, aprobar un SP propio
   `dbo.APP24_Q_SALDOS_LISTAR`, estrictamente read-only y sin reimplementar
   cálculo legacy.

### Contrato HTTP

**No aplica en V1 actual.** No se fijan endpoint, permiso, filtros, response,
orden ni paginación. Propuestas antiguas de saldo inicial/movimientos/final son
hipótesis, no contrato.

**PENDIENTE:** si el caso se confirma como reporte, evaluar
`REPORTES_GENERAR`; si se confirma como consulta operacional por partida,
evaluar `OPERACIONES_CONSULTAR`. Nunca usar permiso de escritura para lectura ni
reutilizar automáticamente permisos de consulta para un futuro reproceso.

## 17. Próximos pasos autorizables

1. Obtener acceso SQL audit-only con certificado confiable, sin bypass TLS.
2. Releer metadata actual de tablas, índices, FKs, columnas y definiciones.
3. Confirmar las 37 columnas de `PR_INFORME_SALDOS`: tipos, nullable, aliases,
   joins, filtros, orden y uso de `@documento`.
4. Ejecutar solamente `PR_INFORME_SALDOS` tras confirmar su carácter read-only;
   comparar resultado contra `v_saldos`/`v_saldosdesp` con SELECT controlado.
5. Medir `PARTIDAS.Saldo` y reconciliar muestra aprobada contra las componentes
   de `DESCARGA`, retornos y flujos especiales sin asumir fórmula.
6. Auditar la pantalla legacy con sesión autorizada, sólo navegación/consulta.
7. Acordar con negocio: fila, corte, categorías, saldo cero, vencimientos,
   universo de subtipos y autorización final.
8. Sólo entonces cerrar contrato, autorizar fuente y abrir implementación
   separada de SQL/backend/frontend.

## 18. Garantías de esta fase

- Cero cambios en `backend/**`, `frontend/**` e `infra/sql/**`.
- Cero objetos SQL creados/modificados.
- Cero procesos `SALDOS*`, descargo, PEPS, historial, concentrado o mantenimiento
  ejecutados.
- Cero PR, merge o revisión automática.
