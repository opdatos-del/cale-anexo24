package com.jovycandy.anexo24.reports.extended.domain.model;

/**
 * Fila read-only de dbo.v_compulsa. Sin llave fisica y sin recalcular estatus legacy.
 */
public record CompulsaDetalle(
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
}
