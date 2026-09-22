package com.jovycandy.anexo24.administration.users.application.command.model;

import java.time.LocalDate;

/** Datos de aplicación para cambiar la vigencia de un usuario. */
public record CambiarVigenciaUsuarioCommand(LocalDate vigencia) {
}
