# Reportes — auditoría, inventario y alcance V1

> **Estado:** auditoría cerrada para catálogo V1. No se aprueba todavía un
> endpoint independiente, backend, frontend ni exportación de reportes.
>
> **Evidencia:** definiciones y metadata read-only capturadas previamente,
> documentación versionada y auditoría funcional Web Forms. La reconsulta actual
> de `CALE_IMMEX` permanece pendiente: la conexión TLS falló antes de ejecutar
> SQL por certificado no confiable. No se usó bypass TLS, no se expusieron
> secretos y no se ejecutó SQL, views ni procedimientos durante esta fase.

## 1. Objetivo

Inventariar reportes reales de Anexo 24 y distinguir consulta segura, generador,
reproceso, historial mutable y exportación. Un objeto llamado `INFORME`,
`REPORTE` o `HISTORIA` no se considera read-only por su nombre.

Etiquetas:

- **CONFIRMADO:** definición, metadata, ejecución read-only previa o auditoría
  funcional documentada.
- **INFERIDO:** conclusión razonable que requiere comprobación actual.
- **DECISIÓN V1:** límite aprobado para V1.
- **PENDIENTE:** falta fuente, contrato o validación funcional.

## 2. Inventario y clasificación

| Candidato | Tipo | Parámetros conocidos | Lee / escribe | Clasificación | V1 |
|---|---|---|---|---|---|
| `PR_INFORME_IMPORTACIONES` | SP | `@DESDE`, `@HASTA`, `@documento` | Lee / no escribe persistente | REPORT/QUERY READ ONLY | Cubierto por Operaciones; no endpoint reporte |
| `v_Importaciones` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Referencia; no contrato API |
| `PR_INFORME_EXPORTACIONES` | SP | `@DESDE`, `@HASTA`, `@documento` | Lee / no escribe persistente | REPORT/QUERY READ ONLY | Cubierto por Operaciones; no endpoint reporte |
| `v_Exportaciones` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Referencia; no contrato API |
| `PR_INFORME_SALDOS` | SP | `@DESDE`, `@HASTA`, `@documento` | Lee / no escribe persistente | REPORT/QUERY READ ONLY | Pendiente; no cerrar Saldos |
| `v_saldos` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Referencia; semántica pendiente |
| `v_saldosdesp` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Referencia especializada |
| `PR_INFORME_ESTRUCTURAS` | SP | `@PRODUCTO`, `@MATERIAL`, `@DESDE`, `@HASTA` | Lee / no escribe persistente | REPORT/QUERY READ ONLY | Cubierto por Catálogos; no endpoint reporte |
| `v_Estructuras` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Referencia; no contrato API |
| `v_descarga` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Cubierto por Materiales Utilizados |
| `V_INFORMEDESCARGAS` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Referencia de conciliación; no contrato API |
| `INFORMEDESCARGOS` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Agregado legacy; contrato pendiente |
| `V_STATUS_DESCARGAS` | View | — | Lee / no escribe | REPORT/QUERY READ ONLY | Estado operativo; no contrato V1 |
| `DESCARGA_CTMA`, `DESCARGA_DESPERDICIO` | Views | — | Lee / no escribe | REPORT READ ONLY | Especializadas; excluidas |
| `PROC_HISTORIADESCARGASALIDA*` | SP | rango, producto, clave, documento | Lee / escribe | REPORT/PROCESS WRITE | Proceso mutable — no GET |
| `HISTORIADESCARGAS*` | SP | variantes | Lee / escribe | REPORT/PROCESS WRITE/MIXED | Proceso mutable — no GET |
| `INFORME_CONCENTRADOSALDOS` | SP | `@DESDE`, `@HASTA` | Lee / llena tabla | REPORT/PROCESS WRITE | Proceso mutable — no GET |
| `SP_GENERA_TXT_COMPLETO` | SP | según proceso legacy | Lee / escribe / archivos | EXPORT MIXED | Excluido; no reutilizar |
| `Trazo_report` | SP | `@material` | efecto actual no revalidado | UNKNOWN | No ejecutar |
| Bitácora legacy | reporte Web Forms observado | rango obligatorio observado | Fuente física no demostrada | UNKNOWN | Pendiente |

Los objetos legacy se clasifican desde evidencia previa. Sus cuerpos actuales no
están versionados en `infra/sql/`; revalidación actual requiere acceso audit-only
con certificado confiable.

## 3. Reportes web legacy

**CONFIRMADO:** auditoría funcional Web Forms observó cinco reportes:

```text
Entradas
Salidas
Saldos
Materiales utilizados
Bitácora
```

Todos seguían patrón fecha inicial + fecha final + generar + exportar, requerían
rango y devolvieron error genérico aun con rango válido. No se confirmó ruta,
source SQL, columnas, totales, paginación, formato de archivo ni exportación
exitosa. La sesión actual autorizada no está disponible.

**DECISIÓN V1:** estos labels son evidencia funcional, no autorización para crear
cinco endpoints ni para ejecutar botones legacy.

## 4. Importaciones

### Fuente legacy

**CONFIRMADO:** `dbo.PR_INFORME_IMPORTACIONES` es REPORT/QUERY read-only sobre
tablas persistentes. Usa tabla variable local `@res`; no tiene `EXEC`, cursor,
transacción ni tabla temporal persistente documentados.

| Aspecto | Evidencia |
|---|---|
| Parámetros | `@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)` |
| Fecha de rango | `Importaciones.Fecha`, fecha de pago |
| Documento | `Importaciones.Numero_ped`; si tiene valor, anula el rango |
| Fuentes | `Importaciones`, `partidas`, `Proveedores`, `categorias`, `BUSCATIPOM` → `material` |
| Grano | una fila plana por partida de importación |
| Resultado | 76 columnas, todas nullable según metadata previa |
| Orden | fecha de pago, documento y secuencia de partida |
| Paginación / total | no existen |

Aporta proveedor, RFC/TAX ID, contribuciones, valores, país, factura, saldo,
categoría, auditoría y cálculos de temporalidad/vencimiento. Incluye `float`
legacy y campos sensibles/comerciales que no se normalizan ni publican sin
contrato.

**CONFIRMADO:** `v_Importaciones` es read-only, con 70 columnas, sin parámetros,
paginación ni total. Conserva valores derivados, saldo y `BUSCATIPOM`.

### Comparación con Operaciones

`GET /api/v1/operaciones/entradas` ya ofrece filas por partida con IDs,
pedimento, clave, fecha de entrada/pago, fracción, unidad, cantidad y parte;
tiene filtros, orden, paginación y total. El informe aporta una proyección más
amplia, pero no existe necesidad funcional aprobada para publicar sus 76
columnas, agregados o cálculos variables.

**DECISIÓN V1:** `PR_INFORME_IMPORTACIONES` y `v_Importaciones` quedan como
referencias legacy. No crear `GET /api/v1/reportes/importaciones`.

## 5. Exportaciones

### Fuente legacy

**CONFIRMADO:** `dbo.PR_INFORME_EXPORTACIONES` es REPORT/QUERY read-only sobre
tablas persistentes y usa tabla variable local `@res`.

| Aspecto | Evidencia |
|---|---|
| Parámetros | `@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)` |
| Fecha de rango | `SALIDAS.Fecha`, fecha de pago/exportación |
| Documento | `SALIDAS.Documento`; si tiene valor, anula el rango |
| Fuentes | `SALIDAS`, `PSALIDAS`, `CLIENTES`, `PROVEEDORES`, `DIRIGIDO` |
| Grano | una fila plana por línea `PSALIDAS` |
| Resultado | 49 columnas, todas nullable según metadata previa |
| Orden | fecha, aduana, patente, pedimento y secuencia |
| Paginación / total | no existen |

El resultado incluye cliente/destinatario, países, factura, valores, descargo,
CTM, cantidades tarifarias, auditoría y claves técnicas. Tipos relevantes:
`Cantidad numeric(18,4)`, `Pedimento char(100)`, fecha `datetime` e IDs
`numeric(18,0)`.

La ejecución read-only histórica devolvió 3,392 líneas de 660 salidas, con
3,392 `psalidakey` distintos. El documento existente anula el filtro de rango;
no copiar esta semántica sin decisión explícita.

**CONFIRMADO:** `v_Exportaciones` es read-only con 44 columnas, sin filtros,
paginación ni total; `TOP (100) PERCENT ... ORDER BY` no fija orden contractual.

### Comparación con Operaciones

`GET /api/v1/operaciones/salidas` ya ofrece el listado operativo paginado de
líneas. El SP/view legacy proyectan datos adicionales, pero no hay contrato
aprobado que seleccione columnas, trate valores/comercialmente sensibles, defina
filtros ni justifique endpoint separado.

**DECISIÓN V1:** no crear `GET /api/v1/reportes/exportaciones`.

## 6. Saldos

**CONFIRMADO:** `PR_INFORME_SALDOS` es candidato REPORT/QUERY read-only: recibe
`@DESDE datetime`, `@HASTA datetime`, `@documento varchar(50)`, lee
`Importaciones`, `partidas` y `categorias`, y metadata previa registra 37
columnas.

`v_saldos` reporta saldo actual y `v_saldosdesp` mezcla saldo/desperdicio. No
hay granularidad, fecha de corte, filtros, aliases, orden, total ni paginación
suficientemente confirmados para contrato API.

**DECISIÓN V1:** no cerrar Saldos por Reportes. No crear
`GET /api/v1/reportes/saldos`; la fórmula, corte, grano, categorías, subtipos y
fuente permanecen pendientes según `mapeo-saldos.md`.

## 7. Estructuras

**CONFIRMADO:** `PR_INFORME_ESTRUCTURAS` es read-only, usa `@res`/`@temp`
locales y devuelve 16 columnas por detalle `productomaterial`.

| Aspecto | Evidencia |
|---|---|
| Filtros efectivos | `@PRODUCTO varchar(50)` y `@MATERIAL varchar(50)` |
| Fechas declaradas | `@DESDE`, `@HASTA datetime` |
| Semántica de fechas | no afectan el resultado |
| Fuentes | `productos`, `estructuras`, `productomaterial`, `material`, `salidas`, `psalidas` |
| Campos distintivos | fecha de fin derivada, última salida, IDs de detalle y estructura |
| Paginación / total | no existen |

`v_Estructuras` es read-only, devuelve 13 columnas por detalle BOM y calcula
`Ultima Salida`; no expone IDs, filtros ni paginación.

`GET /api/v1/catalogos/estructuras` ya es consulta BOM paginada y estable. La
última salida es cálculo operacional no aprobado como necesidad de reporte.

**DECISIÓN V1:** no crear reporte independiente de estructuras.

## 8. Materiales utilizados y descargas

**CONFIRMADO:** `v_descarga` es read-only con 35 columnas; mezcla importación,
partida, salida, producto, proveedor, saldo y componentes de descarga.
`V_INFORMEDESCARGAS` es read-only con 42 columnas y mezcla valores, cantidades,
fechas, destino y saldo actual. Ambas son reportes/conciliaciones sin contrato
parametrizado, orden, paginación ni total.

**CONFIRMADO:** `DESCARGA.Descargakey` es la identidad física de la asignación
histórica; snapshot documentado: 3,866 filas y 223 grupos duplicados legítimos.
`SALIDAS.Fecha` es fecha histórica disponible para salida; no se localizó fecha
propia de proceso en `DESCARGA`.

`GET /api/v1/operaciones/materiales-utilizados` ya cubre esta asignación física
con filtros, rango inclusivo por día, orden, paginación y total. No crea ni
recalcula `DESCARGA`.

**DECISIÓN V1:** no crear reporte independiente de Materiales Utilizados ni
`GET /api/v1/reportes/descargos`. Las views son referencia futura de conciliación
solamente.

## 9. Históricos, concentrados y procesos excluidos

| Objeto/familia | Riesgo confirmado | Decisión |
|---|---|---|
| `PROC_HISTORIADESCARGASALIDA`, `;1`, `...FALTANTES` | `TRUNCATE`/`INSERT` en historia de salida y/o trazo | No ejecutar; no GET |
| `HISTORIADESCARGAS`, `HISTORIADESCARGASF`, `HISTORIADESCARGASP` | borra/inserta historia; variantes ejecutan regularización | No ejecutar; no GET |
| `INFORME_CONCENTRADOSALDOS` | llena `CONCENTRADOSALDOS` | No ejecutar; no fuente canónica |
| `SALDOS*`, `DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`, `DESCARGAXFECHA51` | recalculan, borran/recrean descargas, saldo y trazo | No ejecutar; no dependencia GET |
| CTM, F4, desperdicio, A31 | subtipos y procesos propios | Excluidos; contrato separado |
| `Trazo_report` | semántica/efecto actual no revalidado | UNKNOWN; no ejecutar |

**PENDIENTE:** clasificar `CONCENTRADOSALDOS` como cache, staging, snapshot o
tabla histórica. No se deduce por el nombre ni se ejecuta su generador para
poblarla.

## 10. Bitácora

**CONFIRMADO:** Web Forms mostraba reporte de Bitácora con rango, pero fuente
legacy, ruta, columnas reales, grano, usuario, fecha, acción y resultado no
quedaron técnicamente vinculados a un objeto canónico.

El esquema complementario propone `app24.BitacoraEvento` con usuario, fecha UTC,
módulo, acción, resultado, detalle seguro y `correlationId`. Es diseño/DDL de
aplicación; no hay escritor ni consulta backend implementados y su propiedad
append-only no está verificada como control desplegado.

**DECISIÓN V1:** no crear `GET /api/v1/reportes/bitacora` ni usar tablas
`HISTORIA*`/`*Borradas*` como sustituto. Bitácora requiere proyecto funcional y
de seguridad independiente.

## 11. Exportaciones de archivo

**CONFIRMADO:** Web legacy observó patrón generar + exportar, pero ningún
archivo Excel, CSV, PDF o TXT se generó correctamente durante auditoría.

`dbo.SP_GENERA_TXT_COMPLETO` es EXPORT/MIXED: normaliza datos, ejecuta
actualizaciones, usa `bcp` y `xp_cmdshell`, y genera archivos en servidor SQL.

**DECISIÓN V1:** no ejecutar ni reutilizar este SP. No crear exportación
Excel/PDF/CSV/TXT en esta fase.

Estrategia futura, no implementada:

1. consulta UI paginada con contrato read-only cerrado;
2. exportación separada del dataset completo, con límites, streaming o job según
   volumen;
3. `GET /api/v1/reportes/{tipo}/exportacion` sólo tras definir formato y campos;
4. prohibido usar `tamano = Integer.MAX_VALUE`, `bcp`, `xp_cmdshell` o archivos
   temporales del servidor SQL.

## 12. Granularidad y fechas

| Reporte/candidato | Fila natural | Fecha/filtro documentado | Estado |
|---|---|---|---|
| Informe importaciones | partida de importación | `Importaciones.Fecha` = pago; documento anula rango | CONFIRMADO |
| Informe exportaciones | línea `PSALIDAS` | `SALIDAS.Fecha` = pago/exportación; documento anula rango | CONFIRMADO |
| Informe estructuras | detalle BOM `productomaterial` | inicio/fin estructura; `@DESDE/@HASTA` no filtran | CONFIRMADO |
| Materiales utilizados | fila `DESCARGA` | `SALIDAS.Fecha` | CONFIRMADO |
| Saldos | partida/material/categoría/concentrado no definido | corte no definido | PENDIENTE |
| Bitácora | evento no definido técnicamente | fecha de evento no vinculada a fuente | PENDIENTE |

No se mezclan estos granos en endpoint común.

## 13. Calidad disponible

Las métricas son snapshots documentados, no medición actual:

| Fuente | Resultado |
|---|---|
| Importaciones / partidas | 2 / 2; fechas `2025-09-23` a `2026-05-28` |
| Exportaciones / líneas | 660 / 3,392; fechas `2025-10-31` a `2026-08-18` |
| Descargas | 3,866; duplicados físicos legítimos preservados |
| Estructuras / detalle BOM | 0 / 0 en snapshot técnico |
| Web legacy estructuras | ~4,981 en auditoría funcional; discrepancia no explicada |

**PENDIENTE:** revalidar conteos, nulos, blancos, cantidades, negativos,
huérfanos, fechas y metadata por objeto con conexión SQL audit-only confiable.

## 14. Catálogo de reportes V1

### Catálogo aprobado

**DECISIÓN V1:** no hay reporte API independiente aprobado en este corte.

| Necesidad observada | Cobertura V1 | Decisión |
|---|---|---|
| Entradas | `GET /api/v1/operaciones/entradas` | CUBIERTO POR OPERACIONES |
| Salidas | `GET /api/v1/operaciones/salidas` | CUBIERTO POR OPERACIONES |
| Materiales utilizados | `GET /api/v1/operaciones/materiales-utilizados` | CUBIERTO POR OPERACIONES |
| Estructuras | `GET /api/v1/catalogos/estructuras` | CUBIERTO POR CATÁLOGOS |
| Activos fijos | `GET /api/v1/operaciones/activos-fijos` | CUBIERTO POR OPERACIONES |
| Saldos | fuente read-only candidata, semántica incompleta | PENDIENTE |
| Bitácora | fuente canónica no demostrada | PENDIENTE |
| Históricos/concentrados/export TXT | procesos mutables o mixtos | PROCESO MUTABLE — NO GET |

Un reporte futuro debe aportar proyección útil distinta, filtros, grano, fecha,
orden y respuesta cerrados; no basta renombrar un listado operacional.

## 15. Endpoints y permisos

No se aprueba endpoint independiente en esta fase. Rutas candidatas futuras, no
contratos:

```text
GET /api/v1/reportes/saldos
GET /api/v1/reportes/bitacora
GET /api/v1/reportes/{tipo}/exportacion
```

**CONFIRMADO:** seguridad ya contiene:

```text
REPORTES_GENERAR  → reportes / GENERAR
REPORTES_EXPORTAR → reportes / EXPORTAR
BITACORA_CONSULTAR → bitacora / CONSULTAR
```

`REPORTES_GENERAR` será candidato para consultas de reporte aprobadas;
`REPORTES_EXPORTAR` para exportación aprobada. No se modifican permisos ni se
reutilizan para procesos de escritura. La asignación por reporte y el permiso
final de Bitácora siguen pendientes.

## 16. Fuente futura y pendientes

Orden de decisión para cada reporte nuevo:

1. SP legacy read-only con contrato adecuado;
2. view legacy read-only adecuada;
3. SP propio `APP24_Q_REPORTE_*` autorizado, sin tocar legado.

Pendientes antes de aprobar un endpoint:

1. restaurar acceso SQL audit-only con certificado confiable;
2. revalidar definiciones, metadata, tipos, aliases y row counts actuales;
3. revisar Web legacy con sesión autorizada, sólo navegación/consulta segura;
4. decidir columnas, datos sensibles, filtros, grano, fecha, orden, paginación
   y total de cada reporte;
5. cerrar Saldos por separado: fórmula, corte y subtipos;
6. identificar fuente canónica y controles append-only de Bitácora;
7. comprobar si un export legacy read-only existe y qué formatos/headers/totales
   produce;
8. diseñar exportación API separada sin archivos generados por SQL Server.

## 17. Garantías de esta fase

- Cero cambios en `backend/**`, `frontend/**` e `infra/sql/**`.
- Cero SP APP24, exportación o SQL persistente creados.
- Cero procedimientos, views o procesos legacy ejecutados.
- Cero PR, merge o revisión automática.

## 18. Implementación y validación runtime — Reportes V1

**Esta sección supersede decisiones de no implementación y métricas pendientes
anteriores para Entradas, Salidas, Materiales utilizados y Bitácora.** No cambia
las restricciones legacy: no se ejecutan rutinas mutables.

Consultas implementadas reutilizan los casos de uso/SP existentes; no añaden SQL
ni procedimientos:

| Reporte | Consulta | Exportación | Fuente comparada |
|---|---|---|---|
| Entradas | `GET /api/v1/reportes/entradas` | `.../exportacion` | `GET /api/v1/operaciones/entradas` |
| Salidas | `GET /api/v1/reportes/salidas` | `.../exportacion` | `GET /api/v1/operaciones/salidas` |
| Materiales utilizados | `GET /api/v1/reportes/materiales-utilizados` | `.../exportacion` | `GET /api/v1/operaciones/materiales-utilizados` |
| Bitácora | `GET /api/v1/reportes/bitacora` | `.../exportacion` | `GET /api/v1/bitacora` |

Rutas de consulta requieren `REPORTES_GENERAR`; rutas XLSX requieren
`REPORTES_EXPORTAR`. Rangos son obligatorios, validación `desde <= hasta` y
paginación conservan los límites de los use cases fuentes. Exportes recorren
páginas de 100 filas hasta el total, con máximo de 10,000; sin resultados:
`204 No Content`. Texto potencialmente ejecutable por Excel se emite como cadena
con prefijo de escape para `=`, `+`, `-` y `@`.

Smoke autenticado ejecutado contra instancia local de la feature en `8082`:
login `200`; ambas authorities presentes. Totales del rango histórico probado y
comparación con endpoint fuente: Entradas `2/2`, Salidas `3392/3392`, Materiales
utilizados `3866/3866`, Bitácora `15/15`. Consultas devolvieron `200`; páginas
respetaron sus tamaños. Los conteos son medición runtime del momento, no
assertions permanentes.

Las cuatro exportaciones devolvieron `200`, media type XLSX y filename `.xlsx`;
ZIP/workbook abrieron con una hoja, headers presentes, todas las filas del
resultado (2, 3392, 3866, 15), cero merged cells y cero fórmulas. Exportación
vacía: consulta `200`/total 0 y XLSX `204`. Sin token/token inválido: `401`;
fechas faltantes, rango invertido y tamaño inválido: `400`. No se imprimieron
filas personales ni contenido de detalle de Bitácora.

Fix runtime: parámetro requerido ausente inicialmente causaba `500` porque
`MissingServletRequestParameterException` caía en handler inesperado. Handler
global ahora traduce esta excepción a `400 SOLICITUD_INVALIDA` con correlación;
regresión añadida.

### Saldos — decisión tras metadata LIVE

Consulta metadata en `CALE_IMMEX` (sin leer valores de filas ni ejecutar
procedimientos) encontró:

- `dbo.v_saldos`: columnas `Documento`, `Fecha de Pago`, `Clave`, `Fraccion`,
  `Cant. Importado`, `Saldo`, `Temporalidad(Meses)`, `Fecha de Vencimiento`,
  `PedimentoOriginal`, además de valores, desperdicio y unidades.
- `dbo.v_saldosdesp`: variante con `Saldo`, `SaldoT`, `Temporalidad(Meses)`,
  `Fecha de Vencimiento`, `PedimentoOriginal`, `Descargado`, `Desperdicio` y
  `Saldodesperdicio`.
- Ambas vistas dependen de objetos distintos/compartidos de importación,
  partidas, descargas, categorías y `BUSCATIPOM`; son candidatas de lectura, pero
  no equivalentes ni seleccionadas por contrato.
- Agregados READ-only: ambas vistas devolvieron total `0` en estado actual,
  también `0` para 2025 en `v_saldos`. No fue posible comparar con las 42 filas
  observadas en reporte histórico.
- `PR_INFORME_SALDOS` está clasificado previamente como REPORT/QUERY read-only;
  se ejecutó con rango 2025 y con rango histórico disponible. Ambos calls
  terminaron correctamente pero sin filas; SQL Server emitió warning de NULL en
  agregado. Por eso no se pudo validar columnas/fórmula ni comparar las 42 filas
  del snapshot funcional de 2025.
- `SALDOS`, `SALDOS_FAMILIA`, `SALDOS2` y `SALDOSCTM` no se ejecutaron; metadata
  y uso documentado los clasifican como procesos de cálculo/descarga. No son
  fuente GET segura.

**Decisión:** `REPORT_SALDOS_BLOCKED_DOMAIN_RULE`. No se creó SP wrapper ni
endpoint: cero filas actuales impiden validar resultado y no hay decisión sobre
vista variante, fecha de corte vs rango, saldo de desperdicio, conversiones ni
compatibilidad histórica. Frontend mantiene Saldos deshabilitado con mensaje de
pendiente contractual; no afirma “sin datos”. Se requiere fuente/variante
aprobada y fixture/corte histórico representativo para cerrar.

### Regresión y alcance

Después del fix: backend `clean test build` PASS, 381 tests; frontend `65` tests,
lint y build PASS (warnings de budgets existentes). No hubo DML, DDL de tablas
legacy, ejecución de SP desconocidos, cambios de permisos ni procedimientos SQL
nuevos. Apache POI se usa para archivos XLSX generados en backend; no se dejó
export ni log temporal en el repositorio.
