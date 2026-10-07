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

## 19. Reauditoria SP-FIRST para LEGACY-021 y LEGACY-038

Reauditoria focalizada para intentar implementar la consulta reportable de saldos. Sin LIVE revalidation: solo evidencia versionada. NO se crearon objetos SQL, NO se inventaron columnas, NO se reescribio formula legacy.

### 19.1 Pregunta critica

1. dbo.PR_INFORME_SALDOS ¿representa el contrato canonico read-only del reporte legacy Saldos?

Respuesta: PARCIAL. Inventario confirma parametros (@DESDE datetime, @HASTA datetime, @documento varchar(50)), fuentes (Importaciones, partidas, categorias), 37 columnas confirmadas via sys.dm_exec_describe_first_result_set_for_object, y patron de tabla variable local que sugiere sin DML persistente. No existen en el repo:

- nombres/tipos/aliases de las 37 columnas;
- semantica de @DESDE/@HASTA (fecha de pago? de importacion? aduanera? de vencimiento? otra);
- semantica de @documento (pedimento? factura? material?);
- grano exacto de salida;
- orden contractual;
- reglas NULL;
- filtros equivalentes a la UI legacy.

PR_INFORME_SALDOS no tiene paginacion ni @Total nativos. La auditoria previa intento ejecutarlo con rangos 2025 e historico: devolvio 0 filas y warning de NULL en agregado. Imposible validar dataset contra las 42 filas observadas en el snapshot funcional de 2025 sin una representacion valida.

Conclusion: PR_INFORME_SALDOS es el candidato canonico conocido mas razonable, pero no esta cerrado como contrato API hasta validar las 37 columnas y semantica de parametros contra la UI legacy y un corte historico representativo.

2. LEGACY-021 y LEGACY-038 ¿son la misma superficie o capacidades funcionalmente diferentes?

Respuesta: distintas; no consolidar.

- LEGACY-021 = Consultar saldo con semantica fiscal y corte definidos. Implica semantica fiscal (no solo persistido) y un corte de operacion auditado. Sin sesion autorizada para validar filtros, columnas, totales o botones peligrosos de la pantalla operativa de Saldos.
- LEGACY-038 = Generar y exportar reporte de saldos. Implica read-only del mismo dataset pero solo la superficie reportable/excels, sin semantica operativa.

Cualquier implementacion que cubra LEGACY-038 deja LEGACY-021 con BLOCKED_BUSINESS porque su semantica fiscal + corte no esta demostrada. LEGACY-021 permanece separado.

### 19.2 Forensic SP-FIRST de dbo.PR_INFORME_SALDOS

Auditoria con evidencia versionada exclusivamente:

| Aspecto | Evidencia versionada |
|----------|---------------------|
| PARAMETERS | @DESDE datetime, @HASTA datetime, @documento varchar(50) (mapeo-saldos.md sec 8 + procedimientos-almacenados.md) |
| RESULTSET_COLUMN_COUNT | 37 columnas confirmadas via sys.dm_exec_describe_first_result_set_for_object (mapeo-materiales-utilizados.md sec 12.738, mapeo-productos.md sec 12) |
| RESULTSET_COLUMNS | NO en el repo. Solo metadata de conteo. |
| COLUMN_TYPES | NO en el repo. |
| SOURCE_TABLES | Importaciones, partidas, categorias (procedimientos-almacenados.md sec PR_INFORME_SALDOS) |
| SOURCE_VIEWS | NO confirmado. Descripcion indica solo tablas base. |
| SOURCE_FUNCTIONS | NO confirmado. Posible uso de BUSCATIPOM en views relacionadas (no en PR_INFORME_SALDOS). |
| DEPENDENCIES | NO disponible en repo. Estimadas a partir del cuerpo de views documentadas en mapeo-saldos.md sec 9. |
| GRAIN | NO confirmado. Pendiente de las 37 columnas. |
| ORDER | no documentado en el SP. UI observada sin orden claro. |
| DATE_FILTER_SEMANTICS | @DESDE/@HASTA parametros presentes. NO se confirma si filtran SALIDAS.Fecha o IMPORTACIONES.Fecha o partidas.PFECHAIMPO (mapeo-saldos.md sec 11.1 explicito: PENDIENTE confirmar). |
| DOCUMENT_FILTER_SEMANTICS | @documento presente. NO se confirma semantica (pedimento/factura/material/otro). |
| NULL_HANDLING | warning de NULL emitido en agregado durante prueba previa; reglas no documentadas. |
| DUPLICATE_BEHAVIOR | no documentado. |
| TEMP_TABLES | ninguna confirmada; SP no versionado. |
| TABLE_VARIABLES | Patron de tabla variable local sugerido por auditorias previas (mapeo-materiales-utilizados.md sec 12.738). NO verificado sin LIVE. |
| SIDE_EFFECTS | no documentados con la definicion; descripcion dice CREATE PROCEDURE [dbo].[PR_INFORME_SALDOS] sin acciones declaradas. |
| EXEC_DEPENDENCIES | NO documentadas. Familia SALDOS* es WRITE; PR_INFORME_SALDOS no esta documentado como ejecutor de mutables. Auditoria auditoria-stored-procedures.md clasifica la familia como PROCESS/REPORT, MIXED por agregacion; PR_INFORME_SALDOS especificamente figura como read-only en otras secciones. |
| CROSS_DB_REFERENCES | NO documentadas. |
| MUTABLE_PRECONDITION_REQUIRED | NO segun patron table variable. NO verificable sin LIVE. |
| PERSISTENT_WRITES | 0 segun patron table variable. NO verificable sin LIVE. |
| READ_ONLY_CONFIRMED | YES por inferencia de patron. NO verificable al 100% sin LIVE. |


### 19.3 Clasificacion SP

PR_INFORME_SALDOS = SP_EXISTING_REUSABLE_WITH_ADAPTER (candidato, condicional a revalidacion).

Justificacion:

- Patron de tabla variable local apunta a READ_ONLY en efecto.
- Familia SALDOS* (SALDOS, SALDOS_FAMILIA, SALDOS2, SALDOSDIRIGIDOS, SALDOSCTM) es PROCESS/WRITE; PR_INFORME_SALDOS es la unica variante REPORT/QUERY de la familia.
- Nombre consistente con el patron legacy PR_INFORME_* ya versionado en el proyecto (PR_INFORME_ESTRUCTURAS, PR_INFORME_IMPORTACIONES, PR_INFORME_EXPORTACIONES), todas ellas read-only y reutilizables.

Cautelas:

- Sin LIVE no se valida al 100% que las 37 columnas no incluyan efectos colaterales (por ejemplo, una columna calculada con funcion escalar mutable).
- Sin LIVE no se valida semantica exacta de parametros (@DESDE/@HASTA y @documento).
- La auditoria previa intento ejecutar el SP y devolvio 0 filas con warning de NULL en agregado; sin dataset representativo no se valida contrato de columnas.

NO se ejecuta el SP. NO se crea APP24_Q_SALDOS_LISTAR ni wrapper tecnico en este commit. La regla No inventar un APP24_Q_* antes de esta clasificacion no permite crear wrapper sin validar el SP. La clasificacion es condicional a la revalidacion del codigo fuente.

### 19.4 Gate de implementacion LEGACY-038

| Requisito | Estado |
|-----------|--------|
| LEGACY_SCREEN_SCOPE_CONFIRMED | YES (UI legacy observada con campos documento, partida, fecha de pago, operacion, material, cantidad importada, saldo comercial, saldo tarifario, valores, temporalidad, categoria, vencimiento, pedimento original, descargo, estructura, complementos, desperdicio, factura, lote) |
| CANONICAL_READ_SOURCE_CONFIRMED | NO (PR_INFORME_SALDOS tiene 37 columnas conocidas pero nombres/tipos no en repo. v_saldos/v_saldosdesp son referencias no contrato de filtro) |
| SOURCE_GRAIN_CONFIRMED | NO (grano exacto no documentado sin las 37 columnas) |
| VISIBLE_FIELDS_MAPPING_SUFFICIENT | NO (mapeo columna legacy a columna fisica requiere las 37 columnas; sin LIVE no se asignan labels legacy a columnas fisicas) |
| DATE_FILTER_SEMANTICS_CONFIRMED | NO (@DESDE/@HASTA parametros presentes pero mapeo a columna logica no confirmado: SALIDAS.Fecha vs IMPORTACIONES.Fecha vs partidas.PFECHAIMPO) |
| DOCUMENT_FILTER_SEMANTICS_CONFIRMED | NO (@documento parametro presente pero semantica no confirmada: pedimento vs factura vs material vs otro) |
| PERSISTENT_SIDE_EFFECTS | UNKNOWN (patron table variable sugiere 0; no verificable sin LIVE) |
| MUTABLE_PRECONDITION_REQUIRED | UNKNOWN (patron sugiere NO; no verificable sin LIVE) |

Resultado: 4 de 8 condiciones NO/UNKNOWN. Las dos condiciones CRITICAL (CANONICAL_READ_SOURCE_CONFIRMED y SOURCE_GRAIN_CONFIRMED) son NO. LEGACY-038 NO es implementable en esta sesion.

### 19.5 Gate de implementacion LEGACY-021

| Requisito | Estado |
|-----------|--------|
| LEGACY_SCREEN_SCOPE_CONFIRMED | PARTIAL (la pantalla existe pero no se obtuvo sesion autorizada para validar filtros/columnas/botones peligrosos en esta sesion) |
| FISCAL_SEMANTICS_CONFIRMED | NO (la operacion Saldos implica semantica fiscal con corte; no auditada) |
| CUT_DATE_SEMANTICS_CONFIRMED | NO (corte de operacion no documentado) |
| EXPOSES_OPERATIONAL_ACTIONS | UNKNOWN (botones de recalcular/regenerar/PEPS no auditados) |

Resultado: LEGACY-021 permanece BLOCKED_BUSINESS. Aunque LEGACY-038 se implemente read-only, LEGACY-021 sigue bloqueado por semantica fiscal no confirmada, corte de operacion no auditado, y posibilidad de acciones operacionales (calcular/regenerar) no descartada.

### 19.6 Decision final de esta reauditoria

- PR_INFORME_SALDOS: clasificado tentativamente como SP_EXISTING_REUSABLE_WITH_ADAPTER por evidencia versionada; requiere revalidacion LIVE para confirmar las 37 columnas y la semantica de parametros antes de cualquier wrapper.
- LEGACY-021: permanece BLOCKED_BUSINESS. No consolido contra LEGACY-038.
- LEGACY-038: permanece BLOCKED_CONTRACT. Cero wrapper creado.
- LIVE writes = 0. LIVE mutable executions = 0. NEW_QUERY_SP = 0. NEW_BUSINESS_SP = 0. INLINE_BUSINESS_SQL_JAVA = 0.
- LIVE_ACTIVATION_PENDING = N/A (no se creo SQL nuevo).
- Frontend: la opcion Saldos sigue available=false; sin habilitacion hasta que pase la caja de gates.

### 19.7 Proximos pasos para re-abrir

1. Restablecer acceso LIVE read-only a CALE_IMMEX con TLS valido (sin bypass).
2. Ejecutar sys.dm_exec_describe_first_result_set_for_object (OBJECT_ID('dbo.PR_INFORME_SALDOS'), NULL) y capturar nombres/tipos/nullability de las 37 columnas.
3. Ejecutar SELECT name FROM sys.parameters WHERE object_id = OBJECT_ID('dbo.PR_INFORME_SALDOS') y capturar nombres exactos de parametros y tipos.
4. Auditar la pantalla Web Forms autenticada (UI legacy de Saldos) y mapear columnas legacy observadas a las 37 columnas fisicas.
5. Confirmar @DESDE/@HASTA filtran SALIDAS.Fecha o IMPORTACIONES.Fecha o partidas.PFECHAIMPO.
6. Confirmar @documento representa pedimento/factura/material.
7. Validar con un corte historico representativo (no estado 0 filas actual).
8. Reevaluar gates LEGACY-038 con la nueva evidencia; implementar wrapper APP24_Q_SALDOS_LISTAR solo si las 8 condiciones quedan YES.
