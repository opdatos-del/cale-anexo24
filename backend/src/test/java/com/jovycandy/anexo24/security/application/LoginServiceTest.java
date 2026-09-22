package com.jovycandy.anexo24.security.application;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAcceso;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.security.CredencialesInvalidasException;
import com.jovycandy.anexo24.security.JwtTokenService;
import com.jovycandy.anexo24.security.api.dto.LoginRequest;
import com.jovycandy.anexo24.security.api.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas de auditoría durante el inicio de sesión. */
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String CORRELATION_ID = "req-123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService tokenService;

    @Mock
    private RegistrarEventoBitacoraService bitacoraService;

    private LoginService service;

    @BeforeEach
    void setUp() {
        service = new LoginService(usuarioRepository, passwordEncoder, tokenService, bitacoraService);
    }

    @Test
    void loginExitosoAuditaAntesDeGenerarTokenYConservaResponse() {
        UsuarioApp usuario = usuarioActivo();
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        List<String> permisos = List.of("OPERACIONES_CONSULTAR");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.password(), usuario.passwordHash())).thenReturn(true);
        when(usuarioRepository.findAccesoByUsuario(usuario.id()))
                .thenReturn(Optional.of(new UsuarioAcceso(7L, "ACTIVO", permisos)));
        when(tokenService.generateToken(usuario.id(), usuario.clave(), permisos)).thenReturn("token-ficticio");
        when(tokenService.expirationMinutes()).thenReturn(15L);

        LoginResponse response = service.login(request, CORRELATION_ID);

        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        InOrder orden = inOrder(bitacoraService, tokenService);
        orden.verify(bitacoraService).registrar(evento.capture());
        orden.verify(tokenService).generateToken(usuario.id(), usuario.clave(), permisos);
        assertThat(evento.getValue())
                .extracting(BitacoraEvento::usuarioId, BitacoraEvento::modulo,
                        BitacoraEvento::accion, BitacoraEvento::resultado,
                        BitacoraEvento::detalle, BitacoraEvento::correlationId)
                .containsExactly(usuario.id(), BitacoraModulo.SEGURIDAD,
                        BitacoraAccion.LOGIN_OK, BitacoraResultado.EXITO,
                        null, CORRELATION_ID);
        assertThat(response).isEqualTo(new LoginResponse("token-ficticio", 15L, usuario.nombre(), permisos));
    }

    @Test
    void usuarioInexistenteAuditaSinActorYConservaCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("desconocido", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas o cuenta no disponible.");

        assertEventoFallido(null);
        verify(tokenService, never()).generateToken(any(), any(), any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void passwordIncorrectoAuditaActorConocidoYConservaCredencialesInvalidas() {
        UsuarioApp usuario = usuarioActivo();
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.password(), usuario.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas o cuenta no disponible.");

        assertEventoFallido(usuario.id());
        verify(tokenService, never()).generateToken(any(), any(), any());
        verify(usuarioRepository, never()).findAccesoByUsuario(anyLong());
    }

    @Test
    void usuarioInactivoAuditaActorConocidoYConservaCredencialesInvalidas() {
        UsuarioApp usuario = new UsuarioApp(42L, "operador", "Operador", "operador@example.test",
                "hash-ficticio", "INACTIVO", null, 1L);
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas o cuenta no disponible.");

        assertEventoFallido(usuario.id());
        verifyNoInteractions(passwordEncoder);
        verify(tokenService, never()).generateToken(any(), any(), any());
        verify(usuarioRepository, never()).findAccesoByUsuario(anyLong());
    }

    @Test
    void falloDeWriterEnLoginExitosoSePropagaYSuprimeToken() {
        UsuarioApp usuario = usuarioActivo();
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        DataAccessResourceFailureException error = new DataAccessResourceFailureException("BD no disponible");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.password(), usuario.passwordHash())).thenReturn(true);
        when(usuarioRepository.findAccesoByUsuario(usuario.id()))
                .thenReturn(Optional.of(new UsuarioAcceso(7L, "ACTIVO", List.of())));
        doThrow(error).when(bitacoraService).registrar(any(BitacoraEvento.class));

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID)).isSameAs(error);

        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    void falloDeWriterEnLoginFallidoConservaCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("desconocido", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("Bitácora no disponible"))
                .when(bitacoraService).registrar(any(BitacoraEvento.class));

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas o cuenta no disponible.");

        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    void perfilActivoSinPermisosGeneraTokenConAutenticacionVacia() {
        UsuarioApp usuario = usuarioActivo();
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.password(), usuario.passwordHash())).thenReturn(true);
        when(usuarioRepository.findAccesoByUsuario(usuario.id()))
                .thenReturn(Optional.of(new UsuarioAcceso(7L, "ACTIVO", List.of())));
        when(tokenService.generateToken(usuario.id(), usuario.clave(), List.of()))
                .thenReturn("token-ficticio");
        when(tokenService.expirationMinutes()).thenReturn(15L);

        LoginResponse response = service.login(request, CORRELATION_ID);

        assertThat(response).isEqualTo(
                new LoginResponse("token-ficticio", 15L, usuario.nombre(), List.of()));
    }

    @Test
    void perfilInactivoAuditaFalloYNoGeneraToken() {
        UsuarioApp usuario = usuarioActivo();
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.password(), usuario.passwordHash())).thenReturn(true);
        when(usuarioRepository.findAccesoByUsuario(usuario.id()))
                .thenReturn(Optional.of(new UsuarioAcceso(7L, "INACTIVO",
                        List.of("OPERACIONES_CONSULTAR"))));

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas o cuenta no disponible.");

        assertEventoFallido(usuario.id());
        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    void accesoSinPerfilAuditaFalloYNoGeneraToken() {
        UsuarioApp usuario = usuarioActivo();
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        when(usuarioRepository.findByClave(request.clave())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.password(), usuario.passwordHash())).thenReturn(true);
        when(usuarioRepository.findAccesoByUsuario(usuario.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request, CORRELATION_ID))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas o cuenta no disponible.");

        assertEventoFallido(usuario.id());
        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    private void assertEventoFallido(Long usuarioId) {
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacoraService).registrar(evento.capture());
        assertThat(evento.getValue())
                .extracting(BitacoraEvento::usuarioId, BitacoraEvento::modulo,
                        BitacoraEvento::accion, BitacoraEvento::resultado,
                        BitacoraEvento::detalle, BitacoraEvento::correlationId)
                .containsExactly(usuarioId, BitacoraModulo.SEGURIDAD,
                        BitacoraAccion.LOGIN_FALLIDO, BitacoraResultado.FALLO,
                        null, CORRELATION_ID);
    }

    private UsuarioApp usuarioActivo() {
        return new UsuarioApp(42L, "operador", "Operador", "operador@example.test",
                "hash-ficticio", "ACTIVO", null, 1L);
    }
}
