package com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.port;

import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.model.TipoMaterial;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only de tipos de material. */
public interface TipoMaterialRepository {
    Pagina<TipoMaterial> findPage(String filtro, int pagina, int tamano);
}
