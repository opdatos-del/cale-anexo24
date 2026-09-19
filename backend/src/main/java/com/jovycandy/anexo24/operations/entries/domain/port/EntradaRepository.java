package com.jovycandy.anexo24.operations.entries.domain.port;

import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.LocalDate;

/** Puerto de consulta de líneas de Entradas mediante el contrato SQL aprobado. */
public interface EntradaRepository {

    /**
     * Consulta líneas de entrada aplicando filtros acumulativos.
     *
     * @param desde fecha inicial inclusiva sobre la fecha de pago
     * @param hasta fecha final inclusiva sobre la fecha de pago
     * @param pedimento número de pedimento opcional
     * @param clavePedimento clave de pedimento opcional
     * @param fraccion fracción arancelaria opcional
     * @param numeroParte número de parte opcional
     * @param pagina número de página base 1
     * @param tamano tamaño de página
     * @return página de líneas de entrada y total de coincidencias
     */
    Pagina<EntradaLinea> findPage(LocalDate desde, LocalDate hasta, String pedimento,
                                   String clavePedimento, String fraccion, String numeroParte,
                                   int pagina, int tamano);
}
