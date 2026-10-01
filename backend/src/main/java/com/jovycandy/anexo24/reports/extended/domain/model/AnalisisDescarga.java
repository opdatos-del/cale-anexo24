package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representa una relación histórica de descarga entre entrada y salida. */
public record AnalisisDescarga(
        Long descargaId,
        String pedimentoEntrada,
        String partidaEntrada,
        String pedimentoSalida,
        String partidaSalida,
        String material,
        String producto,
        LocalDateTime fechaImportacion,
        LocalDateTime fechaSalida,
        LocalDateTime fechaVencimiento,
        BigDecimal cantidadImportada,
        BigDecimal cantidadExportada,
        BigDecimal cantidadIncorporada,
        BigDecimal cantidadMerma,
        BigDecimal cantidadDesperdicio,
        String unidad) {
}
