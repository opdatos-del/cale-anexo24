package com.jovycandy.anexo24.administration.users.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicitud HTTP para cambiar el estado de un usuario. */
public record CambiarEstadoUsuarioRequest(@NotBlank String estado) {
}
