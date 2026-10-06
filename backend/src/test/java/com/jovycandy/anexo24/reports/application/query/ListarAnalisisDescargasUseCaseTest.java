package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarAnalisisDescargasUseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.port.AnalisisDescargaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    void exportarRecolectaTodasLasPaginas() {
        AnalisisDescarga primera = analisis(1L);
        AnalisisDescarga segunda = analisis(2L);
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(primera), 101, 1, 100));
        when(repository.findPage(null, 2, 100)).thenReturn(new Pagina<>(List.of(segunda), 101, 2, 100));

        List<AnalisisDescarga> resultado = useCase.exportar(" ");

        assertEquals(List.of(primera, segunda), resultado);
        verify(repository).findPage(null, 1, 100);
        verify(repository).findPage(null, 2, 100);
    }

    @Test
    void exportacionSuperiorAlMaximoEsSolicitudInvalida() {
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(), 10_001, 1, 100));

        assertThrows(SolicitudInvalidaException.class, () -> useCase.exportar(null));
    }

    @Test
    void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }

    private static AnalisisDescarga analisis(Long descargaId) {
        return new AnalisisDescarga(descargaId, "IMP-1", "1", "EXP-1", "2", "MAT-1", "PROD-1",
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 2, 0, 0),
                LocalDateTime.of(2027, 1, 1, 0, 0), BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ZERO, null, "KG");
    }
}
