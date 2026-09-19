package com.jovycandy.anexo24.catalogs.structures.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adapter del procedimiento de estructuras. */
@ExtendWith(MockitoExtension.class)
class EstructuraStoredProcedureAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    private EstructuraStoredProcedureAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EstructuraStoredProcedureAdapter(jdbcTemplate);
    }

    @Test
    void invocaSpMapeaResultSetYLeeTotal() throws Exception {
        EstructuraDetalle detalle = detalleEjemplo();
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(detalle), "Total", 1L));
        when(connection.prepareCall("{call dbo.APP24_Q_ESTRUCTURAS_LISTAR(?, ?, ?, ?, ?)}"))
                .thenReturn(callableStatement);

        Pagina<EstructuraDetalle> resultado = adapter.findPage("200060", "MAT-1", 2, 20);

        assertThat(resultado.items()).containsExactly(detalle);
        assertThat(resultado.total()).isEqualTo(1L);
        assertThat(resultado.pagina()).isEqualTo(2);
        assertThat(resultado.tamano()).isEqualTo(20);

        ArgumentCaptor<CallableStatementCreator> creator =
                ArgumentCaptor.forClass(CallableStatementCreator.class);
        ArgumentCaptor<List<SqlParameter>> declarations = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).call(creator.capture(), declarations.capture());

        creator.getValue().createCallableStatement(connection);
        verify(connection).prepareCall("{call dbo.APP24_Q_ESTRUCTURAS_LISTAR(?, ?, ?, ?, ?)}");
        verify(callableStatement).setString(1, "200060");
        verify(callableStatement).setString(2, "MAT-1");
        verify(callableStatement).setInt(3, 2);
        verify(callableStatement).setInt(4, 20);
        verify(callableStatement).registerOutParameter(5, Types.BIGINT);

        assertThat(declarations.getValue())
                .extracting(SqlParameter::getName)
                .containsExactly("Producto", "Material", "Pagina", "Tamano", "Total", "items");
        assertThat(declarations.getValue().get(4)).isInstanceOf(SqlOutParameter.class);
    }

    @Test
    void rowMapperRealConvierteTodasLasColumnasYAdmiteFechaFinNula() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getLong("ESTRUCTURAKEY")).thenReturn(12L);
        when(resultSet.getBigDecimal("PRODUCTOKEY")).thenReturn(new BigDecimal("204"));
        when(resultSet.getString("CVE_PRODUCTO")).thenReturn("200060");
        when(resultSet.getString("PRODUCTO_DESCRIPCION")).thenReturn("LIMONAZO");
        when(resultSet.getString("PRODUCTO_UNIDAD")).thenReturn("CAJA");
        when(resultSet.getTimestamp("FECHA_INICIO"))
                .thenReturn(Timestamp.valueOf("2026-01-01 10:15:00"));
        when(resultSet.getTimestamp("FECHA_FIN")).thenReturn(null);
        when(resultSet.getBigDecimal("PRODMATKEY")).thenReturn(new BigDecimal("34"));
        when(resultSet.getString("CVE_MATERIAL")).thenReturn("MAT-1");
        when(resultSet.getString("MATERIAL_DESCRIPCION")).thenReturn("Azúcar");
        when(resultSet.getString("MATERIAL_UNIDAD")).thenReturn("KG");
        when(resultSet.getString("MATERIAL_FRACCION")).thenReturn("17019999");
        when(resultSet.getBigDecimal("CANT_UTILIZADA")).thenReturn(new BigDecimal("2.5000000"));
        when(resultSet.getBigDecimal("CANT_MERMADA")).thenReturn(new BigDecimal("0.1000000"));
        when(resultSet.getBigDecimal("CANT_DESPERDICIADA"))
                .thenReturn(new BigDecimal("0.0500000"));
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    SqlReturnResultSet resultSetParameter = declared.stream()
                            .filter(SqlReturnResultSet.class::isInstance)
                            .map(SqlReturnResultSet.class::cast)
                            .findFirst()
                            .orElseThrow();
                    @SuppressWarnings("unchecked")
                    RowMapper<EstructuraDetalle> mapper =
                            (RowMapper<EstructuraDetalle>) resultSetParameter.getRowMapper();
                    EstructuraDetalle mapped = mapper.mapRow(resultSet, 1);
                    return Map.of("items", List.of(mapped), "Total", 1L);
                });

        Pagina<EstructuraDetalle> resultado = adapter.findPage(null, null, 1, 20);

        assertThat(resultado.items()).singleElement().satisfies(detalle -> {
            assertThat(detalle.estructuraId()).isEqualTo(12L);
            assertThat(detalle.productoId()).isEqualByComparingTo("204");
            assertThat(detalle.productoClave()).isEqualTo("200060");
            assertThat(detalle.productoDescripcion()).isEqualTo("LIMONAZO");
            assertThat(detalle.productoUnidad()).isEqualTo("CAJA");
            assertThat(detalle.fechaInicio()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 15));
            assertThat(detalle.fechaFin()).isNull();
            assertThat(detalle.productoMaterialId()).isEqualByComparingTo("34");
            assertThat(detalle.materialClave()).isEqualTo("MAT-1");
            assertThat(detalle.materialDescripcion()).isEqualTo("Azúcar");
            assertThat(detalle.materialUnidad()).isEqualTo("KG");
            assertThat(detalle.materialFraccion()).isEqualTo("17019999");
            assertThat(detalle.cantidadIncorporada()).isEqualByComparingTo("2.5000000");
            assertThat(detalle.cantidadMermada()).isEqualByComparingTo("0.1000000");
            assertThat(detalle.cantidadDesperdiciada()).isEqualByComparingTo("0.0500000");
        });
    }

    @Test
    void devuelveListaVaciaCuandoElSpNoRetornaFilas() {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(), "Total", 0L));

        Pagina<EstructuraDetalle> resultado = adapter.findPage(null, null, 1, 20);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
    }

    @Test
    void propagaErrorDeAccesoADatos() {
        DataAccessResourceFailureException error =
                new DataAccessResourceFailureException("BD no disponible");
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenThrow(error);

        assertThatThrownBy(() -> adapter.findPage(null, null, 1, 20))
                .isSameAs(error);
    }

    private EstructuraDetalle detalleEjemplo() {
        return new EstructuraDetalle(
                12L,
                new BigDecimal("204"),
                "200060",
                "LIMONAZO",
                "CAJA",
                LocalDateTime.of(2026, 1, 1, 10, 15),
                null,
                new BigDecimal("34"),
                "MAT-1",
                "Azúcar",
                "KG",
                "17019999",
                new BigDecimal("2.5000000"),
                new BigDecimal("0.1000000"),
                new BigDecimal("0.0500000"));
    }
}
