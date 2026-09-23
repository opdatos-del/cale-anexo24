package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.ActualizarUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActualizarUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void actualizaCamposYAuditaSinPrequeryCorreo() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        UsuarioAdministracion actualizado = usuario("Nuevo", "nuevo@test");
        AtomicInteger n = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(i -> n.getAndIncrement() == 0 ? Optional.of(actual) : Optional.of(actualizado));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        useCase().ejecutar(42L, new ActualizarUsuarioCommand("Nuevo", "nuevo@test"), "req");
        verify(comandos).actualizarDatos(42L, "Nuevo", "nuevo@test");
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=42;campos=nombre,correo");
    }

    @Test
    void noOpNoLlamaCommandNiActor() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));
        assertThat(useCase().ejecutar(42L, new ActualizarUsuarioCommand(" Nombre ", " correo@test "), "req")).isSameAs(actual);
        verifyNoInteractions(comandos, bitacora, actor);
    }

    @Test
    void usuarioInexistenteYDuplicadoVienenDelContratoCorrecto() {
        when(consultas.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new ActualizarUsuarioCommand("n", "c"), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("n", "old")));
        doThrow(new RecursoDuplicadoException()).when(comandos).actualizarDatos(42L, "n", "new");
        assertThatThrownBy(() -> useCase().ejecutar(42L, new ActualizarUsuarioCommand("n", "new"), "req"))
                .isInstanceOf(RecursoDuplicadoException.class);
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void actorAusenteYBitacoraFallidaSePropagan() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("old", "old")));
        when(actor.currentUser()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new ActualizarUsuarioCommand("new", "old"), "req"))
                .isInstanceOf(IllegalStateException.class);
        reset(actor);
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("old", "old")));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "a")));
        doThrow(new IllegalStateException("fallo")).when(bitacora).registrar(any());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new ActualizarUsuarioCommand("new", "old"), "req"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void usaTransactionManagerApp24() throws Exception {
        Method method = ActualizarUsuarioUseCase.class.getDeclaredMethod("ejecutar", Long.class, ActualizarUsuarioCommand.class, String.class);
        assertThat(method.getAnnotation(Transactional.class).transactionManager()).isEqualTo("appTransactionManager");
    }

    private ActualizarUsuarioUseCase useCase() { return new ActualizarUsuarioUseCase(comandos, consultas, bitacora, actor); }
    private UsuarioAdministracion usuario(String nombre, String correo) { return new UsuarioAdministracion(42L, "op", nombre, correo, "ACTIVO", null, 1L, "Perfil"); }
}
