package com.jovycandy.anexo24.operations.entries.infrastructure.persistence;

import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adapter del procedimiento de Entradas. */
@ExtendWith(MockitoExtension.class)
class EntradaStoredProcedureAdapterTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 9, 23);
    private static final LocalDate HASTA = LocalDate.of(2026, 5, 28);

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    private EntradaStoredProcedureAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EntradaStoredProcedureAdapter(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void invocaSpMapeaParametrosYLeeTotal() throws Exception {
        EntradaLinea linea = lineaEjemplo();
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(linea), "Total", 2L));
        when(connection.prepareCall("{call dbo.APP24_Q_ENTRADAS_LISTAR(?, ?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(callableStatement);

        Pagina<EntradaLinea> resultado = adapter.findPage(DESDE, HASTA,
                "5003971", "A1", "17019999", "500017", 2, 20);

        assertThat(resultado.items()).containsExactly(linea);
        assertThat(resultado.total()).isEqualTo(2L);
        assertThat(resultado.pagina()).isEqualTo(2);
        assertThat(resultado.tamano()).isEqualTo(20);

        ArgumentCaptor<CallableStatementCreator> creator =
                ArgumentCaptor.forClass(CallableStatementCreator.class);
        ArgumentCaptor<List<SqlParameter>> declarations = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).call(creator.capture(), declarations.capture());

        creator.getValue().createCallableStatement(connection);
        verify(connection).prepareCall("{call dbo.APP24_Q_ENTRADAS_LISTAR(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
        verify(callableStatement).setDate(1, java.sql.Date.valueOf(DESDE));
        verify(callableStatement).setDate(2, java.sql.Date.valueOf(HASTA));
        verify(callableStatement).setString(3, "5003971");
        verify(callableStatement).setString(4, "A1");
        verify(callableStatement).setString(5, "17019999");
        verify(callableStatement).setString(6, "500017");
        verify(callableStatement).setInt(7, 2);
        verify(callableStatement).setInt(8, 20);
        verify(callableStatement).registerOutParameter(9, Types.BIGINT);

        assertThat(declarations.getValue())
                .extracting(SqlParameter::getName)
                .containsExactly("Desde", "Hasta", "Pedimento", "ClavePedimento",
                        "Fraccion", "NumeroParte", "Pagina", "Tamano", "Total", "items");
        assertThat(declarations.getValue().get(8)).isInstanceOf(SqlOutParameter.class);
        assertThat(declarations.getValue().subList(0, 9))
                .extracting(SqlParameter::getSqlType)
                .containsExactly(Types.DATE, Types.DATE, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
                        Types.BIGINT);
    }

    @Test
    void rowMapperRealConvierteLosDiezAliasesYAdmiteFechasNulas() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getBigDecimal("IMPORTACION_ID")).thenReturn(new BigDecimal("1001"));
        when(resultSet.getBigDecimal("PARTIDA_ID")).thenReturn(new BigDecimal("2001"));
        when(resultSet.getString("PEDIMENTO")).thenReturn("5003971");
        when(resultSet.getString("CLAVE_PEDIMENTO")).thenReturn("A1");
        when(resultSet.getTimestamp("FECHA_ENTRADA")).thenReturn(null);
        when(resultSet.getString("FRACCION")).thenReturn("17019999");
        when(resultSet.getString("UNIDAD_COMERCIAL")).thenReturn("KG");
        when(resultSet.getBigDecimal("CANTIDAD_COMERCIAL"))
                .thenReturn(new BigDecimal("6000000.0000"));
        when(resultSet.getString("NUMERO_PARTE")).thenReturn("500017");
        when(resultSet.getTimestamp("FECHA_PAGO"))
                .thenReturn(Timestamp.valueOf("2025-09-23 00:00:00"));
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    RowMapper<EntradaLinea> mapper = mapperFrom(declared);
                    EntradaLinea mapped = mapper.mapRow(resultSet, 1);
                    return Map.of("items", List.of(mapped), "Total", 1L);
                });

        Pagina<EntradaLinea> resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20);

        assertThat(resultado.items()).singleElement().satisfies(linea -> {
            assertThat(linea.importacionId()).isEqualByComparingTo("1001");
            assertThat(linea.partidaId()).isEqualByComparingTo("2001");
            assertThat(linea.pedimento()).isEqualTo("5003971");
            assertThat(linea.clavePedimento()).isEqualTo("A1");
            assertThat(linea.fechaEntrada()).isNull();
            assertThat(linea.fraccion()).isEqualTo("17019999");
            assertThat(linea.unidadComercial()).isEqualTo("KG");
            assertThat(linea.cantidadComercial()).isEqualByComparingTo("6000000.0000");
            assertThat(linea.numeroParte()).isEqualTo("500017");
            assertThat(linea.fechaPago())
                    .isEqualTo(LocalDateTime.of(2025, 9, 23, 0, 0));
        });
    }

    @Test
    void devuelveListaVaciaCuandoElSpNoRetornaFilas() {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(), "Total", 0L));

        Pagina<EntradaLinea> resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
    }

    @Test
    void propagaErrorDeAccesoADatos() {
        DataAccessResourceFailureException error =
                new DataAccessResourceFailureException("BD no disponible");
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenThrow(error);

        assertThatThrownBy(() -> adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20))
                .isSameAs(error);
    }

    private static RowMapper<EntradaLinea> mapperFrom(List<SqlParameter> parameters) {
        SqlReturnResultSet resultSetParameter = parameters.stream()
                .filter(SqlReturnResultSet.class::isInstance)
                .map(SqlReturnResultSet.class::cast)
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        RowMapper<EntradaLinea> mapper =
                (RowMapper<EntradaLinea>) resultSetParameter.getRowMapper();
        return mapper;
    }

    private EntradaLinea lineaEjemplo() {
        return new EntradaLinea(
                new BigDecimal("1001"),
                new BigDecimal("2001"),
                "5003971",
                "A1",
                null,
                "17019999",
                "KG",
                new BigDecimal("6000000.0000"),
                "500017",
                LocalDateTime.of(2025, 9, 23, 0, 0));
    }
}
