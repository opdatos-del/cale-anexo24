package com.jovycandy.anexo24.reports.extended.domain.model;

/**
 * Relación de rectificación persistida (v_rectificaciones). Sin llave física: la vista ya aplica DISTINCT.
 * {@code status} conserva literalmente la semántica legacy (p. ej. "CUIDADO AMBOS DESCARGAN").
 */
public record RectificacionDetalle(
        String pedimento, String clavePedimento, String descarga, String pedimentoOriginal,
        String existePedimento, String clavePedimentoOriginal, String descargaOriginal, String status) {
}
