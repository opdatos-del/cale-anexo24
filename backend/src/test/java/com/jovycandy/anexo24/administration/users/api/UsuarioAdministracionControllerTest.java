package com.jovycandy.anexo24.administration.users.api;

import com.jovycandy.anexo24.administration.users.api.dto.UsuarioAdministracionDto;
import com.jovycandy.anexo24.administration.users.application.query.ListarUsuariosUseCase;
import com.jovycandy.anexo24.administration.users.application.query.ObtenerUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del controlador read-only de usuarios. */
@ExtendWith(MockitoExtension.class)
class UsuarioAdministracionControllerTest {

    @Mock
    private ListarUsuariosUseCase listarUsuariosUseCase;

    @Mock
    private ObtenerUsuarioUseCase obtenerUsuarioUseCase;

    private UsuarioAdministracion usuario() {
        return new UsuarioAdministracion(42L, "op01", "Operador Uno",
                "op@example.test", "ACTIVO", LocalDate.of(2026, 12, 31), 3L, "ADMINISTRADOR");
    }

    @Test
    void listadoDelegaParametrosYConvierteDominioADto() {
        UsuarioAdministracion usuario = usuario();
        Pagina<UsuarioAdministracion> pagina = new Pagina<>(List.of(usuario), 7L, 2, 10);
        when(listarUsuariosUseCase.ejecutar(
                "op01", "Juan", "op@example.test", "ACTIVO", 3L, 2, 10))
                .thenReturn(pagina);

        ResponseEntity<Pagina<UsuarioAdministracionDto>> resultado =
                controller().listar("op01", "Juan", "op@example.test", "ACTIVO", 3L, 2, 10);

        verify(listarUsuariosUseCase).ejecutar(
                "op01", "Juan", "op@example.test", "ACTIVO", 3L, 2, 10);
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isNotNull();
        assertThat(resultado.getBody().total()).isEqualTo(7L);
        assertThat(resultado.getBody().pagina()).isEqualTo(2);
        assertThat(resultado.getBody().tamano()).isEqualTo(10);
        assertThat(resultado.getBody().items())
                .containsExactly(UsuarioAdministracionDto.from(usuario));
    }

    @Test
    void detalleDevuelveDto() {
        when(obtenerUsuarioUseCase.ejecutar(42L)).thenReturn(usuario());

        ResponseEntity<UsuarioAdministracionDto> resultado = controller().obtener(42L);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(UsuarioAdministracionDto.from(usuario()));
    }

    @Test
    void detallePropagaRecursoNoEncontrado() {
        when(obtenerUsuarioUseCase.ejecutar(42L))
                .thenThrow(new RecursoNoEncontradoException());

        assertThatThrownBy(() -> controller().obtener(42L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listadoExigePermisoUsuariosAdministrar() throws NoSuchMethodException {
        Method listar = UsuarioAdministracionController.class.getDeclaredMethod(
                "listar",
                String.class,
                String.class,
                String.class,
                String.class,
                Long.class,
                int.class,
                int.class);

        PreAuthorize autorizacion = listar.getAnnotation(PreAuthorize.class);

        assertThat(autorizacion).isNotNull();
        assertThat(autorizacion.value()).isEqualTo("hasAuthority('USUARIOS_ADMINISTRAR')");
    }

    @Test
    void detalleExigePermisoUsuariosAdministrar() throws NoSuchMethodException {
        Method obtener = UsuarioAdministracionController.class.getDeclaredMethod(
                "obtener", Long.class);

        PreAuthorize autorizacion = obtener.getAnnotation(PreAuthorize.class);

        assertThat(autorizacion).isNotNull();
        assertThat(autorizacion.value()).isEqualTo("hasAuthority('USUARIOS_ADMINISTRAR')");
    }

    private UsuarioAdministracionController controller() {
        return new UsuarioAdministracionController(listarUsuariosUseCase, obtenerUsuarioUseCase);
    }
}