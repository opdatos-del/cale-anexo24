package com.jovycandy.anexo24.catalogs.auxiliary.units.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.model.Unidad;
import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.port.UnidadRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada de unidades. */
@Service
public class ListarUnidadesUseCase {
    private final UnidadRepository repository;

    public ListarUnidadesUseCase(UnidadRepository repository) {
        this.repository = repository;
    }

    public Pagina<Unidad> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
