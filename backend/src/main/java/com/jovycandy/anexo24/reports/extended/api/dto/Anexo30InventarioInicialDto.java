package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30InventarioInicial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO del inventario inicial read-only del Anexo 30. */
public record Anexo30InventarioInicialDto(
        String patente,
        String numeroPedimento,
        String claveSeccionAduanera,
        LocalDateTime fechaSeleccion,
        String fraccion,
        BigDecimal valorComercialHistorico,
        String identificadorActivoFijo) {
    public static Anexo30InventarioInicialDto from(Anexo30InventarioInicial item) {
        return new Anexo30InventarioInicialDto(item.patente(), item.numeroPedimento(), item.claveSeccionAduanera(),
                item.fechaSeleccion(), item.fraccion(), item.valorComercialHistorico(), item.identificadorActivoFijo());
    }
}
