package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Vencimiento;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para vencimientos de desperdicio. */
public interface VencimientoRepository {
    Pagina<Vencimiento> findPage(String filtro, int pagina, int tamano);
}
