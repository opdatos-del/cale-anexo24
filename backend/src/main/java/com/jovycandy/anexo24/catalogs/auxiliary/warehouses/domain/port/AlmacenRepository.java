package com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.port;

import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.model.Almacen;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only del catálogo de almacenes. */
public interface AlmacenRepository {
    Pagina<Almacen> findPage(String filtro, int pagina, int tamano);
}
