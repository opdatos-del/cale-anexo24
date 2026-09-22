package com.jovycandy.anexo24.security.api;

import com.jovycandy.anexo24.security.application.LoginService;
import com.jovycandy.anexo24.security.api.dto.LoginRequest;
import com.jovycandy.anexo24.security.api.dto.LoginResponse;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 *
 * <p>Expone el inicio de sesión público (CU-001).</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final LoginService loginService;

    /**
     * Constructor con el caso de uso de login.
     *
     * @param loginService caso de uso de autenticación
     */
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * Inicia sesión y devuelve un token de acceso.
     *
     * @param request        credenciales de acceso
     * @param servletRequest solicitud HTTP con correlationId normalizado
     * @return token JWT con permisos
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest servletRequest) {
        Object correlationId = servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return ResponseEntity.status(HttpStatus.OK)
                .body(loginService.login(request, correlationId == null ? null : correlationId.toString()));
    }
}