package com.jovycandy.anexo24.operations.exits.infrastructure.persistence;

import com.jovycandy.anexo24.operations.exits.domain.model.SalidaLinea;
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

/** Pruebas unitarias del adapter del procedimiento de Salidas. */
@ExtendWith(MockitoExtension.class)
class SalidaStoredProcedureAdapterTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 10, 31);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 18);

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    private SalidaStoredProcedureAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SalidaStoredProcedureAdapter(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void invocaSpMapeaParametrosYLeeTotal() throws Exception {
        SalidaLinea linea = lineaEjemplo();
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(linea), "Total", 3392L));
        when(connection.prepareCall("{call dbo.APP24_Q_SALIDAS_LISTAR(?, ?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(callableStatement);

        Pagina<SalidaLinea> resultado = adapter.findPage(DESDE, HASTA,
                "190-1562-5001241", "F4", "17019999", "300099", 2, 20);

        assertThat(resultado.items()).containsExactly(linea);
        assertThat(resultado.total()).isEqualTo(3392L);
        assertThat(resultado.pagina()).isEqualTo(2);
        assertThat(resultado.tamano()).isEqualTo(20);

        ArgumentCaptor<CallableStatementCreator> creator =
                ArgumentCaptor.forClass(CallableStatementCreator.class);
        ArgumentCaptor<List<SqlParameter>> declarations = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).call(creator.capture(), declarations.capture());

        creator.getValue().createCallableStatement(connection);
        verify(connection).prepareCall("{call dbo.APP24_Q_SALIDAS_LISTAR(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
        verify(callableStatement).setDate(1, java.sql.Date.valueOf(DESDE));
        verify(callableStatement).setDate(2, java.sql.Date.valueOf(HASTA));
        verify(callableStatement).setString(3, "190-1562-5001241");
        verify(callableStatement).setString(4, "F4");
        verify(callableStatement).setString(5, "17019999");
        verify(callableStatement).setString(6, "300099");
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
    void rowMapperRealConvierteLosNueveAliasesYAdmiteFechaNula() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getBigDecimal("SALIDA_ID")).thenReturn(new BigDecimal("3001"));
        when(resultSet.getBigDecimal("PARTIDA_ID")).thenReturn(new BigDecimal("7001"));
        when(resultSet.getString("PEDIMENTO")).thenReturn("190-1562-5001241");
        when(resultSet.getString("CLAVE_PEDIMENTO")).thenReturn("F4");
        when(resultSet.getString("FRACCION")).thenReturn("17019999");
        when(resultSet.getString("UNIDAD_COMERCIAL")).thenReturn("KG");
        when(resultSet.getBigDecimal("CANTIDAD")).thenReturn(new BigDecimal("12.3400"));
        when(resultSet.getString("NUMERO_PARTE")).thenReturn("300099");
        when(resultSet.getTimestamp("FECHA_PAGO")).thenReturn(null);
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    RowMapper<SalidaLinea> mapper = mapperFrom(declared);
                    SalidaLinea mapped = mapper.mapRow(resultSet, 1);
                    return Map.of("items", List.of(mapped), "Total", 1L);
                });

        Pagina<SalidaLinea> resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20);

        assertThat(resultado.items()).singleElement().satisfies(linea -> {
            assertThat(linea.salidaId()).isEqualByComparingTo("3001");
            assertThat(linea.partidaId()).isEqualByComparingTo("7001");
            assertThat(linea.pedimento()).isEqualTo("190-1562-5001241");
            assertThat(linea.clavePedimento()).isEqualTo("F4");
            assertThat(linea.fraccion()).isEqualTo("17019999");
            assertThat(linea.unidadComercial()).isEqualTo("KG");
            assertThat(linea.cantidad()).isEqualByComparingTo("12.3400");
            assertThat(linea.numeroParte()).isEqualTo("300099");
            assertThat(linea.fechaPago()).isNull();
        });
    }

    @Test
    void rowMapperConvierteTimestampAFechaLocal() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getBigDecimal("SALIDA_ID")).thenReturn(new BigDecimal("3001"));
        when(resultSet.getBigDecimal("PARTIDA_ID")).thenReturn(new BigDecimal("7001"));
        when(resultSet.getString("PEDIMENTO")).thenReturn("DOC");
        when(resultSet.getString("CLAVE_PEDIMENTO")).thenReturn("F4");
        when(resultSet.getString("FRACCION")).thenReturn("17019999");
        when(resultSet.getString("UNIDAD_COMERCIAL")).thenReturn("KG");
        when(resultSet.getBigDecimal("CANTIDAD")).thenReturn(BigDecimal.ONE);
        when(resultSet.getString("NUMERO_PARTE")).thenReturn("300099");
        when(resultSet.getTimestamp("FECHA_PAGO"))
                .thenReturn(Timestamp.valueOf("2026-08-18 14:30:00"));
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    SalidaLinea mapped = mapperFrom(declared).mapRow(resultSet, 1);
                    return Map.of("items", List.of(mapped), "Total", 1L);
                });

        SalidaLinea resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20).items().getFirst();

        assertThat(resultado.fechaPago()).isEqualTo(LocalDateTime.of(2026, 8, 18, 14, 30));
    }

    @Test
    void paginaMaximaConservaMetadatos() {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(), "Total", 3392L));

        Pagina<SalidaLinea> resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, Integer.MAX_VALUE, 100);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isEqualTo(3392L);
        assertThat(resultado.pagina()).isEqualTo(Integer.MAX_VALUE);
        assertThat(resultado.tamano()).isEqualTo(100);
    }

    @Test
    void devuelveListaVaciaCuandoElSpNoRetornaFilas() {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(), "Total", 0L));

        Pagina<SalidaLinea> resultado = adapter.findPage(DESDE, HASTA,
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

    private static RowMapper<SalidaLinea> mapperFrom(List<SqlParameter> parameters) {
        SqlReturnResultSet resultSetParameter = parameters.stream()
                .filter(SqlReturnResultSet.class::isInstance)
                .map(SqlReturnResultSet.class::cast)
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        RowMapper<SalidaLinea> mapper =
                (RowMapper<SalidaLinea>) resultSetParameter.getRowMapper();
        return mapper;
    }

    private SalidaLinea lineaEjemplo() {
        return new SalidaLinea(
                new BigDecimal("3001"),
                new BigDecimal("7001"),
                "190-1562-5001241",
                "F4",
                "17019999",
                "KG",
                new BigDecimal("12.3400"),
                "300099",
                LocalDateTime.of(2026, 8, 18, 0, 0));
    }
}
