package com.jovycandy.anexo24.administration.users.domain.model;

import java.time.LocalDate;

/**
 * Proyección administrativa read-only de un usuario.
 *
 * <p>Nunca contiene secretos: no incluye {@code passwordHash} ni permisos
 * (FASE 2 — consulta administrativa).</p>
 *
 * @param id           identificador del usuario
 * @param clave        clave de acceso
 * @param nombre       nombre completo
 * @param correo       correo electrónico
 * @param estado       estado del usuario (ACTIVO o INACTIVO)
 * @param vigencia     fecha de vigencia o {@code null} si es indefinida
 * @param perfilId     identificador del perfil asignado
 * @param perfilNombre nombre del perfil asignado
 */
public record UsuarioAdministracion(
        Long id,
        String clave,
        String nombre,
        String correo,
        String estado,
        LocalDate vigencia,
        Long perfilId,
        String perfilNombre) {
}