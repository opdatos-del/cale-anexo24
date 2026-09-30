package com.jovycandy.anexo24.catalogs.businessparties.providers.domain.port;

import com.jovycandy.anexo24.catalogs.businessparties.providers.domain.model.Proveedor;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto read-only de proveedor. */
public interface ProveedorRepository { Pagina<Proveedor> findPage(String filtro, int pagina, int tamano); }
