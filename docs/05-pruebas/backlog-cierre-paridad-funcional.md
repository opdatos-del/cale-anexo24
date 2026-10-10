# Backlog de cierre de paridad funcional

## Regla de ejecución

Backlog de auditoría, no autorización de implementación. Cada entrega aprobada sigue `DB/SP → backend → frontend → tests → CI → auditoría Controller`. Prohibido crear tablas, ejecutar DML/DDL LIVE o ejecutar SP mutable sin contrato y autorización separados. Una capacidad bloqueada no queda fuera de reemplazo: sólo una decisión empresarial explícita puede aceptarlo.

## A. IMPLEMENTABLE_NOW

| ID | Pantalla/ruta | Acción | Dependencias y fuente/SP | Complejidad | Prioridad | Evidencia requerida | Criterio de aceptación | Desbloqueo |
|---|---|---|---|---|---|---|---|---|
| `QA-001` | `/reportes` | Corregir `reports.spec.ts` obsoleta: verificar Saldos habilitado, generar y exportar contra contrato existente | `ReportListPage`; `ReportApiService`; `GET /api/v1/reportes/saldos`; `APP24_Q_SALDOS_LISTAR` | S | P0 | Fixture sintético de saldos; permiso `REPORTES_GENERAR/EXPORTAR` | Browser comprueba botón habilitado, request, paginación, XLSX no vacío y error legible; no cambia producto | Contrato técnico presente; falta ambiente E2E |
| `QA-002` | 7 rutas sin smoke | Añadir navegación autenticada y estado loading/error/empty | `/catalogos*`, `/operaciones/{pedimentos,actas,constancias}`; APIs actuales | M | P0 | Usuario fixture con permisos y datos sintéticos restaurables | 22/22 rutas smoke; guard/forbidden comprobados; Playwright ejecutable fuera de LIVE | Fixtures y credenciales CI aprobados |
| `QA-003` | `/facturacion` | E2E carga, preview/error, historial propio, deep-link, paginación e IDOR multicuenta | `APP24_Q/C_FACTURACION_*`; endpoint owner-scoped | M | P1 | Dos usuarios y cargas sintéticas aisladas | A ve 200 y sólo sus cargas; B obtiene 404/no lista A; deep-link recupera detalle tras refresh | Fixtures multicuenta |
| `QA-004` | `/catalogos/importaciones`, pedimentos, actas, constancias | E2E upload, validación, CSV error, preview y confirmación sintética | staging `app24`; `APP24_C_*_CARGA_CONFIRMAR` y confirmadores existentes | L | P1 | Archivos sintéticos válidos/inválidos; DB restaurable | Cada flujo comprueba rollback/error y confirmación sólo sobre fixture | Ambiente SQL sintético/Testcontainers o sandbox |
| `QA-005` | `/reportes`, `/bitacora`, `/usuarios` | Verificar filtros, páginas, exportación y RBAC UI/API | query SP ya inventariados; exportadores XLSX/CSV | M | P1 | Fixtures con más de una página y usuarios con/sin permiso | Longitud, página, filtros y archivo exportado coinciden; 401/403 sin fuga | Entorno E2E y permisos fixture |
| `QA-006` | workflow CI | Ejecutar Playwright en job aislado | `.github/workflows/ci.yml`; secreto/fixture aprobados | M | P0 | Imagen/servicios, credenciales no productivas, limpieza garantizada | `pnpm e2e` corre en CI; fallo bloquea PR; nunca toca LIVE | Decisión infraestructura/secretos CI |

## B. BLOCKED_BUSINESS

| ID | Pantalla/ruta | Acción faltante | Dependencias y fuente/SP candidata | Complejidad | Prioridad | Evidencia/decisión requerida | Criterio de aceptación para desbloquear | Estado |
|---|---|---|---|---|---|---|---|---|
| `BUS-021` | `/reportes` Saldos | Aceptar semántica fiscal, corte y activación de reporte/archivo | `PR_INFORME_SALDOS`, `APP24_Q_SALDOS_LISTAR`; no ejecutar `SALDOS*` mutable | L | P0 | Negocio aprueba fórmula, grano, periodo/documento, totales y XLSX de referencia | Contrato firmado y extractos aprobados; prueba compara contenido/orden/totales | Bloqueado negocio; LEGACY-021/038 |
| `BUS-029` | Sin ruta Angular | Generar/reprocesar descargo automático | `DESCARGATSALIDA*`, `DESCARGASALIDAPEPS`, `SALDOS*` | XL | P0 | Algoritmo, rol, idempotencia, concurrencia, transacción, rollback y auditoría | Especificación y escenario sintético aprobado antes de SP-first | Bloqueado negocio; LEGACY-029 |
| `BUS-058` | `/facturacion` | Confirmar factura y efectos operativos | entrypoint legacy no demostrado; candidatos `CARGA_FACTURAS*` | XL | P0 | Mapping, destinos, duplicados, clientes/productos, errores, atomicidad, retry y concurrencia | Contrato forense completo; no DML LIVE durante hallazgo | Bloqueado negocio; LEGACY-058 |
| `BUS-026-030` | Sin ruta Angular | Submaquila, CTM y descargo dirigido | `CARGA_SUBMAQUILA`, `CTMDESCARGA`, `DESCDIRIGIDA` | XL | P1 | Regla fiscal, roles, efectos y rollback autorizados | Contrato individual aprobado por capacidad | Bloqueado negocio; LEGACY-026/028/030/063 |
| `BUS-042` | Sin ruta Angular | Consolidado de saldos | `INFORME_CONCENTRADOSALDOS` escribe `CONCENTRADOSALDOS` | L | P1 | Decisión sobre snapshot/fórmula/corte | Query/read-model autorizado sin mutar legacy | Bloqueado negocio; LEGACY-042 |

## C. BLOCKED_EVIDENCE

| ID | Pantalla/ruta | Acción faltante | Dependencias y fuente/SP | Complejidad | Prioridad | Evidencia requerida | Criterio de aceptación para desbloquear | Estado |
|---|---|---|---|---|---|---|---|---|
| `EVI-ROUTES-083` | Legacy Web Forms | Recuperar catálogo de 83 ventanas y acción por ventana | Navegación legacy autenticada read-only; capturas y metadata | L | P0 | URL ASPX, rol, campos, botones, filtros, paginación, exportación, efecto; incluir dos rutas nominales conocidas | 83 filas nominales sin rutas inventadas; relación con `LEGACY-*` | 2 nombres, 81 rutas pendientes |
| `EVI-017` | `/operaciones/pedimentos` | PED-005/PED-006 | `VALIDAPEDIMENTO`, `VALIDA_I_DETALLENP`, fuente `INVENTARIO` inexistente LIVE | L | P0 | Regla original, fuente vigente, casos válidos/inválidos | Contrato permite prueba sintética y validación determinista | Bloqueado evidencia |
| `EVI-001` | `/catalogos/datos-generales` | Edición de datos generales | `DatosGenerales`; writer aún no confirmado | M | P1 | Identidad, campos editables, requeridos, validación, permiso, writer | Contrato evita INSERT y overwrite de campos ajenos | Bloqueado evidencia |
| `EVI-023-024` | Sin ruta Angular | Cambios de régimen y regularizaciones | writers legacy candidatos; UI/ruta no capturada | XL | P1 | Formulario, mapeo, reglas, efectos, rol y rollback | SP-first clasificable sin ejecutar mutable LIVE | Bloqueado evidencia |
| `EVI-064-072` | Sin ruta Angular | Ajuste anual y Anexo 30 generación/TXT | objetos A31 y generadores mixtos | XL | P1 | Layout, filtros, reglas regulatorias, outputs, permisos | Fuente a pantalla y formato reproducibles | Bloqueado evidencia |
| `EVI-012-048-059-062` | Sin ruta Angular | Submaquila catálogo, Scrap, Servicios, Procesos | objetos legacy sin contrato UI canónico | L | P2 | Ruta, propósito, layout, validación, fuente y permiso | Capacidad clasificada por contrato, no por nombre de tabla | Bloqueado evidencia |

## Primer bloque recomendado para aprobación Controller

**`QA-001 + QA-002`**. Alcance: corregir test de Saldos, completar smoke de las siete rutas omitidas y ejecutar esos flujos con fixtures sintéticos; no modifica dominio, SP, SQL ni datos LIVE.

Criterios verificables: (1) `reports.spec.ts` deja de contradecir `ReportListPage`; (2) 22 rutas tienen smoke autenticado y las siete nuevas muestran main/loading/error o empty controlado; (3) Saldos genera página y descarga XLSX contra fixture; (4) Playwright se ejecuta en entorno aislado, con resultado adjuntable; (5) CI sólo se amplía cuando secretos, servicios y limpieza sean aprobados. No habilita sustitución legacy ni resuelve bloqueos fiscales.

## Puerta de reemplazo

Sólo evaluar `READY_FOR_LEGACY_REPLACEMENT` cuando catálogo de 83 ventanas y acciones esté auditado, P0 implementados o excluidos por negocio explícitamente, E2E crítico corra en CI aislada, y Controller/negocio acepten evidencia de equivalencia pantalla/acción.
