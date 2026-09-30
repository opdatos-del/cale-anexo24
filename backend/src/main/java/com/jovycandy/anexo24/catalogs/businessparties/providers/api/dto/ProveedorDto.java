package com.jovycandy.anexo24.catalogs.businessparties.providers.api.dto;

import com.jovycandy.anexo24.catalogs.businessparties.providers.domain.model.Proveedor;
/** Respuesta de proveedor. */
public record ProveedorDto(Long proveedorkey, String clave, String nombre, String idfiscal, String pais, String correo) { public static ProveedorDto from(Proveedor proveedor) { return new ProveedorDto(proveedor.proveedorkey(), proveedor.clave(), proveedor.nombre(), proveedor.idfiscal(), proveedor.pais(), proveedor.correo()); } }
