package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila read-only de dbo.v_compulsa. Sin llave fisica y sin recalcular estatus legacy.
 */
public record CompulsaDetalle(
        String pedimentoGlosa, Integer secGlosa, String pedimentoA24, Double secA24,
        String clavePedimentoGlosa, String clavePedimentoA24, String statusClavePedimento,
        LocalDateTime fechaGlosa, LocalDateTime fechaA24, String statusFechas,
        String fraccionGlosa, String fraccionA24, String statusFraccion,
        String paisOdGlosa, String paisOdA24, String statusPaisOd,
        String paisCvGlosa, String paisCvA24, String statusPaisCv,
        Double valorAduanaGlosa, BigDecimal valorAduanaA24, String statusValorAduana,
        Double valorComercialGlosa, Double valorComercialA24, String statusValorComercial,
        Double cantidadUmcGlosa, BigDecimal cantidadUmcA24, String statusCantidadComercial,
        Double cantidadUmtGlosa, Double cantidadUmtA24, String statusCantidadTarifa,
        Double tipoOperacionGlosa, Double tipoPedimentoGlosa) {
}
