package com.jovycandy.anexo24.catalogs.imports;

import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaClienteUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaCliente;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaClienteRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba la delegación del caso de uso de confirmación de clientes. */
class ConfirmarCargaClienteUseCaseTest {

    @Test
    void delegaLaConfirmacionAlPuerto() {
        ConfirmacionCargaClienteRepository repository = mock(ConfirmacionCargaClienteRepository.class);
        ConfirmarCargaClienteUseCase useCase = new ConfirmarCargaClienteUseCase(repository);
        ConfirmacionCargaCliente esperado = new ConfirmacionCargaCliente(13L, "CONFIRMADA", 2, 2,
                0, LocalDateTime.parse("2026-10-03T09:30:00"), "CONFIRMED");
        when(repository.confirmar(13L)).thenReturn(esperado);

        ConfirmacionCargaCliente actual = useCase.ejecutar(13L);

        assertSame(esperado, actual);
        verify(repository).confirmar(13L);
    }
}