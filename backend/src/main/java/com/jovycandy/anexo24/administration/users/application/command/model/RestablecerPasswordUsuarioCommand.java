package com.jovycandy.anexo24.administration.users.application.command.model;

/** Datos de aplicación para restablecer la contraseña de un usuario. */
public record RestablecerPasswordUsuarioCommand(String password) {
    @Override
    public String toString() {
        return "RestablecerPasswordUsuarioCommand[password=REDACTED]";
    }
}
