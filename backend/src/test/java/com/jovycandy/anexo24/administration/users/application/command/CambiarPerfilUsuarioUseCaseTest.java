package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarPerfilUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.PerfilReferenciaRepository;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarPerfilUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private PerfilReferenciaRepository perfiles;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void cambiaAPerfilActivoSinDistinguirMayusculasYAseguraEvento() {
        UsuarioAdministracion actual = usuario(1L);
        UsuarioAdministracion actualizado = usuario(2L);
        AtomicInteger lecturas = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(invocacion ->
                lecturas.getAndIncrement() == 0 ? Optional.of(actual) : Optional.of(actualizado));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(perfiles.findEstadoById(2L)).thenReturn(Optional.of("activo"));
        when(comandos.actualizarPerfil(42L, 2L)).thenReturn(1);
        when(comandos.existsConCapacidadAdministrativa(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        assertThat(useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req-perfil")).isSameAs(actualizado);

        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().usuarioId()).isEqualTo(7L);
        assertThat(evento.getValue().correlationId()).isEqualTo("req-perfil");
        assertThat(evento.getValue().detalle()).isEqualTo("usuarioObjetivoId=42;perfilAnteriorId=1;perfilNuevoId=2")
                .doesNotContain("Nombre", "correo@test");
    }

    @Test
    void noOpNoActualizaAuditaNiConsultaActorOPerfil() {
        UsuarioAdministracion actual = usuario(1L);
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));

        assertThat(useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(1L), "req")).isSameAs(actual);

        verify(comandos, never()).actualizarPerfil(anyLong(), anyLong());
        verifyNoInteractions(perfiles, bitacora, actor);
    }

    @Test
    void rechazaUsuarioOPerfilInexistenteSinActualizarNiAuditar() {
        when(consultas.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(comandos, perfiles, bitacora, actor);

        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(1L)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(perfiles.findEstadoById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(comandos, never()).actualizarPerfil(anyLong(), anyLong());
        verifyNoInteractions(bitacora);
    }

    @Test
    void rechazaPerfilInactivoYAutoCambioSinActualizarNiAuditar() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(1L)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(perfiles.findEstadoById(2L)).thenReturn(Optional.of("INACTIVO"));
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);

        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(42L, "admin")));
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);
        verify(comandos, never()).actualizarPerfil(anyLong(), anyLong());
        verifyNoInteractions(bitacora);
    }

    @Test
    void fallaCerradoSiActorAusenteYUpdateCeroEsNoEncontrado() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(1L)));
        when(actor.currentUser()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(IllegalStateException.class);
        verify(comandos, never()).actualizarPerfil(anyLong(), anyLong());

        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(perfiles.findEstadoById(2L)).thenReturn(Optional.of("ACTIVO"));
        when(comandos.actualizarPerfil(42L, 2L)).thenReturn(0);
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(bitacora);
    }

    @Test
    void rechazaGuardrailAdministrativoFalsoSinAuditar() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(1L)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(perfiles.findEstadoById(2L)).thenReturn(Optional.of("ACTIVO"));
        when(comandos.actualizarPerfil(42L, 2L)).thenReturn(1);
        when(comandos.existsConCapacidadAdministrativa(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarPerfilUsuarioCommand(2L), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);
        verifyNoInteractions(bitacora);
    }

    @Test
    void ejecutarUsaTransaccionSerializableDelAplicativo() throws NoSuchMethodException {
        Method metodo = CambiarPerfilUsuarioUseCase.class.getDeclaredMethod(
                "ejecutar", Long.class, CambiarPerfilUsuarioCommand.class, String.class);
        Transactional transactional = metodo.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(transactional.isolation()).isEqualTo(Isolation.SERIALIZABLE);
        assertThat(transactional.propagation().name()).isEqualTo("REQUIRED");
    }

    private CambiarPerfilUsuarioUseCase useCase() {
        return new CambiarPerfilUsuarioUseCase(comandos, consultas, perfiles, bitacora, actor);
    }

    private UsuarioAdministracion usuario(Long perfilId) {
        return new UsuarioAdministracion(42L, "op", "Nombre", "correo@test", "ACTIVO", null, perfilId, "Perfil");
    }
}
