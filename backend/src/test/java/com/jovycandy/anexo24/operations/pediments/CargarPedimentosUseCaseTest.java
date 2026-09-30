package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.operations.pediments.application.usecase.CargarPedimentosUseCase;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.port.CargaPedimentoRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CargarPedimentosUseCaseTest {
    @Test
    void rechazaHashActivoAntesDePersistir() {
        CargaPedimentoRepository repository = mock(CargaPedimentoRepository.class);
        when(repository.existsByHash("h".repeat(64))).thenReturn(true);
        CargarPedimentosUseCase useCase = new CargarPedimentosUseCase(repository);
        CargaPedimentoArchivo file = new CargaPedimentoArchivo("a.xlsx", "h".repeat(64), "V1", List.of(), List.of(), List.of(), false);

        assertThrows(RecursoDuplicadoException.class, () -> useCase.ejecutar(file, 1, "corr"));
        verify(repository, never()).save(any(), anyLong(), anyString());
    }

    @Test
    void persisteStagingCuandoHashEsNuevo() {
        CargaPedimentoRepository repository = mock(CargaPedimentoRepository.class);
        when(repository.existsByHash(anyString())).thenReturn(false);
        when(repository.save(any(), eq(7L), eq("corr"))).thenReturn(42L);
        CargarPedimentosUseCase useCase = new CargarPedimentosUseCase(repository);
        CargaPedimentoArchivo file = new CargaPedimentoArchivo("a.xlsx", "h".repeat(64), "V1", List.of(), List.of(), List.of(), false);

        assertEquals(42L, useCase.ejecutar(file, 7, "corr"));
        verify(repository).save(file, 7L, "corr");
    }
}
