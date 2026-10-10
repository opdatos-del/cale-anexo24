package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30InventarioInicial;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto read-only del inventario inicial del snapshot Anexo 30. */
public interface Anexo30InventarioInicialRepository {
    Pagina<Anexo30InventarioInicial> findPage(String filtro, int pagina, int tamano);
}
