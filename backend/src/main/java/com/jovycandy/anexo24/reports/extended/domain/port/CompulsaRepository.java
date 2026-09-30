package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Compulsa;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para la compulsa aduanera. */
public interface CompulsaRepository {
    Pagina<Compulsa> findPage(String filtro, int pagina, int tamano);
}
