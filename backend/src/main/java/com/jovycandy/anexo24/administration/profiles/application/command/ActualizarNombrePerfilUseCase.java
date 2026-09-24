package com.jovycandy.anexo24.administration.profiles.application.command;

import com.jovycandy.anexo24.administration.profiles.application.command.model.ActualizarNombrePerfilCommand;
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

/** Actualiza exclusivamente el nombre de un perfil. */
@Service
public class ActualizarNombrePerfilUseCase {
    private final PerfilComandoRepository comandoRepository;
    private final PerfilPermisosConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public ActualizarNombrePerfilUseCase(PerfilComandoRepository comandoRepository,
            PerfilPermisosConsultaRepository consultaRepository, RegistrarEventoBitacoraService bitacoraService,
            AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository; this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService; this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public PerfilAdministracion ejecutar(Long id, ActualizarNombrePerfilCommand command, String correlationId) {
        validarId(id);
        PerfilPermisosDetalle actual = detalle(id);
        String nombre = CrearPerfilUseCase.normalizarNombre(command == null ? null : command.nombre());
        if (actual.nombre().equals(nombre)) return perfil(actual);
        Long actorId = actorId();
        comandoRepository.actualizarNombre(id, nombre);
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.PERFIL_ACTUALIZADO, BitacoraResultado.EXITO,
                "perfilObjetivoId=" + id + ";campo=nombre", correlationId));
        return perfil(detalle(id));
    }

    static void validarId(Long id) { if (id == null || id <= 0) throw new SolicitudInvalidaException("El parámetro id debe ser positivo."); }
    private PerfilPermisosDetalle detalle(Long id) { return consultaRepository.findPermissionsByProfileId(id).orElseThrow(RecursoNoEncontradoException::new); }
    private PerfilAdministracion perfil(PerfilPermisosDetalle detalle) { return new PerfilAdministracion(detalle.perfilId(), detalle.nombre(), detalle.estado(), detalle.permisos().size()); }
    private Long actorId() { return authenticatedUserContext.currentUser().map(principal -> principal.userId()).orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible")); }
}
