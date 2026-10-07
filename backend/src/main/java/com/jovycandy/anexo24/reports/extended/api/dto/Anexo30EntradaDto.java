package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Entrada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representacion API de una entrada persistida del snapshot A31. */
public record Anexo30EntradaDto(
        long entradaKey,
        String descarga,
        String tipoOperacion,
        String pedimento,
        String pedimentoOriginal,
        LocalDateTime fecha,
        LocalDateTime fechaOriginal,
        String clavePedimento,
        String fraccion,
        BigDecimal valorComercial,
        BigDecimal ivaFp21,
        BigDecimal ivaFp22,
        BigDecimal saldo,
        Long operacion,
        String partida,
        String esaf) {
    public static Anexo30EntradaDto from(Anexo30Entrada item) {
        return new Anexo30EntradaDto(item.entradaKey(), item.descarga(), item.tipoOperacion(),
                item.pedimento(), item.pedimentoOriginal(), item.fecha(), item.fechaOriginal(),
                item.clavePedimento(), item.fraccion(), item.valorComercial(), item.ivaFp21(),
                item.ivaFp22(), item.saldo(), item.operacion(), item.partida(), item.esaf());
    }
}
