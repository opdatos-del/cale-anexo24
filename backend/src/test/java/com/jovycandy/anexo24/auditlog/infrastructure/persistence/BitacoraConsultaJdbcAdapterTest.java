package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC read-only de Bitácora. */
@ExtendWith(MockitoExtension.class)
class BitacoraConsultaJdbcAdapterTest {

    private static final Instant DESDE = Instant.parse("2026-09-22T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-09-22T23:59:59Z");

    @Mock
    private JdbcTemplate appJdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private BitacoraConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BitacoraConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void consultaReadOnlyConFiltrosParametrizadosOrdenYUTC() throws Exception {
        prepararConsulta(3L, true);

        Pagina<BitacoraRegistro> resultado = adapter.findPage(
                DESDE,
                HASTA,
                42L,
                BitacoraModulo.SEGURIDAD,
                BitacoraResultado.EXITO,
                "req-123",
                2,
                20);

        ArgumentCaptor<String> sqlTotal = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametrosTotal = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(
                sqlTotal.capture(), eq(Long.class), parametrosTotal.capture());
        assertThat(sqlTotal.getValue()).isEqualTo(
                "SELECT COUNT(*) FROM app24.BitacoraEvento b"
                        + " WHERE b.fecha >= ? AND b.fecha <= ?"
                        + " AND b.usuario_id = ?"
                        + " AND b.modulo = ?"
                        + " AND b.resultado = ?"
                        + " AND b.correlacion_id = ?");
        assertThat(parametrosTotal.getValue()).containsExactly(
                LocalDateTime.of(2026, 9, 22, 0, 0),
                LocalDateTime.of(2026, 9, 22, 23, 59, 59),
                42L,
                "SEGURIDAD",
                "EXITO",
                "req-123");

        ArgumentCaptor<String> sqlConsulta = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametrosConsulta = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).query(
                sqlConsulta.capture(), org.mockito.ArgumentMatchers.<RowMapper<BitacoraRegistro>>any(),
                parametrosConsulta.capture());
        assertThat(sqlConsulta.getValue()).contains("FROM app24.BitacoraEvento b");
        assertThat(sqlConsulta.getValue()).contains("LEFT JOIN app24.UsuarioApp u ON u.id = b.usuario_id");
        assertThat(sqlConsulta.getValue()).contains("ORDER BY b.fecha DESC, b.id DESC");
        assertThat(sqlConsulta.getValue()).contains("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        assertThat(parametrosConsulta.getValue()).containsExactly(
                LocalDateTime.of(2026, 9, 22, 0, 0),
                LocalDateTime.of(2026, 9, 22, 23, 59, 59),
                42L,
                "SEGURIDAD",
                "EXITO",
                "req-123",
                20L,
                20);
        assertThat(resultado.total()).isEqualTo(3L);
        assertThat(resultado.items()).singleElement().satisfies(registro -> {
            assertThat(registro.id()).isEqualTo(7L);
            assertThat(registro.fecha()).isEqualTo(Instant.parse("2026-09-22T13:30:00Z"));
            assertThat(registro.usuarioId()).isEqualTo(42L);
            assertThat(registro.usuario()).isEqualTo("operador");
            assertThat(registro.modulo()).isEqualTo(BitacoraModulo.SEGURIDAD);
            assertThat(registro.accion().name()).isEqualTo("LOGIN_OK");
            assertThat(registro.resultado()).isEqualTo(BitacoraResultado.EXITO);
        });
    }

    @Test
    void aceptaPaginaVaciaYCalculaOffsetComoLong() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(appJdbcTemplate.<BitacoraRegistro>query(
                anyString(), org.mockito.ArgumentMatchers.<RowMapper<BitacoraRegistro>>any(),
                any(Object[].class))).thenReturn(List.of());

        Pagina<BitacoraRegistro> resultado = adapter.findPage(
                DESDE, HASTA, null, null, null, null, Integer.MAX_VALUE, 100);

        ArgumentCaptor<Object[]> parametrosConsulta = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).query(
                anyString(), org.mockito.ArgumentMatchers.<RowMapper<BitacoraRegistro>>any(),
                parametrosConsulta.capture());
        assertThat(parametrosConsulta.getValue()).containsExactly(
                LocalDateTime.of(2026, 9, 22, 0, 0),
                LocalDateTime.of(2026, 9, 22, 23, 59, 59),
                214748364600L,
                100);
        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
    }

    @Test
    void mapeaRegistroConUsuarioIdNulo() throws Exception {
        lenient().when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        lenient().when(resultSet.getLong("id")).thenReturn(8L);
        lenient().when(resultSet.getObject("fecha", LocalDateTime.class))
                .thenReturn(LocalDateTime.of(2026, 9, 22, 14, 0));
        lenient().when(resultSet.getObject("usuario_id", Long.class)).thenReturn(null);
        lenient().when(resultSet.getString("modulo")).thenReturn("SISTEMA");
        lenient().when(resultSet.getString("accion")).thenReturn("LOGIN_FALLIDO");
        lenient().when(resultSet.getString("resultado")).thenReturn("FALLO");
        lenient().when(appJdbcTemplate.<BitacoraRegistro>query(
                anyString(), org.mockito.ArgumentMatchers.<RowMapper<BitacoraRegistro>>any(),
                any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<BitacoraRegistro> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        Pagina<BitacoraRegistro> resultado = adapter.findPage(
                DESDE, HASTA, null, null, null, null, 1, 20);

        assertThat(resultado.items()).singleElement().satisfies(registro -> {
            assertThat(registro.usuarioId()).isNull();
            assertThat(registro.usuario()).isNull();
            assertThat(registro.modulo()).isEqualTo(BitacoraModulo.SISTEMA);
            assertThat(registro.resultado()).isEqualTo(BitacoraResultado.FALLO);
        });
    }

    private void prepararConsulta(long total, boolean mapearFila) throws Exception {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(total);
        if (mapearFila) {
            when(resultSet.getLong("id")).thenReturn(7L);
            when(resultSet.getObject("fecha", LocalDateTime.class))
                    .thenReturn(LocalDateTime.of(2026, 9, 22, 13, 30));
            when(resultSet.getObject("usuario_id", Long.class)).thenReturn(42L);
            when(resultSet.getString("usuario")).thenReturn("operador");
            when(resultSet.getString("modulo")).thenReturn("SEGURIDAD");
            when(resultSet.getString("accion")).thenReturn("LOGIN_OK");
            when(resultSet.getString("detalle")).thenReturn(null);
            when(resultSet.getString("resultado")).thenReturn("EXITO");
            when(resultSet.getString("correlacion_id")).thenReturn("req-123");
            when(appJdbcTemplate.<BitacoraRegistro>query(
                    anyString(), org.mockito.ArgumentMatchers.<RowMapper<BitacoraRegistro>>any(),
                    any(Object[].class))).thenAnswer(invocation -> {
                RowMapper<BitacoraRegistro> mapper = invocation.getArgument(1);
                return List.of(mapper.mapRow(resultSet, 0));
            });
        }
    }
}
