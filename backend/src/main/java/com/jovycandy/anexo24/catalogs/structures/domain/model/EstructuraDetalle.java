package com.jovycandy.anexo24.catalogs.structures.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea plana de una estructura BOM del Módulo C. */
public record EstructuraDetalle(
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
}
