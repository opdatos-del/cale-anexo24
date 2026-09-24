package com.jovycandy.anexo24.administration.profiles.domain.port;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;

import java.util.Optional;

/** Puerto de consulta de permisos por perfil. */
public interface PerfilPermisosConsultaRepository {

    /**
     * Obtiene el perfil y su conjunto completo de permisos.
     *
     * @param perfilId identificador del perfil
     * @return detalle del perfil, si existe
     */
    Optional<PerfilPermisosDetalle> findPermissionsByProfileId(Long perfilId);
}
