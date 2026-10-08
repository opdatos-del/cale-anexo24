package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.CompulsaDetalle;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para el detalle de compulsa. */
public interface CompulsaDetalleRepository {
    Pagina<CompulsaDetalle> findPage(String filtro, int pagina, int tamano);
}
