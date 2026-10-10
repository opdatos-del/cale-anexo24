package com.jovycandy.anexo24.billing.domain.model;

import java.time.LocalDateTime;

/** Metadatos paginados de una carga de facturaciÃ³n propiedad del usuario autenticado. */
public record CargaFacturacionResumen(
        long id,
        String archivo,
        String hash,
        LocalDateTime fecha,
        EstadoCargaFacturacionPersistida estadoPersistido,
        int totalRegistros,
        int registrosValidos,
        int registrosInvalidos) {
}
