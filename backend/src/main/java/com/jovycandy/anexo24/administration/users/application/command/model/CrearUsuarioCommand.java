package com.jovycandy.anexo24.administration.users.application.command.model;

import java.time.LocalDate;

/** Datos de aplicación necesarios para crear un usuario. */
public final class CrearUsuarioCommand {
    private final String clave;
    private final String nombre;
    private final String correo;
    private final String password;
    private final LocalDate vigencia;
    private final Long perfilId;

    public CrearUsuarioCommand(String clave, String nombre, String correo, String password,
                               LocalDate vigencia, Long perfilId) {
        this.clave = clave;
        this.nombre = nombre;
        this.correo = correo;
        this.password = password;
        this.vigencia = vigencia;
        this.perfilId = perfilId;
    }

    public String clave() {
        return clave;
    }

    public String nombre() {
        return nombre;
    }

    public String correo() {
        return correo;
    }

    public String password() {
        return password;
    }

    public LocalDate vigencia() {
        return vigencia;
    }

    public Long perfilId() {
        return perfilId;
    }
}
