package com.jovycandy.anexo24.catalogs.imports;

import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaProveedorUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProveedor;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaProveedorRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba la delegación del caso de uso de confirmación de proveedores. */
class ConfirmarCargaProveedorUseCaseTest {

    @Test
    void delegaLaConfirmacionAlPuerto() {
        ConfirmacionCargaProveedorRepository repository = mock(ConfirmacionCargaProveedorRepository.class);
        ConfirmarCargaProveedorUseCase useCase = new ConfirmarCargaProveedorUseCase(repository);
        ConfirmacionCargaProveedor esperado = new ConfirmacionCargaProveedor(15L, "CONFIRMADA", 2, 2,
                0, LocalDateTime.parse("2026-10-03T09:30:00"), "CONFIRMED");
        when(repository.confirmar(15L)).thenReturn(esperado);

        ConfirmacionCargaProveedor actual = useCase.ejecutar(15L);

        assertSame(esperado, actual);
        verify(repository).confirmar(15L);
    }
}