package com.jovycandy.anexo24.operations.usedmaterials.infrastructure.persistence;

import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adapter del procedimiento de materiales utilizados. */
@ExtendWith(MockitoExtension.class)
class MaterialUtilizadoStoredProcedureAdapterTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 10, 31);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 18);

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    private MaterialUtilizadoStoredProcedureAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MaterialUtilizadoStoredProcedureAdapter(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void invocaSpConParametrosOrdenadosYTotalOut() throws Exception {
        MaterialUtilizado fila = filaEjemplo();
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenReturn(Map.of("items", List.of(fila), "Total", 3866L));
        when(connection.prepareCall("{call dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR(?, ?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(callableStatement);

        Pagina<MaterialUtilizado> resultado = adapter.findPage(DESDE, HASTA,
                "500017", "300861", "190-1562-5001284", "F4", 2, 20);

        assertThat(resultado.items()).containsExactly(fila);
        assertThat(resultado.total()).isEqualTo(3866L);

        ArgumentCaptor<CallableStatementCreator> creator =
                ArgumentCaptor.forClass(CallableStatementCreator.class);
        ArgumentCaptor<List<SqlParameter>> declarations = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).call(creator.capture(), declarations.capture());

        creator.getValue().createCallableStatement(connection);
        verify(callableStatement).setDate(1, java.sql.Date.valueOf(DESDE));
        verify(callableStatement).setDate(2, java.sql.Date.valueOf(HASTA));
        verify(callableStatement).setString(3, "500017");
        verify(callableStatement).setString(4, "300861");
        verify(callableStatement).setString(5, "190-1562-5001284");
        verify(callableStatement).setString(6, "F4");
        verify(callableStatement).setInt(7, 2);
        verify(callableStatement).setInt(8, 20);
        verify(callableStatement).registerOutParameter(9, Types.BIGINT);

        assertThat(declarations.getValue()).extracting(SqlParameter::getName)
                .containsExactly("Desde", "Hasta", "Material", "Producto", "PedimentoSalida",
                        "ClavePedimentoSalida", "Pagina", "Tamano", "Total", "items");
        assertThat(declarations.getValue().get(8)).isInstanceOf(SqlOutParameter.class);
        assertThat(declarations.getValue().subList(0, 9)).extracting(SqlParameter::getSqlType)
                .containsExactly(Types.DATE, Types.DATE, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BIGINT);
    }

    @Test
    void rowMapperMapeaLosDiecisieteAliasesSinDoubleYAdmiteNulos() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getLong("DESCARGA_ID")).thenReturn(390L);
        when(resultSet.getBigDecimal("ENTRADA_ID")).thenReturn(new BigDecimal("1001"));
        when(resultSet.getBigDecimal("PARTIDA_ENTRADA_ID")).thenReturn(new BigDecimal("2001"));
        when(resultSet.getBigDecimal("SALIDA_ID")).thenReturn(new BigDecimal("3024"));
        when(resultSet.getBigDecimal("PARTIDA_SALIDA_ID")).thenReturn(new BigDecimal("4124"));
        when(resultSet.getString("PEDIMENTO_ENTRADA")).thenReturn("190-1562-5003971");
        when(resultSet.getString("PEDIMENTO_SALIDA")).thenReturn("190-1562-5001284");
        when(resultSet.getString("MATERIAL_CODE")).thenReturn("500017");
        when(resultSet.getString("MATERIAL_DESCRIPTION")).thenReturn("AZUCAR ESTANDAR");
        when(resultSet.getString("PRODUCT_CODE")).thenReturn("300861");
        when(resultSet.getString("PRODUCT_DESCRIPTION")).thenReturn("CHERRY SLICES");
        when(resultSet.getBigDecimal("CANTIDAD_INCORPORADA")).thenReturn(new BigDecimal("10.5000"));
        when(resultSet.getBigDecimal("CANTIDAD_MERMA")).thenReturn(new BigDecimal("0.5000"));
        when(resultSet.getBigDecimal("CANTIDAD_DESPERDICIO")).thenReturn(null);
        when(resultSet.getBigDecimal("CANTIDAD_TOTAL_DESCARGADA")).thenReturn(new BigDecimal("11.0000"));
        when(resultSet.getString("UNIDAD")).thenReturn("KG");
        when(resultSet.getTimestamp("FECHA")).thenReturn(null);
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    MaterialUtilizado mapped = mapperFrom(declared).mapRow(resultSet, 1);
                    return Map.of("items", List.of(mapped), "Total", 1L);
                });

        Pagina<MaterialUtilizado> resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20);

        assertThat(resultado.items()).singleElement().satisfies(fila -> {
            assertThat(fila.descargaId()).isEqualTo(390L);
            assertThat(fila.entradaId()).isEqualByComparingTo("1001");
            assertThat(fila.partidaEntradaId()).isEqualByComparingTo("2001");
            assertThat(fila.salidaId()).isEqualByComparingTo("3024");
            assertThat(fila.partidaSalidaId()).isEqualByComparingTo("4124");
            assertThat(fila.cantidadIncorporada()).isEqualByComparingTo("10.5000");
            assertThat(fila.cantidadMerma()).isEqualByComparingTo("0.5000");
            assertThat(fila.cantidadDesperdicio()).isNull();
            assertThat(fila.cantidadTotalDescargada()).isEqualByComparingTo("11.0000");
            assertThat(fila.fecha()).isNull();
        });
    }

    @Test
    void convierteFechaYDevuelveItemsVacios() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getLong("DESCARGA_ID")).thenReturn(1L);
        when(resultSet.getTimestamp("FECHA")).thenReturn(Timestamp.valueOf("2025-12-01 00:00:00"));
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<SqlParameter> declared = invocation.getArgument(1, List.class);
                    MaterialUtilizado mapped = mapperFrom(declared).mapRow(resultSet, 1);
                    return Map.of("items", List.of(mapped), "Total", 1L);
                });

        Pagina<MaterialUtilizado> resultado = adapter.findPage(DESDE, HASTA,
                null, null, null, null, 1, 20);

        assertThat(resultado.items()).singleElement()
                .extracting(MaterialUtilizado::fecha)
                .isEqualTo(LocalDateTime.of(2025, 12, 1, 0, 0));
    }

    private static RowMapper<MaterialUtilizado> mapperFrom(List<SqlParameter> parameters) {
        SqlReturnResultSet resultSetParameter = parameters.stream()
                .filter(SqlReturnResultSet.class::isInstance)
                .map(SqlReturnResultSet.class::cast)
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        RowMapper<MaterialUtilizado> mapper =
                (RowMapper<MaterialUtilizado>) resultSetParameter.getRowMapper();
        return mapper;
    }

    private MaterialUtilizado filaEjemplo() {
        return new MaterialUtilizado(390L, new BigDecimal("1001"), new BigDecimal("2001"),
                new BigDecimal("3024"), new BigDecimal("4124"), "190-1562-5003971",
                "190-1562-5001284", "500017", "AZUCAR ESTANDAR", "300861",
                "CHERRY SLICES", new BigDecimal("10.5000"), new BigDecimal("0.5000"),
                null, new BigDecimal("11.0000"), "KG", LocalDateTime.of(2025, 12, 1, 0, 0));
    }
}
