package com.jovycandy.anexo24.administration.activities.api.controller;

import com.jovycandy.anexo24.administration.activities.api.dto.ActividadAdministracionDto;
import com.jovycandy.anexo24.administration.activities.application.query.ListarActividadesUseCase;
import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
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

/** Pruebas unitarias del controlador de actividades administrativas. */
@ExtendWith(MockitoExtension.class)
class ActividadAdministracionControllerTest {

    @Mock private ListarActividadesUseCase listarActividadesUseCase;

    @Test
    void listadoDelegaYConvierteDominioADto() {
        ActividadAdministracion actividad = new ActividadAdministracion(
                1L, "PERFILES_ADMINISTRAR", "Administrar perfiles", "/perfiles", "ADMINISTRAR");
        when(listarActividadesUseCase.ejecutar()).thenReturn(List.of(actividad));

        ResponseEntity<List<ActividadAdministracionDto>> resultado = controller().listar();

        verify(listarActividadesUseCase).ejecutar();
        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).containsExactly(ActividadAdministracionDto.from(actividad));
    }

    @Test
    void endpointDeclaraPermisoYDocumentacionRequeridos() throws Exception {
        Method metodo = ActividadAdministracionController.class.getDeclaredMethod("listar");

        PreAuthorize autorizacion = metodo.getAnnotation(PreAuthorize.class);
        Operation operation = metodo.getAnnotation(Operation.class);

        assertThat(autorizacion.value()).isEqualTo("hasAuthority('PERFILES_ADMINISTRAR')");
        assertThat(operation.summary()).isEqualTo("Listar actividades de administración");
        assertThat(metodo.getAnnotation(ApiResponses.class)).isNotNull();
    }

    private ActividadAdministracionController controller() {
        return new ActividadAdministracionController(listarActividadesUseCase);
    }
}
