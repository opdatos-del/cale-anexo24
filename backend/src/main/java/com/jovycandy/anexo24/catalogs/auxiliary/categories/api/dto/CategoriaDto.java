package com.jovycandy.anexo24.catalogs.auxiliary.categories.api.dto;

import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.model.Categoria;

/** Categoría expuesta por la API de catálogos auxiliares. */
public record CategoriaDto(String clave, String descripcion, Integer diasValidos, Integer meses) {
    public static CategoriaDto from(Categoria categoria) {
        return new CategoriaDto(categoria.clave(), categoria.descripcion(), categoria.diasValidos(), categoria.meses());
    }
}
