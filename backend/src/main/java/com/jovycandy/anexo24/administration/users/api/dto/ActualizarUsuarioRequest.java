package com.jovycandy.anexo24.administration.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud HTTP para editar sólo nombre y correo de un usuario. */
public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 150) String correo) {
}
