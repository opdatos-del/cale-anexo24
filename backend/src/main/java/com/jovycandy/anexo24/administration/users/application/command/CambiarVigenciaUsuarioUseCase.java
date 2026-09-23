package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarVigenciaUsuarioCommand;
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
import java.util.Objects;

/** Cambia vigencia de usuario conservando al menos un administrador efectivo. */
@Service
public class CambiarVigenciaUsuarioUseCase {
    private final UsuarioComandoRepository comandoRepository;
    private final UsuarioConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public CambiarVigenciaUsuarioUseCase(UsuarioComandoRepository comandoRepository,
                                         UsuarioConsultaRepository consultaRepository,
                                         RegistrarEventoBitacoraService bitacoraService,
                                         AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public UsuarioAdministracion ejecutar(Long id, CambiarVigenciaUsuarioCommand command, String correlationId) {
        validarId(id);
        UsuarioAdministracion actual = consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
        LocalDate nuevaVigencia = command.vigencia();
        if (Objects.equals(actual.vigencia(), nuevaVigencia)) return actual;
        Long actorId = authenticatedUserContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        comandoRepository.actualizarVigencia(id, nuevaVigencia, actorId, LocalDate.now());
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.USUARIO_VIGENCIA_CAMBIADA, BitacoraResultado.EXITO,
                "usuarioObjetivoId=" + id + ";vigenciaAnterior=" + etiquetaVigencia(actual.vigencia())
                        + ";vigenciaNueva=" + etiquetaVigencia(nuevaVigencia), correlationId));
        return consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
    }


    private String etiquetaVigencia(LocalDate vigencia) {
        return vigencia == null ? "SIN_VIGENCIA" : vigencia.toString();
    }
}
