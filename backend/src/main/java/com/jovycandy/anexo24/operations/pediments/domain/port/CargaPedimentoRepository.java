package com.jovycandy.anexo24.operations.pediments.domain.port;

import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoDetalle;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;

import java.util.List;
import java.util.Optional;

/** Puerto de staging durable aislado del módulo legacy. */
public interface CargaPedimentoRepository {
    boolean existsByHash(String hash);

    long save(CargaPedimentoArchivo archivo, long usuarioId, String correlationId);

    Optional<CargaPedimentoDetalle> findById(long id, int pagina, int tamano);

    List<PedimentoError> findErrors(long id, int pagina, int tamano);
}
