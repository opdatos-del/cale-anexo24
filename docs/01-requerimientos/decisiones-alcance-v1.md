# Decisiones pendientes V1 — paquete ejecutivo

Versión breve y accionable para la sesión de definición con negocio. Reemplaza la
versión extensa previa: el detalle técnico por tema sigue en
`docs/03-diseno/mapeo-saldos.md`, `mapeo-descargos.md`, `mapeo-facturacion.md` y
en `docs/01-requerimientos/cierre-alcance-v1.md` §9.

Regla de lectura: ninguna opción listada está elegida. Cada decisión se
clasifica al final como `MUST_HAVE_V1`, `POST_V1` o `NOT_REQUIRED`.

Contexto técnico vigente: `docs/05-pruebas/v1-technical-closure.md`
(`V1_TECHNICALLY_READY = YES`, `TECHNICAL_BLOCKERS = 0`; las decisiones de este
paquete no bloquean la entrega técnica del alcance actual, definen el alcance
siguiente).

## A. DECISION_REQUIRED_NOW

### A1. PED-005 / PED-006 — Inventario en validación de pedimentos

| Campo | Contenido |
|---|---|
| Pregunta | ¿Qué fuente y regla definen el inventario que la validación de pedimentos debe consultar? |
| Por qué | Las reglas PED-005/PED-006 dependen de `INVENTARIO`, que no existe en LIVE |
| Feature que desbloquea | Validación completa de pedimentos (`LEGACY-017`) |
| Evidencia que falta | Fuente autoritativa de inventario + regla exacta + caso de aceptación |
| Opciones conocidas | (a) definir vista/tabla equivalente read-only; (b) excluir formalmente esas reglas de V1; (c) POST_V1 |
| Impacto si queda fuera | V1 valida con PED-001..004/007 (ya implementadas); las demás quedan documentadas como pendientes |

### A2. Saldos — consulta, reporte y concentrado

| Campo | Contenido |
|---|---|
| Pregunta | ¿Qué es un “Saldo”, cuál es la fila oficial y cuál es la fórmula/fuente/fecha de corte? |
| Por qué | `PARTIDAS.Saldo` no es fórmula oficial; `PR_INFORME_SALDOS`/`v_saldos`/`v_saldosdesp` son candidatos sin contrato aprobado |
| Feature que desbloquea | Consulta de saldos (`LEGACY-021`), reporte de saldos (`LEGACY-038`) y concentrado (`LEGACY-042`) |
| Evidencia que falta | Fórmula, fuentes, granularidad, corte, unidades/redondeo, tratamiento de ajustes/retornos/mermas/activos fijos + 2–3 casos anonimizados reconciliables |
| Opciones conocidas | (a) fuente oficial entre los objetos legacy observados; (b) nueva proyección read-only sobre tablas existentes; (c) POST_V1 |
| Impacto si queda fuera | Sin reporte fiscal de saldos en V1; el resto de consultas/descargas permanece disponible |

### A3. Descargos / PEPS — motor automático y dirigido

| Campo | Contenido |
|---|---|
| Pregunta | ¿El motor de descargos (automático y dirigido) es `MUST_HAVE_V1`, `POST_V1` o `NOT_REQUIRED`? |
| Por qué | Algoritmo PEPS, idempotencia, autorización dual y rollback no están definidos; el legacy tiene variantes (`DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`, `SALDOS*`) sin regla de elección |
| Feature que desbloquea | Descargo automático (`LEGACY-029`) y dirigido (`LEGACY-030`) |
| Evidencia que falta | Algoritmo oficial, separación de funciones, dry-run esperado, caso de aceptación autorizado |
| Opciones conocidas | (a) POST_V1; (b) MUST_HAVE_V1 con simulación/dry-run previo, permisos separados y trazabilidad |
| Impacto si queda fuera | La operación sigue dependiendo del sistema legacy para generar descargos; V1 sólo consulta el histórico |

### A4. Facturación — confirmación operativa

| Campo | Contenido |
|---|---|
| Pregunta | Después del preview aprobado, ¿qué proceso exacto debe modificar inventario/operación? |
| Por qué | La carga V1 termina en validación/staging; `CARGA_FACTURAS`, `CARGAFACTURASENPSALIDAS` y `CREAPRODUCTOSCARGAFACTURA` tienen efectos distintos y no son sinónimos |
| Feature que desbloquea | Confirmación de facturación (`LEGACY-058`) |
| Evidencia que falta | Pipeline autoritativo, tablas afectadas, plantilla oficial vigente, reglas de faltantes, idempotencia/rollback y caso de 1–3 filas con resultado esperado |
| Opciones conocidas | (a) flujo A; (b) flujo B; (c) flujo C; (d) adaptador nuevo; (e) omitir confirmación en V1 |
| Impacto si queda fuera | Facturación V1 permanece como carga+validación+preview (sin efectos operativos) |

### A5. Dashboard V1 — suficiencia

| Campo | Contenido |
|---|---|
| Pregunta | ¿El dashboard actual (saludo, accesos por permiso, avisos, estado y total de materiales) es suficiente para V1? |
| Por qué | No se agregan KPIs por inferencia |
| Feature que desbloquea | Cierre de alcance del dashboard |
| Evidencia que falta | Clasificación de métricas candidatas (materiales/productos, movimientos del periodo, cargas, errores, actividad) |
| Opciones conocidas | (a) suficiente → ampliaciones `OUT_OF_SCOPE_V1`; (b) ampliar con métricas `MUST_HAVE_V1` priorizadas con fórmula/fuente/periodo |
| Impacto si queda fuera | El dashboard se mantiene como está; no bloquea nada |

### A6. Decisiones adicionales (misma sesión si hay tiempo)

- `LEGACY-054/055` — confirmación de importación de materiales/productos hacia
  catálogo (mismo patrón que pedimentos).
- `LEGACY-056` — importación de clientes/proveedores (comparte efectos con
  facturación).
- `LEGACY-043/044/045/050/052/077` — consolidados/actividades: confirmar si se
  consolidan en superficies actuales o requieren proyección propia.
- `LEGACY-001` — ¿V1 exige edición/mantenimiento de datos generales?
- `LEGACY-064/065` (ajuste anual) y `LEGACY-069..073` (Anexo 30) — decisión de
  alcance y evidencia regulatoria.

## B. EXTERNAL_CONTRACT_REQUIRED — `WAITING_EXTERNAL_CONTRACT`

Sin contrato, layout o pantalla utilizable. No se reauditan; se retoman sólo con
evidencia externa nueva.

| ID | Capacidad | Evidencia externa que falta |
|---|---|---|
| 023 | Cambios de régimen | flujo/pantalla + regla |
| 024 | Regularizaciones | flujo/pantalla + regla |
| 025 | Actas de destrucción (operación) | layout de archivo + reglas (+ decisión) |
| 026 | Transferencias de submaquila | layout + reglas fiscales (+ decisión) |
| 027 | Constancias | layout y efectos |
| 028 | CTM (proceso) | contrato de flujo (+ decisión) |
| 048 | Scrap / desperdicio | contrato de pantalla (columnas, grano, filtros, caso) |
| 059 | Servicios | fuente exacta no localizada |
| 060 | Actas de destrucción (importación) | layout de archivo + contrato de errores |
| 061 | Órdenes de fabricación | fuente exacta no localizada |
| 062 | Procesos | fuente exacta no localizada |
| 063 | CTM / carta de materiales | layout y proceso autoritativo |

## C. POST_V1 — ya clasificado, no requiere decisión ahora

- `014` consultas guardadas.
- `031` resolución/desbloqueo de operaciones bloqueadas.
- `032` extensión de análisis de descarga (faltantes/trazo).
- `041` resto del subsistema CTM y HDE.
- `046` alcance completo de vencimientos.
- `049` generación dirigida/PEPS.
- `066/067/068` secciones restantes del ajuste anual.
- Extensiones read-only de compulsa/rectificaciones detalladas.

## Registro de clasificación (a completar en la sesión)

| Tema | Clasificación | Fecha | Responsable | Nota |
|---|---|---|---|---|
| A1 PED-005/006 | | | | |
| A2 Saldos | | | | |
| A3 Descargos/PEPS | | | | |
| A4 Facturación | | | | |
| A5 Dashboard | | | | |

Clasificaciones válidas: `MUST_HAVE_V1`, `POST_V1`, `NOT_REQUIRED`.

## Estado de control

```text
V1_TECHNICALLY_READY = YES       (ver v1-technical-closure.md)
PROJECT_COMPLETE = NO            (decisiones + evidencia externa pendientes)
TECHNICAL_INTERNAL_BLOCKERS = 0
SALDOS = PENDING_BUSINESS
DESCARGOS_PEPS = PENDING_BUSINESS
FACTURACION_CONFIRM = PENDING_BUSINESS
DASHBOARD_V1 = PENDING_BUSINESS
PED_005_006 = PENDING_BUSINESS
EXTERNAL_CONTRACT_ITEMS = 12 (sección B)
SCOPE_DECISION_REQUIRED = YES
V1_SCOPE_FROZEN = NO             (hasta registrar respuestas y aprobación)
```

Este documento no modifica Java, Angular, SQL, procedimientos ni migraciones.
