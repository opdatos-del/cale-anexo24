# Auditoría funcional del sistema actual Anexo 24

**Fecha:** 03 de septiembre de 2026 · **Empresa:** PROCESADORA DE ALIMENTOS CALE, S.A. de C.V. · **Versión visible:** 1.0.1.0

## Alcance y evidencia

Se auditó la interfaz autenticada con perfil ADMINISTRADOR mediante datos sintéticos `AUDIT_TEST_20260903_`, eliminados al finalizar. No se modificaron datos originales: la verificación final conservó 2 usuarios, 2 perfiles y 52 actividades. La arquitectura observable es ASP.NET Web Forms con controles DevExpress y postbacks; esta auditoría no inspeccionó código, infraestructura ni la base Módulo C.

## Inventario observado

| Área | Evidencia funcional |
|---|---|
| Datos generales | Razón social, RFC XXXXX, IMMEX IM 3547-2006 y domicilio; edición no ejecutada por ser dato único. |
| Materiales | 1,109 registros; fracción, descripción, unidad TIGIE, UMC y número de parte. |
| Productos | 2,630 registros; fracción, descripción, unidad, UMC y número de parte. |
| Estructuras | Relaciona productos y materiales; consulta amplia con ~4,981 registros; filtro por producto y fechas. |
| Entradas, salidas, consumo y activo fijo | Consulta por filtros; exigen rango de fechas. No se observó captura. |
| Gestión | Usuarios, perfiles y 52 actividades; CRUD sintético validado y revertido. |
| Reportes | Entradas, salidas, saldos, materiales utilizados y bitácora; todos requieren fechas y fallaron con error genérico al generar. |
| Facturación | Selección múltiple XLS/XLSX, CARGAR, GUARDAR, LIMPIAR y DESCARGAR; la plantilla sintética fue rechazada por columnas/información. |

## Hallazgos priorizados

1. **Crítico:** la contraseña se renderiza como texto y queda precargada en el DOM al editar; la nueva solución debe usar `type=password`, hash robusto y nunca devolver secretos.
2. **Crítico:** el perfil Usuario SAT tiene permisos administrativos de usuarios, perfiles y actividades; se debe aplicar mínimo privilegio y revisión de privilegios.
3. **Alto:** los cinco reportes reproducen un error genérico con rango válido; la API debe emitir identificador de incidente y trazabilidad sin exponer detalles internos.
4. **Alto:** la carga de facturación no comunica plantilla, formatos ni causa por fila; se propone validación previa y reporte de errores detallado.
5. **Medio:** cancelar usuario produce `TypeError` en `LimpiaControles` (`jsGeneral.js:68`); la interfaz nueva debe manejar cancelación como operación idempotente.

## Decisiones derivadas

La nueva aplicación conserva los dominios observados y separa interfaz, API, seguridad, negocio e integración. Los nombres reales de tablas, vistas y procedimientos de Módulo C no se afirman sin acceso autorizado; se documenta un contrato de integración y un modelo lógico propuesto, sin alterar la base existente.
