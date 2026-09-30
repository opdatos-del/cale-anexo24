package com.jovycandy.anexo24.catalogs.businessparties.clients.domain.port;

import com.jovycandy.anexo24.catalogs.businessparties.clients.domain.model.Cliente;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto read-only de cliente. */
public interface ClienteRepository { Pagina<Cliente> findPage(String filtro, int pagina, int tamano); }
