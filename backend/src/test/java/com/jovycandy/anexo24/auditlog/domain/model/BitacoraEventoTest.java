package com.jovycandy.anexo24.auditlog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/** Pruebas de invariantes del evento interno de Bitácora. */
class BitacoraEventoTest {

    @Test
    void creaEventoValido() {
        BitacoraEvento evento = evento(42L, "Carga validada", "req-01");

        assertThat(evento.usuarioId()).isEqualTo(42L);
        assertThat(evento.modulo()).isEqualTo(BitacoraModulo.SEGURIDAD);
        assertThat(evento.accion()).isEqualTo(BitacoraAccion.LOGIN_OK);
        assertThat(evento.resultado()).isEqualTo(BitacoraResultado.EXITO);
    }

    @Test
    void permiteUsuarioNuloParaEventoTecnicoOAnonimo() {
        assertThat(evento(null, null, null).usuarioId()).isNull();
    }

    @Test
    void rechazaUsuarioNoPositivo() {
        assertThatIllegalArgumentException().isThrownBy(() -> evento(0L, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> evento(-1L, null, null));
    }

    @Test
    void permiteDetalleDeQuinientosCaracteres() {
        assertThat(evento(null, "a".repeat(500), null).detalle()).hasSize(500);
    }

    @Test
    void rechazaDetalleMayorAQuinientosCaracteres() {
        assertThatIllegalArgumentException().isThrownBy(() -> evento(null, "a".repeat(501), null));
    }

    @Test
    void permiteCorrelationIdValido() {
        assertThat(evento(null, null, "abc-123._XYZ").correlationId()).isEqualTo("abc-123._XYZ");
    }

    @Test
    void rechazaCorrelationIdMayorA40Caracteres() {
        assertThatIllegalArgumentException().isThrownBy(() -> evento(null, null, "a".repeat(41)));
    }

    @Test
    void rechazaCorrelationIdConCaracteresInvalidos() {
        assertThatIllegalArgumentException().isThrownBy(() -> evento(null, null, "id con espacio"));
    }

    @Test
    void rechazaModuloNulo() {
        assertThatNullPointerException().isThrownBy(() -> new BitacoraEvento(
                null, null, BitacoraAccion.LOGIN_OK, BitacoraResultado.EXITO, null, null));
    }

    @Test
    void rechazaAccionNula() {
        assertThatNullPointerException().isThrownBy(() -> new BitacoraEvento(
                null, BitacoraModulo.SEGURIDAD, null, BitacoraResultado.EXITO, null, null));
    }

    @Test
    void rechazaResultadoNulo() {
        assertThatNullPointerException().isThrownBy(() -> new BitacoraEvento(
                null, BitacoraModulo.SEGURIDAD, BitacoraAccion.LOGIN_OK, null, null, null));
    }

    private BitacoraEvento evento(Long usuarioId, String detalle, String correlationId) {
        return new BitacoraEvento(
                usuarioId,
                BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK,
                BitacoraResultado.EXITO,
                detalle,
                correlationId);
    }
}
