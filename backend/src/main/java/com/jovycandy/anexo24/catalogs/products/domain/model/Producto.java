package com.jovycandy.anexo24.catalogs.products.domain.model;

import java.math.BigDecimal;

/** Producto del catálogo canónico del Módulo C. */
public record Producto(
        BigDecimal productokey,
        String cveProducto,
        String nombre,
        String fraccion,
        String unidad,
        String unidadt) {
}
