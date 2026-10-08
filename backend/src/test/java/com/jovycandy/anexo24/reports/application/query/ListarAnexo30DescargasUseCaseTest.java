package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30DescargasUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30DescargaRepository;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Descarga;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ListarAnexo30DescargasUseCaseTest {
    private final Anexo30DescargaRepository repository = mock(Anexo30DescargaRepository.class);
    private final ListarAnexo30DescargasUseCase useCase = new ListarAnexo30DescargasUseCase(repository);
    @Test void normalizaYDelega() {
        when(repository.findPage("PED-1", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));
        useCase.ejecutar(" PED-1 ", 2, 20);
        verify(repository).findPage("PED-1", 2, 20);
    }
    @Test void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }

    @Test
    void exportarSinFilasDevuelveListaVacia() {
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(), 0, 1, 100));

        org.junit.jupiter.api.Assertions.assertTrue(useCase.exportar(null).isEmpty());
        verify(repository).findPage(null, 1, 100);
    }

    @Test
    void exportarUnaPaginaNormalizaFiltro() {
        Anexo30Descarga fila = mock(Anexo30Descarga.class);
        when(repository.findPage("PED-1", 1, 100)).thenReturn(new Pagina<>(List.of(fila), 1, 1, 100));

        org.junit.jupiter.api.Assertions.assertEquals(List.of(fila), useCase.exportar(" PED-1 "));
        verify(repository).findPage("PED-1", 1, 100);
    }

    @Test
    void exportarRecorreTodasLasPaginas() {
        Anexo30Descarga primera = mock(Anexo30Descarga.class);
        Anexo30Descarga segunda = mock(Anexo30Descarga.class);
        when(repository.findPage("PED-1", 1, 100)).thenReturn(new Pagina<>(List.of(primera), 101, 1, 100));
        when(repository.findPage("PED-1", 2, 100)).thenReturn(new Pagina<>(List.of(segunda), 101, 2, 100));

        org.junit.jupiter.api.Assertions.assertEquals(List.of(primera, segunda), useCase.exportar("PED-1"));
        verify(repository).findPage("PED-1", 2, 100);
    }

    @Test
    void exportarFiltroVacioSeNormalizaANull() {
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(), 0, 1, 100));

        useCase.exportar("   ");

        verify(repository).findPage(null, 1, 100);
    }

    @Test
    void exportarSuperiorADiezMilFilasEsSolicitudInvalida() {
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(), 10_001, 1, 100));

        assertThrows(SolicitudInvalidaException.class, () -> useCase.exportar(null));
    }
}
