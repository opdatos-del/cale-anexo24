# Desarrollo

Capítulo estructurado por fases cronológicas. Cada fase registra: problema,
método, decisión, implementación, pruebas, resultado y limitación. El detalle
factual por fase está consolidado en
`docs/08-reporte-estadia/evidencias-proyecto.md`.

## Fase 1 — Auditoría del sistema legacy

- **Problema:** operación crítica sin documentación técnica suficiente y con
  riesgos observables (credenciales, permisos, errores).
- **Método:** auditoría funcional de la interfaz legacy con datos sintéticos
  limpios; registro de hallazgos y reglas de negocio.
- **Decisión:** convertir observaciones en requisitos verificables; no asumir
  reglas no observadas.
- **Implementación:** línea base funcional y de riesgos.
- **Pruebas:** N/A (fase de análisis).
- **Resultado:** catálogos, operaciones, reportes, permisos y carga
  inventariados; hallazgos priorizados.
- **Limitación:** sin acceso inicial a código ni a la base.

## Fase 2 — Descubrimiento de `CALE_IMMEX`

- **Problema:** determinar la fuente autoritativa y los objetos reutilizables.
- **Método:** lectura de metadata (`sys.objects`, `sys.sql_modules`,
  `sys.columns`, `sys.sql_expression_dependencies`, `OBJECT_DEFINITION`) y
  conteos read-only; ningún objeto mutable ejecutado.
- **Decisión:** `CALE_IMMEX` es la única base operativa autoritativa; regla
  SP-FIRST (buscar, auditar, reutilizar antes de crear).
- **Implementación:** inventario y clasificación de procedimientos
  (`SP_EXISTING_REUSABLE`, `NEW_SP_REQUIRED`, etc.).
- **Pruebas:** verificación de definiciones y dependencias.
- **Resultado:** mapa de objetos y límites de ejecución.
- **Limitación:** los procesos mutables quedan fuera de ejecución por diseño.

## Fase 3 — Diseño de arquitectura

- **Problema:** modernizar sin reemplazar el modelo legacy ni duplicar datos.
- **Método:** arquitectura hexagonal modular; dos orígenes de datos
  (`CALE_IMMEX` operativo y `ANEXO24_DEV/app24` complementario).
- **Decisión:** el modelo legacy se aísla en infraestructura; `app24` aloja
  usuarios, permisos, bitácora y staging; prohibido crear base operativa
  paralela.
- **Implementación:** esqueleto Spring Boot + Angular, configuración por
  perfiles, CI inicial.
- **Pruebas:** unitarias y de configuración.
- **Resultado:** base del proyecto con gates (SP-FIRST, build).
- **Limitación:** configuración productiva se completó en una fase posterior.

## Fase 4 — Catálogos

- **Problema:** consultar maestros sin CRUD legacy.
- **Método:** SP read-only por catálogo + endpoints paginados + UI agrupada.
- **Decisión:** sólo lectura en V1; División se resolvió como equivalencia de
  Almacén con evidencia.
- **Implementación:** materiales, productos, estructuras, catálogos auxiliares
  (unidades, tipos, categorías, almacenes), datos generales y socios
  comerciales (clientes, proveedores, agentes).
- **Pruebas:** unitarias de mapeo/estado y E2E de consulta y filtros.
- **Resultado:** catálogos operativos contra LIVE.
- **Limitación:** edición/mantenimiento y CRUD fuera de alcance.

## Fase 5 — Operaciones read-only

- **Problema:** consultar movimiento operativo con filtros y paginación.
- **Método:** SP read-only con parámetros de fecha/texto + UI con periodo,
  filtros opcionales y página.
- **Decisión:** proyecciones nuevas (no replicar el número de columnas legacy).
- **Implementación:** entradas, salidas, materiales utilizados y activos
  fijos.
- **Pruebas:** E2E con datasets reales (paginación y filtros verificados).
- **Resultado:** consultas estables contra LIVE.
- **Limitación:** no ejecuta descargos ni recalcula saldos.

## Fase 6 — Usuarios, perfiles, permisos y bitácora

- **Problema:** administración segura con trazabilidad.
- **Método:** esquema `app24` con SP de comando y consulta; RBAC por actividad.
- **Decisión:** autorización en backend; frontend sólo UX.
- **Implementación:** CRUD de usuarios, perfiles/permisos, actividades y
  bitácora con `correlationId`.
- **Pruebas:** unitarias, E2E administrativo y auditoría de bitácora.
- **Resultado:** administración completa y auditable.
- **Limitación:** decisiones de lockout/verificación de correo no forman parte
  del contrato nuevo.

## Fase 7 — Reportes

- **Problema:** consolidados confiables con exportación.
- **Método:** SP read-only por reporte, filtros/paginación, exportador XLSX
  común.
- **Decisión:** V1 pequeña y fiel por contrato; export sólo donde aplica.
- **Implementación:** 11 tipos (entradas, salidas, materiales utilizados,
  bitácora, compulsa, rectificaciones, vencimientos, dirigidos, análisis de
  descargas, operaciones bloqueadas y F4).
- **Pruebas:** unitarias, E2E de generación/paginación y validación de XLSX con
  `openpyxl`.
- **Resultado:** 11/11 API 200; 4 exports operativos verificados; F4 204 con
  dataset vacío.
- **Limitación:** reportes textuales sin export por contrato; detalle fiscal
  completo (faltantes/trazo/saldos) fuera de V1.

## Fase 8 — Staging de pedimentos

- **Problema:** cargar pedimentos sin tocar la operación ni el staging legacy.
- **Método:** upload `.xls/.xlsx`, parser por contrato derivado, validación,
  hash, errores localizables y preview paginado en `app24`.
- **Decisión:** staging aislado propio; no reutilizar `CargaPedimentosIE`.
- **Implementación:** tablas y SP `app24.*`, endpoints y UI de carga.
- **Pruebas:** unitarias, SQL IT y E2E sintético (incluye duplicado 409).
- **Resultado:** staging y validación completos.
- **Limitación:** sin confirmación en esta fase.

## Fase 9 — Confirmación autoritativa de pedimentos

- **Problema:** aplicar la carga validada a la operación real con garantías.
- **Método:** comando `APP24_C_PEDIMENTO_CONFIRMAR` con transacción atómica,
  idempotencia y locks compatibles con el motor legacy.
- **Decisión:** confirmar sólo con contrato auditado y caso de aceptación;
  importación tipo 1 → `IMPORTACIONES`/`PARTIDAS`; tipo 2 →
  `SALIDAS`/`PSALIDAS` (y `DIRIGIDO` condicional).
- **Implementación:** comando, permiso `PEDIMENTOS_CONFIRMAR`, UI con
  confirmación explícita.
- **Pruebas:** SQL IT (15/15), concurrencia `MAX+1`, rollback total,
  `ALREADY_CONFIRMED`, `DUPLICATE_OPERATION`.
- **Resultado:** pipeline autoritativo reproducible.
- **Limitación:** no ejecuta el motor de descargos/PEPS posterior.

## Fase 10 — Staging de materiales y productos

- **Problema:** importar catálogos con diagnóstico sin escribir maestros.
- **Método:** parser por contrato de stage, validación estructural, hash,
  errores y preview.
- **Decisión:** staging aislado; confirmación hacia `MATERIAL`/`PRODUCTOS`
  queda pendiente de negocio.
- **Implementación:** tablas/SP `app24.*` y UI de importación.
- **Pruebas:** unitarias y E2E (material y producto `PREVISUALIZADA`).
- **Resultado:** staging y validación operativos.
- **Limitación:** sin confirmación a catálogo.

## Fase 11 — Staging de facturación

- **Problema:** validar facturación con diagnóstico y control de duplicados.
- **Método:** plantilla activa versionada (`FACTURAS`, 13 columnas), parser,
  validación, hash y preview.
- **Decisión:** la carga termina en validación/staging; la confirmación
  operativa requiere decisión de negocio (pipeline autoritativo).
- **Implementación:** staging v2, endpoints y UI de carga múltiple.
- **Pruebas:** unitarias y E2E (`VALIDADA` y duplicado 409).
- **Resultado:** flujo de carga confiable para revisión.
- **Limitación:** sin efectos operativos.

## Fase 12 — Paridad funcional

- **Problema:** saber qué capacidades legacy están cubiertas y cuáles no.
- **Método:** matriz de 79 capacidades con estados y evidencia; auditorías
  dirigidas sólo donde faltaba contrato (scrap, actas, CTM/HDE, submaquila).
- **Decisión:** no inventar semántica; lo no demostrado permanece
  `UNKNOWN`/`MISSING` con bloqueo clasificado.
- **Implementación:** matriz, backlog por épicas y paquete de decisiones.
- **Pruebas:** recuento fila por fila en el cierre (corrige agregados previos).
- **Resultado:** cobertura clasificada 5/26/12/6/5/6/19.
- **Limitación:** 19 capacidades siguen sin contrato funcional.

## Fase 13 — Auditoría E2E

- **Problema:** verificar que lo construido funciona de principio a fin.
- **Método:** sweep interactivo (Playwright/Edge headless) con JWT efímeros por
  perfil, fixtures sintéticos, validación XLSX y verificación RBAC API/browser.
- **Decisión:** staging sintético permitido; ninguna confirmación ejecutada.
- **Implementación:** arnés temporal de auditoría (no versionado) y evidencia
  documentada.
- **Pruebas:** 21 rutas, 11 reportes, 4 flujos de carga, exportaciones,
  responsive 40/40.
- **Resultado:** API 25/25 esperado; P0=0, P1=1, P2=1, P3=2 (hallazgos
  iniciales).
- **Limitación:** login con credenciales reales queda
  `OPERATOR_ACCEPTANCE_PENDING`.

## Fase 14 — Correcciones

- **Problema:** hallazgos P1/P2 de la auditoría.
- **Método:** corrección mínima con pruebas de regresión.
- **Decisión:** corregir sólo P1/P2; P3 diferidos y documentados.
- **Implementación:** guard de permisos autoritativo por `permissions`;
  formatter de partidas; specs nuevas.
- **Pruebas:** 131/131 frontend; regresión RBAC 13/13 y post-merge 4/4; 0
  celdas `.0` en UI de análisis.
- **Resultado:** P0/P1/P2 abiertos = 0.
- **Limitación:** P3 (`FUN-E2E-001`, `FUN-E2E-004`) permanecen por decisión.

## Fase 15 — Cierre técnico V1

- **Problema:** determinar qué falta para una entrega V1.
- **Método:** snapshot consolidado (matriz recalculada, checklist de calidad,
  inventario SQL LIVE read-only, clasificación de pendientes en 4 categorías).
- **Decisión:** separar entregado / negocio / evidencia externa / POST_V1; no
  declarar `PROJECT_COMPLETE`.
- **Implementación:** `v1-technical-closure.md`, paquete ejecutivo de
  decisiones y anexos candidatos.
- **Pruebas:** gates completos; CI dev verde; inventario 77/77 objetos LIVE.
- **Resultado:** `V1_TECHNICALLY_READY = YES`, `TECHNICAL_BLOCKERS = 0`, tag
  `v1.0.0-rc1` sobre `dev`.
- **Limitación:** decisiones de negocio y 12 contratos externos pendientes.
