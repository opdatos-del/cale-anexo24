package com.jovycandy.anexo24.operations.fixedassets.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Partida de importación marcada como activo fijo en el Módulo C. */
public record ActivoFijo(
        BigDecimal partidaEntradaId,
        BigDecimal importacionId,
        String pedimento,
        String clavePedimento,
        LocalDateTime fechaImportacion,
        String numeroParte,
        String descripcion,
        String fraccion,
        BigDecimal cantidad,
        String unidad,
        String numeroSerie,
        String marca,
        String modelo) {
}
