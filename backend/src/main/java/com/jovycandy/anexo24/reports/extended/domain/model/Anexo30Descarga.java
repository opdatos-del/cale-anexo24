package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Descarga persistida del último snapshot generado externamente para Anexo 30. */
public record Anexo30Descarga(
        long descargaKey, Long entradaKey, Long fraccionKey, String pedimento, String pedimentoOriginal,
        LocalDateTime fechaEntrada, String clavePedimentoEntrada, String fraccionEntrada,
        BigDecimal valorComercialEntrada, BigDecimal saldoPersistidoA31, String esaf, String partida,
        String fraccionDescarga, BigDecimal valorDescargado, String tipoA31, String clavePedimentoA31,
        String ejercicio, String periodo, String fraccionA31, BigDecimal valorA31, String af, String archivo) {
}
