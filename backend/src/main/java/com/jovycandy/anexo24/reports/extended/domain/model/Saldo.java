
package com.jovycandy.anexo24.reports.extended.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entrada persistida del reporte legacy Saldos (LEGACY-038), materializada desde
 * dbo.PR_INFORME_SALDOS a traves de dbo.APP24_Q_SALDOS_LISTAR.
 *
 * <p>Grano: una fila por partida coincidente (Importaciones.Join >= Partidas) con
 * categoria asignada; las subconsultas a Descarga agregan desperdicio y
 * saldo desperdicio por partidakey. La multiplicidad de Categorias.categoria
 * no esta demostrada como PK/UNIQUE; se documenta como riesgo potencial
 * (CATEGORY_JOIN_MULTIPLICATION_RISK = NOT_CONFIRMED).</p>
 *
 * <p>Campos monetarios/cantidades provenientes de FLOAT legacy se modelan como
 * BigDecimal para evitar perdida de precision; row preservada para no perder
 * columnas del contrato legacy.</p>
 */
public record Saldo(
        String documento,
        Instant fechaPago,
        String clavePedimento,
        String tipoOperacion,
        BigDecimal tcMonetaria,
        String clave,
        String descripcion,
        String fraccion,
        BigDecimal cantImportado,
        String unidad,
        BigDecimal saldo,
        BigDecimal valorAduanalDeSaldo,
        BigDecimal valorDolaresDelSaldo,
        String paisOrigen,
        Integer temporalidadMeses,
        String categoria,
        Instant fechaVencimiento,
        String pedimentoOriginal,
        String descarga,
        String lote,
        String complemento1,
        String complemento2,
        String complemento3,
        BigDecimal desperdiciado,
        BigDecimal saldodesperdicio,
        String cove,
        String factura,
        String tipoMaterial,
        BigDecimal puVad,
        BigDecimal puVdo,
        BigDecimal valAduanal,
        BigDecimal valDolares,
        BigDecimal saldoEnUMT,
        String unidadt,
        BigDecimal valorEnPesos,
        BigDecimal saldoEnValorPesos,
        String nico) {
}
