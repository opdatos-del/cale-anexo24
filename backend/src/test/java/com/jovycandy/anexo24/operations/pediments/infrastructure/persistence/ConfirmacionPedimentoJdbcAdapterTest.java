package com.jovycandy.anexo24.operations.pediments.infrastructure.persistence;

import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Verifica el contrato SP-FIRST y la traducción de errores controlados del command. */
class ConfirmacionPedimentoJdbcAdapterTest {

    @Test
    void invocaElCommandVersionado() {
        assertEquals("dbo.APP24_C_PEDIMENTO_CONFIRMAR", ConfirmacionPedimentoJdbcAdapter.PROCEDIMIENTO);
    }

    @Test
    void traduceCargaNoEncontradaA404() {
        assertSame(RecursoNoEncontradoException.class, traducir(51403).getClass());
    }

    @Test
    void traduceDuplicadoYBloqueoAConflicto() {
        assertSame(EstadoIncompatibleException.class, traducir(51411).getClass());
        assertSame(EstadoIncompatibleException.class, traducir(51413).getClass());
        assertSame(EstadoIncompatibleException.class, traducir(51402).getClass());
    }

    @Test
    void traduceCargasNoProcesablesA422() {
        for (int codigo : new int[]{51404, 51405, 51406, 51407, 51408, 51410, 51412}) {
            assertSame(ConfirmacionNoProcesableException.class, traducir(codigo).getClass(), "código " + codigo);
        }
    }

    @Test
    void traduceParametroInvalidoA400() {
        assertSame(SolicitudInvalidaException.class, traducir(51401).getClass());
    }

    @Test
    void propagaErroresNoReconocidos() {
        DataAccessResourceFailureException original = new DataAccessResourceFailureException("x",
                new SQLException("desconocido", "S1", 50000));
        assertSame(original, ConfirmacionPedimentoJdbcAdapter.traducir(original));
    }

    private RuntimeException traducir(int codigoSql) {
        return ConfirmacionPedimentoJdbcAdapter.traducir(new DataAccessResourceFailureException("x",
                new SQLException("error controlado", "S1", codigoSql)));
    }
}
