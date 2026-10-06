package com.jovycandy.anexo24.operations.constancias;

import com.jovycandy.anexo24.operations.constancias.application.usecase.ConfirmarCargaConstanciaUseCase;
import com.jovycandy.anexo24.operations.constancias.domain.model.ConfirmacionCargaConstancia;
import com.jovycandy.anexo24.operations.constancias.domain.port.ConfirmacionCargaConstanciaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba la delegacion del caso de uso de confirmacion de constancias. */
class ConfirmarCargaConstanciaUseCaseTest {

    @Test
    void delegaLaConfirmacionAlPuerto() {
        ConfirmacionCargaConstanciaRepository repository = mock(ConfirmacionCargaConstanciaRepository.class);
        ConfirmarCargaConstanciaUseCase useCase = new ConfirmarCargaConstanciaUseCase(repository);
        ConfirmacionCargaConstancia esperado = new ConfirmacionCargaConstancia(7L, "CONFIRMADA", 3, 3, 0,
                LocalDateTime.parse("2026-10-05T09:30:00"), "CONFIRMED");
        when(repository.confirmar(7L)).thenReturn(esperado);

        ConfirmacionCargaConstancia actual = useCase.ejecutar(7L);

        assertSame(esperado, actual);
        verify(repository).confirmar(7L);
    }
}
