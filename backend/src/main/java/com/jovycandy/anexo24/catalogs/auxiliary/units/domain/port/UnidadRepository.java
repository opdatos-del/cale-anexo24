package com.jovycandy.anexo24.catalogs.auxiliary.units.domain.port;

import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.model.Unidad;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only del catálogo de unidades. */
public interface UnidadRepository {
    Pagina<Unidad> findPage(String filtro, int pagina, int tamano);
}
