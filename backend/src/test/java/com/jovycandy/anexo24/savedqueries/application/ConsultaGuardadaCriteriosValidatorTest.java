package com.jovycandy.anexo24.savedqueries.application;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultaGuardadaCriteriosValidatorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ConsultaGuardadaCriteriosValidator validator = new ConsultaGuardadaCriteriosValidator();

    @Test
    void aceptaCriteriosEstructuradosDeEntradas() {
        ObjectNode criteria = mapper.createObjectNode().put("from", "2026-01-01").put("to", "2026-01-31")
                .put("customsDocument", "123").put("customsCode", "A1").put("tariffFraction", "1234.56").put("partNumber", "P-1");
        assertDoesNotThrow(() -> validator.validarYSerializar(ConsultaGuardadaAlcance.ENTRADAS, criteria));
    }

    @Test
    void rechazaLlavesDesconocidasYSintaxisQueParezcaConsulta() {
        ObjectNode criteria = mapper.createObjectNode().put("from", "2026-01-01").put("to", "2026-01-31")
                .put("where", "SELECT * FROM SALIDAS");
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.ENTRADAS, criteria));
    }

    @Test
    void rechazaTipoIncorrectoYFechaInvalida() {
        ObjectNode wrongType = mapper.createObjectNode();
        wrongType.putObject("customsDocument");
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.ENTRADAS, wrongType));
        ObjectNode invalidDate = mapper.createObjectNode().put("from", "2026-99-99");
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.ENTRADAS, invalidDate));
    }

    @Test
    void rechazaRangoIncompletoEnScopesOperativos() {
        ObjectNode criteria = mapper.createObjectNode().put("from", "2026-01-01");
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.ENTRADAS, criteria));
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.MATERIALES_UTILIZADOS, mapper.createObjectNode()));
    }

    @Test
    void requiereTipoConfirmadoParaReporte() {
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.REPORTES, mapper.createObjectNode()));
    }

    @Test
    void rechazaIdentificadorDeUsuarioNoEnteroEnFiltroDeReporte() {
        ObjectNode criteria = mapper.createObjectNode().put("type", "entradas").put("userId", 1.5);
        assertThrows(SolicitudInvalidaException.class, () -> validator.validarYSerializar(ConsultaGuardadaAlcance.REPORTES, criteria));
    }

    @Test
    void aceptaActivoFijoSinPeriodo() {
        ObjectNode criteria = mapper.createObjectNode().putNull("from").putNull("to").put("serialNumber", "SERIE-1");
        assertDoesNotThrow(() -> validator.validarYSerializar(ConsultaGuardadaAlcance.ACTIVOS_FIJOS, criteria));
    }
}
