package com.jovycandy.anexo24.catalogs.generaldata.application.query;

import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;
import com.jovycandy.anexo24.catalogs.generaldata.domain.port.DatosGeneralesRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerDatosGeneralesUseCaseTest {
    private final DatosGeneralesRepository repository = mock(DatosGeneralesRepository.class);
    private final ObtenerDatosGeneralesUseCase useCase = new ObtenerDatosGeneralesUseCase(repository);

    @Test
    void devuelveLosDatosGeneralesCuandoExisteLaFilaUnica() {
        DatosGenerales datos = new DatosGenerales("Empresa", "RFC", "IMMEX", "Domicilio");
        when(repository.find()).thenReturn(Optional.of(datos));

        assertThat(useCase.ejecutar()).contains(datos);
    }

    @Test
    void conservaFuenteVaciaSinInventarUnRegistro() {
        when(repository.find()).thenReturn(Optional.empty());

        assertThat(useCase.ejecutar()).isEmpty();
    }
}
