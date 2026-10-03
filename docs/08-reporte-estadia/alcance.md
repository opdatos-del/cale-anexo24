# Alcance

El proyecto cubre el análisis funcional, diseño y desarrollo de una aplicación web
moderna para el control de inventarios de comercio exterior conforme al Anexo 24
de PROCESADORA DE ALIMENTOS CALE, S.A. de C.V.: catálogos, consultas operativas,
reportes consolidados con exportación, seguridad con permisos y bitácora,
importación validada de archivos y un adaptador controlado sobre la base
existente. La base operativa autoritativa es `CALE_IMMEX`; `ANEXO24_DEV`
(esquema `app24`) es infraestructura complementaria de la aplicación (usuarios,
permisos, bitácora y staging). No se crea una base operativa paralela ni se
modifica el modelo legacy sin autorización.

Este documento separa el alcance real por estado a la fecha del cierre técnico
V1 (`docs/05-pruebas/v1-technical-closure.md`, `dev` = `fb77be0`, tag
`v1.0.0-rc1`).

## ENTREGADO

- Autenticación con JWT, perfiles, permisos, bitácora y administración
  (usuarios, perfiles, actividades).
- Consultas read-only sobre `CALE_IMMEX` con SP dedicados: materiales,
  productos, estructuras, entradas, salidas, materiales utilizados, activos
  fijos, catálogos auxiliares, socios comerciales y datos generales.
- Reportes consolidados V1 (11 tipos) con paginación, filtros y exportación
  XLSX donde el contrato lo permite.
- Importación de pedimentos: staging durable, parser, validación, hash, errores,
  preview y confirmación autoritativa hacia `IMPORTACIONES`/`PARTIDAS`
  (tipo operación 1) y `SALIDAS`/`PSALIDAS` (tipo 2).
- Staging validado (sin confirmación) de materiales, productos y facturación.
- Infraestructura de despliegue product-like (Docker/compose/nginx), gates de
  CI (SP-FIRST, permisos runtime, tests, build) y documentación de operación.
- Auditoría E2E de la aplicación completa y cierre técnico V1 con
  `TECHNICAL_BLOCKERS = 0`.

## PENDIENTE_NEGOCIO (requiere decisión de la empresa/asesor)

- `PED-005`/`PED-006`: reglas de inventario en validación de pedimentos
  (`INVENTARIO` no existe en LIVE).
- Saldos: significado, granularidad, fórmula, fuentes y corte (consulta,
  reporte y concentrado).
- Descargos/PEPS: alcance V1 del motor automático y dirigido.
- Confirmación operativa de facturación (pipeline autoritativo y efectos).
- Dashboard V1: suficiencia del resumen actual o KPIs adicionales.
- Decisiones adicionales: confirmación de catálogos, importación de socios,
  consolidados, edición de datos generales, ajuste anual y Anexo 30.
- Ver `docs/01-requerimientos/decisiones-alcance-v1.md` (paquete ejecutivo).

## PENDIENTE_EVIDENCIA_EXTERNA (`WAITING_EXTERNAL_CONTRACT`)

- Layouts, pantallas o contratos de: cambios de régimen, regularizaciones,
  actas de destrucción, transferencias de submaquila, constancias, CTM
  (proceso), scrap/desperdicio, servicios, órdenes de fabricación, procesos y
  carta de materiales (12 capacidades).
- No se reauditan; se retoman sólo con evidencia nueva.

## POST_V1

- Extensiones read-only: resolución de operaciones bloqueadas, faltantes/trazo
  de descargas, vencimientos completo, compulsa/rectificaciones detalladas,
  consultas guardadas, resto de CTM/HDE y secciones restantes del ajuste anual.
- Confirmaciones autoritativas de catálogos y facturación (reutilizan el patrón
  ya probado de pedimentos).

## Fuera de alcance explícito

- Reemplazo total del sistema legacy: la matriz de paridad documenta cobertura
  por capacidad, no una sustitución del 100 %.
- Ejecución de procesos legacy mutables desde consultas o confirmaciones no
  autorizadas.
- Uso productivo real: la verificación se realizó en ambiente DEV/local con
  datos LIVE de sólo lectura y staging sintético.
