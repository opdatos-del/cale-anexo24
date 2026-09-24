package com.jovycandy.anexo24.administration.profiles.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Solicitud HTTP del conjunto final de permisos de un perfil. */
public record ReemplazarPermisosPerfilRequest(
        @NotNull List<@NotNull @Positive Long> actividadIds) {
}
