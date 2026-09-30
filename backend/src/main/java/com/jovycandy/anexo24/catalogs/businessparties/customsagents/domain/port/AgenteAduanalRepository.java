package com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.port;

import com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.model.AgenteAduanal;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto read-only de agente aduanal. */
public interface AgenteAduanalRepository { Pagina<AgenteAduanal> findPage(String filtro, int pagina, int tamano); }
