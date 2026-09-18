package com.jovycandy.anexo24.catalogs.materials.application.query;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.catalogs.materials.domain.port.MaterialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del caso de uso de consulta de materiales. */
@ExtendWith(MockitoExtension.class)
class ListarMaterialesUseCaseTest {

    @Mock
    private MaterialRepository materialRepository;

    @Test
    void normalizaLosLimitesDePaginacion() {
        Pagina<Material> esperado = new Pagina<>(List.of(), 0, 1, 100);
        when(materialRepository.findPage(" tornillo ", 1, 100)).thenReturn(esperado);

        Pagina<Material> resultado = new ListarMaterialesUseCase(materialRepository)
                .ejecutar(" tornillo ", 0, 500);

        assertThat(resultado).isSameAs(esperado);
        verify(materialRepository).findPage(" tornillo ", 1, 100);
    }

    @Test
    void conservaUnaPaginacionValida() {
        Pagina<Material> esperado = new Pagina<>(List.of(), 0, 2, 20);
        when(materialRepository.findPage(null, 2, 20)).thenReturn(esperado);

        Pagina<Material> resultado = new ListarMaterialesUseCase(materialRepository)
                .ejecutar(null, 2, 20);

        assertThat(resultado).isSameAs(esperado);
        verify(materialRepository).findPage(null, 2, 20);
    }
}
