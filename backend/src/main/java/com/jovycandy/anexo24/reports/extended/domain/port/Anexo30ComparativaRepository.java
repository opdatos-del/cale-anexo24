package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Comparativa;
import com.jovycandy.anexo24.shared.api.Pagina;

public interface Anexo30ComparativaRepository {
    Pagina<Anexo30Comparativa> findPage(String filtro, int pagina, int tamano);
}
