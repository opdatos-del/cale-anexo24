package com.jovycandy.anexo24.administration.users.api.dto;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;

import java.time.LocalDate;

/**
 * Representación HTTP read-only de un usuario de administración.
 *
 * <p>Nunca expone secretos: no incluye {@code passwordHash} ni permisos
 * (FASE 2).</p>
 *
 * @param id           identificador del usuario
 * @param clave        clave de acceso
 * @param nombre       nombre completo
 * @param correo       correo electrónico
 * @param estado       estado del usuario
 * @param vigencia     fecha de vigencia o {@code null}
 * @param perfilId     identificador del perfil asignado
 * @param perfilNombre nombre del perfil asignado
 */
public record UsuarioAdministracionDto(
        Long id,
        String clave,
        String nombre,
        String correo,
        String estado,
        LocalDate vigencia,
        Long perfilId,
        String perfilNombre) {

    /**
     * Convierte la proyección de dominio a su representación HTTP.
     *
     * @param usuario usuario administrativo de dominio
     * @return DTO de respuesta
     */
    public static UsuarioAdministracionDto from(UsuarioAdministracion usuario) {
        return new UsuarioAdministracionDto(
                usuario.id(),
                usuario.clave(),
                usuario.nombre(),
                usuario.correo(),
                usuario.estado(),
                usuario.vigencia(),
                usuario.perfilId(),
                usuario.perfilNombre());
    }
}