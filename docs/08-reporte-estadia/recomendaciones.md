# Recomendaciones

Separadas por horizonte. No convierten mejoras futuras en requisitos actuales.

## Corto plazo (cierre de V1)

1. Realizar la sesión de negocio con el paquete ejecutivo de
   `docs/01-requerimientos/decisiones-alcance-v1.md` y registrar una
   clasificación por tema (`MUST_HAVE_V1`, `POST_V1`, `NOT_REQUIRED`).
2. Ejecutar la validación operativa con usuarios reales
   (`OPERATOR_ACCEPTANCE_PENDING`): login real, recorrido guiado y acta de
   aceptación.
3. Confirmar contratos externos prioritarios (layouts de actas de destrucción
   y scrap) para desbloquear las interfaces `WAITING_EXTERNAL_CONTRACT` con
   mayor valor.
4. Recolectar los anexos pendientes: capturas UI (checklist en
   `anexos-candidatos.md`) y fixtures sintéticos estables si se requieren.

## Mediano plazo (ampliaciones con contrato)

5. Cerrar el contrato de confirmación de materiales/productos y de
   facturación reutilizando el patrón ya probado de pedimentos (staging →
   validación → comando autoritativo → UI), sólo con caso de aceptación.
6. Ampliar reportes fiscales read-only donde exista evidencia: faltantes/trazo
   de descargas, vencimientos completo, compulsa/rectificaciones detalladas.
7. Definir con negocio el alcance de Saldos y del motor de descargos/PEPS
   (o su exclusión formal de V1).
8. Planear la operación productiva controlada: ambientes, credenciales de
   despliegue, respaldos y ventana de salida.

## Post-V1 / mejora continua

9. **Hardening de identidad SQL (diferido):** migrar del usuario runtime actual
   a un usuario de mínimo privilegio (`anexo24_app`) cuando el proyecto lo
   autorice; los scripts existen como propuesta y no se ejecutan. No es
   requisito actual ni bloquea V1.
10. Observabilidad ampliada: métricas de uso, tablero operativo y alertas
    sobre bitácora/errores de carga (sujeto a decisión).
11. Mantenimiento: actualización de dependencias, presupuesto de bundle del
    frontend (warning actual) y limpieza de documentación histórica.
12. Consultas guardadas y consolidados adicionales, si negocio confirma su
    valor operativo.
