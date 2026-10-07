package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada persistida del snapshot A31 (Revision Anexo 30, LEGACY-073 parcial).
 *
 * <p>Proyeccion directa de dbo.A31_ENTRADAS. El grano es por fila fisica; la
 * columna SALDO refleja el ultimo calculo de DESCARGAS_A31, mientras que el resto
 * de columnas conserva valores originales del pedimento armado y original.</p>
 *
 * <p>La tabla no se trunca por los writers A31, por lo que las filas se conservan
 * entre corridas; lectura read-only sin necesidad de regenerar el snapshot.</p>
 */
public record Anexo30Entrada(
        long entradaKey,
        String descarga,
        String tipoOperacion,
        String pedimento,
        String pedimentoOriginal,
        LocalDateTime fecha,
        LocalDateTime fechaOriginal,
        String clavePedimento,
        String fraccion,
        BigDecimal valorComercial,
        BigDecimal ivaFp21,
        BigDecimal ivaFp22,
        BigDecimal saldo,
        Long operacion,
        String partida,
        String esaf) {
}
