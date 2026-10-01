package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de la consulta read-only de análisis de descargas. */
public record AnalisisDescargaDto(
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

    public static AnalisisDescargaDto from(AnalisisDescarga item) {
        return new AnalisisDescargaDto(item.descargaId(), item.pedimentoEntrada(), item.partidaEntrada(),
                item.pedimentoSalida(), item.partidaSalida(), item.material(), item.producto(),
                item.fechaImportacion(), item.fechaSalida(), item.fechaVencimiento(), item.cantidadImportada(),
                item.cantidadExportada(), item.cantidadIncorporada(), item.cantidadMerma(),
                item.cantidadDesperdicio(), item.unidad());
    }
}
