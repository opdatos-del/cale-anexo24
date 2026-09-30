package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarCompulsaUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.CompulsaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarCompulsaUseCaseTest {
    private final CompulsaRepository repository = mock(CompulsaRepository.class);
    private final ListarCompulsaUseCase useCase = new ListarCompulsaUseCase(repository);

    @Test
    void normalizaFiltroYDelegaPaginacion() {
        when(repository.findPage("A1", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));

        Pagina<?> resultado = useCase.ejecutar("  A1  ", 2, 20);

        assertEquals(2, resultado.pagina());
        verify(repository).findPage("A1", 2, 20);
    }

    @Test
    void rechazaPaginacionFueraDeContrato() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }
}
