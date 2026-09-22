package com.jovycandy.anexo24.auditlog.api.dto;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;

import java.time.Instant;

/** Representación HTTP read-only de un evento de Bitácora. */
public record BitacoraRegistroDto(
        Long id,
        Instant fecha,
        Long usuarioId,
        String usuario,
        BitacoraModulo modulo,
        BitacoraAccion accion,
        String detalle,
        BitacoraResultado resultado,
        String correlationId) {

    /**
     * Convierte un registro de dominio a su representación HTTP.
     *
     * @param registro registro persistido de Bitácora
     * @return DTO de respuesta
     */
    public static BitacoraRegistroDto from(BitacoraRegistro registro) {
        return new BitacoraRegistroDto(
                registro.id(),
                registro.fecha(),
                registro.usuarioId(),
                registro.usuario(),
                registro.modulo(),
                registro.accion(),
                registro.detalle(),
                registro.resultado(),
                registro.correlationId());
    }
}
