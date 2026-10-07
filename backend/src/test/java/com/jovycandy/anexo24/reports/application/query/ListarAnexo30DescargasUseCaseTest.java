package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30DescargasUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30DescargaRepository;
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
}
