package com.jovycandy.anexo24.administration.profiles.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicitud HTTP para cambiar el estado de un perfil. */
public record CambiarEstadoPerfilRequest(@NotBlank String estado) {
}
