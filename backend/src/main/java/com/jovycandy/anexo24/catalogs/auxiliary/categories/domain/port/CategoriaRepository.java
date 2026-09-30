package com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.port;

import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.model.Categoria;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only del catálogo de categorías. */
public interface CategoriaRepository {
    Pagina<Categoria> findPage(String filtro, int pagina, int tamano);
}
