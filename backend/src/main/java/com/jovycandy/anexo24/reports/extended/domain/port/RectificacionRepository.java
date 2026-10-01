package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Rectificacion;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para el resumen de rectificaciones. */
public interface RectificacionRepository {
    Pagina<Rectificacion> findPage(String filtro, int pagina, int tamano);
}
