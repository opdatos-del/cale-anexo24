package com.jovycandy.anexo24.administration.users.application.command.model;

/** Datos de aplicación necesarios para editar nombre y correo de un usuario. */
public final class ActualizarUsuarioCommand {
    private final String nombre;
    private final String correo;

    public ActualizarUsuarioCommand(String nombre, String correo) {
        this.nombre = nombre;
        this.correo = correo;
    }

    public String nombre() {
        return nombre;
    }

    public String correo() {
        return correo;
    }
}
