package com.jovycandy.anexo24.operations.pediments.application.usecase;

import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.operations.pediments.domain.model.ConfirmacionPedimento;
import com.jovycandy.anexo24.operations.pediments.domain.port.ConfirmacionPedimentoRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/**
 * Confirma una carga de pedimentos delegando toda la operación autoritativa al
 * command SQL (transacción, locks, idempotencia y auditoría de éxito).
 *
 * <p>El auditoría de fallo se registra fuera de la transacción revertida, con
 * infraestructura existente y sin datos sensibles.</p>
 */
@Service
public class ConfirmarCargaPedimentoUseCase {

    private final ConfirmacionPedimentoRepository repository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final RegistrarEventoBitacoraService bitacoraService;

    public ConfirmarCargaPedimentoUseCase(ConfirmacionPedimentoRepository repository,
                                          AuthenticatedUserContext authenticatedUserContext,
                                          RegistrarEventoBitacoraService bitacoraService) {
        this.repository = repository;
        this.authenticatedUserContext = authenticatedUserContext;
        this.bitacoraService = bitacoraService;
    }

    /**
     * Ejecuta la confirmación de la carga indicada.
     *
     * @param cargaId       identificador de la carga
     * @param correlationId correlación de la solicitud
     * @return resultado de la confirmación
     */
    public ConfirmacionPedimento ejecutar(long cargaId, String correlationId) {
        Long usuarioId = authenticatedUserContext.currentUser()
                .map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
        try {
            return repository.confirmar(cargaId, usuarioId, correlationId);
        } catch (RuntimeException fallo) {
            registrarFallo(cargaId, usuarioId, correlationId, fallo);
            throw fallo;
        }
    }

    private void registrarFallo(long cargaId, Long usuarioId, String correlationId, RuntimeException fallo) {
        // La bitácora de fallo no debe enmascarar el error original ni la transacción revertida.
        try {
            bitacoraService.registrar(new BitacoraEvento(usuarioId, BitacoraModulo.OPERACIONES,
                    BitacoraAccion.PEDIMENTO_CONFIRMACION_FALLIDA, BitacoraResultado.FALLO,
                    "cargaId=" + cargaId + ";categoria=" + categoria(fallo), correlationId));
        } catch (RuntimeException ignored) {
            // El fallo del registro no altera el resultado funcional.
        }
    }

    private String categoria(RuntimeException fallo) {
        if (fallo instanceof RecursoNoEncontradoException) return "CARGA_NO_ENCONTRADA";
        if (fallo instanceof EstadoIncompatibleException) return "CONFLICTO_OPERATIVO";
        if (fallo instanceof ConfirmacionNoProcesableException) return "NO_PROCESABLE";
        if (fallo instanceof SolicitudInvalidaException) return "PARAMETRO_INVALIDO";
        return "ERROR_TECNICO";
    }
}
