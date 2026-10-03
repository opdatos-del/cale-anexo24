package com.jovycandy.anexo24.catalogs.imports;

import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaMaterialUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaMaterial;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaMaterialRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba la delegación del caso de uso al puerto autoritativo. */
class ConfirmarCargaMaterialUseCaseTest {

    @Test
    void delegaLaConfirmacionAlPuerto() {
        ConfirmacionCargaMaterialRepository repository = mock(ConfirmacionCargaMaterialRepository.class);
        ConfirmarCargaMaterialUseCase useCase = new ConfirmarCargaMaterialUseCase(repository);
        ConfirmacionCargaMaterial esperado = new ConfirmacionCargaMaterial(8L, "CONFIRMADA", 2, 2,
                0, LocalDateTime.parse("2026-10-03T09:30:00"), "CONFIRMED");
        when(repository.confirmar(8L)).thenReturn(esperado);

        ConfirmacionCargaMaterial actual = useCase.ejecutar(8L);

        assertSame(esperado, actual);
        verify(repository).confirmar(8L);
    }
}
