package com.jovycandy.anexo24.catalogs.imports.infrastructure.persistence;

import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Verifica el contrato SP-FIRST y la traducción de errores del command de materiales. */
class ConfirmacionCargaMaterialJdbcAdapterTest {

    @Test
    void invocaElCommandVersionadoConSoloCargaId() {
        assertEquals("dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR", ConfirmacionCargaMaterialJdbcAdapter.PROCEDIMIENTO);
    }

    @Test
    void traduceEtapaLegacyOConfirmacionPreviaAConflicto() {
        assertSame(EstadoIncompatibleException.class, traducir(51502).getClass());
        assertSame(EstadoIncompatibleException.class, traducir(51504).getClass());
    }

    @Test
    void traduceCargaInexistenteA404() {
        assertSame(RecursoNoEncontradoException.class, traducir(51503).getClass());
    }

    @Test
    void traduceCargaNoProcesableA422() {
        for (int codigo : new int[]{51505, 51506, 51507}) {
            assertSame(ConfirmacionNoProcesableException.class, traducir(codigo).getClass());
        }
    }

    @Test
    void traduceParametroInvalidoA400() {
        assertSame(SolicitudInvalidaException.class, traducir(51501).getClass());
    }

    @Test
    void propagaErroresNoControlados() {
        DataAccessResourceFailureException original = new DataAccessResourceFailureException("x",
                new SQLException("desconocido", "S1", 50000));
        assertSame(original, ConfirmacionCargaMaterialJdbcAdapter.traducir(original));
    }

    private RuntimeException traducir(int codigoSql) {
        return ConfirmacionCargaMaterialJdbcAdapter.traducir(new DataAccessResourceFailureException("x",
                new SQLException("error controlado", "S1", codigoSql)));
    }
}
