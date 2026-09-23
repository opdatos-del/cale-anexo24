package com.jovycandy.anexo24.administration.profiles.api.dto;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;

/**
 * Representación HTTP read-only de un perfil de administración.
 *
 * @param id               identificador del perfil
 * @param nombre           nombre del perfil
 * @param estado           estado del perfil
 * @param cantidadPermisos cantidad de permisos asignados
 */
public record PerfilAdministracionDto(Long id, String nombre, String estado, long cantidadPermisos) {

    /**
     * Convierte la proyección de dominio a su representación HTTP.
     *
     * @param perfil perfil administrativo de dominio
     * @return DTO de respuesta
     */
    public static PerfilAdministracionDto from(PerfilAdministracion perfil) {
        return new PerfilAdministracionDto(
                perfil.id(), perfil.nombre(), perfil.estado(), perfil.cantidadPermisos());
    }
}
