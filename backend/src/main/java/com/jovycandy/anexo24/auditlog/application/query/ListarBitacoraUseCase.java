package com.jovycandy.anexo24.auditlog.application.query;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Pattern;

/** Caso de uso de consulta paginada read-only de eventos de Bitácora. */
@Service
public class ListarBitacoraUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,40}");

    private final BitacoraConsultaRepository bitacoraConsultaRepository;

    /**
     * Construye el caso de uso de lectura de Bitácora.
     *
     * @param bitacoraConsultaRepository puerto read-only de Bitácora
     */
    public ListarBitacoraUseCase(BitacoraConsultaRepository bitacoraConsultaRepository) {
        this.bitacoraConsultaRepository = bitacoraConsultaRepository;
    }

    /**
     * Lista eventos con rango UTC inclusivo, filtros exactos y paginación estable.
     *
     * @param desde         instante inicial inclusivo
     * @param hasta         instante final inclusivo
     * @param usuarioId     filtro opcional de actor
     * @param modulo        filtro opcional de módulo
     * @param resultado     filtro opcional de resultado
     * @param correlationId filtro opcional de correlación exacta
     * @param pagina        número de página base 1
     * @param tamano        tamaño de página
     * @return página de registros de Bitácora
     */
    public Pagina<BitacoraRegistro> ejecutar(
            Instant desde,
            Instant hasta,
            Long usuarioId,
            BitacoraModulo modulo,
            BitacoraResultado resultado,
            String correlationId,
            int pagina,
            int tamano) {
        validarRango(desde, hasta);
        validarUsuarioId(usuarioId);
        validarPaginacion(pagina, tamano);
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);

        return bitacoraConsultaRepository.findPage(
                desde,
                hasta,
                usuarioId,
                modulo,
                resultado,
                correlationIdNormalizado,
                pagina,
                tamano);
    }

    private void validarRango(Instant desde, Instant hasta) {
        if (desde == null || hasta == null) {
            throw new SolicitudInvalidaException("Los parámetros desde y hasta son obligatorios.");
        }
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("El parámetro desde no puede ser posterior a hasta.");
        }
    }

    private void validarUsuarioId(Long usuarioId) {
        if (usuarioId != null && usuarioId <= 0) {
            throw new SolicitudInvalidaException("El parámetro usuarioId debe ser positivo.");
        }
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }

    private String normalizarCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        String normalizado = correlationId.trim();
        if (!CORRELATION_ID_PATTERN.matcher(normalizado).matches()) {
            throw new SolicitudInvalidaException("El parámetro correlationId es inválido.");
        }
        return normalizado;
    }
}
