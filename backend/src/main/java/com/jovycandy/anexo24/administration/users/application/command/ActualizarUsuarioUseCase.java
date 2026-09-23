package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.ActualizarUsuarioCommand;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Edita sólo nombre/correo de usuario y audita cambios efectivos. */
@Service
public class ActualizarUsuarioUseCase {
    private final UsuarioComandoRepository comandoRepository;
    private final UsuarioConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public ActualizarUsuarioUseCase(UsuarioComandoRepository comandoRepository,
                                    UsuarioConsultaRepository consultaRepository,
                                    RegistrarEventoBitacoraService bitacoraService,
                                    AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public UsuarioAdministracion ejecutar(Long id, ActualizarUsuarioCommand command, String correlationId) {
        if (id == null || id <= 0) throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
        UsuarioAdministracion actual = consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
        String nombre = command.nombre().trim();
        String correo = command.correo().trim();

        List<String> campos = new ArrayList<>();
        if (!nombre.equals(actual.nombre())) campos.add("nombre");
        if (!correo.equals(actual.correo())) campos.add("correo");
        if (campos.isEmpty()) return actual;
        comandoRepository.actualizarDatos(id, nombre, correo);
        Long actorId = authenticatedUserContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.USUARIO_ACTUALIZADO, BitacoraResultado.EXITO,
                "usuarioObjetivoId=" + id + ";campos=" + String.join(",", campos), correlationId));
        return consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
    }
}
