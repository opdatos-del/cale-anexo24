package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionBloqueada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representación API del snapshot read-only de operaciones bloqueadas. */
public record OperacionBloqueadaDto(
        Long bloqueoId,
        String pedimentoExportacion,
        String claveExportacion,
        LocalDateTime fechaExportacion,
        String producto,
        BigDecimal cantidadProducto,
        String fraccionProducto,
        Long psalidaKey,
        String pedimentoImportacion,
        String claveImportacion,
        LocalDateTime fechaImportacion,
        String material,
        BigDecimal cantidadMaterial,
        String fraccionMaterial,
        Long partidaKey,
        BigDecimal incorporado,
        BigDecimal desperdicio,
        BigDecimal merma,
        LocalDateTime fechaBloqueo,
        Integer folio) {

    public static OperacionBloqueadaDto from(OperacionBloqueada item) {
        return new OperacionBloqueadaDto(item.bloqueoId(), item.pedimentoExportacion(), item.claveExportacion(),
                item.fechaExportacion(), item.producto(), item.cantidadProducto(), item.fraccionProducto(),
                item.psalidaKey(), item.pedimentoImportacion(), item.claveImportacion(), item.fechaImportacion(),
                item.material(), item.cantidadMaterial(), item.fraccionMaterial(), item.partidaKey(),
                item.incorporado(), item.desperdicio(), item.merma(), item.fechaBloqueo(), item.folio());
    }
}
