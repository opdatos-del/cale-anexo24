package com.jovycandy.anexo24.operations.fixedassets.domain.port;

import com.jovycandy.anexo24.operations.fixedassets.domain.model.ActivoFijo;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.LocalDate;

/** Puerto de consulta de partidas de importación marcadas como activos fijos. */
public interface ActivoFijoRepository {

    /**
     * Consulta activos fijos con rango opcional en pareja y filtros acumulativos.
     *
     * @param desde fecha inicial inclusiva de importación; puede ser nula junto con hasta
     * @param hasta fecha final inclusiva de importación; puede ser nula junto con desde
     * @param pedimento número de pedimento opcional
     * @param clavePedimento clave de pedimento opcional
     * @param numeroParte número de parte opcional
     * @param descripcion descripción histórica opcional
     * @param serie número de serie histórico opcional
     * @param marca marca histórica opcional
     * @param modelo modelo histórico opcional
     * @param pagina número de página base 1
     * @param tamano tamaño de página
     * @return página de partidas activas y total de coincidencias
     */
    Pagina<ActivoFijo> findPage(LocalDate desde, LocalDate hasta, String pedimento,
                                String clavePedimento, String numeroParte, String descripcion,
                                String serie, String marca, String modelo, int pagina, int tamano);
}
