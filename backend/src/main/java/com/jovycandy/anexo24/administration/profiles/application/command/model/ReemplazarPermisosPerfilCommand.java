package com.jovycandy.anexo24.administration.profiles.application.command.model;

import java.util.List;

/** Conjunto final de actividades asignadas a un perfil. */
public record ReemplazarPermisosPerfilCommand(List<Long> actividadIds) {
}
