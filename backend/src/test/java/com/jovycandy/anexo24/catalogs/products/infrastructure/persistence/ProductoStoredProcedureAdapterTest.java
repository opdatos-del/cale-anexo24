package com.jovycandy.anexo24.catalogs.products.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adapter del procedimiento de Productos. */
@ExtendWith(MockitoExtension.class)
class ProductoStoredProcedureAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    private ProductoStoredProcedureAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductoStoredProcedureAdapter(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void invocaSpMapeaResultSetYLeeTotal() throws Exception {
        Producto producto = new Producto(
                new java.math.BigDecimal("1"), "200060", "LIMONAZO", "17049099", "CAJA", "CAJA");
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(producto), "Total", 204L));
        when(connection.prepareCall("{call dbo.APP24_Q_PRODUCTOS_LISTAR(?, ?, ?, ?)}"))
                .thenReturn(callableStatement);

        Pagina<Producto> resultado = adapter.findPage("  dulce  ", 2, 20);

        assertThat(resultado.items()).containsExactly(producto);
        assertThat(resultado.total()).isEqualTo(204L);
        assertThat(resultado.pagina()).isEqualTo(2);
        assertThat(resultado.tamano()).isEqualTo(20);

        ArgumentCaptor<CallableStatementCreator> creator =
                ArgumentCaptor.forClass(CallableStatementCreator.class);
        ArgumentCaptor<List<SqlParameter>> declarations = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).call(creator.capture(), declarations.capture());

        creator.getValue().createCallableStatement(connection);
        verify(connection).prepareCall("{call dbo.APP24_Q_PRODUCTOS_LISTAR(?, ?, ?, ?)}");
        verify(callableStatement).setString(1, "  dulce  ");
        verify(callableStatement).setInt(2, 2);
        verify(callableStatement).setInt(3, 20);
        verify(callableStatement).registerOutParameter(4, Types.BIGINT);

        assertThat(declarations.getValue())
                .extracting(SqlParameter::getName)
                .containsExactly("Filtro", "Pagina", "Tamano", "Total", "items");
        assertThat(declarations.getValue().get(3)).isInstanceOf(SqlOutParameter.class);
    }

    @Test
    void devuelveListaVaciaCuandoElSpNoRetornaFilas() {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(), "Total", 0L));

        Pagina<Producto> resultado = adapter.findPage(null, 1, 20);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
    }

    @Test
    void propagaErrorDeAccesoADatos() {
        DataAccessResourceFailureException error =
                new DataAccessResourceFailureException("BD no disponible");
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenThrow(error);

        assertThatThrownBy(() -> adapter.findPage(null, 1, 20))
                .isSameAs(error);
    }
}
