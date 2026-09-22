package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarEstadoUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void cambiaEstadoNormalizadoAuditaConActorYCorrelacion() {
        UsuarioAdministracion actual = usuario("ACTIVO");
        UsuarioAdministracion actualizado = usuario("INACTIVO");
        prepararCambio(actual, actualizado, "INACTIVO");

        assertThat(useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand(" inactivo "), "req-estado"))
                .isSameAs(actualizado);

        verify(comandos).actualizarEstado(42L, "INACTIVO");
        verify(comandos).existsConCapacidadAdministrativa(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().usuarioId()).isEqualTo(7L);
        assertThat(evento.getValue().correlationId()).isEqualTo("req-estado");
        assertThat(evento.getValue().detalle())
                .isEqualTo("usuarioObjetivoId=42;estadoAnterior=ACTIVO;estadoNuevo=INACTIVO")
                .doesNotContain("Nombre", "correo@test");
    }

    @Test
    void noOpNoActualizaAuditaNiConsultaActor() {
        UsuarioAdministracion actual = usuario("ACTIVO");
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));

        assertThat(useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand(" activo "), "req")).isSameAs(actual);

        verify(comandos, never()).actualizarEstado(anyLong(), anyString());
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void rechazaUsuarioInexistenteSinEfectos() {
        when(consultas.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(comandos, bitacora, actor);
    }

    @Test
    void fallaCerradoCuandoNoHayActorAntesDeActualizar() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "req"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Actor autenticado no disponible");

        verify(comandos, never()).actualizarEstado(anyLong(), anyString());
        verifyNoInteractions(bitacora);
    }

    @Test
    void updateSinFilaAfectadaEsNoEncontradoSinAuditar() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(comandos.actualizarEstado(42L, "INACTIVO")).thenReturn(0);

        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(bitacora);
    }

    @Test
    void rechazaAutoInactivacionSinActualizarNiAuditar() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(42L, "admin")));

        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);

        verify(comandos, never()).actualizarEstado(anyLong(), anyString());
        verifyNoInteractions(bitacora);
    }

    @Test
    void rechazaCuandoGuardrailAdministrativoEsFalsoSinAuditar() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario("ACTIVO")));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(comandos.actualizarEstado(42L, "INACTIVO")).thenReturn(1);
        when(comandos.existsConCapacidadAdministrativa(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarEstadoUsuarioCommand("INACTIVO"), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);

        verifyNoInteractions(bitacora);
    }

    @Test
    void ejecutarUsaTransaccionSerializableDelAplicativo() throws NoSuchMethodException {
        Method metodo = CambiarEstadoUsuarioUseCase.class.getDeclaredMethod(
                "ejecutar", Long.class, CambiarEstadoUsuarioCommand.class, String.class);
        Transactional transactional = metodo.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(transactional.isolation()).isEqualTo(Isolation.SERIALIZABLE);
        assertThat(transactional.propagation().name()).isEqualTo("REQUIRED");
    }

    private void prepararCambio(UsuarioAdministracion actual, UsuarioAdministracion actualizado, String estado) {
        AtomicInteger lecturas = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(invocacion ->
                lecturas.getAndIncrement() == 0 ? Optional.of(actual) : Optional.of(actualizado));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(comandos.actualizarEstado(42L, estado)).thenReturn(1);
        when(comandos.existsConCapacidadAdministrativa(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    private CambiarEstadoUsuarioUseCase useCase() {
        return new CambiarEstadoUsuarioUseCase(comandos, consultas, bitacora, actor);
    }

    private UsuarioAdministracion usuario(String estado) {
        return new UsuarioAdministracion(42L, "op", "Nombre", "correo@test", estado, null, 1L, "Admin");
    }
}
