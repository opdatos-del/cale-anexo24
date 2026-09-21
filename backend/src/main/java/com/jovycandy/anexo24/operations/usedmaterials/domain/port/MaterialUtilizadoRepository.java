package com.jovycandy.anexo24.operations.usedmaterials.domain.port;

import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.LocalDate;

/** Puerto de consulta del histórico de materiales utilizados. */
public interface MaterialUtilizadoRepository {

    /**
     * Consulta filas históricas de descarga con filtros acumulativos.
     *
     * @param desde fecha inicial inclusiva sobre la salida
     * @param hasta fecha final inclusiva sobre la salida
     * @param material código de material opcional
     * @param producto código de producto opcional
     * @param pedimentoSalida documento de salida opcional
     * @param clavePedimentoSalida clave de pedimento de salida opcional
     * @param pagina número de página base 1
     * @param tamano tamaño de página
     * @return página de filas históricas y total de coincidencias
     */
    Pagina<MaterialUtilizado> findPage(LocalDate desde, LocalDate hasta, String material,
                                       String producto, String pedimentoSalida,
                                       String clavePedimentoSalida, int pagina, int tamano);
}
