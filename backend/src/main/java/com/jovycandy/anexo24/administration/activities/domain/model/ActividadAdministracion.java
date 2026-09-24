package com.jovycandy.anexo24.administration.activities.domain.model;

/**
 * Proyección read-only de una actividad disponible para la administración de perfiles.
 *
 * @param id      identificador de la actividad
 * @param clave   autoridad estable de la actividad
 * @param nombre  nombre descriptivo de la actividad
 * @param recurso recurso HTTP o funcional protegido
 * @param accion  acción autorizada sobre el recurso
 */
public record ActividadAdministracion(Long id, String clave, String nombre, String recurso, String accion) {
}
