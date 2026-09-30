package com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.model.TipoMaterial;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.port.TipoMaterialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada de tipos de material. */
@Service
public class ListarTiposMaterialUseCase {
    private final TipoMaterialRepository repository;

    public ListarTiposMaterialUseCase(TipoMaterialRepository repository) {
        this.repository = repository;
    }

    public Pagina<TipoMaterial> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
