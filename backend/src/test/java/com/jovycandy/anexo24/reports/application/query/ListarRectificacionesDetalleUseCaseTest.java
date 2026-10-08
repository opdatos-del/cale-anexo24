package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarRectificacionesDetalleUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.RectificacionDetalleRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarRectificacionesDetalleUseCaseTest {
    private final RectificacionDetalleRepository repository = mock(RectificacionDetalleRepository.class);
    private final ListarRectificacionesDetalleUseCase useCase = new ListarRectificacionesDetalleUseCase(repository);

    @Test
    void normalizaYDelega() {
        when(repository.findPage("26", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));
        useCase.ejecutar(" 26 ", 2, 20);
        verify(repository).findPage("26", 2, 20);
    }

    @Test
    void filtroEnBlancoSeConvierteANull() {
        when(repository.findPage(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        useCase.ejecutar("   ", 1, 20);
        verify(repository).findPage(null, 1, 20);
    }

    @Test
    void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 0));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }
}
