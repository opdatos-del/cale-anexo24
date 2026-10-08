package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.CompulsaDetalle;

/** Representacion API del detalle read-only de compulsa. */
public record CompulsaDetalleDto(
        String pedimentoGlosa, String secGlosa, String pedimentoA24, String secA24,
        String clavePedimentoGlosa, String clavePedimentoA24, String statusClavePedimento,
        String fechaGlosa, String fechaA24, String statusFechas,
        String fraccionGlosa, String fraccionA24, String statusFraccion,
        String paisOdGlosa, String paisOdA24, String statusPaisOd,
        String paisCvGlosa, String paisCvA24, String statusPaisCv,
        String valorAduanaGlosa, String valorAduanaA24, String statusValorAduana,
        String valorComercialGlosa, String valorComercialA24, String statusValorComercial,
        String cantidadUmcGlosa, String cantidadUmcA24, String statusCantidadComercial,
        String cantidadUmtGlosa, String cantidadUmtA24, String statusCantidadTarifa,
        String tipoOperacionGlosa, String tipoPedimentoGlosa) {
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
