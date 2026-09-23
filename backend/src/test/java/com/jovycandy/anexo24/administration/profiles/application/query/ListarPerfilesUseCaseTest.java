package com.jovycandy.anexo24.administration.profiles.application.query;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de normalización y validación del listado de perfiles. */
@ExtendWith(MockitoExtension.class)
class ListarPerfilesUseCaseTest {

    @Mock private PerfilConsultaRepository perfilConsultaRepository;
    private ListarPerfilesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarPerfilesUseCase(perfilConsultaRepository);
    }

    @Test
    void sinFiltrosValidosDelegaConNulos() {
        Pagina<PerfilAdministracion> pagina = new Pagina<>(List.of(), 0L, 1, 20);
        when(perfilConsultaRepository.findPage(null, null, 1, 20)).thenReturn(pagina);

        Pagina<PerfilAdministracion> resultado = useCase.ejecutar(null, null, 1, 20);

        verify(perfilConsultaRepository).findPage(null, null, 1, 20);
        assertThat(resultado).isSameAs(pagina);
    }

    @Test
    void nombreConEspaciosSeRecortaYBlankSeNormalizaANull() {
        when(perfilConsultaRepository.findPage(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar("  Administrador  ", "   ", 1, 20);

        verify(perfilConsultaRepository).findPage("Administrador", null, 1, 20);
    }

    @Test
    void nombreMayorAOchentaCaracteresRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar("x".repeat(81), null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void estadoSeRecortaYNormalizaAMayuscula() {
        when(perfilConsultaRepository.findPage(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar(null, " activo ", 1, 20);

        verify(perfilConsultaRepository).findPage(null, "ACTIVO", 1, 20);
    }

    @Test
    void estadoInvalidoRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, "PENDIENTE", 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void paginaMenorAUnoRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void tamanoFueraDeRangoRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> useCase.ejecutar(null, null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}
