package com.jovycandy.anexo24.catalogs.businessparties.clients.domain.model;

/** Cliente del maestro legacy, consultado sin mutaciones. */
public record Cliente(Long clientekey, String clave, String nombre, String idfiscal, String pais, String correo) {}
