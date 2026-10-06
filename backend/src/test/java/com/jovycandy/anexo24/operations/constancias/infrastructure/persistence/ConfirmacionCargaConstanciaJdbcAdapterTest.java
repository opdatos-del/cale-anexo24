package com.jovycandy.anexo24.operations.constancias.infrastructure.persistence;

import com.jovycandy.anexo24.operations.constancias.domain.model.ConfirmacionCargaConstancia;
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

/** Verifica el contrato SP-FIRST y la traduccion de errores del command de constancias. */
class ConfirmacionCargaConstanciaJdbcAdapterTest {

    @Test
    void invocaElCommandVersionadoConSoloCargaId() {
        assertEquals("dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR", ConfirmacionCargaConstanciaJdbcAdapter.PROCEDIMIENTO);
    }

    @Test
    void configuraTimeoutControladoParaEtapaLegacyBloqueada() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        CallableStatement statement = mock(CallableStatement.class);
        ConfirmacionCargaConstanciaJdbcAdapter adapter = new ConfirmacionCargaConstanciaJdbcAdapter(jdbcTemplate);
        ConfirmacionCargaConstancia confirmacion = new ConfirmacionCargaConstancia(7L, "CONFIRMADA", 1, 1, 0, null, "CONFIRMED");
        when(jdbcTemplate.call(any(), anyList())).thenReturn(Map.of("confirmacion", List.of(confirmacion)));

        adapter.confirmar(7L);

        ArgumentCaptor<CallableStatementCreator> captor = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbcTemplate).call(captor.capture(), anyList());
        when(connection.prepareCall("{call dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR(?)}")).thenReturn(statement);
        captor.getValue().createCallableStatement(connection);
        verify(statement).setQueryTimeout(30);
        verify(statement).setLong(1, 7L);
    }

    @Test
    void traduceEtapaLegacyOConfirmacionPreviaAConflicto() {
        assertSame(EstadoIncompatibleException.class, traducir(51702).getClass());
        assertSame(EstadoIncompatibleException.class, traducir(51704).getClass());
        assertSame(EstadoIncompatibleException.class, traducir(51708).getClass());
    }

    @Test
    void traduceTimeoutDeBloqueoAConflicto() {
        assertSame(EstadoIncompatibleException.class,
                ConfirmacionCargaConstanciaJdbcAdapter.traducir(new QueryTimeoutException("bloqueo temporal")).getClass());
        assertSame(EstadoIncompatibleException.class,
                ConfirmacionCargaConstanciaJdbcAdapter.traducir(new DataAccessResourceFailureException("bloqueo temporal",
                        new SQLTimeoutException("bloqueo temporal", "HYT00", 0))).getClass());
    }

    @Test
    void traduceCargaInexistenteA404() {
        assertSame(RecursoNoEncontradoException.class, traducir(51703).getClass());
    }

    @Test
    void traduceCargaNoProcesableA422() {
        for (int codigo : new int[]{51705, 51706, 51707}) {
            assertSame(ConfirmacionNoProcesableException.class, traducir(codigo).getClass());
        }
    }

    @Test
    void traduceParametroInvalidoA400() {
        assertSame(SolicitudInvalidaException.class, traducir(51701).getClass());
    }

    @Test
    void propagaErroresNoControlados() {
        DataAccessResourceFailureException original = new DataAccessResourceFailureException("x",
                new SQLException("desconocido", "S1", 50000));
        assertSame(original, ConfirmacionCargaConstanciaJdbcAdapter.traducir(original));
    }

    private RuntimeException traducir(int codigoSql) {
        return ConfirmacionCargaConstanciaJdbcAdapter.traducir(new DataAccessResourceFailureException("x",
                new SQLException("error controlado", "S1", codigoSql)));
    }
}
