package com.jovycandy.anexo24.administration.users.application.query;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del caso de uso de detalle read-only de usuario. */
@ExtendWith(MockitoExtension.class)
class ObtenerUsuarioUseCaseTest {

    @Mock
    private UsuarioConsultaRepository usuarioConsultaRepository;

    private ObtenerUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ObtenerUsuarioUseCase(usuarioConsultaRepository);
    }

    private UsuarioAdministracion usuario() {
        return new UsuarioAdministracion(42L, "op01", "Operador Uno",
                "op@example.test", "ACTIVO", LocalDate.of(2026, 12, 31), 3L, "ADMINISTRADOR");
    }

    @Test
    void idPositivoEncontradoDevuelveUsuario() {
        when(usuarioConsultaRepository.findById(42L))
                .thenReturn(Optional.of(usuario()));

        UsuarioAdministracion resultado = useCase.ejecutar(42L);

        assertThat(resultado).isEqualTo(usuario());
        verify(usuarioConsultaRepository).findById(42L);
    }

    @Test
    void idCeroRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(0L))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void idNegativoRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(-5L))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void idNuloRechaza() {
        assertThatThrownBy(() -> useCase.ejecutar(null))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void idInexistenteLanzaRecursoNoEncontrado() {
        when(usuarioConsultaRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(42L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}