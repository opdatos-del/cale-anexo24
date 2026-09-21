package com.jovycandy.anexo24.operations.usedmaterials.api.dto;

import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila histórica de material utilizado expuesta por la API. */
public record MaterialUtilizadoDto(
        Long descargaId,
        BigDecimal entradaId,
        BigDecimal partidaEntradaId,
        BigDecimal salidaId,
        BigDecimal partidaSalidaId,
        String pedimentoEntrada,
        String pedimentoSalida,
        String materialCode,
        String materialDescription,
        String productCode,
        String productDescription,
        BigDecimal cantidadIncorporada,
        BigDecimal cantidadMerma,
        BigDecimal cantidadDesperdicio,
        BigDecimal cantidadTotalDescargada,
        String unidad,
        LocalDateTime fecha) {

    /**
     * Convierte una fila de dominio a DTO de presentación.
     *
     * @param materialUtilizado fila histórica de dominio
     * @return DTO de material utilizado
     */
    public static MaterialUtilizadoDto from(MaterialUtilizado materialUtilizado) {
        return new MaterialUtilizadoDto(
                materialUtilizado.descargaId(),
                materialUtilizado.entradaId(),
                materialUtilizado.partidaEntradaId(),
                materialUtilizado.salidaId(),
                materialUtilizado.partidaSalidaId(),
                materialUtilizado.pedimentoEntrada(),
                materialUtilizado.pedimentoSalida(),
                materialUtilizado.materialCode(),
                materialUtilizado.materialDescription(),
                materialUtilizado.productCode(),
                materialUtilizado.productDescription(),
                materialUtilizado.cantidadIncorporada(),
                materialUtilizado.cantidadMerma(),
                materialUtilizado.cantidadDesperdicio(),
                materialUtilizado.cantidadTotalDescargada(),
                materialUtilizado.unidad(),
                materialUtilizado.fecha());
    }
}
