package com.jovycandy.anexo24.administration.activities.application.query;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
import com.jovycandy.anexo24.administration.activities.domain.port.ActividadConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del caso de uso de listado de actividades. */
@ExtendWith(MockitoExtension.class)
class ListarActividadesUseCaseTest {

    @Mock private ActividadConsultaRepository actividadConsultaRepository;

    @Test
    void listaActividadesDesdeElPuertoReadOnly() {
        List<ActividadAdministracion> actividades = List.of(
                new ActividadAdministracion(1L, "PERFILES_ADMINISTRAR", "Administrar perfiles", "/perfiles", "ADMINISTRAR"));
        when(actividadConsultaRepository.findAll()).thenReturn(actividades);

        List<ActividadAdministracion> resultado = new ListarActividadesUseCase(actividadConsultaRepository).ejecutar();

        verify(actividadConsultaRepository).findAll();
        assertThat(resultado).isSameAs(actividades);
    }
}
