package com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.api.dto;

import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.model.TipoMaterial;

/** Tipo de material expuesto por la API. */
public record TipoMaterialDto(String nombre) {
    public static TipoMaterialDto from(TipoMaterial tipo) {
        return new TipoMaterialDto(tipo.nombre());
    }
}
