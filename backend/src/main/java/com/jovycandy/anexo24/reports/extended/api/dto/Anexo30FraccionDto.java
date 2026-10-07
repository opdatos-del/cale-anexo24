package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Fraccion;

import java.math.BigDecimal;

/** Representación API de una fracción persistida del proceso Anexo 30. */
public record Anexo30FraccionDto(
        long fraccionKey, String tipo, String clavePedimento, String ejercicio, String periodo,
        String fraccion, BigDecimal valor, String af, String archivo) {

    public static Anexo30FraccionDto from(Anexo30Fraccion item) {
        return new Anexo30FraccionDto(item.fraccionKey(), item.tipo(), item.clavePedimento(),
                item.ejercicio(), item.periodo(), item.fraccion(), item.valor(), item.af(), item.archivo());
    }
}
