package com.jovycandy.anexo24.administration.users.api;

import com.jovycandy.anexo24.administration.users.api.dto.ActualizarUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CrearUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CambiarEstadoUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CambiarPerfilUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CambiarVigenciaUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.UsuarioAdministracionDto;
import com.jovycandy.anexo24.administration.users.api.dto.RestablecerPasswordUsuarioRequest;
import com.jovycandy.anexo24.administration.users.application.command.ActualizarUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CrearUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CambiarEstadoUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CambiarPerfilUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CambiarVigenciaUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.RestablecerPasswordUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.model.ActualizarUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CrearUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CambiarEstadoUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CambiarPerfilUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CambiarVigenciaUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.RestablecerPasswordUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.query.ListarUsuariosUseCase;
import com.jovycandy.anexo24.administration.users.application.query.ObtenerUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

@ExtendWith(MockitoExtension.class)
class UsuarioAdministracionControllerTest {
    @Mock private ListarUsuariosUseCase listarUsuariosUseCase;
    @Mock private ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    @Mock private CrearUsuarioUseCase crearUsuarioUseCase;
    @Mock private ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    @Mock private CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;
    @Mock private CambiarPerfilUsuarioUseCase cambiarPerfilUsuarioUseCase;
    @Mock private CambiarVigenciaUsuarioUseCase cambiarVigenciaUsuarioUseCase;
    @Mock private RestablecerPasswordUsuarioUseCase restablecerPasswordUsuarioUseCase;
    @Mock private HttpServletRequest servletRequest;

    @Test
    void listadoDelegaParametrosYConvierteDominioADto() {
        UsuarioAdministracion usuario = usuario();
        Pagina<UsuarioAdministracion> pagina = new Pagina<>(List.of(usuario), 7L, 2, 10);
        when(listarUsuariosUseCase.ejecutar("op01", "Juan", "op@example.test", "ACTIVO", 3L, 2, 10))
                .thenReturn(pagina);

        ResponseEntity<Pagina<UsuarioAdministracionDto>> resultado = controller().listar(
                "op01", "Juan", "op@example.test", "ACTIVO", 3L, 2, 10);

        verify(listarUsuariosUseCase).ejecutar("op01", "Juan", "op@example.test", "ACTIVO", 3L, 2, 10);
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().items()).containsExactly(UsuarioAdministracionDto.from(usuario));
    }

    @Test
    void detalleDevuelveDtoYPropagaNoEncontrado() {
        when(obtenerUsuarioUseCase.ejecutar(42L)).thenReturn(usuario());
        assertThat(controller().obtener(42L).getBody()).isEqualTo(UsuarioAdministracionDto.from(usuario()));

        when(obtenerUsuarioUseCase.ejecutar(43L)).thenThrow(new RecursoNoEncontradoException());
        assertThatThrownBy(() -> controller().obtener(43L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearMapeaRequestACommandConCorrelacionYLocation() {
        CrearUsuarioRequest request = new CrearUsuarioRequest("op01", "Nombre", "correo@test", "Abcdefgh1!", null, 3L);
        when(servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR)).thenReturn("corr-1");
        when(crearUsuarioUseCase.ejecutar(org.mockito.ArgumentMatchers.any(CrearUsuarioCommand.class), org.mockito.ArgumentMatchers.eq("corr-1")))
                .thenReturn(usuario());

        ResponseEntity<UsuarioAdministracionDto> resultado = controller().crear(request, servletRequest);

        ArgumentCaptor<CrearUsuarioCommand> command = ArgumentCaptor.forClass(CrearUsuarioCommand.class);
        verify(crearUsuarioUseCase).ejecutar(command.capture(), org.mockito.ArgumentMatchers.eq("corr-1"));
        assertThat(command.getValue().clave()).isEqualTo("op01");
        assertThat(command.getValue().password()).isEqualTo("Abcdefgh1!");
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultado.getHeaders().getLocation()).hasToString("/api/v1/administracion/usuarios/42");
        assertThat(resultado.getBody()).isEqualTo(UsuarioAdministracionDto.from(usuario()));
    }

    @Test
    void actualizarMapeaRequestACommandConCorrelacion() {
        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest("Nombre", "correo@test");
        when(servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR)).thenReturn("corr-2");
        when(actualizarUsuarioUseCase.ejecutar(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.any(ActualizarUsuarioCommand.class), org.mockito.ArgumentMatchers.eq("corr-2")))
                .thenReturn(usuario());

        ResponseEntity<UsuarioAdministracionDto> resultado = controller().actualizar(42L, request, servletRequest);

        ArgumentCaptor<ActualizarUsuarioCommand> command = ArgumentCaptor.forClass(ActualizarUsuarioCommand.class);
        verify(actualizarUsuarioUseCase).ejecutar(org.mockito.ArgumentMatchers.eq(42L), command.capture(),
                org.mockito.ArgumentMatchers.eq("corr-2"));
        assertThat(command.getValue().nombre()).isEqualTo("Nombre");
        assertThat(command.getValue().correo()).isEqualTo("correo@test");
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(UsuarioAdministracionDto.from(usuario()));
    }

    @Test
    void patchesMapeanCommandsYCorrelacion() {
        when(servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR)).thenReturn("corr-3");
        when(cambiarEstadoUsuarioUseCase.ejecutar(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.any(CambiarEstadoUsuarioCommand.class), org.mockito.ArgumentMatchers.eq("corr-3"))).thenReturn(usuario());
        when(cambiarPerfilUsuarioUseCase.ejecutar(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.any(CambiarPerfilUsuarioCommand.class), org.mockito.ArgumentMatchers.eq("corr-3"))).thenReturn(usuario());
        when(cambiarVigenciaUsuarioUseCase.ejecutar(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.any(CambiarVigenciaUsuarioCommand.class), org.mockito.ArgumentMatchers.eq("corr-3"))).thenReturn(usuario());

        assertThat(controller().cambiarEstado(42L, new CambiarEstadoUsuarioRequest("ACTIVO"), servletRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller().cambiarPerfil(42L, new CambiarPerfilUsuarioRequest(7L), servletRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller().cambiarVigencia(42L, new CambiarVigenciaUsuarioRequest(null), servletRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cambiarEstadoUsuarioUseCase).ejecutar(42L, new CambiarEstadoUsuarioCommand("ACTIVO"), "corr-3");
        verify(cambiarPerfilUsuarioUseCase).ejecutar(42L, new CambiarPerfilUsuarioCommand(7L), "corr-3");
        verify(cambiarVigenciaUsuarioUseCase).ejecutar(42L, new CambiarVigenciaUsuarioCommand(null), "corr-3");
    }

    @Test
    void restablecerPasswordMapeaCommandResponde204YSinBody() {
        when(servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR)).thenReturn("corr-4");
        ResponseEntity<Void> resultado = controller().restablecerPassword(42L,
                new RestablecerPasswordUsuarioRequest("Abcdefgh1!"), servletRequest);
        verify(restablecerPasswordUsuarioUseCase).ejecutar(42L,
                new RestablecerPasswordUsuarioCommand("Abcdefgh1!"), "corr-4");
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(resultado.getBody()).isNull();
    }

    @Test
    void endpointsExigenPermisoUsuariosAdministrarYDocumentanOperacionCorrecta() throws NoSuchMethodException {
        assertPermisoYOperacion("listar", new Class[]{String.class, String.class, String.class, String.class, Long.class, int.class, int.class},
                "Listar usuarios de administración");
        assertPermisoYOperacion("obtener", new Class[]{Long.class}, "Obtener usuario de administración");
        assertPermisoYOperacion("crear", new Class[]{CrearUsuarioRequest.class, HttpServletRequest.class}, "Crear usuario de administración");
        assertPermisoYOperacion("actualizar", new Class[]{Long.class, ActualizarUsuarioRequest.class, HttpServletRequest.class},
                "Actualizar datos básicos de usuario");
        assertPermisoYOperacion("cambiarEstado", new Class[]{Long.class, CambiarEstadoUsuarioRequest.class, HttpServletRequest.class},
                "Cambiar estado de usuario");
        assertPermisoYOperacion("cambiarPerfil", new Class[]{Long.class, CambiarPerfilUsuarioRequest.class, HttpServletRequest.class},
                "Cambiar perfil de usuario");
        assertPermisoYOperacion("cambiarVigencia", new Class[]{Long.class, CambiarVigenciaUsuarioRequest.class, HttpServletRequest.class},
                "Cambiar vigencia de usuario");
        assertPermisoYOperacion("restablecerPassword", new Class[]{Long.class, RestablecerPasswordUsuarioRequest.class, HttpServletRequest.class},
                "Restablecer contraseña de usuario");
    }

    private void assertPermisoYOperacion(String nombre, Class<?>[] parametros, String resumen) throws NoSuchMethodException {
        Method metodo = UsuarioAdministracionController.class.getDeclaredMethod(nombre, parametros);
        PreAuthorize autorizacion = metodo.getAnnotation(PreAuthorize.class);
        Operation operation = metodo.getAnnotation(Operation.class);

        assertThat(autorizacion).isNotNull();
        assertThat(autorizacion.value()).isEqualTo("hasAuthority('USUARIOS_ADMINISTRAR')");
        assertThat(operation).isNotNull();
        assertThat(operation.summary()).isEqualTo(resumen);
        assertThat(metodo.getAnnotation(ApiResponses.class)).isNotNull();
    }

    private UsuarioAdministracionController controller() {
        return new UsuarioAdministracionController(listarUsuariosUseCase, obtenerUsuarioUseCase,
                crearUsuarioUseCase, actualizarUsuarioUseCase, cambiarEstadoUsuarioUseCase,
                cambiarPerfilUsuarioUseCase, cambiarVigenciaUsuarioUseCase, restablecerPasswordUsuarioUseCase);
    }

    private UsuarioAdministracion usuario() {
        return new UsuarioAdministracion(42L, "op01", "Operador Uno", "op@example.test", "ACTIVO",
                LocalDate.of(2026, 12, 31), 3L, "ADMINISTRADOR");
    }
}
