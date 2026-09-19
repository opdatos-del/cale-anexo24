package com.jovycandy.anexo24.catalogs.products.api.dto;

import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;

import java.math.BigDecimal;

/** Producto del catálogo expuesto por la API. */
public record ProductoDto(
        BigDecimal id,
        String clave,
        String descripcion,
        String fraccion,
        String unidadComercial,
        String unidadTarifaria) {

    /**
     * Convierte un producto de dominio a DTO.
     *
     * @param producto producto de dominio
     * @return DTO de presentación
     */
    public static ProductoDto from(Producto producto) {
        return new ProductoDto(
                producto.id(),
                producto.clave(),
                producto.descripcion(),
                producto.fraccion(),
                producto.unidadComercial(),
                producto.unidadTarifaria());
    }
}
