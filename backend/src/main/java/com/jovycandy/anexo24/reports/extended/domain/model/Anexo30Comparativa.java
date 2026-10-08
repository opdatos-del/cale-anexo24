package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;

/** Fila de la última comparativa A31/A24 persistida (snapshot read-only). */
public record Anexo30Comparativa(
        String clavePedimento, String ejercicio, String periodo, String fraccion,
        BigDecimal valorA31, BigDecimal valorA24, BigDecimal diferencia,
        BigDecimal iva21Total, BigDecimal iva22Total, BigDecimal valorTotal,
        BigDecimal ivaDescargadoA31, BigDecimal ivaDescargadoA24) {
}
