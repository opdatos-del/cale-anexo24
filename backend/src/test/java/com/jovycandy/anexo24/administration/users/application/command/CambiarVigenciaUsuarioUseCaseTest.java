package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.administration.users.application.command.model.CambiarVigenciaUsuarioCommand;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CambiarVigenciaUsuarioUseCaseTest {
    @Mock private UsuarioComandoRepository comandos;
    @Mock private UsuarioConsultaRepository consultas;
    @Mock private RegistrarEventoBitacoraService bitacora;
    @Mock private AuthenticatedUserContext actor;

    @Test
    void cambiaVigenciaIncluyeNullActorFechaYAudita() {
        LocalDate nueva = LocalDate.of(2027, 12, 31);
        AtomicInteger n = new AtomicInteger();
        when(consultas.findById(42L)).thenAnswer(i -> Optional.of(usuario(n.getAndIncrement() == 0 ? null : nueva)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(nueva), "req");
        verify(comandos).actualizarVigencia(eq(42L), eq(nueva), eq(7L), any(LocalDate.class));
        verify(bitacora).registrar(any());
    }

    @Test
    void noOpNoLlamaCommandNiActor() {
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(null)));
        assertThat(useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(null), "r")).isNotNull();
        verifyNoInteractions(comandos, actor, bitacora);
    }

    @Test
    void lastAdminVieneDelCommandYFechaPasadaEsPermitida() {
        LocalDate pasada = LocalDate.now().minusDays(1);
        when(consultas.findById(42L)).thenReturn(Optional.of(usuario(null)));
        when(actor.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(7L, "admin")));
        doThrow(new EstadoIncompatibleException()).when(comandos)
                .actualizarVigencia(eq(42L), eq(pasada), eq(7L), any(LocalDate.class));
        assertThatThrownBy(() -> useCase().ejecutar(42L, new CambiarVigenciaUsuarioCommand(pasada), "r"))
                .isInstanceOf(EstadoIncompatibleException.class);
    }

    @Test
    void conservaTransaccionSerializable() throws Exception {
        Method m = CambiarVigenciaUsuarioUseCase.class.getDeclaredMethod("ejecutar", Long.class, CambiarVigenciaUsuarioCommand.class, String.class);
        Transactional t = m.getAnnotation(Transactional.class);
        assertThat(t.transactionManager()).isEqualTo("appTransactionManager");
        assertThat(t.isolation()).isEqualTo(Isolation.SERIALIZABLE);
    }

    private CambiarVigenciaUsuarioUseCase useCase() { return new CambiarVigenciaUsuarioUseCase(comandos, consultas, bitacora, actor); }
    private UsuarioAdministracion usuario(LocalDate vigencia) { return new UsuarioAdministracion(42L, "op", "N", "c", "ACTIVO", vigencia, 1L, "P"); }
}
