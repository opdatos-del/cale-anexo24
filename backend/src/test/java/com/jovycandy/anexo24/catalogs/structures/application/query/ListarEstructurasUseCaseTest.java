package com.jovycandy.anexo24.catalogs.structures.application.query;

import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;
import com.jovycandy.anexo24.catalogs.structures.domain.port.EstructuraRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del caso de uso de consulta de estructuras. */
@ExtendWith(MockitoExtension.class)
class ListarEstructurasUseCaseTest {

    @Mock
    private EstructuraRepository estructuraRepository;

    @Test
    void normalizaProductoYMaterialNulosVaciosYBlank() {
        Pagina<EstructuraDetalle> esperado = paginaVacia(1, 20);
        when(estructuraRepository.findPage(null, null, 1, 20)).thenReturn(esperado);
        ListarEstructurasUseCase useCase = new ListarEstructurasUseCase(estructuraRepository);

        assertThat(useCase.ejecutar(null, null, 1, 20)).isSameAs(esperado);
        assertThat(useCase.ejecutar("", "", 1, 20)).isSameAs(esperado);
        assertThat(useCase.ejecutar("   ", "   ", 1, 20)).isSameAs(esperado);
        verify(estructuraRepository, org.mockito.Mockito.times(3))
                .findPage(null, null, 1, 20);
    }

    @Test
    void recortaProductoYMaterial() {
        Pagina<EstructuraDetalle> esperado = paginaVacia(2, 20);
        when(estructuraRepository.findPage("200060", "MAT-1", 2, 20)).thenReturn(esperado);

        assertThat(new ListarEstructurasUseCase(estructuraRepository)
                .ejecutar("  200060  ", "  MAT-1  ", 2, 20)).isSameAs(esperado);
        verify(estructuraRepository).findPage("200060", "MAT-1", 2, 20);
    }

    @Test
    void aceptaTamanoMaximo() {
        Pagina<EstructuraDetalle> esperado = paginaVacia(1, 100);
        when(estructuraRepository.findPage(null, null, 1, 100)).thenReturn(esperado);

        assertThat(new ListarEstructurasUseCase(estructuraRepository)
                .ejecutar(null, null, 1, 100)).isSameAs(esperado);
        verify(estructuraRepository).findPage(null, null, 1, 100);
    }

    @Test
    void aceptaPaginaGrandeYLaEnviaAlRepository() {
        Pagina<EstructuraDetalle> esperado = paginaVacia(Integer.MAX_VALUE, 100);
        when(estructuraRepository.findPage(null, null, Integer.MAX_VALUE, 100))
                .thenReturn(esperado);

        assertThat(new ListarEstructurasUseCase(estructuraRepository)
                .ejecutar(null, null, Integer.MAX_VALUE, 100)).isSameAs(esperado);
        verify(estructuraRepository).findPage(null, null, Integer.MAX_VALUE, 100);
    }

    @Test
    void rechazaPaginaMenorQueUno() {
        assertThatThrownBy(() -> new ListarEstructurasUseCase(estructuraRepository)
                .ejecutar(null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMenorQueUno() {
        assertThatThrownBy(() -> new ListarEstructurasUseCase(estructuraRepository)
                .ejecutar(null, null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMayorQueCien() {
        assertThatThrownBy(() -> new ListarEstructurasUseCase(estructuraRepository)
                .ejecutar(null, null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private Pagina<EstructuraDetalle> paginaVacia(int pagina, int tamano) {
        return new Pagina<>(List.of(), 0, pagina, tamano);
    }
}
