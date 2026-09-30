package com.jovycandy.anexo24.auditlog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Compatibilidad de las acciones de cargas con los mappers basados en enum. */
class BitacoraAccionTest {
    @Test
    void reconoceAccionesDeCargaDeCatalogos() {
        assertThat(BitacoraAccion.valueOf("CARGA_VALIDADA")).isEqualTo(BitacoraAccion.CARGA_VALIDADA);
        assertThat(BitacoraAccion.valueOf("CARGA_CON_ERRORES")).isEqualTo(BitacoraAccion.CARGA_CON_ERRORES);
    }
}
