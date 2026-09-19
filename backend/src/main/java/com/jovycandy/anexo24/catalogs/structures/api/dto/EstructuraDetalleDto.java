package com.jovycandy.anexo24.catalogs.structures.api.dto;

import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea BOM expuesta por la API. */
public record EstructuraDetalleDto(
        Long estructuraId,
        BigDecimal productoId,
        String productoClave,
        String productoDescripcion,
        String productoUnidad,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        BigDecimal productoMaterialId,
        String materialClave,
        String materialDescripcion,
        String materialUnidad,
        String materialFraccion,
        BigDecimal cantidadIncorporada,
        BigDecimal cantidadMermada,
        BigDecimal cantidadDesperdiciada) {

    /**
     * Convierte una línea BOM de dominio a DTO.
     *
     * @param detalle línea BOM de dominio
     * @return DTO de presentación
     */
    public static EstructuraDetalleDto from(EstructuraDetalle detalle) {
        return new EstructuraDetalleDto(
                detalle.estructuraId(),
                detalle.productoId(),
                detalle.productoClave(),
                detalle.productoDescripcion(),
                detalle.productoUnidad(),
                detalle.fechaInicio(),
                detalle.fechaFin(),
                detalle.productoMaterialId(),
                detalle.materialClave(),
                detalle.materialDescripcion(),
                detalle.materialUnidad(),
                detalle.materialFraccion(),
                detalle.cantidadIncorporada(),
                detalle.cantidadMermada(),
                detalle.cantidadDesperdiciada());
    }
}
