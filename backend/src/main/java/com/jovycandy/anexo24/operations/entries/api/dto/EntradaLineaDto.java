package com.jovycandy.anexo24.operations.entries.api.dto;

import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea de Entrada expuesta por la API. */
public record EntradaLineaDto(
        BigDecimal importacionId,
        BigDecimal partidaId,
        String pedimento,
        String clavePedimento,
        LocalDateTime fechaEntrada,
        String fraccion,
        String unidadComercial,
        BigDecimal cantidadComercial,
        String numeroParte,
        LocalDateTime fechaPago) {

    /**
     * Convierte una línea de dominio a DTO de presentación.
     *
     * @param entrada línea de Entrada de dominio
     * @return DTO de Entrada
     */
    public static EntradaLineaDto from(EntradaLinea entrada) {
        return new EntradaLineaDto(
                entrada.importacionId(),
                entrada.partidaId(),
                entrada.pedimento(),
                entrada.clavePedimento(),
                entrada.fechaEntrada(),
                entrada.fraccion(),
                entrada.unidadComercial(),
                entrada.cantidadComercial(),
                entrada.numeroParte(),
                entrada.fechaPago());
    }
}
