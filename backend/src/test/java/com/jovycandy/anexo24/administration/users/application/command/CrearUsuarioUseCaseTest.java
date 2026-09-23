package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CrearUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private PasswordEncoder encoder;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void creaConHashYAuditaDetalleSeguro() {
        UsuarioAdministracion usuario = usuario();
        when(encoder.encode("Abcdefgh1!")).thenReturn("hash");
        when(comandos.crear("op01", "Nombre", "correo@test", "hash", null, 7L)).thenReturn(8L);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        when(consultas.findById(8L)).thenReturn(Optional.of(usuario));
        assertThat(useCase().ejecutar(command(), "req")).isSameAs(usuario);
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=8;perfilId=7");
        assertThat(evento.getValue().detalle()).doesNotContain("hash", "correo@test");
    }

    @Test
    void commandDevuelveErroresDePerfilYDuplicado() {
        when(encoder.encode(anyString())).thenReturn("hash");
        when(comandos.crear(anyString(), anyString(), anyString(), anyString(), any(), anyLong()))
                .thenThrow(new RecursoNoEncontradoException());
        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        reset(comandos);
        when(comandos.crear(anyString(), anyString(), anyString(), anyString(), any(), anyLong()))
                .thenThrow(new EstadoIncompatibleException());
        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);
        reset(comandos);
        when(comandos.crear(anyString(), anyString(), anyString(), anyString(), any(), anyLong()))
                .thenThrow(new RecursoDuplicadoException());
        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(RecursoDuplicadoException.class);
        verifyNoInteractions(actor, bitacora, consultas);
    }

    @Test
    void passwordInvalidoNoLlegaAlCommand() {
        assertThatThrownBy(() -> useCase().ejecutar(command("corta"), "req"))
                .isInstanceOf(com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException.class);
        verifyNoInteractions(comandos, encoder, bitacora, actor, consultas);
    }

    @Test
    void falloBitacoraSePropagaYNoRelee() {
        when(encoder.encode("Abcdefgh1!")).thenReturn("hash");
        when(comandos.crear(anyString(), anyString(), anyString(), anyString(), any(), anyLong())).thenReturn(8L);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        doThrow(new IllegalStateException("fallo")).when(bitacora).registrar(any(BitacoraEvento.class));
        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(consultas);
    }

    @Test
    void usaTransactionManagerApp24() throws Exception {
        Method method = CrearUsuarioUseCase.class.getDeclaredMethod("ejecutar", CrearUsuarioCommand.class, String.class);
        assertThat(method.getAnnotation(Transactional.class).transactionManager()).isEqualTo("appTransactionManager");
    }

    private CrearUsuarioUseCase useCase() { return new CrearUsuarioUseCase(comandos, consultas, encoder, bitacora, actor); }
    private CrearUsuarioCommand command() { return command("Abcdefgh1!"); }
    private CrearUsuarioCommand command(String password) { return new CrearUsuarioCommand("op01", "Nombre", "correo@test", password, null, 7L); }
    private UsuarioAdministracion usuario() { return new UsuarioAdministracion(8L, "op01", "Nombre", "correo@test", "ACTIVO", LocalDate.now(), 7L, "Admin"); }
}
