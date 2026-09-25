package com.jovycandy.anexo24.billing.application.usecase;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CargarFacturacionUseCaseTest {
    @Mock private CargaFacturacionRepository repository;

    @Test
    void detectaHashDuplicadoEnElBatchAntesDePersistir() {
        var useCase = new CargarFacturacionUseCase(repository);
        var archivoA = new ArchivoFacturacion("a.xlsx", "a", 1, 0, List.of(), List.of(), List.of(), false);
        var archivoB = new ArchivoFacturacion("b.xlsx", "a", 1, 0, List.of(), List.of(), List.of(), false);
        assertThrows(RecursoDuplicadoException.class, () -> useCase.ejecutarLote(List.of(archivoA, archivoB), 5L, "corr-5"));
        verifyNoInteractions(repository);
    }

    @Test
    void transmiteCorrelationIdAlPuertoDePersistencia() {
        var useCase = new CargarFacturacionUseCase(repository);
        var archivo = new ArchivoFacturacion("x.xlsx", "a".repeat(64), 1, 0,
                List.of("Documento"), List.of(), List.of(), false);
        when(repository.existsByHash(archivo.hash())).thenReturn(false);
        when(repository.save(archivo, 5L, "corr-5", "[]")).thenReturn(17L);

        List<Long> ids = useCase.ejecutarLote(List.of(archivo), 5L, "corr-5");

        org.junit.jupiter.api.Assertions.assertEquals(List.of(17L), ids);
        verify(repository).save(archivo, 5L, "corr-5", "[]");
    }

    @Test
    void transmiteFilasNormalizadasAlStaging() {
        var useCase = new CargarFacturacionUseCase(repository);
        var archivo = new ArchivoFacturacion("x.xlsx", "c".repeat(64), 1, 1,
                List.of("Documento"), List.of(),
                List.of(new ArchivoFacturacion.Fila("FACTURAS", 2, List.of("DOC-1"))), List.of(), false);
        when(repository.existsByHash(archivo.hash())).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.eq(archivo), org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("corr-6"), org.mockito.ArgumentMatchers.contains("DOC-1"))).thenReturn(18L);
        org.junit.jupiter.api.Assertions.assertEquals(List.of(18L), useCase.ejecutarLote(List.of(archivo), 5L, "corr-6"));
        verify(repository).save(org.mockito.ArgumentMatchers.eq(archivo), org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("corr-6"), org.mockito.ArgumentMatchers.contains("DOC-1"));
    }

    @Test
    void revisaTodosLosHashesExistentesAntesDeCualquierEscritura() {
        var useCase = new CargarFacturacionUseCase(repository);
        var archivoA = new ArchivoFacturacion("a.xlsx", "a", 1, 0, List.of(), List.of(), List.of(), false);
        var archivoB = new ArchivoFacturacion("b.xlsx", "b", 1, 0, List.of(), List.of(), List.of(), false);
        when(repository.existsByHash("a")).thenReturn(false);
        when(repository.existsByHash("b")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class,
                () -> useCase.ejecutarLote(List.of(archivoA, archivoB), 5L, "corr-5"));
        verify(repository).existsByHash("a");
        verify(repository).existsByHash("b");
    }
}
