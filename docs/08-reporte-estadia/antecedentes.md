# Antecedentes

PROCESADORA DE ALIMENTOS CALE, S.A. de C.V. opera su control de inventarios de
comercio exterior (Anexo 24) con un sistema legacy observable en versión
1.0.1.0, basado en ASP.NET Web Forms. La auditoría funcional inicial identificó
catálogos, operaciones, reportes, gestión de permisos y carga de facturación,
además de oportunidades en seguridad, mensajes de error y experiencia de carga.

El sistema legacy concentra la operación sobre la base `CALE_IMMEX` (referida
en la documentación temprana como “Módulo C”), con procesos almacenados propios
para descargos, saldos y operaciones especiales. La modernización se planteó
con Angular y Spring Boot, integrando la información existente de forma
controlada: la nueva aplicación consume procedimientos almacenados autorizados
y no crea una base operativa paralela.

El proyecto evolucionó por fases verificables (auditoría legacy, descubrimiento
de `CALE_IMMEX`, diseño de arquitectura, desarrollo de módulos, pruebas y
cierre técnico V1). La cronología con problema, método, resultado y limitación
de cada fase está consolidada en `docs/08-reporte-estadia/evidencias-proyecto.md`
y el estado final en `docs/05-pruebas/v1-technical-closure.md`.
