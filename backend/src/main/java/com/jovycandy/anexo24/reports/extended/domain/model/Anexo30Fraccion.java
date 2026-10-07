package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;

/** Registro persistido de fracción utilizado por el proceso Anexo 30.
 *
 * <p>El grano corresponde a una fila física de {@code dbo.A31_DESCARGASF},
 * identificada por {@code A31_FRACCIONKEY}. El origen y ciclo de vida de estos
 * registros no están disponibles en el dump del proyecto.</p>
 */
public record Anexo30Fraccion(
        long fraccionKey,
        String tipo,
        String clavePedimento,
        String ejercicio,
        String periodo,
        String fraccion,
        BigDecimal valor,
        String af,
        String archivo) {
}
