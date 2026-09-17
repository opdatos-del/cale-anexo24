package com.jovycandy.anexo24.shared.api;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo de error estándar de la API.
 *
 * <p>Sigue el contrato definido en {@code docs/04-desarrollo/api.md}:
 * {@code { code, message, correlationId, details? }}.</p>
 *
 * @param code          código de error estable y accionable
 * @param message       mensaje legible sin detalles internos
 * @param correlationId identificador de correlación para rastreo
 * @param timestamp     momento en que ocurrió el error
 * @param details       detalles adicionales opcionales
 */
public record ApiError(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        Map<String, Object> details) {

    /**
     * Crea un error sin detalles adicionales.
     *
     * @param code          código de error
     * @param message       mensaje legible
     * @param correlationId identificador de correlación
     */
    public ApiError(String code, String message, String correlationId) {
        this(code, message, correlationId, Instant.now(), null);
    }
}