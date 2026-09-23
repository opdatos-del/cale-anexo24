package com.jovycandy.anexo24.administration.profiles.domain.port;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto read-only de consulta administrativa de perfiles. */
public interface PerfilConsultaRepository {

    /**
     * Lista perfiles paginados con filtros opcionales.
     *
     * @param nombre filtro opcional parcial por nombre
     * @param estado filtro opcional exacto por estado
     * @param pagina número de página base 1
     * @param tamano tamaño de página
     * @return página de perfiles administrativos
     */
    Pagina<PerfilAdministracion> findPage(String nombre, String estado, int pagina, int tamano);
}
