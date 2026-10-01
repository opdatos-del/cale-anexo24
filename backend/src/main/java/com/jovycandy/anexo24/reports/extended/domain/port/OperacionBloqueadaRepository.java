package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionBloqueada;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta del snapshot histórico de operaciones bloqueadas. */
public interface OperacionBloqueadaRepository {
    Pagina<OperacionBloqueada> findPage(String filtro, int pagina, int tamano);
}
