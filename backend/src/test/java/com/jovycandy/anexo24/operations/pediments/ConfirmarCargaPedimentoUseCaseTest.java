package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.operations.pediments.application.usecase.ConfirmarCargaPedimentoUseCase;
import com.jovycandy.anexo24.operations.pediments.domain.model.ConfirmacionPedimento;
import com.jovycandy.anexo24.operations.pediments.domain.port.ConfirmacionPedimentoRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfirmarCargaPedimentoUseCaseTest {

    private ConfirmacionPedimentoRepository repository;
    private RegistrarEventoBitacoraService bitacora;
    private ConfirmarCargaPedimentoUseCase useCase;

    @BeforeEach
    void preparar() {
        repository = mock(ConfirmacionPedimentoRepository.class);
        bitacora = mock(RegistrarEventoBitacoraService.class);
        AuthenticatedUserContext contexto = mock(AuthenticatedUserContext.class);
        when(contexto.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        useCase = new ConfirmarCargaPedimentoUseCase(repository, contexto, bitacora);
    }

    @Test
    void confirmaYNoDuplicaBitacoraDeExitoEnJava() {
        ConfirmacionPedimento esperado = new ConfirmacionPedimento(1L, "CONFIRMADA",
                ConfirmacionPedimento.CONFIRMADO, 1, 1, 2, LocalDateTime.now());
        when(repository.confirmar(1L, 7L, "c-1")).thenReturn(esperado);

        assertEquals(esperado, useCase.ejecutar(1L, "c-1"));
        verifyNoInteractions(bitacora);
    }

    @Test
    void confirmaCargaMixtaConTipoNulo() {
        ConfirmacionPedimento mixta = new ConfirmacionPedimento(2L, "CONFIRMADA",
                ConfirmacionPedimento.CONFIRMADO, null, 2, 2, LocalDateTime.now());
        when(repository.confirmar(2L, 7L, "c-2")).thenReturn(mixta);

        assertEquals(null, useCase.ejecutar(2L, "c-2").tipoOperacion());
        verifyNoInteractions(bitacora);
    }

    @Test
    void alreadyConfirmedNoRegistraFallo() {
        ConfirmacionPedimento ya = new ConfirmacionPedimento(3L, "CONFIRMADA",
                ConfirmacionPedimento.YA_CONFIRMADO, 1, 0, 0, LocalDateTime.now());
        when(repository.confirmar(3L, 7L, "c-3")).thenReturn(ya);

        assertEquals(ConfirmacionPedimento.YA_CONFIRMADO, useCase.ejecutar(3L, "c-3").resultado());
        verifyNoInteractions(bitacora);
    }

    @Test
    void registrarFalloConCategoriaFuncional() {
        when(repository.confirmar(4L, 7L, "c-4")).thenThrow(new RecursoNoEncontradoException());

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.ejecutar(4L, "c-4"));

        ArgumentCaptor<BitacoraEvento> captor = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(captor.capture());
        BitacoraEvento evento = captor.getValue();
        assertEquals(BitacoraAccion.PEDIMENTO_CONFIRMACION_FALLIDA, evento.accion());
        assertEquals(7L, evento.usuarioId());
        assertEquals("cargaId=4;categoria=CARGA_NO_ENCONTRADA", evento.detalle());
    }

    @Test
    void registrarFalloDeConflictoYNoProcesable() {
        when(repository.confirmar(5L, 7L, "c-5")).thenThrow(new EstadoIncompatibleException());
        when(repository.confirmar(6L, 7L, "c-6")).thenThrow(new ConfirmacionNoProcesableException());

        assertThrows(EstadoIncompatibleException.class, () -> useCase.ejecutar(5L, "c-5"));
        assertThrows(ConfirmacionNoProcesableException.class, () -> useCase.ejecutar(6L, "c-6"));

        ArgumentCaptor<BitacoraEvento> captor = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora, times(2)).registrar(captor.capture());
        assertEquals("cargaId=5;categoria=CONFLICTO_OPERATIVO", captor.getAllValues().get(0).detalle());
        assertEquals("cargaId=6;categoria=NO_PROCESABLE", captor.getAllValues().get(1).detalle());
    }

    @Test
    void falloDelRegistroNoEnmascaraElErrorOriginal() {
        when(repository.confirmar(7L, 7L, "c-7")).thenThrow(new EstadoIncompatibleException());
        doThrow(new IllegalStateException("bitácora caída")).when(bitacora).registrar(any());

        assertThrows(EstadoIncompatibleException.class, () -> useCase.ejecutar(7L, "c-7"));
        verify(bitacora).registrar(any());
    }

    @Test
    void errorTecnicoSeRegistraComoCategoriaGenerica() {
        when(repository.confirmar(eq(8L), eq(7L), any())).thenThrow(new IllegalStateException("db caída"));

        assertThrows(IllegalStateException.class, () -> useCase.ejecutar(8L, null));

        ArgumentCaptor<BitacoraEvento> captor = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(captor.capture());
        assertEquals("cargaId=8;categoria=ERROR_TECNICO", captor.getValue().detalle());
    }
}
