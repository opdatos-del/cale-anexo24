package com.jovycandy.anexo24.catalogs.generaldata.api.dto;

import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;

/** Datos generales empresariales expuestos exclusivamente para consulta. */
public record DatosGeneralesDto(
        String razonSocial,
        String rfc,
        String registroImmex,
        String domicilioFiscal) {

    public static DatosGeneralesDto from(DatosGenerales datos) {
        return new DatosGeneralesDto(datos.razonSocial(), datos.rfc(), datos.registroImmex(), datos.domicilioFiscal());
    }
}
