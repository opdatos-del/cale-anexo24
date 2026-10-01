package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representación API de una línea de salida dirigida. */
public record OperacionDirigidaDto(
        Long salidaKey,
        Long psalidaKey,
        String documento,
        LocalDateTime fechaSalida,
        String clavePedimento,
        Integer secuencia,
        String producto,
        BigDecimal cantidad,
        String factura,
        String descargo,
        String dirigido,
        BigDecimal valorDescargaDolares,
        BigDecimal valorDescargaPesos,
        String tieneEstructura,
        Integer numeroMateriales) {
    public static OperacionDirigidaDto from(OperacionDirigida item) {
        return new OperacionDirigidaDto(item.salidaKey(), item.psalidaKey(), item.documento(), item.fechaSalida(),
                item.clavePedimento(), item.secuencia(), item.producto(), item.cantidad(), item.factura(),
                item.descargo(), item.dirigido(), item.valorDescargaDolares(), item.valorDescargaPesos(),
                item.tieneEstructura(), item.numeroMateriales());
    }
}
