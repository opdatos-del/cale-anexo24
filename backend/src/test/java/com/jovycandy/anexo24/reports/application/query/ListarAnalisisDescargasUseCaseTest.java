package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnalisisDescargasUseCase;
import com.jovycandy.anexo24.reports.extended.domain.port.AnalisisDescargaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarAnalisisDescargasUseCaseTest {
    private final AnalisisDescargaRepository repository = mock(AnalisisDescargaRepository.class);
    private final ListarAnalisisDescargasUseCase useCase = new ListarAnalisisDescargasUseCase(repository);

    @Test
    void normalizaFiltroYDelegaPaginacion() {
        when(repository.findPage("190-1562", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));

        Pagina<?> resultado = useCase.ejecutar(" 190-1562 ", 2, 20);

        assertEquals(2, resultado.pagina());
        verify(repository).findPage("190-1562", 2, 20);
    }

    @Test
    void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }
}
