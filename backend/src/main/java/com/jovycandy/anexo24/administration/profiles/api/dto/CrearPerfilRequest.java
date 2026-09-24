package com.jovycandy.anexo24.administration.profiles.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud HTTP para crear un perfil. */
public record CrearPerfilRequest(@NotBlank @Size(max = 80) String nombre) {
}
