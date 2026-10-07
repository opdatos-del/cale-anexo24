package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Descarga;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representación API de una descarga persistida de Anexo 30. */
public record Anexo30DescargaDto(long descargaKey, Long entradaKey, Long fraccionKey, String pedimento, String pedimentoOriginal, LocalDateTime fechaEntrada, String clavePedimentoEntrada, String fraccionEntrada, BigDecimal valorComercialEntrada, BigDecimal saldoPersistidoA31, String esaf, String partida, String fraccionDescarga, BigDecimal valorDescargado, String tipoA31, String clavePedimentoA31, String ejercicio, String periodo, String fraccionA31, BigDecimal valorA31, String af, String archivo) {
    public static Anexo30DescargaDto from(Anexo30Descarga item) {
        return new Anexo30DescargaDto(item.descargaKey(), item.entradaKey(), item.fraccionKey(), item.pedimento(), item.pedimentoOriginal(), item.fechaEntrada(), item.clavePedimentoEntrada(), item.fraccionEntrada(), item.valorComercialEntrada(), item.saldoPersistidoA31(), item.esaf(), item.partida(), item.fraccionDescarga(), item.valorDescargado(), item.tipoA31(), item.clavePedimentoA31(), item.ejercicio(), item.periodo(), item.fraccionA31(), item.valorA31(), item.af(), item.archivo());
    }
}
