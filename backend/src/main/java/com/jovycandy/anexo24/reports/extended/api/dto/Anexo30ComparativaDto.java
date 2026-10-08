package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Comparativa;
import java.math.BigDecimal;

/** Representación API de una fila de la comparativa A31/A24 persistida. */
public record Anexo30ComparativaDto(
        String clavePedimento, String ejercicio, String periodo, String fraccion,
        BigDecimal valorA31, BigDecimal valorA24, BigDecimal diferencia,
        BigDecimal iva21Total, BigDecimal iva22Total, BigDecimal valorTotal,
        BigDecimal ivaDescargadoA31, BigDecimal ivaDescargadoA24) {
    public static Anexo30ComparativaDto from(Anexo30Comparativa item) {
        return new Anexo30ComparativaDto(
            item.clavePedimento(), item.ejercicio(), item.periodo(), item.fraccion(),
            item.valorA31(), item.valorA24(), item.diferencia(),
            item.iva21Total(), item.iva22Total(), item.valorTotal(),
            item.ivaDescargadoA31(), item.ivaDescargadoA24());
    }
}
