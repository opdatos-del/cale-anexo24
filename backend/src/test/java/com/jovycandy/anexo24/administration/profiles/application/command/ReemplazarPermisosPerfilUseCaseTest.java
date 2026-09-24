package com.jovycandy.anexo24.administration.profiles.application.command;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
import com.jovycandy.anexo24.administration.profiles.application.command.model.ReemplazarPermisosPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilComandoRepository;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilPermisosConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas del reemplazo atómico e idempotente de permisos. */
@ExtendWith(MockitoExtension.class)
class ReemplazarPermisosPerfilUseCaseTest {
    @Mock private PerfilComandoRepository comandoRepository;
    @Mock private PerfilPermisosConsultaRepository consultaRepository;
    @Mock private RegistrarEventoBitacoraService bitacoraService;
    @Mock private AuthenticatedUserContext authenticatedUserContext;
    private ReemplazarPermisosPerfilUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReemplazarPermisosPerfilUseCase(comandoRepository, consultaRepository,
                bitacoraService, authenticatedUserContext);
    }

    @Test
    void mismoConjuntoEnOtroOrdenNoEjecutaCommandNiBitacoraNiConsultaActor() {
        PerfilPermisosDetalle actual = new PerfilPermisosDetalle(7L, "ADMIN", "ACTIVO", List.of(
                new ActividadAdministracion(2L, "B", "B", "recurso", "CONSULTAR"),
                new ActividadAdministracion(1L, "A", "A", "recurso", "CONSULTAR")));
        when(consultaRepository.findPermissionsByProfileId(7L)).thenReturn(Optional.of(actual));

        PerfilPermisosDetalle resultado = useCase.ejecutar(7L,
                new ReemplazarPermisosPerfilCommand(List.of(1L, 2L)), "corr-1");

        assertThat(resultado).isSameAs(actual);
        verify(consultaRepository).findPermissionsByProfileId(7L);
        verifyNoInteractions(comandoRepository, bitacoraService, authenticatedUserContext);
    }
}
