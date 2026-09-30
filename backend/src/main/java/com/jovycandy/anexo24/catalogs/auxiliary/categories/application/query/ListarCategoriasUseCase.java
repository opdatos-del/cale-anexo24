package com.jovycandy.anexo24.catalogs.auxiliary.categories.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.model.Categoria;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.port.CategoriaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada de categorías. */
@Service
public class ListarCategoriasUseCase {
    private final CategoriaRepository repository;

    public ListarCategoriasUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    public Pagina<Categoria> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
