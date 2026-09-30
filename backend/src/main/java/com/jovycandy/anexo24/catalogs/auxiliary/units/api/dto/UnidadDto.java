package com.jovycandy.anexo24.catalogs.auxiliary.units.api.dto;

import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.model.Unidad;

/** Unidad expuesta por la API de catálogos auxiliares. */
public record UnidadDto(String clave, String nombre, String alias) {
    public static UnidadDto from(Unidad unidad) {
        return new UnidadDto(unidad.clave(), unidad.nombre(), unidad.alias());
    }
}
