package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Entrada;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta read-only para entradas A31 (Revision Anexo 30). */
public interface Anexo30EntradaRepository {
    Pagina<Anexo30Entrada> findPage(String filtro, int pagina, int tamano);
}
