package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.api.dto.ActualizarUsuarioRequest;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActualizarUsuarioUseCaseTest {
    @Mock UsuarioComandoRepository comandos;
    @Mock UsuarioConsultaRepository consultas;
    @Mock RegistrarEventoBitacoraService bitacora;
    @Mock AuthenticatedUserContext actor;

    @Test
    void noOpNoActualizaNiAudita() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));
        assertThat(useCase().ejecutar(42L, new ActualizarUsuarioRequest(" Nombre ", " correo@test "), "req")).isSameAs(actual);
        verify(comandos).existsByCorreoExceptoUsuario("correo@test", 42L);
        verify(comandos, never()).actualizarDatos(anyLong(), anyString(), anyString());
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void actualizaYAditaSoloCamposModificados() {
        UsuarioAdministracion actual = usuario("Nombre", "correo@test");
        UsuarioAdministracion actualizado = usuario("Nuevo", "correo@test");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual), Optional.of(actualizado));
        when(comandos.actualizarDatos(42L, "Nuevo", "correo@test")).thenReturn(1);
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        assertThat(useCase().ejecutar(42L, new ActualizarUsuarioRequest("Nuevo", "correo@test"), "req")).isSameAs(actualizado);
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=42;campos=nombre");
        assertThat(evento.getValue().usuarioId()).isEqualTo(7L);
    }

    private ActualizarUsuarioUseCase useCase() { return new ActualizarUsuarioUseCase(comandos, consultas, bitacora, actor); }
    private UsuarioAdministracion usuario(String nombre, String correo) { return new UsuarioAdministracion(42L, "op", nombre, correo, "ACTIVO", null, 1L, "Perfil"); }
}
