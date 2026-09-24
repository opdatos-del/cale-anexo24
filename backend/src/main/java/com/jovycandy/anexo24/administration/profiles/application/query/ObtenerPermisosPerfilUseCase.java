package com.jovycandy.anexo24.administration.profiles.application.query;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilPermisosConsultaRepository;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Obtiene el conjunto completo de permisos de un perfil. */
@Service
public class ObtenerPermisosPerfilUseCase {
    private final PerfilPermisosConsultaRepository consultaRepository;

    public ObtenerPermisosPerfilUseCase(PerfilPermisosConsultaRepository consultaRepository) {
        this.consultaRepository = consultaRepository;
    }

    public PerfilPermisosDetalle ejecutar(Long perfilId) {
        if (perfilId == null || perfilId <= 0) throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
        return consultaRepository.findPermissionsByProfileId(perfilId).orElseThrow(RecursoNoEncontradoException::new);
    }
}
