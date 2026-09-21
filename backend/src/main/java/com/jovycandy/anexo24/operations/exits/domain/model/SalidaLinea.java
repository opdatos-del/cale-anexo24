package com.jovycandy.anexo24.operations.exits.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea plana de una salida/exportación del Módulo C. */
public record SalidaLinea(
        BigDecimal salidaId,
        BigDecimal partidaId,
        String pedimento,
        String clavePedimento,
        String fraccion,
        String unidadComercial,
        BigDecimal cantidad,
        String numeroParte,
        LocalDateTime fechaPago) {
}
