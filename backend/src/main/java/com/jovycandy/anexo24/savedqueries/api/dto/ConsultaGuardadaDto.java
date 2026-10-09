package com.jovycandy.anexo24.savedqueries.api.dto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;

import java.time.LocalDateTime;

/** Respuesta tipada de un preset de filtros estructurados. */
public record ConsultaGuardadaDto(Long id, String nombre, String descripcion, String alcance,
                                  JsonNode criterios, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static ConsultaGuardadaDto from(ConsultaGuardada consulta) {
        try {
            return new ConsultaGuardadaDto(consulta.id(), consulta.nombre(), consulta.descripcion(), consulta.alcance().name(),
                    JSON_MAPPER.readTree(consulta.criteriosJson()), consulta.fechaCreacion(), consulta.fechaActualizacion());
        } catch (Exception exception) {
            throw new IllegalStateException("Criterios persistidos inválidos", exception);
        }
    }
}
