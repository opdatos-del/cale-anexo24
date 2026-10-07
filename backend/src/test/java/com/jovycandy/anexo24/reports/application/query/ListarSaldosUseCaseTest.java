package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.reports.extended.application.query.ListarSaldosUseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;
import com.jovycandy.anexo24.reports.extended.domain.port.SaldoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarSaldosUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2025, 12, 31);
    private static final String DOCUMENTO = "PED-1";

    private final SaldoRepository repository = mock(SaldoRepository.class);
    private final ListarSaldosUseCase useCase = new ListarSaldosUseCase(repository);

    @Test
    void normalizaDocumentoYDelegaFiltros() {
        when(repository.findPage(eq(DESDE), eq(HASTA), eq("PED-1"), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        useCase.ejecutar(DESDE, HASTA, " PED-1 ", 2, 20);

        verify(repository).findPage(DESDE, HASTA, "PED-1", 2, 20);
    }

    @Test
    void permiteDocumentoNulo() {
        when(repository.findPage(eq(DESDE), eq(HASTA), eq(null), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        useCase.ejecutar(DESDE, HASTA, null, 1, 20);

        verify(repository).findPage(DESDE, HASTA, null, 1, 20);
    }

    @Test
    void rechazaRangoFechasIncompleto() {
        assertThrows(SolicitudInvalidaException.class,
                () -> useCase.ejecutar(null, HASTA, null, 1, 20));
        assertThrows(SolicitudInvalidaException.class,
                () -> useCase.ejecutar(DESDE, null, null, 1, 20));
    }

    @Test
    void rechazaPaginacionFueraDeContrato() {
        assertThrows(SolicitudInvalidaException.class,
                () -> useCase.ejecutar(DESDE, HASTA, null, 0, 20));
        assertThrows(SolicitudInvalidaException.class,
                () -> useCase.ejecutar(DESDE, HASTA, null, 1, 101));
    }

    @Test
    void exportarRechazaDatasetSobreLimite() {
        when(repository.findPage(eq(DESDE), eq(HASTA), eq(null), eq(1), eq(100)))
                .thenReturn(new Pagina<>(List.of(), 10_001, 1, 100));

        assertThrows(SolicitudInvalidaException.class,
                () -> useCase.exportar(DESDE, HASTA, null));
    }

    @Test
    void exportarDevuelveListaVaciaSiTotalCero() {
        when(repository.findPage(eq(DESDE), eq(HASTA), eq(null), eq(1), eq(100)))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 100));

        List<Saldo> resultado = useCase.exportar(DESDE, HASTA, null);
        assertEquals(0, resultado.size());
    }
}
