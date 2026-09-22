package com.jovycandy.anexo24.auditlog.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Evento de Bitácora ya persistido en {@code app24.BitacoraEvento}.
 *
 * <p>La etiqueta {@code usuario} se resuelve contra el estado actual de
 * {@code UsuarioApp}; no representa un snapshot histórico y puede ser nula.</p>
 *
 * @param id            identidad técnica persistida
 * @param fecha         instante UTC persistido por la base de datos
 * @param usuarioId     actor persistido, si aplica
 * @param usuario       clave actual del usuario, si puede resolverse
 * @param modulo        módulo controlado del evento
 * @param accion        acción controlada del evento
 * @param detalle       resumen seguro opcional
 * @param resultado     resultado controlado del evento
 * @param correlationId correlación normalizada, si aplica
 */
public record BitacoraRegistro(
        Long id,
        Instant fecha,
        Long usuarioId,
        String usuario,
        BitacoraModulo modulo,
        BitacoraAccion accion,
        String detalle,
        BitacoraResultado resultado,
        String correlationId) {

    /** Valida los campos obligatorios de un registro ya persistido. */
    public BitacoraRegistro {
        Objects.requireNonNull(id, "id es obligatorio");
        Objects.requireNonNull(fecha, "fecha es obligatoria");
        Objects.requireNonNull(modulo, "modulo es obligatorio");
        Objects.requireNonNull(accion, "accion es obligatoria");
        Objects.requireNonNull(resultado, "resultado es obligatorio");
    }
}
