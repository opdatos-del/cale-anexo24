package com.jovycandy.anexo24.catalogs.materials.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adapter del procedimiento de Materiales. */
@ExtendWith(MockitoExtension.class)
class MaterialJdbcAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    private MaterialJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MaterialJdbcAdapter(jdbcTemplate);
    }

    @Test
    void invocaSpConFiltroPaginacionYLeeTotal() throws Exception {
        Material material = material();
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(material), "Total", 204L));
        when(connection.prepareCall("{call dbo.APP24_Q_MATERIALES_LISTAR(?, ?, ?, ?)}"))
                .thenReturn(callableStatement);

        Pagina<Material> resultado = adapter.findPage("clave", 2, 20);

        assertThat(resultado.items()).containsExactly(material);
        assertThat(resultado.total()).isEqualTo(204L);
        assertThat(resultado.pagina()).isEqualTo(2);
        assertThat(resultado.tamano()).isEqualTo(20);

        ArgumentCaptor<CallableStatementCreator> creator =
                ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbcTemplate).call(creator.capture(), anyList());
        creator.getValue().createCallableStatement(connection);

        verify(connection).prepareCall("{call dbo.APP24_Q_MATERIALES_LISTAR(?, ?, ?, ?)}");
        verify(callableStatement).setString(1, "clave");
        verify(callableStatement).setInt(2, 2);
        verify(callableStatement).setInt(3, 20);
        verify(callableStatement).registerOutParameter(4, Types.BIGINT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void declaraParametrosYMapeaLasDiezColumnas() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getBigDecimal("materialkey"))
                .thenReturn(new java.math.BigDecimal("42"));
        when(resultSet.getString("clave")).thenReturn("MAT-42");
        when(resultSet.getString("descripcion")).thenReturn("Descripción");
        when(resultSet.getString("fraccion")).thenReturn("1234567890");
        when(resultSet.getString("unidad")).thenReturn("KG");
        when(resultSet.getString("unidadt")).thenReturn("KG");
        when(resultSet.getString("tipomaterial")).thenReturn("NACIONAL");
        when(resultSet.getString("tipo")).thenReturn("MP");
        when(resultSet.getBigDecimal("FactorUM")).thenReturn(new java.math.BigDecimal("1.5"));
        when(resultSet.getBigDecimal("IGIE")).thenReturn(new java.math.BigDecimal("16"));
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    SqlReturnResultSet resultSetParameter = declared.stream()
                            .filter(SqlReturnResultSet.class::isInstance)
                            .map(SqlReturnResultSet.class::cast)
                            .findFirst()
                            .orElseThrow();
                    RowMapper<Material> mapper = (RowMapper<Material>) resultSetParameter.getRowMapper();
                    return Map.of("items", List.of(mapper.mapRow(resultSet, 1)), "Total", 1L);
                });

        Pagina<Material> resultado = adapter.findPage(null, 1, 20);

        assertThat(resultado.items()).singleElement().satisfies(item -> {
            assertThat(item.materialkey()).isEqualByComparingTo("42");
            assertThat(item.clave()).isEqualTo("MAT-42");
            assertThat(item.descripcion()).isEqualTo("Descripción");
            assertThat(item.fraccion()).isEqualTo("1234567890");
            assertThat(item.unidad()).isEqualTo("KG");
            assertThat(item.unidadt()).isEqualTo("KG");
            assertThat(item.tipomaterial()).isEqualTo("NACIONAL");
            assertThat(item.tipo()).isEqualTo("MP");
            assertThat(item.factorUM()).isEqualByComparingTo("1.5");
            assertThat(item.igie()).isEqualByComparingTo("16");
        });

        ArgumentCaptor<List<SqlParameter>> declarations = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).call(any(CallableStatementCreator.class), declarations.capture());
        assertThat(declarations.getValue())
                .extracting(SqlParameter::getName)
                .containsExactly("Filtro", "Pagina", "Tamano", "Total", "items");
        assertThat(declarations.getValue().get(3)).isInstanceOf(SqlOutParameter.class);
    }

    @Test
    void devuelveListaVaciaCuandoElSpNoRetornaFilas() {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(), "Total", 0L));

        Pagina<Material> resultado = adapter.findPage(null, 99, 100);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
        assertThat(resultado.pagina()).isEqualTo(99);
        assertThat(resultado.tamano()).isEqualTo(100);
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

    private Material material() {
        return new Material(
                new java.math.BigDecimal("42"), "MAT-42", "Descripción", "1234567890",
                "KG", "KG", "NACIONAL", "MP", new java.math.BigDecimal("1.5"),
                new java.math.BigDecimal("16"));
    }
}
