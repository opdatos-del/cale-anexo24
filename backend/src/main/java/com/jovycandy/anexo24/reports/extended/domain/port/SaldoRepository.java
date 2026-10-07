package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.Instant;

/** Puerto de consulta read-only para el reporte legacy Saldos (LEGACY-038). */
public interface SaldoRepository {
    Pagina<Saldo> findPage(Instant desde, Instant hasta, String documento,
                           int pagina, int tamano);
}
