package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representa el snapshot histórico de una operación de descarga bloqueada. */
public record OperacionBloqueada(
        Long bloqueoId,
        String pedimentoExportacion,
        String claveExportacion,
        LocalDateTime fechaExportacion,
        String producto,
        BigDecimal cantidadProducto,
        String fraccionProducto,
        Long psalidaKey,
        String pedimentoImportacion,
        String claveImportacion,
        LocalDateTime fechaImportacion,
        String material,
        BigDecimal cantidadMaterial,
        String fraccionMaterial,
        Long partidaKey,
        BigDecimal incorporado,
        BigDecimal desperdicio,
        BigDecimal merma,
        LocalDateTime fechaBloqueo,
        Integer folio) {
}
