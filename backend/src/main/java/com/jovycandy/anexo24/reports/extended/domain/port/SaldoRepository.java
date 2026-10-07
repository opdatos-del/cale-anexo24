package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.LocalDate;

/** Puerto de consulta read-only para el reporte legacy Saldos (LEGACY-038). */
public interface SaldoRepository {
    Pagina<Saldo> findPage(LocalDate desde, LocalDate hasta, String documento,
                           int pagina, int tamano);
}
