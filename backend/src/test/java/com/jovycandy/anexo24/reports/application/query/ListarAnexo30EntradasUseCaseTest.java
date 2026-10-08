package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30EntradasUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30EntradaRepository;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Entrada;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarAnexo30EntradasUseCaseTest {
    private final Anexo30EntradaRepository repository = mock(Anexo30EntradaRepository.class);
    private final ListarAnexo30EntradasUseCase useCase = new ListarAnexo30EntradasUseCase(repository);

    @Test
    void normalizaFiltroYDelegaPaginacion() {
        when(repository.findPage("26", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));

        Pagina<?> resultado = useCase.ejecutar(" 26 ", 2, 20);

        assertEquals(2, resultado.pagina());
        verify(repository).findPage("26", 2, 20);
    }

    @Test
    void permiteConsultaSinFiltro() {
        when(repository.findPage(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        useCase.ejecutar(null, 1, 20);

        verify(repository).findPage(null, 1, 20);
    }

    @Test
    void rechazaPaginacionFueraDeContrato() {
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
        Anexo30Entrada fila = mock(Anexo30Entrada.class);
        when(repository.findPage("PED-1", 1, 100)).thenReturn(new Pagina<>(List.of(fila), 1, 1, 100));

        org.junit.jupiter.api.Assertions.assertEquals(List.of(fila), useCase.exportar(" PED-1 "));
        verify(repository).findPage("PED-1", 1, 100);
    }

    @Test
    void exportarRecorreTodasLasPaginas() {
        Anexo30Entrada primera = mock(Anexo30Entrada.class);
        Anexo30Entrada segunda = mock(Anexo30Entrada.class);
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
