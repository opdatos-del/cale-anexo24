package com.jovycandy.anexo24.savedqueries.domain.port;

import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;

import java.util.List;

/** Puerto owner-scoped para persistencia de presets sin semántica de ejecución. */
public interface ConsultaGuardadaRepository {
    List<ConsultaGuardada> listar(Long usuarioId, ConsultaGuardadaAlcance alcance);
    ConsultaGuardada crear(Long usuarioId, String nombre, String descripcion, ConsultaGuardadaAlcance alcance, String criteriosJson);
    ConsultaGuardada actualizar(Long id, Long usuarioId, String nombre, String descripcion, ConsultaGuardadaAlcance alcance, String criteriosJson);
    void eliminar(Long id, Long usuarioId);
}
