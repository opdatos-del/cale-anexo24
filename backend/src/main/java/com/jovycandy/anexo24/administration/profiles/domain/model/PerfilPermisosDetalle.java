package com.jovycandy.anexo24.administration.profiles.domain.model;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;

import java.util.List;

/** Detalle de permisos asignados a un perfil administrativo. */
public record PerfilPermisosDetalle(Long perfilId, String nombre, String estado,
                                    List<ActividadAdministracion> permisos) {
}
