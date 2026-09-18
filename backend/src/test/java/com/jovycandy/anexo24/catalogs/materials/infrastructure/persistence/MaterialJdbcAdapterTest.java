package com.jovycandy.anexo24.catalogs.materials.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC de Materiales. */
@ExtendWith(MockitoExtension.class)
class MaterialJdbcAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MaterialJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MaterialJdbcAdapter(jdbcTemplate);
    }

    @Test
    void consultaConFiltroYPaginacion() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(3L);
        when(jdbcTemplate.<Material>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Material>>any(), any(Object[].class)))
                .thenReturn(List.of());

        Pagina<Material> resultado = adapter.findPage("tornillo", 2, 20);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isEqualTo(3L);
        assertThat(resultado.pagina()).isEqualTo(2);
        assertThat(resultado.tamano()).isEqualTo(20);

        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Material>>any(), parametros.capture());
        assertThat(parametros.getValue()).containsExactly("%tornillo%", "%tornillo%",
                "%tornillo%", 20, 20);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<Material>>any(), any(Object[].class));
        assertThat(sql.getValue()).contains("WHERE clave LIKE ? OR descripcion LIKE ? OR fraccion LIKE ?");
        assertThat(sql.getValue()).contains("ORDER BY clave, materialkey OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
    }

    @Test
    void consultaSinFiltroYConPaginaVacia() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(jdbcTemplate.<Material>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Material>>any(), any(Object[].class)))
                .thenReturn(List.of());

        Pagina<Material> resultado = adapter.findPage(null, 4, 10);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Material>>any(), parametros.capture());
        assertThat(parametros.getValue()).containsExactly(30, 10);
    }

    @Test
    void propagaErrorDeAccesoADatos() {
        DataAccessResourceFailureException error =
                new DataAccessResourceFailureException("BD no disponible");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenThrow(error);

        assertThatThrownBy(() -> adapter.findPage(null, 1, 20))
                .isSameAs(error);
    }
}
