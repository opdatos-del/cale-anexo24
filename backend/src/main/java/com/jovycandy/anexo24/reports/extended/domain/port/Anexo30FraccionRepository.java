package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Fraccion;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta paginada de fracciones persistidas de Anexo 30. */
public interface Anexo30FraccionRepository {

    /** Obtiene una página de registros persistidos mediante el contrato SQL read-only. */
    Pagina<Anexo30Fraccion> findPage(String filtro, int pagina, int tamano);
}
