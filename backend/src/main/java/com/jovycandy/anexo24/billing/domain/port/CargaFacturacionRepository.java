package com.jovycandy.anexo24.billing.domain.port;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionDetalle;
import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionResumen;
import com.jovycandy.anexo24.billing.domain.model.EstadoCargaFacturacionPersistida;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.LocalDate;
import java.util.Optional;

public interface CargaFacturacionRepository {
    boolean existsByHash(String hash);
    long save(ArchivoFacturacion archivo, long usuarioId, String correlationId);
    long save(ArchivoFacturacion archivo, long usuarioId, String correlationId, String filasJson);
    Optional<CargaFacturacionDetalle> findById(long id, long usuarioId, int pagina, int tamano);
    Pagina<CargaFacturacionResumen> buscar(long usuarioId, EstadoCargaFacturacionPersistida estado,
                                            LocalDate desde, LocalDate hasta, int pagina, int tamano);
}
