package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CrearUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.PerfilReferenciaRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private PerfilReferenciaRepository perfiles;
    @Mock private PasswordEncoder encoder;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void creaConHashActorYDetalleSeguro() {
        CrearUsuarioCommand command = command(" op01 ", " Nombre ", " correo@test ", " Abcdefgh1! ");
        UsuarioAdministracion usuario = usuario();
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("ACTIVO"));
        when(encoder.encode(" Abcdefgh1! ")).thenReturn("hash-seguro");
        when(comandos.crear("op01", "Nombre", "correo@test", "hash-seguro", null, 7L)).thenReturn(8L);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        when(consultas.findById(8L)).thenReturn(Optional.of(usuario));

        assertThat(useCase().ejecutar(command, "req-1")).isSameAs(usuario);

        verify(encoder).encode(" Abcdefgh1! ");
        verify(comandos).crear("op01", "Nombre", "correo@test", "hash-seguro", null, 7L);
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().usuarioId()).isEqualTo(99L);
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=8;perfilId=7");
        assertThat(evento.getValue().detalle()).doesNotContain("op01", "Nombre", "correo@test", "hash-seguro");
        assertThat(evento.getValue().correlationId()).isEqualTo("req-1");
    }

    @Test
    void rechazaPerfilInexistenteSinCodificarPersistirNiAuditar() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(encoder, comandos, bitacora);
    }

    @Test
    void rechazaPerfilInactivoSinCodificarPersistirNiAuditar() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("inactivo"));

        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);

        verifyNoInteractions(encoder, comandos, bitacora);
    }

    @Test
    void rechazaClaveDuplicadaSinCodificarPersistirNiAuditar() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("ACTIVO"));
        when(comandos.existsByClave("op01")).thenReturn(true);

        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(comandos, never()).existsByCorreo("correo@test");
        verifyNoInteractions(encoder, bitacora);
        verify(comandos, never()).crear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaCorreoDuplicadoSinCodificarPersistirNiAuditar() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("ACTIVO"));
        when(comandos.existsByCorreo("correo@test")).thenReturn(true);

        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(RecursoDuplicadoException.class);

        verifyNoInteractions(encoder, bitacora);
        verify(comandos, never()).crear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void fallaCerradoCuandoNoHayActor() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("ACTIVO"));
        when(encoder.encode("Abcdefgh1!")).thenReturn("hash-seguro");
        when(comandos.crear("op01", "Nombre", "correo@test", "hash-seguro", null, 7L)).thenReturn(8L);
        when(actor.currentUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(bitacora, consultas);
    }

    @Test
    void propagaFalloDeBitacora() {
        prepararCreacionValida();
        doThrow(new IllegalStateException("fallo bitacora")).when(bitacora)
                .registrar(org.mockito.ArgumentMatchers.any(BitacoraEvento.class));

        assertThatThrownBy(() -> useCase().ejecutar(command(), "req"))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo bitacora");

        verifyNoInteractions(consultas);
    }

    @Test
    void rechazaPasswordsFueraDePolitica() {
        for (String password : new String[]{"Abcdef1!", "abcdefgh1!", "Abcdefgh!!", "Abcdefgh11", "Abcdefghij1!Abcdefghij1!Abcdefghij1!Abcdefghij1!Abc"}) {
            assertThatThrownBy(() -> useCase().ejecutar(command("op01", "Nombre", "correo@test", password), "req"))
                    .isInstanceOf(SolicitudInvalidaException.class);
        }
        verifyNoInteractions(perfiles, comandos, encoder, bitacora, actor, consultas);
    }

    @Test
    void ejecutarUsaAppTransactionManager() throws NoSuchMethodException {
        Method metodo = CrearUsuarioUseCase.class.getDeclaredMethod("ejecutar", CrearUsuarioCommand.class, String.class);
        Transactional transactional = metodo.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(transactional.propagation().name()).isEqualTo("REQUIRED");
    }

    private void prepararCreacionValida() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("ACTIVO"));
        when(encoder.encode("Abcdefgh1!")).thenReturn("hash-seguro");
        when(comandos.crear("op01", "Nombre", "correo@test", "hash-seguro", null, 7L)).thenReturn(8L);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
    }

    private CrearUsuarioUseCase useCase() {
        return new CrearUsuarioUseCase(comandos, consultas, perfiles, encoder, bitacora, actor);
    }

    private CrearUsuarioCommand command() {
        return command("op01", "Nombre", "correo@test", "Abcdefgh1!");
    }

    private CrearUsuarioCommand command(String clave, String nombre, String correo, String password) {
        return new CrearUsuarioCommand(clave, nombre, correo, password, null, 7L);
    }

    private UsuarioAdministracion usuario() {
        return new UsuarioAdministracion(8L, "op01", "Nombre", "correo@test", "ACTIVO", LocalDate.now(), 7L, "Admin");
    }
}
