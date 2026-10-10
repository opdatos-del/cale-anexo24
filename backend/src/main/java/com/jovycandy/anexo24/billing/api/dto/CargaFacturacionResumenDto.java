package com.jovycandy.anexo24.billing.api.dto;

import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionResumen;

import java.time.LocalDateTime;

public record CargaFacturacionResumenDto(long id, String archivo, String hash, LocalDateTime fecha,
                                         String estado, int totalRegistros, int registrosValidos,
                                         int registrosInvalidos) {
    public static CargaFacturacionResumenDto from(CargaFacturacionResumen carga) {
        return new CargaFacturacionResumenDto(carga.id(), carga.archivo(), carga.hash(), carga.fecha(),
                carga.estadoPersistido().name(), carga.totalRegistros(), carga.registrosValidos(), carga.registrosInvalidos());
    }
}
