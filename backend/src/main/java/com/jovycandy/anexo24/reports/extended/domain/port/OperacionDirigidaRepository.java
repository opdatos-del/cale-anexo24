package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para líneas de salida dirigidas. */
public interface OperacionDirigidaRepository {
    Pagina<OperacionDirigida> findPage(String filtro, int pagina, int tamano);
}
