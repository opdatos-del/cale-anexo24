package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.operations.pediments.application.validation.ValidarPedimentoUseCase;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoFila;
import com.jovycandy.anexo24.operations.pediments.domain.port.PedimentoReglasRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ValidarPedimentoUseCaseTest {
    @Test
    void agregaErroresBatchSinDuplicarReglasExistentes() {
        PedimentoReglasRepository repository = mock(PedimentoReglasRepository.class);
        ValidarPedimentoUseCase useCase = new ValidarPedimentoUseCase(repository);
        PedimentoFila fila = new PedimentoFila("Hoja1", 2, Map.of("Clave", "MAT-1"));
        PedimentoError existente = new PedimentoError("Hoja1", 2, "Clave", "no almacenado", "PED-003", "No existe");
        CargaPedimentoArchivo archivo = new CargaPedimentoArchivo("a.xlsx", "h", "V1", List.of(), List.of(fila),
                List.of(existente), false);
        when(repository.validar(List.of(fila))).thenReturn(List.of(existente,
                new PedimentoError("Hoja1", 2, "UnidadComercial", "no almacenado", "PED-001", "Unidad inválida")));

        CargaPedimentoArchivo resultado = useCase.ejecutar(archivo);

        assertEquals(2, resultado.errores().size());
        verify(repository).validar(List.of(fila));
    }

    @Test
    void noConsultaSiNoHayFilas() {
        PedimentoReglasRepository repository = mock(PedimentoReglasRepository.class);
        ValidarPedimentoUseCase useCase = new ValidarPedimentoUseCase(repository);
        CargaPedimentoArchivo archivo = new CargaPedimentoArchivo("a.xlsx", "h", "V1", List.of(), List.of(), List.of(), true);

        assertEquals(archivo, useCase.ejecutar(archivo));
        verifyNoInteractions(repository);
    }
}
