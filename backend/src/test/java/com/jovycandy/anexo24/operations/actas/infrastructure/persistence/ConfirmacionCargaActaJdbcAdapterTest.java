package com.jovycandy.anexo24.operations.actas.infrastructure.persistence;

import com.jovycandy.anexo24.operations.actas.domain.model.ConfirmacionCargaActa;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifica el contrato SP-FIRST y la traduccion de errores del command de actas. */
class ConfirmacionCargaActaJdbcAdapterTest {

    @Test
    void invocaElCommandVersionadoConSoloCargaId() {
        assertEquals("dbo.APP24_C_ACTA_CARGA_CONFIRMAR", ConfirmacionCargaActaJdbcAdapter.PROCEDIMIENTO);
    }

    @Test
    void configuraTimeoutControladoParaEtapaLegacyBloqueada() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        CallableStatement statement = mock(CallableStatement.class);
        ConfirmacionCargaActaJdbcAdapter adapter = new ConfirmacionCargaActaJdbcAdapter(jdbcTemplate);
        ConfirmacionCargaActa confirmacion = new ConfirmacionCargaActa(7L, "CONFIRMADA", 1, 1, 0, null, "CONFIRMED");
        when(jdbcTemplate.call(any(), anyList())).thenReturn(Map.of("confirmacion", List.of(confirmacion)));

        adapter.confirmar(7L);

        ArgumentCaptor<CallableStatementCreator> captor = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbcTemplate).call(captor.capture(), anyList());
        when(connection.prepareCall("{call dbo.APP24_C_ACTA_CARGA_CONFIRMAR(?)}")).thenReturn(statement);
        captor.getValue().createCallableStatement(connection);
        verify(statement).setQueryTimeout(30);
        verify(statement).setLong(1, 7L);
    }

    @Test
    void traduceEtapaLegacyOConfirmacionPreviaAConflicto() {
        assertSame(EstadoIncompatibleException.class, traducir(51602).getClass());
        assertSame(EstadoIncompatibleException.class, traducir(51604).getClass());
    }

    @Test
    void traduceTimeoutDeBloqueoAConflicto() {
        assertSame(EstadoIncompatibleException.class,
                ConfirmacionCargaActaJdbcAdapter.traducir(new QueryTimeoutException("bloqueo temporal")).getClass());
        assertSame(EstadoIncompatibleException.class,
                ConfirmacionCargaActaJdbcAdapter.traducir(new DataAccessResourceFailureException("bloqueo temporal",
                        new SQLTimeoutException("bloqueo temporal", "HYT00", 0))).getClass());
    }

    @Test
    void traduceCargaInexistenteA404() {
        assertSame(RecursoNoEncontradoException.class, traducir(51603).getClass());
    }

    @Test
    void traduceCargaNoProcesableA422() {
        for (int codigo : new int[]{51605, 51606, 51607}) {
            assertSame(ConfirmacionNoProcesableException.class, traducir(codigo).getClass());
        }
    }

    @Test
    void traduceParametroInvalidoA400() {
        assertSame(SolicitudInvalidaException.class, traducir(51601).getClass());
    }

    @Test
    void propagaErroresNoControlados() {
        DataAccessResourceFailureException original = new DataAccessResourceFailureException("x",
                new SQLException("desconocido", "S1", 50000));
        assertSame(original, ConfirmacionCargaActaJdbcAdapter.traducir(original));
    }

    private RuntimeException traducir(int codigoSql) {
        return ConfirmacionCargaActaJdbcAdapter.traducir(new DataAccessResourceFailureException("x",
                new SQLException("error controlado", "S1", codigoSql)));
    }
}
