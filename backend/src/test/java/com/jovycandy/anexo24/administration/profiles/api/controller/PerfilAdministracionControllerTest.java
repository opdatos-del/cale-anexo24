package com.jovycandy.anexo24.administration.profiles.api.controller;

import com.jovycandy.anexo24.administration.profiles.api.dto.PerfilAdministracionDto;
import com.jovycandy.anexo24.administration.profiles.application.query.ListarPerfilesUseCase;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del controlador de administración de perfiles. */
@ExtendWith(MockitoExtension.class)
class PerfilAdministracionControllerTest {

    @Mock private ListarPerfilesUseCase listarPerfilesUseCase;

    @Test
    void listadoDelegaParametrosYConvierteDominioADto() {
        PerfilAdministracion perfil = new PerfilAdministracion(7L, "ADMIN", "ACTIVO", 3L);
        Pagina<PerfilAdministracion> pagina = new Pagina<>(List.of(perfil), 1L, 2, 10);
        when(listarPerfilesUseCase.ejecutar("Admin", "ACTIVO", 2, 10)).thenReturn(pagina);

        ResponseEntity<Pagina<PerfilAdministracionDto>> resultado = controller().listar("Admin", "ACTIVO", 2, 10);

        verify(listarPerfilesUseCase).ejecutar("Admin", "ACTIVO", 2, 10);
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().items()).containsExactly(PerfilAdministracionDto.from(perfil));
    }

    @Test
    void endpointDeclaraPermisosYDocumentacionRequeridos() throws Exception {
        Method metodo = PerfilAdministracionController.class.getDeclaredMethod(
                "listar", String.class, String.class, int.class, int.class);

        PreAuthorize autorizacion = metodo.getAnnotation(PreAuthorize.class);
        Operation operation = metodo.getAnnotation(Operation.class);

        assertThat(autorizacion.value())
                .isEqualTo("hasAnyAuthority('USUARIOS_ADMINISTRAR','PERFILES_ADMINISTRAR')");
        assertThat(operation.summary()).isEqualTo("Listar perfiles de administración");
        assertThat(metodo.getAnnotation(ApiResponses.class)).isNotNull();
    }

    private PerfilAdministracionController controller() {
        return new PerfilAdministracionController(listarPerfilesUseCase);
    }
}
