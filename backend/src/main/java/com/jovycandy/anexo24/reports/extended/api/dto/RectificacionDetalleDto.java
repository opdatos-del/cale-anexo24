package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.RectificacionDetalle;

/** Representación API del detalle de rectificaciones persistidas. */
public record RectificacionDetalleDto(
        String pedimento, String clavePedimento, String descarga, String pedimentoOriginal,
        String existePedimento, String clavePedimentoOriginal, String descargaOriginal, String status) {
    public static RectificacionDetalleDto from(RectificacionDetalle item) {
        return new RectificacionDetalleDto(item.pedimento(), item.clavePedimento(), item.descarga(),
                item.pedimentoOriginal(), item.existePedimento(), item.clavePedimentoOriginal(),
                item.descargaOriginal(), item.status());
    }
}
