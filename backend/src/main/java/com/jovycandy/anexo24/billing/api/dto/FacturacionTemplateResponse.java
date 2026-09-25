package com.jovycandy.anexo24.billing.api.dto;

import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;

import java.util.List;

public record FacturacionTemplateResponse(String nombre, String version, String hoja,
                                          List<Columna> columnas) {
    public record Columna(String nombre, boolean obligatoria, String tipo) {}

    public static FacturacionTemplateResponse from(PlantillaFacturacion template) {
        return new FacturacionTemplateResponse(template.nombre(), template.version(), template.hoja(),
                template.columnas().stream().map(c -> new Columna(c.nombre(), c.obligatoria(), c.tipo())).toList());
    }
}
