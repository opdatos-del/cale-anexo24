package com.jovycandy.anexo24.operations.entries.application.query;

import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
import com.jovycandy.anexo24.operations.entries.domain.port.EntradaRepository;
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
import static org.mockito.Mockito.when;

/** Pruebas unitarias del caso de uso de consulta de Entradas. */
@ExtendWith(MockitoExtension.class)
class ListarEntradasUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 9, 23);
    private static final LocalDate HASTA = LocalDate.of(2026, 5, 28);

    @Mock
    private EntradaRepository entradaRepository;

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
        Pagina<EntradaLinea> esperado = paginaVacia(DESDE, DESDE, 1, 20);
        when(entradaRepository.findPage(DESDE, DESDE, null, null, null, null, 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, DESDE, null, null, null, null, 1, 20))
                .isSameAs(esperado);
    }

    @Test
    void aceptaFechaMaximaDeSqlServerYLaEnviaAlRepository() {
        LocalDate fechaMaxima = LocalDate.of(9999, 12, 31);
        Pagina<EntradaLinea> esperado = paginaVacia(fechaMaxima, fechaMaxima, 1, 20);
        when(entradaRepository.findPage(fechaMaxima, fechaMaxima,
                null, null, null, null, 1, 20)).thenReturn(esperado);

        assertThat(ejecutar(fechaMaxima, fechaMaxima,
                null, null, null, null, 1, 20)).isSameAs(esperado);
        verify(entradaRepository).findPage(fechaMaxima, fechaMaxima,
                null, null, null, null, 1, 20);
    }

    @Test
    void aceptaTamanoMaximoYPaginaGrande() {
        Pagina<EntradaLinea> esperado = paginaVacia(DESDE, HASTA, Integer.MAX_VALUE, 100);
        when(entradaRepository.findPage(DESDE, HASTA, null, null, null, null,
                Integer.MAX_VALUE, 100)).thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, null, null, null, null, Integer.MAX_VALUE, 100))
                .isSameAs(esperado);
        verify(entradaRepository).findPage(DESDE, HASTA, null, null, null, null,
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
        Pagina<EntradaLinea> esperado = paginaVacia(DESDE, HASTA, 1, 20);
        when(entradaRepository.findPage(DESDE, HASTA,
                "5003971", "A1", "17019999", "500017", 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA,
                " 5003971 ", " A1 ", " 17019999 ", " 500017 ", 1, 20))
                .isSameAs(esperado);
        verify(entradaRepository).findPage(DESDE, HASTA,
                "5003971", "A1", "17019999", "500017", 1, 20);
    }

    @Test
    void normalizaFiltrosNulosVaciosYBlank() {
        Pagina<EntradaLinea> esperado = paginaVacia(DESDE, HASTA, 1, 20);
        when(entradaRepository.findPage(DESDE, HASTA, null, null, null, null, 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, null, "", "   ", null, 1, 20))
                .isSameAs(esperado);
        verify(entradaRepository).findPage(DESDE, HASTA, null, null, null, null, 1, 20);
    }

    private Pagina<EntradaLinea> ejecutar(LocalDate desde, LocalDate hasta, String pedimento,
                                          String clavePedimento, String fraccion, String numeroParte,
                                          int pagina, int tamano) {
        return new ListarEntradasUseCase(entradaRepository).ejecutar(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano);
    }

    private Pagina<EntradaLinea> paginaVacia(LocalDate desde, LocalDate hasta,
                                             int pagina, int tamano) {
        return new Pagina<>(List.of(), 0, pagina, tamano);
    }
}
