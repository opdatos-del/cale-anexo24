package com.jovycandy.anexo24.catalogs.products.application.query;

import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;
import com.jovycandy.anexo24.catalogs.products.domain.port.ProductoRepository;
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

/** Pruebas unitarias del caso de uso de consulta de Productos. */
@ExtendWith(MockitoExtension.class)
class ListarProductosUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    @Test
    void normalizaFiltroNuloVacioYBlank() {
        Pagina<Producto> esperado = paginaVacia(1, 20);
        when(productoRepository.findPage(null, 1, 20)).thenReturn(esperado);
        ListarProductosUseCase useCase = new ListarProductosUseCase(productoRepository);

        assertThat(useCase.ejecutar(null, 1, 20)).isSameAs(esperado);
        assertThat(useCase.ejecutar("", 1, 20)).isSameAs(esperado);
        assertThat(useCase.ejecutar("   ", 1, 20)).isSameAs(esperado);
        verify(productoRepository, org.mockito.Mockito.times(3)).findPage(null, 1, 20);
    }

    @Test
    void recortaEspaciosDelFiltro() {
        Pagina<Producto> esperado = paginaVacia(2, 20);
        when(productoRepository.findPage("200060", 2, 20)).thenReturn(esperado);

        assertThat(new ListarProductosUseCase(productoRepository)
                .ejecutar("  200060  ", 2, 20)).isSameAs(esperado);
        verify(productoRepository).findPage("200060", 2, 20);
    }

    @Test
    void aceptaTamanoMaximo() {
        Pagina<Producto> esperado = paginaVacia(1, 100);
        when(productoRepository.findPage(null, 1, 100)).thenReturn(esperado);

        assertThat(new ListarProductosUseCase(productoRepository)
                .ejecutar(null, 1, 100)).isSameAs(esperado);
        verify(productoRepository).findPage(null, 1, 100);
    }

    @Test
    void rechazaPaginaMenorQueUno() {
        assertThatThrownBy(() -> new ListarProductosUseCase(productoRepository)
                .ejecutar(null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMenorQueUno() {
        assertThatThrownBy(() -> new ListarProductosUseCase(productoRepository)
                .ejecutar(null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMayorQueCien() {
        assertThatThrownBy(() -> new ListarProductosUseCase(productoRepository)
                .ejecutar(null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private Pagina<Producto> paginaVacia(int pagina, int tamano) {
        return new Pagina<>(List.of(), 0, pagina, tamano);
    }
}
