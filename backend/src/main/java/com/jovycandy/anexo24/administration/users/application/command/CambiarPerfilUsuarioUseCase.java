package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarPerfilUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.PerfilReferenciaRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Cambia perfil de usuario conservando al menos un administrador efectivo. */
@Service
public class CambiarPerfilUsuarioUseCase {
    private final UsuarioComandoRepository comandoRepository;
    private final UsuarioConsultaRepository consultaRepository;
    private final PerfilReferenciaRepository perfilRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public CambiarPerfilUsuarioUseCase(UsuarioComandoRepository comandoRepository,
                                       UsuarioConsultaRepository consultaRepository,
                                       PerfilReferenciaRepository perfilRepository,
                                       RegistrarEventoBitacoraService bitacoraService,
                                       AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.perfilRepository = perfilRepository;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public UsuarioAdministracion ejecutar(Long id, CambiarPerfilUsuarioCommand command, String correlationId) {
        validarId(id);
        UsuarioAdministracion actual = consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
        validarPerfilId(command.perfilId());
        if (actual.perfilId().equals(command.perfilId())) return actual;
        Long actorId = authenticatedUserContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        if (actorId.equals(id)) throw new EstadoIncompatibleException();
        String estadoPerfil = perfilRepository.findEstadoById(command.perfilId())
                .orElseThrow(RecursoNoEncontradoException::new);
        if (!"ACTIVO".equalsIgnoreCase(estadoPerfil)) throw new EstadoIncompatibleException();
        exigirUnaFila(comandoRepository.actualizarPerfil(id, command.perfilId()));
        if (!comandoRepository.existsConCapacidadAdministrativa(LocalDate.now())) throw new EstadoIncompatibleException();
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.USUARIO_PERFIL_CAMBIADO, BitacoraResultado.EXITO,
                "usuarioObjetivoId=" + id + ";perfilAnteriorId=" + actual.perfilId()
                        + ";perfilNuevoId=" + command.perfilId(), correlationId));
        return consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
    }

    private void validarPerfilId(Long perfilId) {
        if (perfilId == null || perfilId <= 0) throw new SolicitudInvalidaException("El perfil debe ser positivo.");
    }

    private void exigirUnaFila(int filas) {
        if (filas == 0) throw new RecursoNoEncontradoException();
        if (filas != 1) throw new IllegalStateException("Actualización de usuario inconsistente");
    }
}
