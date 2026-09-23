package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarEstadoUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;

import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

/** Cambia estado de usuario conservando al menos un administrador efectivo. */
@Service
public class CambiarEstadoUsuarioUseCase {
    private final UsuarioComandoRepository comandoRepository;
    private final UsuarioConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public CambiarEstadoUsuarioUseCase(UsuarioComandoRepository comandoRepository,
                                       UsuarioConsultaRepository consultaRepository,
                                       RegistrarEventoBitacoraService bitacoraService,
                                       AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public UsuarioAdministracion ejecutar(Long id, CambiarEstadoUsuarioCommand command, String correlationId) {
        validarId(id);
        UsuarioAdministracion actual = consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
        String nuevoEstado = normalizarEstado(command.estado());
        if (actual.estado().equals(nuevoEstado)) return actual;
        Long actorId = authenticatedUserContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        comandoRepository.actualizarEstado(id, nuevoEstado, actorId, LocalDate.now());
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.USUARIO_ESTADO_CAMBIADO, BitacoraResultado.EXITO,
                "usuarioObjetivoId=" + id + ";estadoAnterior=" + actual.estado() + ";estadoNuevo=" + nuevoEstado,
                correlationId));
        return consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
    }

    private String normalizarEstado(String estado) {
        if (estado == null) throw new SolicitudInvalidaException("El estado es obligatorio.");
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVO".equals(normalizado) && !"INACTIVO".equals(normalizado)) {
            throw new SolicitudInvalidaException("El estado es inválido.");
        }
        return normalizado;
    }

}
