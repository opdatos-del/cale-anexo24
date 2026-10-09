package com.jovycandy.anexo24.savedqueries.api.dto;

import tools.jackson.databind.JsonNode;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload de preset; no contiene identidad de usuario ni texto ejecutable. */
public record GuardarConsultaRequest(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 250) String descripcion,
        @NotNull ConsultaGuardadaAlcance alcance,
        @NotNull JsonNode criterios) { }
