package com.jovycandy.anexo24.administration.users.application.query;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de normalización y validación del listado de usuarios. */
@ExtendWith(MockitoExtension.class)
class ListarUsuariosUseCaseTest {

    @Mock
    private UsuarioConsultaRepository usuarioConsultaRepository;

    private ListarUsuariosUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarUsuariosUseCase(usuarioConsultaRepository);
    }

    private String texto(int longitud) {
        return "x".repeat(longitud);
    }

    @Test
    void sinFiltrosValidosDelegaConNulos() {
        Pagina<UsuarioAdministracion> pagina = new Pagina<>(List.of(), 0L, 1, 20);
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                eq(1), eq(20))).thenReturn(pagina);

        Pagina<UsuarioAdministracion> resultado =
                useCase.ejecutar(null, null, null, null, null, 1, 20);

        verify(usuarioConsultaRepository).findPage(
                null, null, null, null, null, 1, 20);
        assertThat(resultado).isSameAs(pagina);
    }

    @Test
    void claveConEspaciosSeRecorta() {
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar("  op01  ", null, null, null, null, 1, 20);

        verify(usuarioConsultaRepository).findPage(
                "op01", null, null, null, null, 1, 20);
    }

    @Test
    void claveBlankSeNormalizaANull() {
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar("   ", null, null, null, null, 1, 20);

        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(usuarioConsultaRepository).findPage(
                clave.capture(), any(), any(), any(), any(), anyInt(), anyInt());
        assertThat(clave.getValue()).isNull();
    }

    @Test
    void nombreConEspaciosSeRecorta() {
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar(null, "  Juan Pérez  ", null, null, null, 1, 20);

        verify(usuarioConsultaRepository).findPage(
                null, "Juan Pérez", null, null, null, 1, 20);
    }

    @Test
    void nombreMayorAlLimiteDelDdlRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, texto(121), null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void claveMayorAlLimiteDelDdlRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(texto(31), null, null, null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void correoMayorAlLimiteDelDdlRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, texto(151), null, null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void estadoActivoEsValido() {
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar(null, null, null, "ACTIVO", null, 1, 20);

        verify(usuarioConsultaRepository).findPage(
                null, null, null, "ACTIVO", null, 1, 20);
    }

    @Test
    void estadoInactivoEsValido() {
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar(null, null, null, "INACTIVO", null, 1, 20);

        verify(usuarioConsultaRepository).findPage(
                null, null, null, "INACTIVO", null, 1, 20);
    }

    @Test
    void estadoMinusculaSeNormalizaAMayuscula() {
        when(usuarioConsultaRepository.findPage(any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        useCase.ejecutar(null, null, null, " activo ", null, 1, 20);

        verify(usuarioConsultaRepository).findPage(
                null, null, null, "ACTIVO", null, 1, 20);
    }

    @Test
    void estadoDesconocidoRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, null, "SUSPENDIDO", null, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void perfilIdCeroRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, null, null, 0L, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void perfilIdNegativoRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, null, null, -5L, 1, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void paginaCeroRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, null, null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void tamanoCeroRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, null, null, null, 1, 0))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void tamanoMayorACienRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null, null, null, null, null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}