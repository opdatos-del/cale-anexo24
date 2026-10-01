package com.jovycandy.anexo24.catalogs.generaldata.domain.model;

/** Datos empresariales generales consultados desde el maestro legacy. */
public record DatosGenerales(
        String razonSocial,
        String rfc,
        String registroImmex,
        String domicilioFiscal) {
}
