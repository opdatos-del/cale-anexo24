package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea de salida marcada como dirigida por la fuente legacy read-only. */
public record OperacionDirigida(
        Long salidaKey,
        Long psalidaKey,
        String documento,
        LocalDateTime fechaSalida,
        String clavePedimento,
        Integer secuencia,
        String producto,
        BigDecimal cantidad,
        String factura,
        String descargo,
        String dirigido,
        BigDecimal valorDescargaDolares,
        BigDecimal valorDescargaPesos,
        String tieneEstructura,
        Integer numeroMateriales) {
}
