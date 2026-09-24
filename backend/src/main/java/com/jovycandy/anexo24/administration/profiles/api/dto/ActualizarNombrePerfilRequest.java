package com.jovycandy.anexo24.administration.profiles.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud HTTP para actualizar el nombre de un perfil. */
public record ActualizarNombrePerfilRequest(@NotBlank @Size(max = 80) String nombre) {
}
