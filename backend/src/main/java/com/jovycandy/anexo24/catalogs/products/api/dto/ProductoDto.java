package com.jovycandy.anexo24.catalogs.products.api.dto;

import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;

import java.math.BigDecimal;

/** Producto del catálogo expuesto por la API. */
public record ProductoDto(
        BigDecimal productokey,
        String cveProducto,
        String nombre,
        String fraccion,
        String unidad,
        String unidadt) {

    /**
     * Convierte un producto de dominio a DTO.
     *
     * @param producto producto de dominio
     * @return DTO de presentación
     */
    public static ProductoDto from(Producto producto) {
        return new ProductoDto(
                producto.productokey(),
                producto.cveProducto(),
                producto.nombre(),
                producto.fraccion(),
                producto.unidad(),
                producto.unidadt());
    }
}
