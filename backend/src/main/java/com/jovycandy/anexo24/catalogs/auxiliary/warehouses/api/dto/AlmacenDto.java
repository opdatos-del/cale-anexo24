package com.jovycandy.anexo24.catalogs.auxiliary.warehouses.api.dto;

import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.model.Almacen;

/** Almacén expuesto por la API de catálogos auxiliares. */
public record AlmacenDto(Long almacenKey, String clave, String descripcion) {
    public static AlmacenDto from(Almacen almacen) {
        return new AlmacenDto(almacen.almacenKey(), almacen.clave(), almacen.descripcion());
    }
}
