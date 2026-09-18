package com.jovycandy.anexo24.shared.api;

/**
 * Estado técnico de la aplicación y sus dependencias de datos.
 *
 * @param application       nombre de la aplicación
 * @param status             estado general de la aplicación ("UP" / "DOWN")
 * @param moduleCDatabase   estado de la conexión al Módulo C
 * @param applicationDatabase estado de la conexión al esquema app24
 */
public record SystemStatus(
        String application,
        String status,
        String moduleCDatabase,
        String applicationDatabase) {
}
