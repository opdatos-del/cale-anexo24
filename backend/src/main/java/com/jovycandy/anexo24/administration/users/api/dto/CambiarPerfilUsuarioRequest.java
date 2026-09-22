package com.jovycandy.anexo24.administration.users.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Solicitud HTTP para cambiar el perfil de un usuario. */
public record CambiarPerfilUsuarioRequest(@NotNull @Positive Long perfilId) {
}
