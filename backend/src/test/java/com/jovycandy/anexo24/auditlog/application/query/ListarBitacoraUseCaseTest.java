package com.jovycandy.anexo24.auditlog.application.query;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del caso de uso read-only de Bitácora. */
@ExtendWith(MockitoExtension.class)
class ListarBitacoraUseCaseTest {

    private static final Instant DESDE = Instant.parse("2026-09-22T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-09-22T23:59:59Z");

    @Mock
    private BitacoraConsultaRepository bitacoraConsultaRepository;

    @Test
    void delegaFiltrosExactosValidos() {
        Pagina<BitacoraRegistro> esperado = new Pagina<>(List.of(), 0L, 2, 50);
        when(bitacoraConsultaRepository.findPage(
                DESDE, HASTA, 42L, BitacoraModulo.SEGURIDAD, BitacoraResultado.EXITO,
                "req-123", 2, 50)).thenReturn(esperado);

        Pagina<BitacoraRegistro> resultado = useCase().ejecutar(
                DESDE, HASTA, 42L, BitacoraModulo.SEGURIDAD, BitacoraResultado.EXITO,
                " req-123 ", 2, 50);

        assertThat(resultado).isSameAs(esperado);
        verify(bitacoraConsultaRepository).findPage(
                DESDE, HASTA, 42L, BitacoraModulo.SEGURIDAD, BitacoraResultado.EXITO,
                "req-123", 2, 50);
    }

    @Test
    void normalizaCorrelationIdBlankANull() {
        Pagina<BitacoraRegistro> esperado = new Pagina<>(List.of(), 0L, 1, 20);
        when(bitacoraConsultaRepository.findPage(
                DESDE, HASTA, null, null, null, null, 1, 20)).thenReturn(esperado);

        assertThat(useCase().ejecutar(DESDE, HASTA, null, null, null, "   ", 1, 20))
                .isSameAs(esperado);
        verify(bitacoraConsultaRepository).findPage(
                DESDE, HASTA, null, null, null, null, 1, 20);
    }

    @Test
    void rechazaDesdeNulo() {
        assertInvalido(null, HASTA, null, 1, 20);
    }

    @Test
    void rechazaHastaNulo() {
        assertInvalido(DESDE, null, null, 1, 20);
    }

    @Test
    void rechazaDesdePosteriorAHasta() {
        assertInvalido(HASTA, DESDE, null, 1, 20);
    }

    @Test
    void rechazaUsuarioIdCeroONegativo() {
        assertInvalido(DESDE, HASTA, "req-123", 1, 20, 0L);
        assertInvalido(DESDE, HASTA, "req-123", 1, 20, -1L);
    }

    @Test
    void rechazaPaginaCero() {
        assertInvalido(DESDE, HASTA, null, 0, 20);
    }

    @Test
    void rechazaTamanoFueraDeRango() {
        assertInvalido(DESDE, HASTA, null, 1, 0);
        assertInvalido(DESDE, HASTA, null, 1, 101);
    }

    @Test
    void rechazaCorrelationIdDeMasDeCuarentaCaracteres() {
        assertInvalido(DESDE, HASTA, "a".repeat(41), 1, 20);
    }

    @Test
    void rechazaCorrelationIdConCaracteresInvalidos() {
        assertInvalido(DESDE, HASTA, "req/123", 1, 20);
    }

    private ListarBitacoraUseCase useCase() {
        return new ListarBitacoraUseCase(bitacoraConsultaRepository);
    }

    private void assertInvalido(Instant desde, Instant hasta, String correlationId, int pagina, int tamano) {
        assertInvalido(desde, hasta, correlationId, pagina, tamano, null);
    }

    private void assertInvalido(
            Instant desde, Instant hasta, String correlationId, int pagina, int tamano, Long usuarioId) {
        assertThatThrownBy(() -> useCase().ejecutar(
                desde, hasta, usuarioId, null, null, correlationId, pagina, tamano))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(bitacoraConsultaRepository);
    }
}
