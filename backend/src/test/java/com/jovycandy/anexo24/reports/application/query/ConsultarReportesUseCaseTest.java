package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.auditlog.application.query.ListarBitacoraUseCase;
import com.jovycandy.anexo24.operations.entries.application.query.ListarEntradasUseCase;
import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
import com.jovycandy.anexo24.operations.exits.application.query.ListarSalidasUseCase;
import com.jovycandy.anexo24.operations.usedmaterials.application.query.ListarMaterialesUtilizadosUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba de composición de Reportes V1 sobre los casos de uso existentes. */
class ConsultarReportesUseCaseTest {

    @Test
    void entradasReutilizaCasoDeUsoYConvierteResultadoADto() {
        ListarEntradasUseCase entradas = mock(ListarEntradasUseCase.class);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        EntradaLinea linea = new EntradaLinea(BigDecimal.ONE, BigDecimal.TWO, "2400001", "A1",
                LocalDateTime.of(2026, 1, 2, 10, 0), "01010101", "PZA", BigDecimal.TEN,
                "PARTE-1", LocalDateTime.of(2026, 1, 3, 10, 0));
        when(entradas.ejecutar(desde, hasta, "2400001", "A1", "01010101", "PARTE-1", 1, 20))
                .thenReturn(new Pagina<>(List.of(linea), 1, 1, 20));
        ConsultarReportesUseCase useCase = new ConsultarReportesUseCase(entradas,
                mock(ListarSalidasUseCase.class), mock(ListarMaterialesUtilizadosUseCase.class),
                mock(ListarBitacoraUseCase.class));

        var resultado = useCase.entradas(desde, hasta, "2400001", "A1", "01010101", "PARTE-1", 1, 20);

        verify(entradas).ejecutar(desde, hasta, "2400001", "A1", "01010101", "PARTE-1", 1, 20);
        assertEquals(1, resultado.total());
        assertEquals("2400001", resultado.items().getFirst().pedimento());
        assertEquals(BigDecimal.TEN, resultado.items().getFirst().cantidadComercial());
    }

    @Test
    void exportarEntradasRecorreTodasLasPaginasDeCienFilas() {
        ListarEntradasUseCase entradas = mock(ListarEntradasUseCase.class);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        List<EntradaLinea> primeraPagina = IntStream.range(0, 100)
                .mapToObj(this::entrada).toList();
        EntradaLinea ultimaLinea = entrada(100);
        when(entradas.ejecutar(desde, hasta, null, null, null, null, 1, 100))
                .thenReturn(new Pagina<>(primeraPagina, 101, 1, 100));
        when(entradas.ejecutar(desde, hasta, null, null, null, null, 2, 100))
                .thenReturn(new Pagina<>(List.of(ultimaLinea), 101, 2, 100));
        ConsultarReportesUseCase useCase = new ConsultarReportesUseCase(entradas,
                mock(ListarSalidasUseCase.class), mock(ListarMaterialesUtilizadosUseCase.class),
                mock(ListarBitacoraUseCase.class));

        var resultado = useCase.exportarEntradas(desde, hasta, null, null, null, null);

        assertEquals(101, resultado.size());
        assertEquals("PED-100", resultado.getLast().pedimento());
        verify(entradas).ejecutar(desde, hasta, null, null, null, null, 1, 100);
        verify(entradas).ejecutar(desde, hasta, null, null, null, null, 2, 100);
    }

    @Test
    void exportacionSuperiorAlMaximoEsSolicitudInvalida() {
        ListarEntradasUseCase entradas = mock(ListarEntradasUseCase.class);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        when(entradas.ejecutar(desde, hasta, null, null, null, null, 1, 100))
                .thenReturn(new Pagina<>(List.of(), 10_001, 1, 100));
        ConsultarReportesUseCase useCase = new ConsultarReportesUseCase(entradas,
                mock(ListarSalidasUseCase.class), mock(ListarMaterialesUtilizadosUseCase.class),
                mock(ListarBitacoraUseCase.class));

        assertThrows(com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException.class,
                () -> useCase.exportarEntradas(desde, hasta, null, null, null, null));
    }

    private EntradaLinea entrada(int indice) {
        return new EntradaLinea(BigDecimal.valueOf(indice), BigDecimal.ONE, "PED-" + indice, "A1",
                LocalDateTime.of(2026, 1, 2, 10, 0), "01010101", "PZA", BigDecimal.TEN,
                "PARTE-" + indice, LocalDateTime.of(2026, 1, 3, 10, 0));
    }
}
