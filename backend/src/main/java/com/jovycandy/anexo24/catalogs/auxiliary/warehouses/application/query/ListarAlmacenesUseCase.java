package com.jovycandy.anexo24.catalogs.auxiliary.warehouses.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.model.Almacen;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.port.AlmacenRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada de almacenes. */
@Service
public class ListarAlmacenesUseCase {
    private final AlmacenRepository repository;

    public ListarAlmacenesUseCase(AlmacenRepository repository) {
        this.repository = repository;
    }

    public Pagina<Almacen> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
