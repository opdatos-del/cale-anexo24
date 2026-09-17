package com.jovycandy.anexo24.security.application;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioRepository;
import com.jovycandy.anexo24.security.CredencialesInvalidasException;
import com.jovycandy.anexo24.security.JwtTokenService;
import com.jovycandy.anexo24.security.api.dto.LoginRequest;
import com.jovycandy.anexo24.security.api.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso de inicio de sesión (CU-001).
 *
 * <p>Valida credenciales contra el esquema {@code app24} y emite un
 * token JWT con los permisos del usuario (ADR-002).</p>
 */
@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    /**
     * Constructor con dependencias del caso de uso.
     *
     * @param usuarioRepository acceso a usuarios del esquema app
     * @param passwordEncoder   codificador de contraseñas
     * @param tokenService      emisión de tokens JWT
     */
    public LoginService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Autentica al usuario y genera un token de acceso.
     *
     * @param request credenciales de acceso
     * @return token con permisos asignados
     * @throws CredencialesInvalidasException si las credenciales fallan
     */
    public LoginResponse login(LoginRequest request) {
        UsuarioApp usuario = usuarioRepository.findByClave(request.clave())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!usuario.estaActiva()
                || !passwordEncoder.matches(request.password(), usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }

        List<String> permisos = usuarioRepository.findPermisosByUsuario(usuario.id());
        String token = tokenService.generateToken(usuario.id(), usuario.clave(), permisos);
        return new LoginResponse(token, 480, usuario.nombre(), permisos);
    }
}