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
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualizarUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void noOpNoActualizaAuditaNiConsultaActor() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));

        assertThat(useCase().ejecutar(42L, command(" Nombre ", " correo@test "), "req")).isSameAs(actual);

        verify(comandos).existsByCorreoExceptoUsuario("correo@test", 42L);
        verify(comandos, never()).actualizarDatos(anyLong(), anyString(), anyString());
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void actualizaSoloNombreYAuditaSinValoresPii() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        UsuarioAdministracion actualizado = usuario("Nuevo", "correo@test");
        prepararActualizacion(actual, actualizado, "Nuevo", "correo@test");

        assertThat(useCase().ejecutar(42L, command("Nuevo", "correo@test"), "req")).isSameAs(actualizado);

        validarEvento("usuarioObjetivoId=42;campos=nombre");
    }

    @Test
    void actualizaSoloCorreo() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        UsuarioAdministracion actualizado = usuario("Nombre", "nuevo@test");
        prepararActualizacion(actual, actualizado, "Nombre", "nuevo@test");

        useCase().ejecutar(42L, command("Nombre", "nuevo@test"), "req");

        validarEvento("usuarioObjetivoId=42;campos=correo");
    }

    @Test
    void actualizaNombreYCorreo() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        UsuarioAdministracion actualizado = usuario("Nuevo", "nuevo@test");
        prepararActualizacion(actual, actualizado, "Nuevo", "nuevo@test");

        useCase().ejecutar(42L, command("Nuevo", "nuevo@test"), "req");

        validarEvento("usuarioObjetivoId=42;campos=nombre,correo");
    }

    @Test
    void rechazaIdNuloCeroONegativo() {
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertThatThrownBy(() -> useCase().ejecutar(id, command("Nombre", "correo@test"), "req"))
                    .isInstanceOf(SolicitudInvalidaException.class);
        }
        verifyNoInteractions(comandos, consultas, bitacora, actor);
    }

    @Test
    void rechazaUsuarioInexistente() {
        when(consultas.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().ejecutar(42L, command("Nombre", "correo@test"), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(comandos, bitacora, actor);
    }

    @Test
    void rechazaCorreoDeOtroUsuario() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("Nombre", "correo@test")));
        when(comandos.existsByCorreoExceptoUsuario("nuevo@test", 42L)).thenReturn(true);

        assertThatThrownBy(() -> useCase().ejecutar(42L, command("Nombre", "nuevo@test"), "req"))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(comandos, never()).actualizarDatos(anyLong(), anyString(), anyString());
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void permiteCorreoActualDelPropioUsuario() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));

        assertThat(useCase().ejecutar(42L, command("Nombre", "correo@test"), "req")).isSameAs(actual);
        verify(comandos).existsByCorreoExceptoUsuario("correo@test", 42L);
    }

    @Test
    void updateSinFilaAfectadaEsNoEncontrado() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));
        when(comandos.actualizarDatos(42L, "Nuevo", "correo@test")).thenReturn(0);

        assertThatThrownBy(() -> useCase().ejecutar(42L, command("Nuevo", "correo@test"), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void fallaCerradoCuandoNoHayActor() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));
        when(comandos.actualizarDatos(42L, "Nuevo", "correo@test")).thenReturn(1);
        when(actor.currentUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().ejecutar(42L, command("Nuevo", "correo@test"), "req"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(bitacora);
    }

    @Test
    void propagaFalloDeBitacora() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));
        when(comandos.actualizarDatos(42L, "Nuevo", "correo@test")).thenReturn(1);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        doThrow(new IllegalStateException("fallo bitacora")).when(bitacora)
                .registrar(org.mockito.ArgumentMatchers.any(BitacoraEvento.class));

        assertThatThrownBy(() -> useCase().ejecutar(42L, command("Nuevo", "correo@test"), "req"))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo bitacora");
    }

    @Test
    void ejecutarUsaAppTransactionManager() throws NoSuchMethodException {
        Method metodo = ActualizarUsuarioUseCase.class.getDeclaredMethod(
                "ejecutar", Long.class, ActualizarUsuarioCommand.class, String.class);
        Transactional transactional = metodo.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(transactional.propagation().name()).isEqualTo("REQUIRED");
    }

    private void prepararActualizacion(UsuarioAdministracion actual, UsuarioAdministracion actualizado,
                                       String nombre, String correo) {
        AtomicInteger consultasRealizadas = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(invocacion ->
                consultasRealizadas.getAndIncrement() == 0 ? Optional.of(actual) : Optional.of(actualizado));
        when(comandos.actualizarDatos(42L, nombre, correo)).thenReturn(1);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
    }

    private void validarEvento(String detalle) {
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().detalle()).isEqualTo(detalle);
        assertThat(evento.getValue().detalle()).doesNotContain("Nombre", "Nuevo", "correo@test", "nuevo@test");
        assertThat(evento.getValue().usuarioId()).isEqualTo(7L);
    }

    private ActualizarUsuarioUseCase useCase() {
        return new ActualizarUsuarioUseCase(comandos, consultas, bitacora, actor);
    }

    private ActualizarUsuarioCommand command(String nombre, String correo) {
        return new ActualizarUsuarioCommand(nombre, correo);
    }

    private UsuarioAdministracion usuario(String nombre, String correo) {
        return new UsuarioAdministracion(42L, "op", nombre, correo, "ACTIVO", null, 1L, "Perfil");
    }
}
