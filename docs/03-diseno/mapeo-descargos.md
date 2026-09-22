# Descargos — ingeniería inversa y delimitación de alcance

## 0. Estado y evidencia

- **Fecha:** 2026-09-22.
- **Rama:** `feature/backend-discharges`.
- **Base:** `dev` en `8a53824e4abcf11eccd0292bb6dad9495e9d1407`.
- **Modo:** auditoría documental y SQL estrictamente read-only.
- **Datos / objetos legacy:** no modificados; ningún procedimiento ejecutado.
- **Estado:** **ALCANCE V1 DELIMITADO — SIN ENDPOINT GET DESCARGOS INDEPENDIENTE**.

### Convención

- **CONFIRMADO:** definición SQL, metadata o consulta read-only de auditorías previas documentadas.
- **INFERIDO:** conclusión técnica consistente, no demostrada por una ejecución funcional del proceso.
- **DECISIÓN V1:** límite deliberado de nuevo sistema.
- **PENDIENTE:** requiere acceso autorizado adicional, sesión legacy o decisión negocio.

### Límite de revalidación actual

**PENDIENTE:** intento actual de conexión read-only a `CALE_IMMEX` con SQLCMD e
Integrated Authentication no llegó a ejecutar SQL: SQL Server presentó cadena de
certificado no confiable. No se debilitó validación TLS, no se expusieron ni usaron
credenciales y no se ejecutó ningún procedimiento. Por tanto, conteos y metadata
abajo son evidencia del snapshot read-only documentado el 2026-09-21 en
[`mapeo-materiales-utilizados.md`](mapeo-materiales-utilizados.md), no una nueva
medición de 2026-09-22.

## 1. Objetivo

Definir qué significa **Descargos** en `CALE_IMMEX` y separar:

```text
Consulta histórica de asignaciones
≠ ejecución/reproceso de descargos
≠ recálculo de saldos
```

## 2. Definición funcional observada

**CONFIRMADO:** `dbo.DESCARGA` conserva asignaciones históricas entre una partida
de entrada, material y línea de salida. Sus cantidades separan incorporación,
merma y desperdicio.

**CONFIRMADO:** procedimientos `DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`,
`DESCARGAXFECHA51` y `SALDOS*` generan o recalculan esas asignaciones. Borran o
recrean datos, actualizan `PARTIDAS.Saldo` y escriben trazabilidad.

**DECISIÓN V1:** “Descargos” no equivale a un CRUD ni a un `GET` que active esos
procesos. La consulta histórica ya está cubierta por Materiales Utilizados.

## 3. Diferencia con Materiales Utilizados

| Pregunta | Evidencia / conclusión |
|---|---|
| Fuente histórica | **CONFIRMADO:** ambas consultas potenciales parten de `dbo.DESCARGA`. |
| Granularidad | **CONFIRMADO:** una fila física `DESCARGA.Descargakey`; asignación entrada → salida + material. |
| Snapshot conocido | **CONFIRMADO (2026-09-21):** 3,866 filas; 3,643 combinaciones distintas `(Pentradalink, Psalidalink, Clave)`; 223 duplicados históricos legítimos. |
| Encabezado / folio / lote de descargo | **PENDIENTE:** no encontrado en columnas de `DESCARGA` documentadas ni demostrado en tablas de historia. |
| Fecha propia de ejecución | **PENDIENTE / no encontrada:** `DESCARGA` no documenta fecha de proceso; `SALIDAS.FECHADESCARGA` fue nula en 660/660 filas del snapshot. |
| Status / vínculo G5-G6 / tipo fiscal superior | **PENDIENTE:** existen reportes y subsistemas especializados, no una entidad canónica demostrada para V1. |
| Valor nuevo para GET Descargos | **No demostrado.** Repetir `DESCARGA` duplicaría `GET /api/v1/operaciones/materiales-utilizados`. |

**DECISIÓN V1:** no crear `GET /api/v1/operaciones/descargos`. Materiales
Utilizados sigue siendo única consulta paginada del resultado persistido.

## 4. Tablas y relaciones

### 4.1 Mapa confirmado

```text
IMPORTACIONES.Ipedimentokey
  ← PARTIDAS.Importacionlink
  ← DESCARGA.Entradalink / Pentradalink (float legacy)

SALIDAS.SalidaKey
  ← PSALIDAS.Salidalink
  ← DESCARGA.Salidalink / Psalidalink (float legacy)

DESCARGA
  = asignación histórica persistida
```

**CONFIRMADO:** relaciones principales son lógicas; no se observaron FKs DDL
entre estas tablas en auditoría previa. Links `DESCARGA` son `float`; cualquier
join debe validar integralidad antes de convertirlos.

### 4.2 `dbo.DESCARGA`

**CONFIRMADO (snapshot 2026-09-21):**

- PK identity: `Descargakey bigint`.
- Índices: `IHD1(Entradalink, Pentradalink)`, `INDICE2_G5(Pentradalink)`,
  `op4(Psalidalink)` con `Descargakey` incluido.
- Sin FKs documentadas.
- No hay campos documentados de fecha de proceso, usuario, folio o status.
- Campos operativos relevantes:

```text
Descargakey,
Entradalink, Pentradalink, Salidalink, Psalidalink,
CantUtil, Merma, Desperdicio, CantUtilT, MermaT, DesperdicioT,
Unidad, UnidadT, Importacion, Salida, Clave, PT,
linea, orden, PROMKEY, tipoItem, SALDOCTMA
```

**CONFIRMADO:** `linea = 1`, `orden = 10` y `PROMKEY = NULL` en las 3,866 filas
conocidas; no forman identidad funcional. Identidad segura: `Descargakey`.

### 4.3 Calidad conocida

| Medición snapshot | Resultado |
|---|---:|
| `DESCARGA` | 3,866 filas |
| IDs | 1 a 3,866; 3,866 distintos |
| `CantUtil` | no nulo; 0.4234 a 14664.0892; sin negativos ni ceros |
| `Merma` | no nulo; 0 a 320.632; sin negativos |
| `Desperdicio` | 3,866 nulos |
| `TRAZO` relacionado | 0 filas / 3,866 descargas sin vínculo |
| `PARTIDAS`, `IMPORTACIONES`, `PSALIDAS`, `SALIDAS` sin vínculo a descarga | 0 en mediciones inversas documentadas |

**PENDIENTE:** repetir conteos, nulos, blancos, huérfanos y validación de links
`float` con login read-only/certificado confiable.

## 5. Fecha y cantidades

### 5.1 Fecha

**CONFIRMADO:** no se encontró fecha propia utilizable en `DESCARGA`.

| Candidato | Snapshot | Uso / decisión |
|---|---|---|
| `SALIDAS.Fecha` | 660/660; 2025-10-31 a 2026-08-18 | Fecha operacional de salida e histórico. Materiales Utilizados V1 la usa. |
| `PSALIDAS.Fecha` | 3,392/3,392; mismo rango | Fecha de línea/factura; no elegida como fecha principal. |
| `SALIDAS.FECHADESCARGA` | 0/660 | No utilizable. |
| fecha proceso en `DESCARGA` | no documentada | No demostrada. |

**DECISIÓN V1:** no se crea fecha “de descargo” artificial. La consulta
histórica existente conserva `SALIDAS.Fecha`.

### 5.2 Cantidades

**CONFIRMADO:** `SALDOS` separa e inserta `CantUtil`, `Merma` y `Desperdicio`;
actualiza `PARTIDAS.Saldo` e inserta `TRAZO`.

**DECISIÓN V1 existente:** Materiales Utilizados normaliza origen `float` a
`DECIMAL(18,4)` y conserva componentes nulos. No recalcula saldos en frontend ni
backend.

## 6. Trazo, dirigido, saldos e históricos

### 6.1 `TRAZO`

**CONFIRMADO:** tabla de trazabilidad/faltantes del proceso; vacía en snapshot.
Procesos de descargo la borran y rellenan. No es vínculo obligatorio para leer
histórico `DESCARGA`.

**DECISIÓN V1:** no exponer `TRAZO` como API de Descargos.

### 6.2 Descargo dirigido

**CONFIRMADO:** `DIRIGIDO` estaba vacío en snapshot. `DESCDIRIGIDA` recibe
`@SALIDAKEY`, `@PSALIDAKEY` y delega flujos dirigidos hacia cálculo de saldo.
`CARGAPEDIMENTOS` puede crear filas dirigidas.

**INFERIDO:** dirigido es subtipo de generación, no listado histórico
independiente. Queda fuera de V1.

### 6.3 Saldos

**CONFIRMADO:** `SALDOS` selecciona partidas descargables, inserta `DESCARGA`,
actualiza `PARTIDAS.Saldo` e inserta `TRAZO`. `PARTIDAS.Saldo` es resultado
mutable/cached del cálculo, no fuente inmutable de consulta.

**DECISIÓN V1:** Saldos será auditoría/módulo independiente; no agregar saldo a
Materiales Utilizados ni crear GET Descargos basado en saldo actual.

### 6.4 Históricos y respaldos

**CONFIRMADO:** nombres `HISTORIADESCARGAS*` no implican lectura segura:

- `HISTORIADESCARGAS` borra/inserta historia.
- `HISTORIADESCARGASF` y `HISTORIADESCARGASP` ejecutan `LIGADESPERDICIOS`.
- `PROC_HISTORIADESCARGASALIDA*` truncan e insertan historia de salida.
- `DESCARGAXFECHA51` referencia `descargaRespaldo` y `MensajeDescarga`.

**PENDIENTE:** determinar con definición SQL actual si alguna tabla histórica
preserva ejecución, usuario, versión o snapshot apto para consulta independiente.
No se ejecutó ningún proceso para poblarla.

## 7. F4, CTM, desperdicio y A31

| Área | Evidencia | Alcance V1 |
|---|---|---|
| Desperdicio | `DESCARGA_DESPERDICIO`, `LIGADESPERDICIOS`, `DescargaDesp` y variantes. Procesos mutables. | Excluido; valor histórico nullable ya visible en Materiales Utilizados. |
| CTM | `DESCARGA_CTMA`, `CTMDESCARGA`, `SALDOSCTM`, `GUARDADESCARGACTMA`, `LIGACTMA`. | Excluido; flujo especializado con saldos/trazos propios. |
| F4 | `V_F4CTMA`, `V_F4DESP`, `PROC_DESCARGASF4CTMA`. | Excluido; subtipo/regla especializada sin contrato común demostrado. |
| A31 | `A31_DESCARGAS`, `A31_SALDOS`, `A31_TRAZO`, `DESCARGAS_A31`, hojas de trabajo. | Excluido; subsistema y proceso separado. |

## 8. Procedimientos y clasificación

| Objeto | Tipo | Efecto | Evidencia / dependencia |
|---|---|---|---|
| `DESCARGATSALIDA` | PROCESS | WRITE / confirmado por familia | Despacha gestión de descargas; cuerpo actual pendiente de releer. |
| `DESCARGATSALIDA1` | PROCESS | WRITE | “BORRO EL TRAZO”; `SETTINGS`; borra/recrea descargas y saldos. |
| `DESCARGATSALIDAFECHA(@HASTA)` | PROCESS | WRITE | Variante hasta fecha; limpia/recalcula y afecta saldos. |
| `DESCARGASALIDAPEPS(@SALIDAKEY)` | PROCESS/CALCULATION | WRITE | `GETPRODUCTSTRUCT` → `SALDOS`; borra/recrea `TRAZO`/`DESCARGA`. |
| `DESCARGAXFECHA51(@hasta)` | PROCESS/CALCULATION | WRITE | `alternativo`, `SETTINGS`, `descargaRespaldo`, `MensajeDescarga`, `estructurasML`. |
| `SALDOS`, `SALDOS_FAMILIA`, `SALDOS2` | CALCULATION | WRITE | Inserta descarga, actualiza saldo; variantes. |
| `SALDOSDIRIGIDOS`, `DESCDIRIGIDA`, `INSERTADIRIGIDOS` | CALCULATION/PROCESS | WRITE or UNKNOWN | Flujo dirigido; no ejecutar. |
| `HISTORIADESCARGAS*` | REPORT/PROCESS | WRITE/MIXED | Borrado/insert de historia y regularización desperdicio. |
| `LIGADESPERDICIOS`, `LIGACTMA`, `GUARDADESCARGACTMA` | PROCESS/COMMAND | WRITE | Regulariza/vincula flujos especiales y saldos. |
| `DESCARGASCTM*`, `SALDOSCTM` | PROCESS/CALCULATION | WRITE | CTM y trazo especializado. |
| `DESCARGAS_A31` | PROCESS | WRITE | Tablas y saldos A31. |

### Mapa de dependencias observado

```text
SALIDAS / PSALIDAS
  → GETPRODUCTSTRUCT
  → ESTRUCTURAS / PRODUCTOMATERIAL
  → DESCARGASALIDAPEPS o DESCARGATSALIDA*
      → DESCDIRIGIDA / SALDOSDIRIGIDOS (cuando aplica)
      → SALDOS
          → INSERT / recreación DESCARGA
          → UPDATE PARTIDAS.Saldo
          → INSERT / recreación TRAZO
```

**CONFIRMADO:** no ejecutar esta cadena desde GET ni durante auditoría.

## 9. Fuentes read-only y reportes

**CONFIRMADO (auditoría previa):** existen views `v_descarga`,
`V_INFORMEDESCARGAS`, `INFORMEDESCARGOS`, `V_STATUS_DESCARGAS`,
`DESCARGA_CTMA`, `DESCARGA_DESPERDICIO`, `v_saldos` y `v_saldosdesp`.

**DECISIÓN V1:** ninguna se reutiliza como API:

- mezclan saldo actual, conciliación, valores fiscales o subtipos;
- no prueban entidad superior de descargo;
- no ofrecen contrato paginado/filtros/orden estable;
- su objeto fuente común para detalle histórico es `DESCARGA`, ya expuesto.

**PENDIENTE:** seleccionar/examinar controladamente views actuales sólo con acceso
read-only restablecido. No se ejecutó ninguna view en esta fase.

## 10. Pantalla legacy

**PENDIENTE:** no existió sesión autorizada Web Forms durante esta fase.

No se navegaron ni accionaron botones de procesar, recalcular, borrar, confirmar,
PEPS, descargo por fecha o historial. Se requiere observación read-only para
confirmar labels, filtros, acciones peligrosas y si UI presenta entidad distinta.

## 11. Decisión arquitectónica

**DECISIÓN V1 — opción C:** consulta histórica relevante ya queda cubierta por
`GET /api/v1/operaciones/materiales-utilizados`. Crear
`GET /api/v1/operaciones/descargos` hoy duplicaría fuente, granularidad, 3,866
filas y fecha de salida, sin aporte funcional demostrado.

No existe contrato HTTP Descargos V1, SP propio ni frontend Descargos.

## 12. Command futuro

**Sí, futuro proyecto separado:** ejecución/reproceso de descargos.

| Tema | Estado / riesgo |
|---|---|
| Raíces candidatas | `DESCARGASALIDAPEPS`, `DESCARGATSALIDA*`, `DESCARGAXFECHA51`; confirmar dispatcher actual. |
| Side effects | `DESCARGA`, `PARTIDAS.Saldo`, `TRAZO`, historia, ajustes dirigidos/CTM/desperdicio. |
| Idempotencia | PENDIENTE; nombres/definiciones indican borrado y recreación. |
| Rollback / transacción | PENDIENTE; inspeccionar definición actual. |
| Locking / concurrencia / duración | PENDIENTE; medir sólo en ambiente seguro con plan autorizado. |
| Audit trail | PENDIENTE; tablas de historia no son lectura segura por sí mismas. |
| Permiso | No reutilizar `OPERACIONES_CONSULTAR`; requiere permiso explícito y separación de funciones. |

Un comando futuro no es CRUD: necesita caso de uso aprobado, simulación o dry-run,
protección contra reproceso concurrente, auditoría, reversión y ownership fiscal.

## 13. Pendientes

1. Reestablecer conexión SQL con certificado confiable y login audit-only; repetir
   metadata, row counts, links float, tablas historia y definiciones completas.
2. Auditar UI legacy sin ejecutar acciones mutables.
3. Confirmar entidad/encabezado de ejecución, usuario, fecha proceso y status.
4. Inspeccionar `DESCARGATSALIDA1` vs `DESCARGATSALIDAFECHA` línea por línea.
5. Inspeccionar algoritmo PEPS, transacciones, cursores, errores, locks y rollback.
6. Decidir con negocio si comando futuro requiere simulación, aprobación dual y
   bitácora inmutable.
