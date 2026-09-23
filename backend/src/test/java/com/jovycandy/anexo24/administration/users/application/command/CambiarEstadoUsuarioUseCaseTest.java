package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarEstadoUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void cambiaEstadoPasaActorYFechaYAudita() {
        AtomicInteger n = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(i -> Optional.of(usuario(n.getAndIncrement() == 0 ? "ACTIVO" : "INACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand(" inactivo "), "req");
        verify(comandos).actualizarEstado(eq(42L), eq("INACTIVO"), eq(7L), any(LocalDate.class));
        verify(bitacora).registrar(any(BitacoraEvento.class));
    }

    @Test
    void noOpUsuarioInexistenteYActorAusenteSeMantienen() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        assertThat(useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("activo"), "r")).isNotNull();
        verifyNoInteractions(comandos, actor, bitacora);
        reset(consultas);
        when(consultas.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "r"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        reset(consultas);
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "r"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void selfGuardYLastAdminVienenDelCommand() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(42L, "admin")));
        doThrow(new EstadoIncompatibleException()).when(comandos).actualizarEstado(eq(42L), eq("INACTIVO"), eq(42L), any());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "r"))
                .isInstanceOf(EstadoIncompatibleException.class);
    }

    @Test
    void conservaTransaccionSerializable() throws Exception {
        Method m = CambiarEstadoUsuarioUseCase.class.getDeclaredMethod("ejecutar", Long.class, CambiarEstadoUsuarioCommand.class, String.class);
        Transactional t = m.getAnnotation(Transactional.class);
        assertThat(t.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(t.isolation()).isEqualTo(Isolation.SERIALIZABLE);
    }

    private CambiarEstadoUsuarioUseCase useCase() { return new CambiarEstadoUsuarioUseCase(comandos, consultas, bitacora, actor); }
    private UsuarioAdministracion usuario(String estado) { return new UsuarioAdministracion(42L, "op", "N", "c", estado, null, 1L, "P"); }
}
