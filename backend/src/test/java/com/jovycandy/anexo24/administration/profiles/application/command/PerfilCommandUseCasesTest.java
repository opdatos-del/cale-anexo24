package com.jovycandy.anexo24.administration.profiles.application.command;

import com.jovycandy.anexo24.administration.profiles.application.command.model.ActualizarNombrePerfilCommand;
import com.jovycandy.anexo24.administration.profiles.application.command.model.CambiarEstadoPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.application.command.model.CrearPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilComandoRepository;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilPermisosConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas de validación, idempotencia y auditoría de commands de perfiles. */
@ExtendWith(MockitoExtension.class)
class PerfilCommandUseCasesTest {
    @Mock private PerfilComandoRepository comandoRepository;
    @Mock private PerfilPermisosConsultaRepository consultaRepository;
    @Mock private RegistrarEventoBitacoraService bitacoraService;
    @Mock private AuthenticatedUserContext authenticatedUserContext;

    private CrearPerfilUseCase crear;
    private ActualizarNombrePerfilUseCase actualizar;
    private CambiarEstadoPerfilUseCase cambiarEstado;

    @BeforeEach
    void setUp() {
        crear = new CrearPerfilUseCase(comandoRepository, consultaRepository, bitacoraService, authenticatedUserContext);
        actualizar = new ActualizarNombrePerfilUseCase(comandoRepository, consultaRepository, bitacoraService, authenticatedUserContext);
        cambiarEstado = new CambiarEstadoPerfilUseCase(comandoRepository, consultaRepository, bitacoraService, authenticatedUserContext);
    }

    @Test
    void crearValidaNombreAntesDeConsultarActorOPersistir() {
        assertThatThrownBy(() -> crear.ejecutar(new CrearPerfilCommand(" "), "corr-1"))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(comandoRepository, consultaRepository, bitacoraService, authenticatedUserContext);
    }

    @Test
    void crearNormalizaNombreRegistraActorYBitacora() {
        PerfilPermisosDetalle detalle = detalle(8L, "OPERACION", "ACTIVO");
        when(authenticatedUserContext.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(4L, "admin")));
        when(comandoRepository.crear("OPERACION")).thenReturn(8L);
        when(consultaRepository.findPermissionsByProfileId(8L)).thenReturn(Optional.of(detalle));

        PerfilAdministracion resultado = crear.ejecutar(new CrearPerfilCommand(" OPERACION "), "corr-1");

        assertThat(resultado).isEqualTo(new PerfilAdministracion(8L, "OPERACION", "ACTIVO", 0L));
        verify(comandoRepository).crear("OPERACION");
        assertBitacora(BitacoraAccion.PERFIL_CREADO, "perfilObjetivoId=8");
    }

    @Test
    void actualizarNoOpNoConsultaActorNiRegistraBitacora() {
        PerfilPermisosDetalle actual = detalle(7L, "ADMIN", "ACTIVO");
        when(consultaRepository.findPermissionsByProfileId(7L)).thenReturn(Optional.of(actual));

        assertThat(actualizar.ejecutar(7L, new ActualizarNombrePerfilCommand("ADMIN"), "corr-1"))
                .isEqualTo(new PerfilAdministracion(7L, "ADMIN", "ACTIVO", 0L));

        verify(comandoRepository, never()).actualizarNombre(any(), any());
        verifyNoInteractions(bitacoraService, authenticatedUserContext);
    }

    @Test
    void actualizarValidaIdYNombre() {
        assertThatThrownBy(() -> actualizar.ejecutar(0L, new ActualizarNombrePerfilCommand("N"), "corr-1"))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(consultaRepository, comandoRepository, bitacoraService, authenticatedUserContext);

        when(consultaRepository.findPermissionsByProfileId(7L)).thenReturn(Optional.of(detalle(7L, "ADMIN", "ACTIVO")));
        assertThatThrownBy(() -> actualizar.ejecutar(7L, new ActualizarNombrePerfilCommand(null), "corr-1"))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(comandoRepository, bitacoraService, authenticatedUserContext);
    }

    @Test
    void actualizarRegistraActorBitacoraYRecargaResultado() {
        when(authenticatedUserContext.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(4L, "admin")));
        when(consultaRepository.findPermissionsByProfileId(7L))
                .thenReturn(Optional.of(detalle(7L, "ADMIN", "ACTIVO")))
                .thenReturn(Optional.of(detalle(7L, "OPERACION", "ACTIVO")));

        PerfilAdministracion resultado = actualizar.ejecutar(7L, new ActualizarNombrePerfilCommand("OPERACION"), "corr-1");

        assertThat(resultado.nombre()).isEqualTo("OPERACION");
        verify(comandoRepository).actualizarNombre(7L, "OPERACION");
        assertBitacora(BitacoraAccion.PERFIL_ACTUALIZADO, "perfilObjetivoId=7;campo=nombre");
    }

    @Test
    void cambiarEstadoValidaEstadoYNoOpEvitaActorYBitacora() {
        when(consultaRepository.findPermissionsByProfileId(7L)).thenReturn(Optional.of(detalle(7L, "ADMIN", "ACTIVO")));
        assertThatThrownBy(() -> cambiarEstado.ejecutar(7L, new CambiarEstadoPerfilCommand("PENDIENTE"), "corr-1"))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(comandoRepository, bitacoraService, authenticatedUserContext);

        assertThat(cambiarEstado.ejecutar(7L, new CambiarEstadoPerfilCommand(" activo "), "corr-1").estado())
                .isEqualTo("ACTIVO");
        verify(comandoRepository, never()).cambiarEstado(any(), any(), any());
        verifyNoInteractions(bitacoraService, authenticatedUserContext);
    }

    @Test
    void cambiarEstadoNormalizaRegistraActorBitacoraYFechaActual() {
        when(authenticatedUserContext.currentUser()).thenReturn(Optional.of(new AuthenticatedUserPrincipal(4L, "admin")));
        when(consultaRepository.findPermissionsByProfileId(7L))
                .thenReturn(Optional.of(detalle(7L, "ADMIN", "ACTIVO")))
                .thenReturn(Optional.of(detalle(7L, "ADMIN", "INACTIVO")));

        PerfilAdministracion resultado = cambiarEstado.ejecutar(7L, new CambiarEstadoPerfilCommand(" inactivo "), "corr-1");

        assertThat(resultado.estado()).isEqualTo("INACTIVO");
        verify(comandoRepository).cambiarEstado(eq(7L), eq("INACTIVO"), eq(LocalDate.now()));
        assertBitacora(BitacoraAccion.PERFIL_ESTADO_CAMBIADO,
                "perfilObjetivoId=7;estadoAnterior=ACTIVO;estadoNuevo=INACTIVO");
    }

    private PerfilPermisosDetalle detalle(Long id, String nombre, String estado) {
        return new PerfilPermisosDetalle(id, nombre, estado, List.of());
    }

    private void assertBitacora(BitacoraAccion accion, String detalle) {
        ArgumentCaptor<BitacoraEvento> captor = ArgumentCaptor.forClass(BitacoraEvento.class);
        verify(bitacoraService).registrar(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(4L);
        assertThat(captor.getValue().accion()).isEqualTo(accion);
        assertThat(captor.getValue().detalle()).isEqualTo(detalle);
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
    }
}
