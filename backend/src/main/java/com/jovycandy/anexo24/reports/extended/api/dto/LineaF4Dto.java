package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representación API de una línea dirigida F4 (CTM APAA o desperdicio). */
public record LineaF4Dto(
        String tipoDescarga,
        String f4,
        LocalDateTime fecha,
        String importacion,
        String clave,
        BigDecimal incorporado,
        BigDecimal saldo) {
    public static LineaF4Dto from(LineaF4 item) {
        return new LineaF4Dto(item.tipoDescarga(), item.f4(), item.fecha(), item.importacion(),
                item.clave(), item.incorporado(), item.saldo());
    }
}
