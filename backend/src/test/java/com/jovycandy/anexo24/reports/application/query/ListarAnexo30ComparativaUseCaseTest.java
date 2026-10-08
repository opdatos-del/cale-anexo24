package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30ComparativaUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30ComparativaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ListarAnexo30ComparativaUseCaseTest {
    private final Anexo30ComparativaRepository repository = mock(Anexo30ComparativaRepository.class);
    private final ListarAnexo30ComparativaUseCase useCase = new ListarAnexo30ComparativaUseCase(repository);
    @Test void normalizaYDelega() {
        when(repository.findPage("A1", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));
        useCase.ejecutar(" A1 ", 2, 20);
        verify(repository).findPage("A1", 2, 20);
    }
    @Test void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 0));
    }
}
