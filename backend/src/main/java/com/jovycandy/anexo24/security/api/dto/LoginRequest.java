package com.jovycandy.anexo24.security.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de inicio de sesión.
 *
 * @param clave    clave de acceso del usuario
 * @param password contraseña del usuario
 */
public record LoginRequest(
        @NotBlank(message = "La clave es obligatoria") String clave,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}