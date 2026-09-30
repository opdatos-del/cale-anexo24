package com.jovycandy.anexo24.catalogs.auxiliary.application;

import com.jovycandy.anexo24.catalogs.auxiliary.categories.application.query.ListarCategoriasUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.model.Categoria;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.port.CategoriaRepository;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.application.query.ListarTiposMaterialUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.model.TipoMaterial;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.port.TipoMaterialRepository;
import com.jovycandy.anexo24.catalogs.auxiliary.units.application.query.ListarUnidadesUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.model.Unidad;
import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.port.UnidadRepository;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.application.query.ListarAlmacenesUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.model.Almacen;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.port.AlmacenRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de normalización y paginación de los cuatro catálogos auxiliares. */
@ExtendWith(MockitoExtension.class)
class ListarCatalogosAuxiliaresUseCaseTest {
    @Mock UnidadRepository unidadRepository;
    @Mock TipoMaterialRepository tipoMaterialRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock CategoriaRepository categoriaRepository;

    @Test
    void normalizaFiltroDeUnidades() {
        Pagina<Unidad> pagina = paginaVacia(2, 20);
        when(unidadRepository.findPage("kg", 2, 20)).thenReturn(pagina);
        assertThat(new ListarUnidadesUseCase(unidadRepository).ejecutar("  kg  ", 2, 20)).isSameAs(pagina);
        verify(unidadRepository).findPage("kg", 2, 20);
    }

    @Test
    void consultaTiposMaterialConFiltroNulo() {
        Pagina<TipoMaterial> pagina = paginaVacia(1, 20);
        when(tipoMaterialRepository.findPage(null, 1, 20)).thenReturn(pagina);
        assertThat(new ListarTiposMaterialUseCase(tipoMaterialRepository).ejecutar("   ", 1, 20)).isSameAs(pagina);
        verify(tipoMaterialRepository).findPage(null, 1, 20);
    }

    @Test
    void consultaAlmacenesConPaginacionValida() {
        Pagina<Almacen> pagina = paginaVacia(3, 50);
        when(almacenRepository.findPage(null, 3, 50)).thenReturn(pagina);
        assertThat(new ListarAlmacenesUseCase(almacenRepository).ejecutar(null, 3, 50)).isSameAs(pagina);
        verify(almacenRepository).findPage(null, 3, 50);
    }

    @Test
    void consultaCategoriasConFiltroNormalizado() {
        Pagina<Categoria> pagina = paginaVacia(1, 20);
        when(categoriaRepository.findPage("retorno", 1, 20)).thenReturn(pagina);
        assertThat(new ListarCategoriasUseCase(categoriaRepository).ejecutar(" retorno ", 1, 20)).isSameAs(pagina);
        verify(categoriaRepository).findPage("retorno", 1, 20);
    }

    @Test
    void rechazaPaginacionInvalidaEnLaReglaCompartida() {
        assertThatThrownBy(() -> new ListarUnidadesUseCase(unidadRepository).ejecutar(null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> new ListarCategoriasUseCase(categoriaRepository).ejecutar(null, 1, 101))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private <T> Pagina<T> paginaVacia(int pagina, int tamano) {
        return new Pagina<>(List.of(), 0L, pagina, tamano);
    }
}
