package com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.model;

/** Categoría de partida con vigencia documentada en CALE_IMMEX. */
public record Categoria(String clave, String descripcion, Integer diasValidos, Integer meses) {
}
