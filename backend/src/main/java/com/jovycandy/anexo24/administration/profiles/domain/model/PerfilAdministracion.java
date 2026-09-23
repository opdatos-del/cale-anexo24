package com.jovycandy.anexo24.administration.profiles.domain.model;

/**
 * Proyección read-only de un perfil para su administración.
 *
 * @param id               identificador del perfil
 * @param nombre           nombre del perfil
 * @param estado           estado del perfil
 * @param cantidadPermisos cantidad de permisos asignados
 */
public record PerfilAdministracion(Long id, String nombre, String estado, long cantidadPermisos) {
}
