package com.jovycandy.anexo24.administration.profiles.application.command;

import com.jovycandy.anexo24.administration.profiles.application.command.model.ReemplazarPermisosPerfilCommand;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reemplaza atómicamente todos los permisos de un perfil. */
@Service
public class ReemplazarPermisosPerfilUseCase {
    private final PerfilComandoRepository comandoRepository;
    private final PerfilPermisosConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public ReemplazarPermisosPerfilUseCase(PerfilComandoRepository comandoRepository,
            PerfilPermisosConsultaRepository consultaRepository, RegistrarEventoBitacoraService bitacoraService,
            AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public PerfilPermisosDetalle ejecutar(Long id, ReemplazarPermisosPerfilCommand command, String correlationId) {
        ActualizarNombrePerfilUseCase.validarId(id);
        PerfilPermisosDetalle actual = consultaRepository.findPermissionsByProfileId(id)
                .orElseThrow(RecursoNoEncontradoException::new);
        List<Long> actividadIds = validarActividades(command == null ? null : command.actividadIds());
        Set<Long> permisosActuales = actual.permisos().stream().map(permiso -> permiso.id()).collect(java.util.stream.Collectors.toSet());
        if (permisosActuales.equals(Set.copyOf(actividadIds))) return actual;

        Long actorId = actorId();
        comandoRepository.reemplazarPermisos(id, actividadIds, LocalDate.now());
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.PERFIL_PERMISOS_CAMBIADOS, BitacoraResultado.EXITO,
                "perfilObjetivoId=" + id + ";cantidadPermisos=" + actividadIds.size(), correlationId));
        return consultaRepository.findPermissionsByProfileId(id).orElseThrow(RecursoNoEncontradoException::new);
    }

    private List<Long> validarActividades(List<Long> actividadIds) {
        if (actividadIds == null) throw new SolicitudInvalidaException("actividadIds es obligatorio.");
        if (actividadIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new SolicitudInvalidaException("Cada actividad debe tener un ID positivo.");
        }
        if (new HashSet<>(actividadIds).size() != actividadIds.size()) {
            throw new SolicitudInvalidaException("actividadIds no admite valores repetidos.");
        }
        return List.copyOf(actividadIds);
    }

    private Long actorId() {
        return authenticatedUserContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
    }
}
