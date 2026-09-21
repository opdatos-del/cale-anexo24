package com.jovycandy.anexo24.operations.usedmaterials.application.query;

import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.operations.usedmaterials.domain.port.MaterialUtilizadoRepository;
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

/** Pruebas unitarias del caso de uso de materiales utilizados. */
@ExtendWith(MockitoExtension.class)
class ListarMaterialesUtilizadosUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 10, 31);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 18);

    @Mock
    private MaterialUtilizadoRepository materialUtilizadoRepository;

    @Test
    void rechazaRangoInvalidoSinInvocarRepository() {
        assertThatThrownBy(() -> ejecutar(null, HASTA, null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> ejecutar(DESDE, null, null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> ejecutar(HASTA, DESDE, null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(materialUtilizadoRepository);
    }

    @Test
    void rechazaPaginacionInvalidaSinInvocarRepository() {
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, null, null, null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, null, null, null, null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, null, null, null, null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(materialUtilizadoRepository);
    }

    @Test
    void normalizaBlankYTrimAntesDeConsultar() {
        Pagina<MaterialUtilizado> esperado = paginaVacia(1, 20);
        when(materialUtilizadoRepository.findPage(DESDE, HASTA,
                "500017", "300861", "190-1562-5001284", "F4", 1, 20))
                .thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, " 500017 ", " 300861 ",
                " 190-1562-5001284 ", " F4 ", 1, 20)).isSameAs(esperado);
        verify(materialUtilizadoRepository).findPage(DESDE, HASTA,
                "500017", "300861", "190-1562-5001284", "F4", 1, 20);
    }

    @Test
    void convierteBlanksEnNulos() {
        Pagina<MaterialUtilizado> esperado = paginaVacia(1, 20);
        when(materialUtilizadoRepository.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20)).thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, " ", "", "   ", null, 1, 20))
                .isSameAs(esperado);
        verify(materialUtilizadoRepository).findPage(DESDE, HASTA,
                null, null, null, null, 1, 20);
    }

    @Test
    void aceptaLongitudesMaximas() {
        String material = "M".repeat(50);
        String producto = "P".repeat(50);
        String pedimentoSalida = "S".repeat(50);
        String clavePedimentoSalida = "C".repeat(5);
        Pagina<MaterialUtilizado> esperado = paginaVacia(1, 20);
        when(materialUtilizadoRepository.findPage(DESDE, HASTA, material, producto,
                pedimentoSalida, clavePedimentoSalida, 1, 20)).thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, material, producto, pedimentoSalida,
                clavePedimentoSalida, 1, 20)).isSameAs(esperado);
        verify(materialUtilizadoRepository).findPage(DESDE, HASTA, material, producto,
                pedimentoSalida, clavePedimentoSalida, 1, 20);
    }

    @Test
    void rechazaLongitudesExcedidasSinInvocarRepository() {
        assertFiltroInvalido("M".repeat(51), null, null, null);
        assertFiltroInvalido(null, "P".repeat(51), null, null);
        assertFiltroInvalido(null, null, "S".repeat(51), null);
        assertFiltroInvalido(null, null, null, "C".repeat(6));
    }

    @Test
    void recortaAntesDeValidarLongitud() {
        String materialConEspacios = " " + "M".repeat(50) + " ";
        Pagina<MaterialUtilizado> esperado = paginaVacia(1, 20);
        when(materialUtilizadoRepository.findPage(DESDE, HASTA,
                "M".repeat(50), null, null, null, 1, 20)).thenReturn(esperado);

        assertThat(ejecutar(DESDE, HASTA, materialConEspacios, null, null, null, 1, 20))
                .isSameAs(esperado);
    }

    @Test
    void aceptaPaginaExtremaYFechaMaxima() {
        LocalDate fechaMaxima = LocalDate.of(9999, 12, 31);
        Pagina<MaterialUtilizado> esperado = paginaVacia(Integer.MAX_VALUE, 100);
        when(materialUtilizadoRepository.findPage(fechaMaxima, fechaMaxima,
                null, null, null, null, Integer.MAX_VALUE, 100)).thenReturn(esperado);

        assertThat(ejecutar(fechaMaxima, fechaMaxima,
                null, null, null, null, Integer.MAX_VALUE, 100)).isSameAs(esperado);
        verify(materialUtilizadoRepository).findPage(fechaMaxima, fechaMaxima,
                null, null, null, null, Integer.MAX_VALUE, 100);
    }

    private void assertFiltroInvalido(String material, String producto, String pedimentoSalida,
                                      String clavePedimentoSalida) {
        assertThatThrownBy(() -> ejecutar(DESDE, HASTA, material, producto, pedimentoSalida,
                clavePedimentoSalida, 1, 20)).isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(materialUtilizadoRepository);
    }

    private Pagina<MaterialUtilizado> ejecutar(LocalDate desde, LocalDate hasta, String material,
                                               String producto, String pedimentoSalida,
                                               String clavePedimentoSalida, int pagina, int tamano) {
        return new ListarMaterialesUtilizadosUseCase(materialUtilizadoRepository).ejecutar(
                desde, hasta, material, producto, pedimentoSalida, clavePedimentoSalida,
                pagina, tamano);
    }

    private Pagina<MaterialUtilizado> paginaVacia(int pagina, int tamano) {
        return new Pagina<>(List.of(), 0, pagina, tamano);
    }
}
