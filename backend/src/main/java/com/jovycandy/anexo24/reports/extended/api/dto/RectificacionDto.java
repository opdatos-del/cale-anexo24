package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Rectificacion;

/** Representación API del resumen read-only de rectificaciones. */
public record RectificacionDto(String pedimento, int total) {
    public static RectificacionDto from(Rectificacion item) {
        return new RectificacionDto(item.pedimento(), item.total());
    }
}
