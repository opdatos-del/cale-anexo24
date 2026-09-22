package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.api.dto.CrearUsuarioRequest;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.PerfilReferenciaRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearUsuarioUseCaseTest {
    @Mock UsuarioComandoRepository comandos;
    @Mock UsuarioConsultaRepository consultas;
    @Mock PerfilReferenciaRepository perfiles;
    @Mock PasswordEncoder encoder;
    @Mock RegistrarEventoBitacoraService bitacora;
    @Mock AuthenticatedUserContext actor;

    @Test
    void creaConHashActorYDetalleSeguro() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(" op01 ", " Nombre ", " correo@test ", " sin-trim ", null, 7L);
        UsuarioAdministracion usuario = new UsuarioAdministracion(8L, "op01", "Nombre", "correo@test", "ACTIVO", null, 7L, "Admin");
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.of("ACTIVO"));
        when(comandos.crear("op01", "Nombre", "correo@test", "hash", null, 7L)).thenReturn(8L);
        when(encoder.encode(" sin-trim ")).thenReturn("hash");
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(99L, "admin")));
        when(consultas.findById(8L)).thenReturn(Optional.of(usuario));

        assertThat(useCase().ejecutar(request, "req-1")).isSameAs(usuario);
        verify(encoder).encode(" sin-trim ");
        verify(comandos).crear("op01", "Nombre", "correo@test", "hash", null, 7L);
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().usuarioId()).isEqualTo(99L);
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=8;perfilId=7");
        assertThat(evento.getValue().correlationId()).isEqualTo("req-1");
    }

    @Test
    void rechazaPerfilInexistenteSinCodificarOPersistir() {
        when(perfiles.findEstadoById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(request(), "req-1")).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(encoder, comandos, bitacora);
    }

    private CrearUsuarioUseCase useCase() { return new CrearUsuarioUseCase(comandos, consultas, perfiles, encoder, bitacora, actor); }
    private CrearUsuarioRequest request() { return new CrearUsuarioRequest("op", "Nombre", "c@test", "ficticio", LocalDate.now(), 7L); }
}
