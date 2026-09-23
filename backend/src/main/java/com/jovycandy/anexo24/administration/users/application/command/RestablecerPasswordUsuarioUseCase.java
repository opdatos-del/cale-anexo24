package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.RestablecerPasswordUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Restablece una contraseña y registra el evento en la misma transacción app24. */
@Service
public class RestablecerPasswordUsuarioUseCase {
    private final UsuarioComandoRepository comandoRepository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public RestablecerPasswordUsuarioUseCase(UsuarioComandoRepository comandoRepository,
                                             PasswordPolicy passwordPolicy,
                                             PasswordEncoder passwordEncoder,
                                             RegistrarEventoBitacoraService bitacoraService,
                                             AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public void ejecutar(Long id, RestablecerPasswordUsuarioCommand command, String correlationId) {
        validarId(id);
        passwordPolicy.validar(command.password());
        Long actorId = authenticatedUserContext.currentUser()
                .map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        String passwordHash = passwordEncoder.encode(command.password());
        comandoRepository.restablecerPassword(id, passwordHash);
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.USUARIO_PASSWORD_RESTABLECIDA, BitacoraResultado.EXITO,
                "usuarioObjetivoId=" + id, correlationId));
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new SolicitudInvalidaException("El parámetro id debe ser positivo.");
        }
    }
}
