package com.jovycandy.anexo24.reports.extended.api.dto;

import com.jovycandy.anexo24.reports.extended.domain.model.Compulsa;
import java.time.LocalDateTime;

/** Representación API de una comparación de compulsa. */
public record CompulsaDto(String pedimentoGlosa, String pedimentoAnexo24,
        LocalDateTime fechaGlosa, LocalDateTime fechaAnexo24,
        String claveGlosa, String claveAnexo24,
        String fraccionGlosa, String fraccionAnexo24) {
    public static CompulsaDto from(Compulsa item) {
        return new CompulsaDto(item.pedimentoGlosa(), item.pedimentoAnexo24(),
                item.fechaGlosa(), item.fechaAnexo24(), item.claveGlosa(),
                item.claveAnexo24(), item.fraccionGlosa(), item.fraccionAnexo24());
    }
}
