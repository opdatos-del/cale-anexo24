package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.RectificacionDetalle;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para el detalle de rectificaciones. */
public interface RectificacionDetalleRepository {
    Pagina<RectificacionDetalle> findPage(String filtro, int pagina, int tamano);
}
