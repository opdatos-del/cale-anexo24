package com.jovycandy.anexo24.auditlog.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Evento interno append-only para {@code app24.BitacoraEvento}.
 *
 * <p>El detalle debe ser previamente seguro y minimizado por el caller. No debe
 * contener secretos, tokens, credenciales, payloads crudos ni stack traces.</p>
 *
 * @param usuarioId     actor confiable o {@code null} para eventos técnicos/anónimos
 * @param modulo        módulo controlado del evento
 * @param accion        acción controlada del evento
 * @param resultado     resultado controlado del evento
 * @param detalle       resumen seguro opcional, máximo 500 caracteres
 * @param correlationId correlación normalizada de la solicitud, si aplica
 */
public record BitacoraEvento(
        Long usuarioId,
        BitacoraModulo modulo,
        BitacoraAccion accion,
        BitacoraResultado resultado,
        String detalle,
        String correlationId) {

    private static final int DETALLE_MAXIMO = 500;
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,40}");

    /** Valida invariantes del evento antes de cualquier persistencia. */
    public BitacoraEvento {
        if (usuarioId != null && usuarioId <= 0) {
            throw new IllegalArgumentException("usuarioId debe ser positivo");
        }
        Objects.requireNonNull(modulo, "modulo es obligatorio");
        Objects.requireNonNull(accion, "accion es obligatoria");
        Objects.requireNonNull(resultado, "resultado es obligatorio");
        if (detalle != null && detalle.length() > DETALLE_MAXIMO) {
            throw new IllegalArgumentException("detalle excede 500 caracteres");
        }
        if (correlationId != null && !CORRELATION_ID_PATTERN.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("correlationId inválido");
        }
    }
}
