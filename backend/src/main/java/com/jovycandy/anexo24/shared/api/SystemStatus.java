package com.jovycandy.anexo24.shared.api;

/**
 * Estado técnico del sistema.
 *
 * @param application nombre de la aplicación
 * @param status      estado general de la aplicación ("UP" / "DOWN")
 * @param database    estado de la conexión a la base de datos ("UP" / "DOWN")
 */
public record SystemStatus(String application, String status, String database) {
}