package com.jovycandy.anexo24.administration.profiles.application.command;

import com.jovycandy.anexo24.administration.profiles.application.command.model.CambiarEstadoPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilComandoRepository;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilPermisosConsultaRepository;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

/** Cambia el estado de un perfil con guardrail de administrador efectivo. */
@Service
public class CambiarEstadoPerfilUseCase {
    private final PerfilComandoRepository comandoRepository;
    private final PerfilPermisosConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public CambiarEstadoPerfilUseCase(PerfilComandoRepository comandoRepository,
            PerfilPermisosConsultaRepository consultaRepository, RegistrarEventoBitacoraService bitacoraService,
            AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository; this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService; this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public PerfilAdministracion ejecutar(Long id, CambiarEstadoPerfilCommand command, String correlationId) {
        ActualizarNombrePerfilUseCase.validarId(id);
        PerfilPermisosDetalle actual = consultaRepository.findPermissionsByProfileId(id).orElseThrow(RecursoNoEncontradoException::new);
        String nuevoEstado = normalizarEstado(command == null ? null : command.estado());
        if (actual.estado().equals(nuevoEstado)) return perfil(actual);
        Long actorId = actorId();
        comandoRepository.cambiarEstado(id, nuevoEstado, LocalDate.now());
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.PERFIL_ESTADO_CAMBIADO, BitacoraResultado.EXITO,
                "perfilObjetivoId=" + id + ";estadoAnterior=" + actual.estado() + ";estadoNuevo=" + nuevoEstado,
                correlationId));
        return perfil(consultaRepository.findPermissionsByProfileId(id).orElseThrow(RecursoNoEncontradoException::new));
    }

    static String normalizarEstado(String estado) {
        if (estado == null) throw new SolicitudInvalidaException("El estado es obligatorio.");
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVO".equals(normalizado) && !"INACTIVO".equals(normalizado)) throw new SolicitudInvalidaException("El estado es inválido.");
        return normalizado;
    }
    private PerfilAdministracion perfil(PerfilPermisosDetalle detalle) { return new PerfilAdministracion(detalle.perfilId(), detalle.nombre(), detalle.estado(), detalle.permisos().size()); }
    private Long actorId() { return authenticatedUserContext.currentUser().map(principal -> principal.userId()).orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible")); }
}
