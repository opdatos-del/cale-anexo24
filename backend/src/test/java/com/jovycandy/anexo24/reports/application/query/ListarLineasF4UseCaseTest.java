package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarLineasF4UseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.reports.extended.domain.port.LineaF4Repository;
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

class ListarLineasF4UseCaseTest {
    private final LineaF4Repository repository = mock(LineaF4Repository.class);
    private final ListarLineasF4UseCase useCase = new ListarLineasF4UseCase(repository);

    @Test
    void normalizaFiltroYDelegaPaginacion() {
        when(repository.findPage("CTMAPAA", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));

        Pagina<?> resultado = useCase.ejecutar("  CTMAPAA  ", 2, 20);

        assertEquals(2, resultado.pagina());
        verify(repository).findPage("CTMAPAA", 2, 20);
    }

    @Test
    void rechazaPaginacionInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.ejecutar(null, 1, 101));
    }

    @Test
    void exportarRecolectaTodasLasPaginas() {
        LineaF4 primera = linea("CTMAPAA", "F4-1");
        LineaF4 segunda = linea("DESP", "F4-2");
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(primera), 101, 1, 100));
        when(repository.findPage(null, 2, 100)).thenReturn(new Pagina<>(List.of(segunda), 101, 2, 100));

        List<LineaF4> resultado = useCase.exportar(" ");

        assertEquals(2, resultado.size());
        verify(repository).findPage(null, 1, 100);
        verify(repository).findPage(null, 2, 100);
    }

    @Test
    void exportacionSuperiorAlMaximoEsSolicitudInvalida() {
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(), 10_001, 1, 100));

        assertThrows(SolicitudInvalidaException.class, () -> useCase.exportar(null));
    }

    private static LineaF4 linea(String tipoDescarga, String f4) {
        return new LineaF4(tipoDescarga, f4, LocalDateTime.of(2026, 1, 15, 0, 0),
                "IMP-1", "MAT-1", BigDecimal.TEN, BigDecimal.ONE);
    }
}
