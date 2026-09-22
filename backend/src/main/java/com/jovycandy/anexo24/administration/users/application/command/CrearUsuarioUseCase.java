package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CrearUsuarioCommand;
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
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/** Crea usuario administrativo y su evento de Bitácora en transacción app24. */
@Service
public class CrearUsuarioUseCase {
    private static final Pattern PASSWORD_VALIDA = Pattern.compile(
            "(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{Alnum}\\s]).{10,50}");

    private final UsuarioComandoRepository comandoRepository;
    private final UsuarioConsultaRepository consultaRepository;
    private final PerfilReferenciaRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public CrearUsuarioUseCase(UsuarioComandoRepository comandoRepository,
                               UsuarioConsultaRepository consultaRepository,
                               PerfilReferenciaRepository perfilRepository,
                               PasswordEncoder passwordEncoder,
                               RegistrarEventoBitacoraService bitacoraService,
                               AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public UsuarioAdministracion ejecutar(CrearUsuarioCommand command, String correlationId) {
        validarPassword(command.password());
        String clave = command.clave().trim();
        String nombre = command.nombre().trim();
        String correo = command.correo().trim();
        validarPerfil(command.perfilId());
        if (comandoRepository.existsByClave(clave) || comandoRepository.existsByCorreo(correo)) {
            throw new RecursoDuplicadoException();
        }
        String passwordHash = passwordEncoder.encode(command.password());
        Long id = comandoRepository.crear(clave, nombre, correo, passwordHash, command.vigencia(), command.perfilId());
        Long actorId = authenticatedUserContext.currentUser()
                .map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.USUARIO_CREADO, BitacoraResultado.EXITO,
                "usuarioObjetivoId=" + id + ";perfilId=" + command.perfilId(), correlationId));
        return consultaRepository.findById(id).orElseThrow(RecursoNoEncontradoException::new);
    }

    private void validarPerfil(Long perfilId) {
        String estado = perfilRepository.findEstadoById(perfilId).orElseThrow(RecursoNoEncontradoException::new);
        if (!"ACTIVO".equalsIgnoreCase(estado)) throw new EstadoIncompatibleException();
    }

    private void validarPassword(String password) {
        if (password == null || !PASSWORD_VALIDA.matcher(password).matches()) {
            throw new SolicitudInvalidaException("La contraseña no cumple la política de seguridad.");
        }
    }
}
