package com.jovycandy.anexo24.administration.profiles.domain.port;

import java.time.LocalDate;
import java.util.List;

/** Puerto de commands administrativos de perfiles. */
public interface PerfilComandoRepository {

    /** Crea un perfil activo sin permisos y devuelve su identificador. */
    Long crear(String nombre);

    /** Actualiza exclusivamente el nombre de un perfil. */
    void actualizarNombre(Long perfilId, String nombre);

    /** Cambia el estado de un perfil aplicando el guardrail en el SP. */
    void cambiarEstado(Long perfilId, String estado, LocalDate fechaActual);

    /** Reemplaza atómicamente el conjunto completo de actividades del perfil. */
    void reemplazarPermisos(Long perfilId, List<Long> actividadIds, LocalDate fechaActual);
}
