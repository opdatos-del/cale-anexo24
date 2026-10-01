package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vencimiento read-only de un desperdicio agrupado por la vista legacy. */
public record Vencimiento(
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
}
