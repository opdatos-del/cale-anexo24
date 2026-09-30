package com.jovycandy.anexo24.catalogs.businessparties.clients.api.dto;

import com.jovycandy.anexo24.catalogs.businessparties.clients.domain.model.Cliente;
/** Respuesta de cliente. */
public record ClienteDto(Long clientekey, String clave, String nombre, String idfiscal, String pais, String correo) { public static ClienteDto from(Cliente cliente) { return new ClienteDto(cliente.clientekey(), cliente.clave(), cliente.nombre(), cliente.idfiscal(), cliente.pais(), cliente.correo()); } }
