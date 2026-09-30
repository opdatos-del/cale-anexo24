package com.jovycandy.anexo24.catalogs.businessparties.clients.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.businessparties.clients.domain.model.Cliente;
import com.jovycandy.anexo24.catalogs.businessparties.clients.domain.port.ClienteRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Consulta paginada de cliente. */
@Service
public class ListarClientesUseCase {
    private final ClienteRepository repository;
    public ListarClientesUseCase(ClienteRepository repository) { this.repository = repository; }
    public Pagina<Cliente> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
