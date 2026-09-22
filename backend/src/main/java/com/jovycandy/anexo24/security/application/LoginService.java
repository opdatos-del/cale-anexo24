package com.jovycandy.anexo24.security.application;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAcceso;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.security.CredencialesInvalidasException;
import com.jovycandy.anexo24.security.JwtTokenService;
import com.jovycandy.anexo24.security.api.dto.LoginRequest;
import com.jovycandy.anexo24.security.api.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso de inicio de sesión (CU-001).
 *
 * <p>Valida credenciales contra el esquema {@code app24} y emite un
 * token JWT con los permisos del usuario (ADR-002).</p>
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final RegistrarEventoBitacoraService bitacoraService;

    /**
     * Constructor con dependencias del caso de uso.
     *
     * @param usuarioRepository acceso a usuarios del esquema app
     * @param passwordEncoder   codificador de contraseñas
     * @param tokenService      emisión de tokens JWT
     * @param bitacoraService    registro interno de eventos de autenticación
     */
    public LoginService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenService tokenService,
                        RegistrarEventoBitacoraService bitacoraService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.bitacoraService = bitacoraService;
    }

    /**
     * Autentica al usuario y genera un token de acceso.
     *
     * @param request       credenciales de acceso
     * @param correlationId identificador ya normalizado por la capa API o {@code null}
     * @return token con permisos asignados
     * @throws CredencialesInvalidasException si las credenciales fallan
     */
    public LoginResponse login(LoginRequest request, String correlationId) {
        Optional<UsuarioApp> usuarioEncontrado = usuarioRepository.findByClave(request.clave());
        if (usuarioEncontrado.isEmpty()) {
            registrarLoginFallido(null, correlationId);
            throw new CredencialesInvalidasException();
        }

        UsuarioApp usuario = usuarioEncontrado.get();
        if (!usuario.estaActiva()) {
            registrarLoginFallido(usuario.id(), correlationId);
            throw new CredencialesInvalidasException();
        }

        if (!passwordEncoder.matches(request.password(), usuario.passwordHash())) {
            registrarLoginFallido(usuario.id(), correlationId);
            throw new CredencialesInvalidasException();
        }

        Optional<UsuarioAcceso> accesoEncontrado = usuarioRepository.findAccesoByUsuario(usuario.id());
        if (accesoEncontrado.isEmpty()) {
            registrarLoginFallido(usuario.id(), correlationId);
            throw new CredencialesInvalidasException();
        }

        UsuarioAcceso acceso = accesoEncontrado.get();
        if (!acceso.perfilActivo()) {
            registrarLoginFallido(usuario.id(), correlationId);
            throw new CredencialesInvalidasException();
        }

        List<String> permisos = acceso.permisos();
        bitacoraService.registrar(new BitacoraEvento(
                usuario.id(),
                BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK,
                BitacoraResultado.EXITO,
                null,
                correlationId));
        String token = tokenService.generateToken(usuario.id(), usuario.clave(), permisos);
        return new LoginResponse(token, tokenService.expirationMinutes(), usuario.nombre(), permisos);
    }

    /**
     * Registra un fallo de autenticación sin alterar la respuesta genérica al cliente.
     *
     * @param usuarioId     actor conocido o {@code null} cuando no existe
     * @param correlationId identificador normalizado de la solicitud
     */
    private void registrarLoginFallido(Long usuarioId, String correlationId) {
        try {
            bitacoraService.registrar(new BitacoraEvento(
                    usuarioId,
                    BitacoraModulo.SEGURIDAD,
                    BitacoraAccion.LOGIN_FALLIDO,
                    BitacoraResultado.FALLO,
                    null,
                    correlationId));
        } catch (RuntimeException exception) {
            log.error("No fue posible registrar LOGIN_FALLIDO [{}]", correlationId, exception);
        }
    }
}