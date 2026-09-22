package com.jovycandy.anexo24.auditlog.domain.port;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.time.Instant;

/** Puerto de consulta read-only de eventos persistidos de Bitácora. */
public interface BitacoraConsultaRepository {

    /**
     * Obtiene una página de eventos con rango inclusivo y filtros exactos opcionales.
     *
     * @param desde         instante UTC inicial inclusivo
     * @param hasta         instante UTC final inclusivo
     * @param usuarioId     actor persistido opcional
     * @param modulo        módulo opcional
     * @param resultado     resultado opcional
     * @param correlationId correlación exacta opcional
     * @param pagina        número de página base 1
     * @param tamano        tamaño de página
     * @return página de registros persistidos
     */
    Pagina<BitacoraRegistro> findPage(
            Instant desde,
            Instant hasta,
            Long usuarioId,
            BitacoraModulo modulo,
            BitacoraResultado resultado,
            String correlationId,
            int pagina,
            int tamano);
}
