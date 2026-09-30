package com.jovycandy.anexo24.catalogs.businessparties.providers.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.businessparties.providers.domain.model.Proveedor;
import com.jovycandy.anexo24.catalogs.businessparties.providers.domain.port.ProveedorRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Consulta paginada de proveedor. */
@Service
public class ListarProveedoresUseCase {
    private final ProveedorRepository repository;
    public ListarProveedoresUseCase(ProveedorRepository repository) { this.repository = repository; }
    public Pagina<Proveedor> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
