package com.jovycandy.anexo24.administration.activities.api.dto;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;

/**
 * Representación HTTP read-only de una actividad administrativa.
 *
 * @param id      identificador de la actividad
 * @param clave   autoridad estable de la actividad
 * @param nombre  nombre descriptivo de la actividad
 * @param recurso recurso HTTP o funcional protegido
 * @param accion  acción autorizada sobre el recurso
 */
public record ActividadAdministracionDto(Long id, String clave, String nombre, String recurso, String accion) {

    /**
     * Convierte la proyección de dominio a su representación HTTP.
     *
     * @param actividad actividad administrativa de dominio
     * @return DTO de respuesta
     */
    public static ActividadAdministracionDto from(ActividadAdministracion actividad) {
        return new ActividadAdministracionDto(
                actividad.id(), actividad.clave(), actividad.nombre(), actividad.recurso(), actividad.accion());
    }
}
