package com.jovycandy.anexo24.reports.extended.domain.model;

import java.time.LocalDateTime;

/** Comparación general entre pedimentos de glosa y registros de Anexo 24. */
public record Compulsa(
        String pedimentoGlosa,
        String pedimentoAnexo24,
        LocalDateTime fechaGlosa,
        LocalDateTime fechaAnexo24,
        String claveGlosa,
        String claveAnexo24,
        String fraccionGlosa,
        String fraccionAnexo24) {
}
