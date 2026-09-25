package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC read-only de Bitácora mediante SP. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BitacoraConsultaJdbcAdapterTest {

    private static final Instant DESDE = Instant.parse("2026-09-22T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-09-22T23:59:59Z");

    @Mock private JdbcTemplate appJdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    @Mock private ResultSet resultSet;
    private BitacoraConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BitacoraConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void consultaPasaTodosLosParametrosLeeTotalYConservaUTC() throws Exception {
        BitacoraRegistro registro = new BitacoraRegistro(7L,
                Instant.parse("2026-09-22T13:30:00Z"), 42L, "operador",
                BitacoraModulo.SEGURIDAD, BitacoraAccion.LOGIN_OK, null,
                BitacoraResultado.EXITO, "req-123");
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(
                Map.of("items", List.of(registro), "Total", 3L));

        Pagina<BitacoraRegistro> resultado = adapter.findPage(
                DESDE, HASTA, 42L, BitacoraModulo.SEGURIDAD,
                BitacoraResultado.EXITO, "req-123", 2, 20);

        assertThat(resultado.total()).isEqualTo(3L);
        assertThat(resultado.items()).containsExactly(registro);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_Q_BITACORA_LISTAR(?, ?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setObject(1, java.time.LocalDateTime.of(2026, 9, 22, 0, 0));
        verify(statement).setObject(2, java.time.LocalDateTime.of(2026, 9, 22, 23, 59, 59));
        verify(statement).setLong(3, 42L);
        verify(statement).setString(4, "SEGURIDAD");
        verify(statement).setString(5, "EXITO");
        verify(statement).setString(6, "req-123");
        verify(statement).setInt(7, 2);
        verify(statement).setInt(8, 20);
        verify(statement).registerOutParameter(9, java.sql.Types.BIGINT);
    }

    @Test
    void rowMapperConvierteAccionesDeFacturacionPersistidas() throws Exception {
        when(resultSet.getObject("fecha", java.time.LocalDateTime.class))
                .thenReturn(java.time.LocalDateTime.of(2026, 9, 22, 13, 30));
        when(resultSet.getLong("id")).thenReturn(8L);
        when(resultSet.getObject("usuario_id", Long.class)).thenReturn(42L);
        when(resultSet.getString("usuario")).thenReturn("operador");
        when(resultSet.getString("modulo")).thenReturn("FACTURACION");
        when(resultSet.getString("resultado")).thenReturn("EXITO", "FALLO");
        when(resultSet.getString("correlacion_id")).thenReturn("req-456");

        when(resultSet.getString("accion")).thenReturn("CARGA_VALIDADA", "CARGA_CON_ERRORES");
        BitacoraRegistro validada = BitacoraConsultaJdbcAdapter.MAPPER.mapRow(resultSet, 0);
        assertThat(validada.modulo()).isEqualTo(BitacoraModulo.FACTURACION);
        assertThat(validada.accion()).isEqualTo(BitacoraAccion.CARGA_VALIDADA);
        assertThat(validada.resultado()).isEqualTo(BitacoraResultado.EXITO);

        BitacoraRegistro conErrores = BitacoraConsultaJdbcAdapter.MAPPER.mapRow(resultSet, 1);
        assertThat(conErrores.modulo()).isEqualTo(BitacoraModulo.FACTURACION);
        assertThat(conErrores.accion()).isEqualTo(BitacoraAccion.CARGA_CON_ERRORES);
        assertThat(conErrores.resultado()).isEqualTo(BitacoraResultado.FALLO);
    }

    @Test
    void enumIncluyeAccionesEmitidasPorFacturacion() {
        assertThatCode(() -> BitacoraAccion.valueOf("CARGA_VALIDADA")).doesNotThrowAnyException();
        assertThatCode(() -> BitacoraAccion.valueOf("CARGA_CON_ERRORES")).doesNotThrowAnyException();
    }

    @Test
    void consultaSinResultadosDevuelvePaginaVacia() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(), "Total", 0L));
        assertThat(adapter.findPage(DESDE, HASTA, null, null, null, null, 1, 20).items()).isEmpty();
    }
}
