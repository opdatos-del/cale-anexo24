package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Vencimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representación API de un vencimiento de desperdicio. */
public record VencimientoDto(
        String pedimento,
        LocalDateTime fechaBase,
        String clavePedimento,
        String clave,
        String unidad,
        String unidadT,
        BigDecimal desperdicio,
        BigDecimal desperdicioT,
        BigDecimal valorDesperdicio,
        BigDecimal aplicado,
        String factura,
        LocalDateTime vencimiento) {
    public static VencimientoDto from(Vencimiento item) {
        return new VencimientoDto(item.pedimento(), item.fechaBase(), item.clavePedimento(), item.clave(),
                item.unidad(), item.unidadT(), item.desperdicio(), item.desperdicioT(),
                item.valorDesperdicio(), item.aplicado(), item.factura(), item.vencimiento());
    }
}
