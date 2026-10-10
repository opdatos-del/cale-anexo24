package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del inventario inicial persistido expuesto por la vista legacy. */
public record Anexo30InventarioInicial(
        String patente,
        String numeroPedimento,
        String claveSeccionAduanera,
        LocalDateTime fechaSeleccion,
        String fraccion,
        BigDecimal valorComercialHistorico,
        String identificadorActivoFijo) {
}
