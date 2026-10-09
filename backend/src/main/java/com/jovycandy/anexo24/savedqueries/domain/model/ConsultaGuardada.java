package com.jovycandy.anexo24.savedqueries.domain.model;

import java.time.LocalDateTime;

/** Preset de filtros estructurados, propiedad exclusiva de un usuario autenticado. */
public record ConsultaGuardada(Long id, String nombre, String descripcion,
                               ConsultaGuardadaAlcance alcance, String criteriosJson,
                               LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
}
