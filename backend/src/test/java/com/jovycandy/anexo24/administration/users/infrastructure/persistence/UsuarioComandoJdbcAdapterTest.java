package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del adaptador de commands administrativos mediante SP. */
@ExtendWith(MockitoExtension.class)
class UsuarioComandoJdbcAdapterTest {
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private UsuarioComandoJdbcAdapter adapter;

    @BeforeEach
    void setUp() { adapter = new UsuarioComandoJdbcAdapter(jdbcTemplate); }

    @Test
    void crearPasaParametrosYDevuelveIdOutput() throws Exception {
        when(jdbcTemplate.call(any(), anyList())).thenReturn(Map.of("NuevoUsuarioId", 7L));
        assertThat(adapter.crear("op01", "Operador", "op@test", "hash", null, 3L)).isEqualTo(7L);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_C_USUARIO_CREAR(?, ?, ?, ?, ?, ?, ?)}")).thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setString(1, "op01");
        verify(statement).setString(4, "hash");
        verify(statement).setLong(6, 3L);
        verify(statement).registerOutParameter(7, java.sql.Types.BIGINT);
    }

    @Test
    void erroresSql51104SeMapeanADuplicado() {
        SQLException sql = new SQLException("RECURSO_DUPLICADO", "", 51104);
        when(jdbcTemplate.call(any(), anyList()))
                .thenThrow(new org.springframework.jdbc.UncategorizedSQLException("command", "call", sql));
        assertThatThrownBy(() -> adapter.crear("op", "n", "c", "h", null, 1L))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void errorInfraestructuraDesconocidoSePropaga() {
        DataAccessResourceFailureException error = new DataAccessResourceFailureException("BD");
        when(jdbcTemplate.call(any(), anyList())).thenThrow(error);
        assertThatThrownBy(() -> adapter.actualizarDatos(1L, "n", "c")).isSameAs(error);
    }

    @Test
    void commandsUpdatePasanActorYFecha() throws Exception {
        when(jdbcTemplate.call(any(), anyList())).thenReturn(Map.of());
        LocalDate fecha = LocalDate.of(2026, 9, 23);
        adapter.actualizarDatos(42L, "n", "c");
        adapter.actualizarEstado(42L, "INACTIVO", 7L, fecha);
        adapter.actualizarPerfil(42L, 3L, 7L, fecha);
        adapter.actualizarVigencia(42L, null, 7L, fecha);
        verify(jdbcTemplate, org.mockito.Mockito.times(4)).call(any(), anyList());
    }
}
