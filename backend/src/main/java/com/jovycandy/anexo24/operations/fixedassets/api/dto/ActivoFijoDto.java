package com.jovycandy.anexo24.operations.fixedassets.api.dto;

import com.jovycandy.anexo24.operations.fixedassets.domain.model.ActivoFijo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Partida activa expuesta por la API de Activos Fijos. */
public record ActivoFijoDto(
        BigDecimal partidaEntradaId,
        BigDecimal importacionId,
        String pedimento,
        String clavePedimento,
        LocalDateTime fechaImportacion,
        String numeroParte,
        String descripcion,
        String fraccion,
        BigDecimal cantidad,
        String unidad,
        String numeroSerie,
        String marca,
        String modelo) {

    /**
     * Convierte un activo fijo de dominio a DTO de presentación.
     *
     * @param activoFijo partida activa de dominio
     * @return DTO de activo fijo
     */
    public static ActivoFijoDto from(ActivoFijo activoFijo) {
        return new ActivoFijoDto(
                activoFijo.partidaEntradaId(),
                activoFijo.importacionId(),
                activoFijo.pedimento(),
                activoFijo.clavePedimento(),
                activoFijo.fechaImportacion(),
                activoFijo.numeroParte(),
                activoFijo.descripcion(),
                activoFijo.fraccion(),
                activoFijo.cantidad(),
                activoFijo.unidad(),
                activoFijo.numeroSerie(),
                activoFijo.marca(),
                activoFijo.modelo());
    }
}
