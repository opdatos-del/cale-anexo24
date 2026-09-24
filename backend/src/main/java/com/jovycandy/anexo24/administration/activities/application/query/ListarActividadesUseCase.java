package com.jovycandy.anexo24.administration.activities.application.query;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
import com.jovycandy.anexo24.administration.activities.domain.port.ActividadConsultaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Caso de uso read-only para listar el catálogo técnico de actividades. */
@Service
public class ListarActividadesUseCase {

    private final ActividadConsultaRepository actividadConsultaRepository;

    /**
     * Construye el caso de uso de listado.
     *
     * @param actividadConsultaRepository puerto read-only de actividades
     */
    public ListarActividadesUseCase(ActividadConsultaRepository actividadConsultaRepository) {
        this.actividadConsultaRepository = actividadConsultaRepository;
    }

    /**
     * Lista las actividades disponibles para asignación a perfiles.
     *
     * @return actividades ordenadas de forma estable por clave e identificador
     */
    public List<ActividadAdministracion> ejecutar() {
        return actividadConsultaRepository.findAll();
    }
}
