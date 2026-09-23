package com.jovycandy.anexo24.administration.users.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicitud HTTP para restablecer una contraseña administrativa. */
public record RestablecerPasswordUsuarioRequest(@NotBlank String password) {
    @Override
    public String toString() {
        return "RestablecerPasswordUsuarioRequest[password=REDACTED]";
    }
}
