package com.jovycandy.anexo24.administration.activities.domain.port;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;

import java.util.List;

/** Puerto read-only de consulta del catálogo técnico de actividades. */
public interface ActividadConsultaRepository {

    /**
     * Lista todas las actividades disponibles, ordenadas de forma estable por clave e identificador.
     *
     * @return actividades disponibles para asignación a perfiles
     */
    List<ActividadAdministracion> findAll();
}
