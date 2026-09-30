package com.jovycandy.anexo24.catalogs.businessparties.customsagents.api.dto;

import com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.model.AgenteAduanal;
/** Respuesta de agente aduanal. */
public record AgenteAduanalDto(String clave, String nombre, String rfc, String patente, String agenciaAduanal) { public static AgenteAduanalDto from(AgenteAduanal agenteAduanal) { return new AgenteAduanalDto(agenteAduanal.clave(), agenteAduanal.nombre(), agenteAduanal.rfc(), agenteAduanal.patente(), agenteAduanal.agenciaAduanal()); } }
