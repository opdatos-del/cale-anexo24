package com.jovycandy.anexo24.catalogs.imports;

import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaProductoUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProducto;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaProductoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba la delegación del caso de uso de confirmación de productos. */
class ConfirmarCargaProductoUseCaseTest {

    @Test
    void delegaLaConfirmacionAlPuerto() {
        ConfirmacionCargaProductoRepository repository = mock(ConfirmacionCargaProductoRepository.class);
        ConfirmarCargaProductoUseCase useCase = new ConfirmarCargaProductoUseCase(repository);
        ConfirmacionCargaProducto esperado = new ConfirmacionCargaProducto(11L, "CONFIRMADA", 2, 2,
                0, LocalDateTime.parse("2026-10-03T09:30:00"), "CONFIRMED");
        when(repository.confirmar(11L)).thenReturn(esperado);

        ConfirmacionCargaProducto actual = useCase.ejecutar(11L);

        assertSame(esperado, actual);
        verify(repository).confirmar(11L);
    }
}