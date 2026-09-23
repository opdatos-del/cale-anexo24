package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.RestablecerPasswordUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestablecerPasswordUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private PasswordPolicy passwordPolicy;
    @Mock private PasswordEncoder encoder;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void restableceConHashYAuditaDetalleSeguroEnOrden() {
        RestablecerPasswordUsuarioCommand command = new RestablecerPasswordUsuarioCommand("Abcdefgh1!");
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        when(encoder.encode("Abcdefgh1!")).thenReturn("hash-bcrypt");

        useCase().ejecutar(42L, command, "corr-1");

        InOrder orden = inOrder(passwordPolicy, actor, encoder, comandos, bitacora);
        orden.verify(passwordPolicy).validar("Abcdefgh1!");
        orden.verify(actor).currentUser();
        orden.verify(encoder).encode("Abcdefgh1!");
        orden.verify(comandos).restablecerPassword(42L, "hash-bcrypt");
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        orden.verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().accion()).isEqualTo(BitacoraAccion.USUARIO_PASSWORD_RESTABLECIDA);
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=42");
        assertThat(evento.getValue().detalle()).doesNotContain("password", "hash", "Abcdefgh1!");
    }

    @Test
    void rechazaIdNullONoPositivoAntesDePassword() {
        assertThatThrownBy(() -> useCase().ejecutar(null, command(), null)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> useCase().ejecutar(0L, command(), null)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> useCase().ejecutar(-1L, command(), null)).isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(passwordPolicy, actor, encoder, comandos, bitacora);
    }

    @Test
    void passwordInvalidoNoResuelveActorNiModifica() {
        doThrow(new SolicitudInvalidaException("inválida")).when(passwordPolicy).validar("corta");
        assertThatThrownBy(() -> useCase().ejecutar(42L, new RestablecerPasswordUsuarioCommand("corta"), null))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(actor, encoder, comandos, bitacora);
    }

    @Test
    void actorAusenteNoCodificaNiModifica() {
        when(actor.currentUser()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, command(), null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(encoder, comandos, bitacora);
    }

    @Test
    void propagaUsuarioInexistenteYFalloCommandSinAuditar() {
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        when(encoder.encode(anyString())).thenReturn("hash");
        doThrow(new RecursoNoEncontradoException()).when(comandos).restablecerPassword(42L, "hash");
        assertThatThrownBy(() -> useCase().ejecutar(42L, command(), null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(bitacora);
    }

    @Test
    void falloBitacoraSePropagaParaRollbackExterior() {
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        when(encoder.encode(anyString())).thenReturn("hash");
        doThrow(new IllegalStateException("fallo bitácora")).when(bitacora).registrar(any());
        assertThatThrownBy(() -> useCase().ejecutar(42L, command(), null))
                .isInstanceOf(IllegalStateException.class);
        verify(comandos).restablecerPassword(42L, "hash");
    }

    @Test
    void usaAppTransactionManagerSinSerializableNiRequiresNew() throws Exception {
        Method method = RestablecerPasswordUsuarioUseCase.class.getDeclaredMethod(
                "ejecutar", Long.class, RestablecerPasswordUsuarioCommand.class, String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(transactional.isolation()).isEqualTo(Isolation.DEFAULT);
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    private RestablecerPasswordUsuarioUseCase useCase() {
        return new RestablecerPasswordUsuarioUseCase(comandos, passwordPolicy, encoder, bitacora, actor);
    }

    private RestablecerPasswordUsuarioCommand command() {
        return new RestablecerPasswordUsuarioCommand("Abcdefgh1!");
    }
}
