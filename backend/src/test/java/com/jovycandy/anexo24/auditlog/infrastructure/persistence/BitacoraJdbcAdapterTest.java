package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del command append-only de Bitácora. */
@ExtendWith(MockitoExtension.class)
class BitacoraJdbcAdapterTest {
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private BitacoraJdbcAdapter adapter;

    @BeforeEach
    void setUp() { adapter = new BitacoraJdbcAdapter(jdbcTemplate); }

    @Test
    void registraConUsuarioNuloYLeeEventoId() throws Exception {
        when(jdbcTemplate.call(any(), anyList())).thenReturn(Map.of("EventoId", 9L));
        adapter.registrar(new BitacoraEvento(null, BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK, BitacoraResultado.EXITO, "detalle", "req"));
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_C_BITACORA_REGISTRAR(?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setNull(1, java.sql.Types.BIGINT);
        verify(statement).setString(2, "SEGURIDAD");
        verify(statement).setString(5, "req");
        verify(statement).registerOutParameter(7, java.sql.Types.BIGINT);
    }

    @Test
    void eventoIdNuloOCeroEsInconsistente() {
        when(jdbcTemplate.call(any(), anyList())).thenReturn(Map.of("EventoId", 0L));
        assertThatThrownBy(() -> adapter.registrar(evento())).isInstanceOf(IllegalStateException.class);
    }

    private BitacoraEvento evento() {
        return new BitacoraEvento(42L, BitacoraModulo.OPERACIONES,
                BitacoraAccion.LOGIN_FALLIDO, BitacoraResultado.FALLO, null, null);
    }
}
