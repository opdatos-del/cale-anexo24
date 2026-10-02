package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea dirigida de salida F4/A3 con tipo de descarga CTM APAA o desperdicio (vistas legacy read-only). */
public record LineaF4(
        String tipoDescarga,
        String f4,
        LocalDateTime fecha,
        String importacion,
        String clave,
        BigDecimal incorporado,
        BigDecimal saldo) {
}
