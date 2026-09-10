# Diseño de base de datos propuesto

## Estrategia: integración más esquema de aplicación

Se propone conservar Módulo C como fuente de datos operativos de Anexo 24 y crear un esquema separado para lo que es responsabilidad de la nueva aplicación: seguridad, bitácora, configuración y control de cargas. Esta separación evita una migración riesgosa y permite que el equipo nuevo avance antes de conocer el detalle físico de Módulo C.

## Tablas propuestas

| Tabla | Llave y campos esenciales | Reglas e índices |
|---|---|---|
| `usuario` | `usuario_id`, `clave`, `nombre`, `correo`, `password_hash`, `estado`, `vigencia_hasta`, fechas de auditoría. | Único en `clave` y correo; índice en estado/vigencia. |
| `perfil` | `perfil_id`, `nombre`, `descripcion`, `estado`. | Único en nombre activo. |
| `actividad` | `actividad_id`, `clave`, `nombre`, `recurso`, `accion`, `estado`. | Único en clave; permisos legibles y estables. |
| `perfil_actividad` | `perfil_id`, `actividad_id`, fechas. | PK compuesta; evita permisos duplicados. |
| `bitacora_evento` | `evento_id`, `usuario_id`, fecha UTC, módulo, acción, resultado, detalle seguro, `correlation_id`. | Índices por fecha, usuario, módulo y correlación; registros solo de inserción. |
| `plantilla_carga` | `plantilla_id`, nombre, versión, definición JSON, extensiones, activa. | Único nombre + versión; una versión activa por tipo. |
| `carga_facturacion` | `carga_id`, `usuario_id`, `plantilla_id`, archivo, hash, estado, fecha, totales, `correlation_id`. | Único hash cuando la política de duplicados aplique; índice por estado/fecha. |
| `error_carga` | `error_id`, `carga_id`, hoja, fila, columna, código, valor enmascarado, mensaje. | Índice por carga y posición; no almacenar valores sensibles completos. |

## Relaciones lógicas

`perfil` N:M `actividad` mediante `perfil_actividad`; `usuario` N:1 `perfil`; `usuario` 1:N `bitacora_evento`; `usuario` 1:N `carga_facturacion`; `plantilla_carga` 1:N `carga_facturacion`; `carga_facturacion` 1:N `error_carga`.

## Contrato para datos de comercio exterior

Los DTO de API deben contener claves de negocio estables y no exponer directamente tablas. Ejemplo: `MaterialDto { numeroParte, descripcion, fraccionArancelaria, unidadTigie, umc }`. El adaptador traduce ese contrato a procedimientos o vistas confirmadas de Módulo C. La decisión de crear replicas, vistas nuevas o procedimientos nuevos se toma después de medir volumen, latencia y autorización formal.

## Integridad, concurrencia y respaldo

Las escrituras propias se ejecutan dentro de transacciones. Una carga pasa por `VALIDANDO`, `RECHAZADA`, `LISTA`, `GUARDADA` o `FALLIDA`; solo `LISTA` puede confirmarse. El hash del archivo y la combinación de contexto funcional sirven para detectar reintentos. Los respaldos, recuperación y retención quedan bajo la política de infraestructura de la empresa; la aplicación debe permitir rastrear el lote y su resultado sin reconstruir información desde logs.
