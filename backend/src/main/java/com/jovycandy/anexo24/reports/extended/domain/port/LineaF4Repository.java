package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para líneas dirigidas F4 (CTM APAA y desperdicio). */
public interface LineaF4Repository {
    Pagina<LineaF4> findPage(String filtro, int pagina, int tamano);
}
