package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;

import java.math.BigDecimal;
import java.time.Instant;

/** Representacion API de una fila del reporte legacy Saldos (LEGACY-038). */
public record SaldoDto(
        String documento,
        Instant fechaPago,
        String clavePedimento,
        String tipoOperacion,
        BigDecimal tcMonetaria,
        String clave,
        String descripcion,
        String fraccion,
        BigDecimal cantImportado,
        String unidad,
        BigDecimal saldo,
        BigDecimal valorAduanalDeSaldo,
        BigDecimal valorDolaresDelSaldo,
        String paisOrigen,
        Integer temporalidadMeses,
        String categoria,
        Instant fechaVencimiento,
        String pedimentoOriginal,
        String descarga,
        String lote,
        String complemento1,
        String complemento2,
        String complemento3,
        BigDecimal desperdiciado,
        BigDecimal saldodesperdicio,
        String cove,
        String factura,
        String tipoMaterial,
        BigDecimal puVad,
        BigDecimal puVdo,
        BigDecimal valAduanal,
        BigDecimal valDolares,
        BigDecimal saldoEnUMT,
        String unidadt,
        BigDecimal valorEnPesos,
        BigDecimal saldoEnValorPesos,
        String nico) {
    public static SaldoDto from(Saldo item) {
        return new SaldoDto(item.documento(), item.fechaPago(), item.clavePedimento(),
                item.tipoOperacion(), item.tcMonetaria(), item.clave(), item.descripcion(),
                item.fraccion(), item.cantImportado(), item.unidad(), item.saldo(),
                item.valorAduanalDeSaldo(), item.valorDolaresDelSaldo(), item.paisOrigen(),
                item.temporalidadMeses(), item.categoria(), item.fechaVencimiento(),
                item.pedimentoOriginal(), item.descarga(), item.lote(), item.complemento1(),
                item.complemento2(), item.complemento3(), item.desperdiciado(), item.saldodesperdicio(),
                item.cove(), item.factura(), item.tipoMaterial(), item.puVad(), item.puVdo(),
                item.valAduanal(), item.valDolares(), item.saldoEnUMT(), item.unidadt(),
                item.valorEnPesos(), item.saldoEnValorPesos(), item.nico());
    }
}
