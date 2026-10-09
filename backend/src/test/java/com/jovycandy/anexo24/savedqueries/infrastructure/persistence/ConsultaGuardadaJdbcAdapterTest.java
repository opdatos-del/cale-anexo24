package com.jovycandy.anexo24.savedqueries.infrastructure.persistence;

import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
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

@ExtendWith(MockitoExtension.class)
class ConsultaGuardadaJdbcAdapterTest {
    @Mock private JdbcTemplate jdbc;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private ConsultaGuardadaJdbcAdapter adapter;

    @BeforeEach
    void setUp() { adapter = new ConsultaGuardadaJdbcAdapter(jdbc); }

    @Test
    void listaInvocaProcedimientoOwnerScopedConUsuarioYAlcance() throws Exception {
        when(jdbc.call(any(CallableStatementCreator.class), anyList())).thenReturn(Map.of("items", List.of()));
        when(connection.prepareCall("{call app24.APP24_Q_CONSULTAS_GUARDADAS_LISTAR(?, ?)}")).thenReturn(statement);
        assertThat(adapter.listar(17L, ConsultaGuardadaAlcance.ENTRADAS)).isEmpty();
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbc).call(creator.capture(), anyList());
        creator.getValue().createCallableStatement(connection);
        verify(statement).setLong(1, 17L);
        verify(statement).setString(2, "ENTRADAS");
    }

    @Test
    void crearPermiteDescripcionNulaYUsaSoloSpFijo() throws Exception {
        ConsultaGuardada saved = entity();
        when(jdbc.call(any(CallableStatementCreator.class), anyList())).thenReturn(Map.of("items", List.of(saved)));
        when(connection.prepareCall("{call app24.APP24_C_CONSULTA_GUARDADA_CREAR(?,?,?,?,?)}")).thenReturn(statement);
        assertThat(adapter.crear(17L, "Consulta", null, ConsultaGuardadaAlcance.ENTRADAS, "{}"))
                .isEqualTo(saved);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbc).call(creator.capture(), anyList());
        creator.getValue().createCallableStatement(connection);
        verify(statement).setObject(1, 17L);
        verify(statement).setObject(2, "Consulta");
        verify(statement).setNull(3, Types.NVARCHAR);
        verify(statement).setObject(4, "ENTRADAS");
        verify(statement).setObject(5, "{}");
    }

    @Test
    void transformaIdForaneoEnNoEncontrado() {
        SQLException sql = new SQLException("RECURSO_NO_ENCONTRADO", "", 51202);
        when(jdbc.call(any(CallableStatementCreator.class), anyList()))
                .thenThrow(new org.springframework.jdbc.UncategorizedSQLException("call", "call", sql));
        assertThatThrownBy(() -> adapter.actualizar(4L, 17L, "n", null, ConsultaGuardadaAlcance.ENTRADAS, "{}"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private ConsultaGuardada entity() {
        LocalDateTime now = LocalDateTime.parse("2026-10-01T00:00:00");
        return new ConsultaGuardada(9L, "Consulta", null, ConsultaGuardadaAlcance.ENTRADAS, "{}", now, now);
    }
}
