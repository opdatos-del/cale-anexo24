package com.jovycandy.anexo24.operations.actas;

import com.jovycandy.anexo24.operations.actas.application.usecase.ConfirmarCargaActaUseCase;
import com.jovycandy.anexo24.operations.actas.domain.model.ConfirmacionCargaActa;
import com.jovycandy.anexo24.operations.actas.domain.port.ConfirmacionCargaActaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba la delegacion del caso de uso de confirmacion de actas. */
class ConfirmarCargaActaUseCaseTest {

    @Test
    void delegaLaConfirmacionAlPuerto() {
        ConfirmacionCargaActaRepository repository = mock(ConfirmacionCargaActaRepository.class);
        ConfirmarCargaActaUseCase useCase = new ConfirmarCargaActaUseCase(repository);
        ConfirmacionCargaActa esperado = new ConfirmacionCargaActa(7L, "CONFIRMADA", 3, 3, 0,
                LocalDateTime.parse("2026-10-05T09:30:00"), "CONFIRMED");
        when(repository.confirmar(7L)).thenReturn(esperado);

        ConfirmacionCargaActa actual = useCase.ejecutar(7L);

        assertSame(esperado, actual);
        verify(repository).confirmar(7L);
    }
}
