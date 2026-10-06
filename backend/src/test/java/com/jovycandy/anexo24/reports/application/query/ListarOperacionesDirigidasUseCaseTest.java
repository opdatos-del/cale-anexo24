package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesDirigidasUseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
import com.jovycandy.anexo24.reports.extended.domain.port.OperacionDirigidaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarOperacionesDirigidasUseCaseTest {
    private final OperacionDirigidaRepository repository = mock(OperacionDirigidaRepository.class);
    private final ListarOperacionesDirigidasUseCase useCase = new ListarOperacionesDirigidasUseCase(repository);

    @Test
    void normalizaFiltroYDelegaPaginacion() {
        when(repository.findPage("26", 2, 20)).thenReturn(new Pagina<>(List.of(), 0, 2, 20));

        Pagina<?> resultado = useCase.ejecutar(" 26 ", 2, 20);

        assertEquals(2, resultado.pagina());
        verify(repository).findPage("26", 2, 20);
    }

    @Test
    void exportarSinFilasDevuelveListaVacia() {
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(), 0, 1, 100));

        assertEquals(List.of(), useCase.exportar(null));
        verify(repository).findPage(null, 1, 100);
    }

    @Test
    void exportarRecolectaTodasLasPaginas() {
        OperacionDirigida primera = dirigida(1L);
        OperacionDirigida segunda = dirigida(2L);
        when(repository.findPage(null, 1, 100)).thenReturn(new Pagina<>(List.of(primera), 101, 1, 100));
        when(repository.findPage(null, 2, 100)).thenReturn(new Pagina<>(List.of(segunda), 101, 2, 100));

        assertEquals(List.of(primera, segunda), useCase.exportar(" "));
    }

    @Test
    void exportacionDeDiezMilFilasEsAceptada() {
        OperacionDirigida primera = dirigida(1L);
        when(repository.findPage(any(), anyInt(), anyInt())).thenAnswer(invocacion -> {
            int pagina = invocacion.getArgument(1);
            return new Pagina<>(pagina == 1 ? List.of(primera) : List.of(), 10_000, pagina, 100);
        });

        assertEquals(List.of(primera), useCase.exportar(null));
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

    private static OperacionDirigida dirigida(Long salidaKey) {
        return new OperacionDirigida(salidaKey, 2L, "EXP-1", LocalDateTime.of(2026, 1, 15, 0, 0),
                "A1", 2, "PROD-1", BigDecimal.TEN, "FAC-1", "SI", "SI", BigDecimal.ONE,
                BigDecimal.TWO, "SI", 3);
    }
}
