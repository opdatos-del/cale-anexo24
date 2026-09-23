package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarPerfilUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CambiarPerfilUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void cambiaPerfilPasaActorFechaYAudita() {
        AtomicInteger n = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(i -> Optional.of(usuario(n.getAndIncrement() == 0 ? 1L : 2L)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req");
        verify(comandos).actualizarPerfil(eq(42L), eq(2L), eq(7L), any(LocalDate.class));
        verify(bitacora).registrar(any());
    }

    @Test
    void noOpNoLlamaCommandNiActor() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(1L)));
        assertThat(useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(1L), "r")).isNotNull();
        verifyNoInteractions(comandos, actor, bitacora);
    }

    @Test
    void erroresPerfilUsuarioSelfYGuardrailVienenDelCommand() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(1L)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));

        doThrow(new RecursoNoEncontradoException()).when(comandos).actualizarPerfil(eq(42L), eq(2L), eq(7L), any(LocalDate.class));
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "r"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        reset(comandos);
        doThrow(new EstadoIncompatibleException()).when(comandos).actualizarPerfil(eq(42L), eq(2L), eq(42L), any(LocalDate.class));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(42L, "admin")));
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "r"))
                .isInstanceOf(EstadoIncompatibleException.class);
    }

    @Test
    void conservaTransaccionSerializable() throws Exception {
        Method m = CambiarPerfilUsuarioUseCase.class.getDeclaredMethod("ejecutar", Long.class, CambiarPerfilUsuarioCommand.class, String.class);
        Transactional t = m.getAnnotation(Transactional.class);
        assertThat(t.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(t.isolation()).isEqualTo(Isolation.SERIALIZABLE);
    }

    private CambiarPerfilUsuarioUseCase useCase() { return new CambiarPerfilUsuarioUseCase(comandos, consultas, bitacora, actor); }
    private UsuarioAdministracion usuario(Long perfil) { return new UsuarioAdministracion(42L, "op", "N", "c", "ACTIVO", null, perfil, "P"); }
}
