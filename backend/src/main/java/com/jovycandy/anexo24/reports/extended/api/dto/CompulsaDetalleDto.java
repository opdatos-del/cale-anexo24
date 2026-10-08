package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.CompulsaDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representacion API del detalle read-only de compulsa. */
public record CompulsaDetalleDto(
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
    public static CompulsaDetalleDto from(CompulsaDetalle item) {
        return new CompulsaDetalleDto(item.pedimentoGlosa(), item.secGlosa(), item.pedimentoA24(), item.secA24(),
                item.clavePedimentoGlosa(), item.clavePedimentoA24(), item.statusClavePedimento(),
                item.fechaGlosa(), item.fechaA24(), item.statusFechas(), item.fraccionGlosa(), item.fraccionA24(),
                item.statusFraccion(), item.paisOdGlosa(), item.paisOdA24(), item.statusPaisOd(), item.paisCvGlosa(),
                item.paisCvA24(), item.statusPaisCv(), item.valorAduanaGlosa(), item.valorAduanaA24(),
                item.statusValorAduana(), item.valorComercialGlosa(), item.valorComercialA24(),
                item.statusValorComercial(), item.cantidadUmcGlosa(), item.cantidadUmcA24(),
                item.statusCantidadComercial(), item.cantidadUmtGlosa(), item.cantidadUmtA24(),
                item.statusCantidadTarifa(), item.tipoOperacionGlosa(), item.tipoPedimentoGlosa());
    }
}
