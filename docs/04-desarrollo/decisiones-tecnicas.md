# Decisiones técnicas

## ADR-001: Backend modular con adaptador Módulo C

**Estado:** Aprobada para diseño. **Decisión:** usar Spring Boot por módulos y un puerto/adaptador para procedimientos, vistas y consultas SQL Server. **Consecuencia:** la interfaz y dominio no dependen de nombres físicos; se requiere inventario técnico autorizado para completar el adaptador.

## ADR-002: Seguridad basada en permisos

**Decisión:** Spring Security valida autenticación y permiso en cada endpoint; las contraseñas se almacenan con Argon2id/bcrypt y los formularios nunca precargan secretos. **Consecuencia:** se corrige la exposición visible de contraseña y se obliga a revisar el perfil Usuario SAT.

## ADR-003: Carga con validación previa

**Decisión:** procesar XLS/XLSX en etapas, conservar resumen y errores por ubicación antes de persistir. **Consecuencia:** elimina mensajes genéricos y reduce cargas incompletas; la plantilla se versiona como configuración.

## ADR-004: Bitácora inmutable y correlación

**Decisión:** registrar acciones críticas, resultado y `correlationId` sin secretos. **Consecuencia:** mejora investigación de fallas en reportes y cargas.
