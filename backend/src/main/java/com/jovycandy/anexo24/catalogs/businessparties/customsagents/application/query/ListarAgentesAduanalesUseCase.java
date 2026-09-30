package com.jovycandy.anexo24.catalogs.businessparties.customsagents.application.query;

import com.jovycandy.anexo24.catalogs.auxiliary.application.PaginacionCatalogo;
import com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.model.AgenteAduanal;
import com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.port.AgenteAduanalRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Consulta paginada de agente aduanal. */
@Service
public class ListarAgentesAduanalesUseCase {
    private final AgenteAduanalRepository repository;
    public ListarAgentesAduanalesUseCase(AgenteAduanalRepository repository) { this.repository = repository; }
    public Pagina<AgenteAduanal> ejecutar(String filtro, int pagina, int tamano) {
        PaginacionCatalogo.validar(pagina, tamano);
        return repository.findPage(PaginacionCatalogo.normalizarFiltro(filtro), pagina, tamano);
    }
}
