package com.jovycandy.anexo24.administration.profiles.api.dto;

import com.jovycandy.anexo24.administration.activities.api.dto.ActividadAdministracionDto;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;

import java.util.List;

/** Representación HTTP del conjunto de permisos de un perfil. */
public record PerfilPermisosDetalleDto(Long perfilId, List<ActividadAdministracionDto> permisos) {

    public static PerfilPermisosDetalleDto from(PerfilPermisosDetalle detalle) {
        return new PerfilPermisosDetalleDto(detalle.perfilId(),
                detalle.permisos().stream().map(ActividadAdministracionDto::from).toList());
    }
}
