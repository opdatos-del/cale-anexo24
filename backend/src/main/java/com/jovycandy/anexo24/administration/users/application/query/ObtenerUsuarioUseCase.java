package com.jovycandy.anexo24.administration.users.application.query;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de detalle read-only de un usuario (administración).
 *
 * <p>Valida el identificador y delega al puerto de consulta; no conoce
 * JDBC (FASE 2).</p>
 */
@Service
public class ObtenerUsuarioUseCase {

    private final UsuarioConsultaRepository usuarioConsultaRepository;

    /**
     * Construye el caso de uso de detalle.
     *
     * @param usuarioConsultaRepository puerto read-only de usuarios
     */
    public ObtenerUsuarioUseCase(UsuarioConsultaRepository usuarioConsultaRepository) {
        this.usuarioConsultaRepository = usuarioConsultaRepository;
    }

    /**
     * Obtiene el detalle administrativo de un usuario.
     *
     * @param id identificador del usuario
     * @return usuario administrativo
     * @throws SolicitudInvalidaException   si el identificador no es positivo
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    public UsuarioAdministracion ejecutar(Long id) {
        if (id == null || id <= 0) {
            throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
        }
        return usuarioConsultaRepository.findById(id)
                .orElseThrow(RecursoNoEncontradoException::new);
    }
}