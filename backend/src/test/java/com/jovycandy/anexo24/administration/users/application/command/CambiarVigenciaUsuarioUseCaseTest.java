package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarVigenciaUsuarioCommand;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarVigenciaUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void cambiaVigenciaYAseguraEventoActorCorrelacionYDetalle() {
        LocalDate anterior = LocalDate.of(2027, 1, 1);
        LocalDate nueva = LocalDate.of(2027, 12, 31);
        prepararCambio(anterior, nueva);

        useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(nueva), "req-vigencia");

        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().usuarioId()).isEqualTo(7L);
        assertThat(evento.getValue().correlationId()).isEqualTo("req-vigencia");
        assertThat(evento.getValue().detalle())
                .isEqualTo("usuarioObjetivoId=42;vigenciaAnterior=2027-01-01;vigenciaNueva=2027-12-31")
                .doesNotContain("Nombre", "correo@test");
    }

    @Test
    void permiteVigenciaNulaYRegistraEtiquetasSeguras() {
        prepararCambio(LocalDate.of(2027, 1, 1), null);

        useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(null), "req");

        verify(comandos).actualizarVigencia(42L, null);
        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacora).registrar(evento.capture());
        assertThat(evento.getValue().detalle()).contains("vigenciaNueva=SIN_VIGENCIA");
    }

    @Test
    void permiteVigenciaPasada() {
        LocalDate pasada = LocalDate.now().minusDays(1);
        prepararCambio(null, pasada);

        assertThat(useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(pasada), "req").vigencia())
                .isEqualTo(pasada);
        verify(comandos).actualizarVigencia(42L, pasada);
    }

    @Test
    void noOpNoActualizaAuditaNiConsultaActor() {
        UsuarioAdministracion actual = usuario(null);
        when(consultas.findById(42L)).thenReturn(Optional.of(actual));

        assertThat(useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(null), "req")).isSameAs(actual);

        verify(comandos, never()).actualizarVigencia(anyLong(), any());
        verifyNoInteractions(bitacora, actor);
    }

    @Test
    void rechazaInexistenteOActorAusenteAntesDeActualizar() {
        when(consultas.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(null), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(comandos, bitacora, actor);

        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(null)));
        when(actor.currentUser()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase().ejecutar(42L,
                new CambiarVigenciaUsuarioCommand(LocalDate.of(2027, 1, 1)), "req"))
                .isInstanceOf(IllegalStateException.class);
        verify(comandos, never()).actualizarVigencia(anyLong(), any());
        verifyNoInteractions(bitacora);
    }

    @Test
    void updateCeroEsNoEncontradoYGuardrailFalsoEsIncompatible() {
        LocalDate nueva = LocalDate.of(2027, 1, 1);
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(null)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(comandos.actualizarVigencia(42L, nueva)).thenReturn(0);
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(nueva), "req"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        when(comandos.actualizarVigencia(42L, nueva)).thenReturn(1);
        when(comandos.existsConCapacidadAdministrativa(any())).thenReturn(false);
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(nueva), "req"))
                .isInstanceOf(EstadoIncompatibleException.class);
        verifyNoInteractions(bitacora);
    }

    @Test
    void ejecutarUsaTransaccionSerializableDelAplicativo() throws NoSuchMethodException {
        Method metodo = CambiarVigenciaUsuarioUseCase.class.getDeclaredMethod(
                "ejecutar", Long.class, CambiarVigenciaUsuarioCommand.class, String.class);
        Transactional transactional = metodo.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(transactional.isolation()).isEqualTo(Isolation.SERIALIZABLE);
        assertThat(transactional.propagation().name()).isEqualTo("REQUIRED");
    }

    private void prepararCambio(LocalDate anterior, LocalDate nueva) {
        AtomicInteger lecturas = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(invocacion -> lecturas.getAndIncrement() == 0
                ? Optional.of(usuario(anterior)) : Optional.of(usuario(nueva)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        when(comandos.actualizarVigencia(42L, nueva)).thenReturn(1);
        when(comandos.existsConCapacidadAdministrativa(any())).thenReturn(true);
    }

    private CambiarVigenciaUsuarioUseCase useCase() {
        return new CambiarVigenciaUsuarioUseCase(comandos, consultas, bitacora, actor);
    }

    private UsuarioAdministracion usuario(LocalDate vigencia) {
        return new UsuarioAdministracion(42L, "op", "Nombre", "correo@test", "ACTIVO", vigencia, 1L, "Admin");
    }
}
