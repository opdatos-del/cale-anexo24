package com.jovycandy.anexo24.administration.users.domain.model;

import java.time.LocalDate;

/**
 * Usuario de la aplicación (esquema complementario {@code app24}).
 *
 * <p>Corresponde a la entidad UsuarioApp definida en
 * {@code docs/03-diseno/modelo-datos.md}.</p>
 *
 * @param id           identificador único
 * @param clave        clave de acceso
 * @param nombre       nombre completo
 * @param correo       correo electrónico
 * @param passwordHash hash de la contraseña (bcrypt, ADR-002)
 * @param estado       estado del usuario (ACTIVO / INACTIVO)
 * @param vigencia     fecha de vencimiento de la cuenta
 * @param perfilId     identificador del perfil asignado
 */
public record UsuarioApp(
        Long id,
        String clave,
        String nombre,
        String correo,
        String passwordHash,
        String estado,
        LocalDate vigencia,
        Long perfilId) {

    @Override
    public String toString() {
        return "UsuarioApp[id=" + id + ", clave=" + clave + ", nombre=" + nombre + ", correo=" + correo
                + ", passwordHash=REDACTED, estado=" + estado + ", vigencia=" + vigencia
                + ", perfilId=" + perfilId + "]";
    }

    /**
     * Indica si la cuenta está activa y vigente.
     *
     * @return {@code true} si el estado es ACTIVO y no venció
     */
    public boolean estaActiva() {
        boolean activo = "ACTIVO".equalsIgnoreCase(estado);
        boolean vigente = vigencia == null || !vigencia.isBefore(LocalDate.now());
        return activo && vigente;
    }
}