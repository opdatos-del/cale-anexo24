package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30FraccionesUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30FraccionRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ListarAnexo30FraccionesUseCaseTest {
    private final Anexo30FraccionRepository repository = mock(Anexo30FraccionRepository.class);
    private final ListarAnexo30FraccionesUseCase useCase = new ListarAnexo30FraccionesUseCase(repository);
    @Test void normalizaYDelega() {
        when(repository.findPage("F4", 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        useCase.ejecutar(" F4 ", 1, 20);
        verify(repository).findPage("F4", 1, 20);
    }
    @Test void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }
}
