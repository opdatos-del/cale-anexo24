# API REST propuesta

**Base:** `/api/v1` · **Formato:** JSON UTF-8 · **Seguridad:** Bearer token y autorización por permiso · **Errores:** `{ code, message, correlationId, details? }`.

| Método | Ruta | Función | Permiso |
|---|---|---|---|
| POST | `/auth/login` | Inicia sesión. | Público con protección antiabuso |
| GET | `/catalogos/materiales` | Lista materiales paginados. | `MATERIALES_CONSULTAR` |
| GET | `/catalogos/productos` | Lista productos paginados. | `PRODUCTOS_CONSULTAR` |
| GET | `/operaciones/{tipo}` | Consulta entradas, salidas, consumo o activo fijo por rango. | Permiso del módulo |
| GET | `/reportes/{tipo}` | Genera datos de reporte por rango. | `REPORTES_GENERAR` |
| GET | `/reportes/{tipo}/exportacion` | Exporta reporte con resultados. | `REPORTES_EXPORTAR` |
| POST | `/facturacion/cargas` | Valida archivo y crea lote no persistido. | `FACTURACION_CARGAR` |
| POST | `/facturacion/cargas/{id}/confirmar` | Guarda lote validado. | `FACTURACION_GUARDAR` |
| GET | `/bitacora` | Consulta read-only paginada con rango UTC obligatorio (`desde`/`hasta`), filtros `usuarioId`, `modulo`, `resultado`, `correlationId` y orden `fecha DESC, id DESC`. | `BITACORA_CONSULTAR` |

Los endpoints de administración siguen `/usuarios`, `/perfiles` y `/actividades`; aplican validación de esquema, paginación y bitácora. Los nombres de procedimientos detrás de la API se mantienen internos.

Consumidor frontend del endpoint `GET /bitacora`: pantalla read-only en `frontend/src/app/features/administration/audit-log` (ruta `/bitacora`, permiso `BITACORA_CONSULTAR`). Sin consumidores frontend de escritura.
