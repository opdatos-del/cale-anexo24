package com.jovycandy.anexo24.billing.domain.port;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionDetalle;

import java.util.Optional;

public interface CargaFacturacionRepository {
    boolean existsByHash(String hash);
    long save(ArchivoFacturacion archivo, long usuarioId, String correlationId);
    long save(ArchivoFacturacion archivo, long usuarioId, String correlationId, String filasJson);
    Optional<CargaFacturacionDetalle> findById(long id, int pagina, int tamano);
}
