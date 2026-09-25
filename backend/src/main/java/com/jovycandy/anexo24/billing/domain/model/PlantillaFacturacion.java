package com.jovycandy.anexo24.billing.domain.model;

import java.util.List;

/** Contrato estructural versionado del layout de Facturación. */
public record PlantillaFacturacion(String nombre, String version, String extension, String hoja,
                                   List<Columna> columnas) {
    public PlantillaFacturacion {
        columnas = List.copyOf(columnas);
    }

    public record Columna(String nombre, boolean obligatoria, String tipo) {}

    public List<String> nombres() {
        return columnas.stream().map(Columna::nombre).toList();
    }
}
