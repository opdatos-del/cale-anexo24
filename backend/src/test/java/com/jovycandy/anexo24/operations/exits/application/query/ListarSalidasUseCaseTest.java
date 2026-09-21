package com.jovycandy.anexo24.operations.exits.application.query;

import com.jovycandy.anexo24.operations.exits.domain.model.SalidaLinea;
import com.jovycandy.anexo24.operations.exits.domain.port.SalidaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del caso de uso de consulta de Salidas. */
@ExtendWith(MockitoExtension.class)
class ListarSalidasUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 10, 31);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 18);

    @Mock
    private SalidaRepository salidaRepository;

    @Test
    void rechazaDesdeNulo() {
        assertThatThrownBy(() -> ejecutar(null, HASTA, null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaHastaNulo() {
        assertThatThrownBy(() -> ejecutar(DESDE, null, null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaDesdePosteriorAHasta() {
        assertThatThrownBy(() -> ejecutar(HASTA, DESDE, null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void aceptaMismoDia() {
        Pagina<SalidaLinea> esperado = paginaVacia(DESDE, DESDE, 1, 20);
        when(salidaRepository.findPage(DESDE, DESDE, null, null, null, null, 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, DESDE, null, null, null, null, 1, 20))
                .isSameAs(esperado);
    }

    @Test
    void aceptaFechaMaximaDeSqlServerYLaEnviaAlRepository() {
        LocalDate fechaMaxima = LocalDate.of(9999, 12, 31);
        Pagina<SalidaLinea> esperado = paginaVacia(fechaMaxima, fechaMaxima, 1, 20);
        when(salidaRepository.findPage(fechaMaxima, fechaMaxima,
                null, null, null, null, 1, 20)).thenReturn(esperado);

        assertThat(ejecutar(fechaMaxima, fechaMaxima,
                null, null, null, null, 1, 20)).isSameAs(esperado);
        verify(salidaRepository).findPage(fechaMaxima, fechaMaxima,
                null, null, null, null, 1, 20);
    }

    @Test
    void aceptaTamanoMaximoYPaginaGrande() {
        Pagina<SalidaLinea> esperado = paginaVacia(DESDE, HASTA, Integer.MAX_VALUE, 100);
        when(salidaRepository.findPage(DESDE, HASTA, null, null, null, null,
                Integer.MAX_VALUE, 100)).thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, null, null, null, null,
                Integer.MAX_VALUE, 100)).isSameAs(esperado);
        verify(salidaRepository).findPage(DESDE, HASTA, null, null, null, null,
                Integer.MAX_VALUE, 100);
    }

    @Test
    void rechazaPaginaCero() {
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, null, null, null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoCero() {
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, null, null, null, null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMayorQueCien() {
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, null, null, null, null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void normalizaFiltrosBlankYTrimSinEliminarElRango() {
        Pagina<SalidaLinea> esperado = paginaVacia(DESDE, HASTA, 1, 20);
        when(salidaRepository.findPage(DESDE, HASTA,
                "190-1562-5001241", "F4", "17019999", "300099", 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA,
                " 190-1562-5001241 ", " F4 ", " 17019999 ", " 300099 ", 1, 20))
                .isSameAs(esperado);
        verify(salidaRepository).findPage(DESDE, HASTA,
                "190-1562-5001241", "F4", "17019999", "300099", 1, 20);
    }

    @Test
    void normalizaFiltrosNulosVaciosYBlank() {
        Pagina<SalidaLinea> esperado = paginaVacia(DESDE, HASTA, 1, 20);
        when(salidaRepository.findPage(DESDE, HASTA, null, null, null, null, 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, null, "", "   ", null, 1, 20))
                .isSameAs(esperado);
        verify(salidaRepository).findPage(DESDE, HASTA, null, null, null, null, 1, 20);
    }

    @Test
    void aceptaLongitudesMaximasDeFiltros() {
        String pedimento = "1".repeat(60);
        String clavePedimento = "1".repeat(5);
        String fraccion = "1".repeat(12);
        String numeroParte = "1".repeat(50);
        Pagina<SalidaLinea> esperado = paginaVacia(DESDE, HASTA, 1, 20);
        when(salidaRepository.findPage(DESDE, HASTA, pedimento, clavePedimento,
                fraccion, numeroParte, 1, 20)).thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, pedimento, clavePedimento,
                fraccion, numeroParte, 1, 20)).isSameAs(esperado);
        verify(salidaRepository).findPage(DESDE, HASTA, pedimento, clavePedimento,
                fraccion, numeroParte, 1, 20);
    }

    @Test
    void rechazaPedimentoDe61CaracteresYSinLlamarRepository() {
        assertFiltroInvalido("1".repeat(61), null, null, null);
    }

    @Test
    void rechazaClavePedimentoDe6CaracteresYSinLlamarRepository() {
        assertFiltroInvalido(null, "1".repeat(6), null, null);
    }

    @Test
    void rechazaFraccionDe13CaracteresYSinLlamarRepository() {
        assertFiltroInvalido(null, null, "1".repeat(13), null);
    }

    @Test
    void rechazaNumeroParteDe51CaracteresYSinLlamarRepository() {
        assertFiltroInvalido(null, null, null, "1".repeat(51));
    }

    @Test
    void recortaAntesDeValidarLongitud() {
        String clavePedimentoConEspacios = " " + "1".repeat(5) + " ";
        Pagina<SalidaLinea> esperado = paginaVacia(DESDE, HASTA, 1, 20);
        when(salidaRepository.findPage(DESDE, HASTA, null, "11111", null, null, 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, null, clavePedimentoConEspacios,
                null, null, 1, 20)).isSameAs(esperado);
        verify(salidaRepository).findPage(DESDE, HASTA,
                null, "11111", null, null, 1, 20);
    }

    private void assertFiltroInvalido(String pedimento, String clavePedimento,
                                      String fraccion, String numeroParte) {
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, pedimento, clavePedimento,
                fraccion, numeroParte, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(salidaRepository);
    }

    private Pagina<SalidaLinea> ejecutar(LocalDate desde, LocalDate hasta, String pedimento,
                                         String clavePedimento, String fraccion, String numeroParte,
                                         int pagina, int tamano) {
        return new ListarSalidasUseCase(salidaRepository).ejecutar(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano);
    }

    private Pagina<SalidaLinea> paginaVacia(LocalDate desde, LocalDate hasta,
                                            int pagina, int tamano) {
        return new Pagina<>(List.of(), 0, pagina, tamano);
    }
}
