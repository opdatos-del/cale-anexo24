package com.jovycandy.anexo24.billing.domain.port;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;

public interface CargaFacturacionRepository {
    boolean existsByHash(String hash);
    long save(ArchivoFacturacion archivo, long usuarioId, String correlationId);
}
