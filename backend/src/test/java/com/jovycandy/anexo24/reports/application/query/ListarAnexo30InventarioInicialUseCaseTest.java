package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30InventarioInicialUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30InventarioInicialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarAnexo30InventarioInicialUseCaseTest {
    private final Anexo30InventarioInicialRepository repository = mock(Anexo30InventarioInicialRepository.class);
    private final ListarAnexo30InventarioInicialUseCase useCase = new ListarAnexo30InventarioInicialUseCase(repository);

    @Test
    void normalizaFiltroYDelegaPaginacion() {
        when(repository.findPage("F4", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));
        useCase.ejecutar(" F4 ", 2, 20);
        verify(repository).findPage("F4", 2, 20);
    }

    @Test
    void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }
}
