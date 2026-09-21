package com.jovycandy.anexo24.operations.entries.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea plana de una entrada/importación del Módulo C. */
public record EntradaLinea(
        BigDecimal importacionId,
        BigDecimal partidaId,
        String pedimento,
        String clavePedimento,
        LocalDateTime fechaEntrada,
        String fraccion,
        String unidadComercial,
        BigDecimal cantidadComercial,
        String numeroParte,
        LocalDateTime fechaPago) {
}
