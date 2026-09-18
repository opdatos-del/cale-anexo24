package com.jovycandy.anexo24.catalogs.materials.application.query;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.catalogs.materials.domain.port.MaterialRepository;
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

/** Pruebas unitarias del caso de uso de consulta de materiales. */
@ExtendWith(MockitoExtension.class)
class ListarMaterialesUseCaseTest {

    @Mock
    private MaterialRepository materialRepository;

    @Test
    void normalizaFiltroNulo() {
        Pagina<Material> esperado = paginaVacia(1, 20);
        when(materialRepository.findPage(null, 1, 20)).thenReturn(esperado);

        assertThat(new ListarMaterialesUseCase(materialRepository)
                .ejecutar(null, 1, 20)).isSameAs(esperado);
        verify(materialRepository).findPage(null, 1, 20);
    }

    @Test
    void normalizaFiltroVacioYBlank() {
        Pagina<Material> esperado = paginaVacia(1, 20);
        when(materialRepository.findPage(null, 1, 20)).thenReturn(esperado);
        ListarMaterialesUseCase useCase = new ListarMaterialesUseCase(materialRepository);

        assertThat(useCase.ejecutar("", 1, 20)).isSameAs(esperado);
        assertThat(useCase.ejecutar("   ", 1, 20)).isSameAs(esperado);
        verify(materialRepository, org.mockito.Mockito.times(2)).findPage(null, 1, 20);
    }

    @Test
    void recortaEspaciosDelFiltro() {
        Pagina<Material> esperado = paginaVacia(2, 20);
        when(materialRepository.findPage("tornillo", 2, 20)).thenReturn(esperado);

        assertThat(new ListarMaterialesUseCase(materialRepository)
                .ejecutar("  tornillo  ", 2, 20)).isSameAs(esperado);
        verify(materialRepository).findPage("tornillo", 2, 20);
    }

    @Test
    void conservaPaginacionValida() {
        Pagina<Material> esperado = paginaVacia(2, 100);
        when(materialRepository.findPage(null, 2, 100)).thenReturn(esperado);

        assertThat(new ListarMaterialesUseCase(materialRepository)
                .ejecutar(null, 2, 100)).isSameAs(esperado);
        verify(materialRepository).findPage(null, 2, 100);
    }

    @Test
    void rechazaPaginaMenorQueUno() {
        assertThatThrownBy(() -> new ListarMaterialesUseCase(materialRepository)
                .ejecutar(null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMenorQueUno() {
        assertThatThrownBy(() -> new ListarMaterialesUseCase(materialRepository)
                .ejecutar(null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaTamanoMayorQueCien() {
        assertThatThrownBy(() -> new ListarMaterialesUseCase(materialRepository)
                .ejecutar(null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private Pagina<Material> paginaVacia(int pagina, int tamano) {
        return new Pagina<>(List.of(), 0, pagina, tamano);
    }
}
