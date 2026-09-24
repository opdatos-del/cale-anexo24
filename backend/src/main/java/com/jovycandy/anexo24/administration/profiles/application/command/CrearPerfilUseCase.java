package com.jovycandy.anexo24.administration.profiles.application.command;

import com.jovycandy.anexo24.administration.profiles.application.command.model.CrearPerfilCommand;
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

/** Crea un perfil activo sin permisos implícitos. */
@Service
public class CrearPerfilUseCase {
    private final PerfilComandoRepository comandoRepository;
    private final PerfilPermisosConsultaRepository consultaRepository;
    private final RegistrarEventoBitacoraService bitacoraService;
    private final AuthenticatedUserContext authenticatedUserContext;

    public CrearPerfilUseCase(PerfilComandoRepository comandoRepository,
                              PerfilPermisosConsultaRepository consultaRepository,
                              RegistrarEventoBitacoraService bitacoraService,
                              AuthenticatedUserContext authenticatedUserContext) {
        this.comandoRepository = comandoRepository;
        this.consultaRepository = consultaRepository;
        this.bitacoraService = bitacoraService;
        this.authenticatedUserContext = authenticatedUserContext;
    }

    @Transactional(transactionManager = "appTransactionManager", isolation = Isolation.SERIALIZABLE)
    public PerfilAdministracion ejecutar(CrearPerfilCommand command, String correlationId) {
        String nombre = normalizarNombre(command == null ? null : command.nombre());
        Long actorId = actorId();
        Long id = comandoRepository.crear(nombre);
        bitacoraService.registrar(new BitacoraEvento(actorId, BitacoraModulo.ADMINISTRACION,
                BitacoraAccion.PERFIL_CREADO, BitacoraResultado.EXITO, "perfilObjetivoId=" + id, correlationId));
        return consultaRepository.findPermissionsByProfileId(id)
                .map(this::perfil)
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    static String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new SolicitudInvalidaException("El nombre es obligatorio.");
        String normalizado = nombre.trim();
        if (normalizado.length() > 80) throw new SolicitudInvalidaException("El nombre supera la longitud máxima.");
        return normalizado;
    }

    private PerfilAdministracion perfil(PerfilPermisosDetalle detalle) {
        return new PerfilAdministracion(detalle.perfilId(), detalle.nombre(), detalle.estado(), detalle.permisos().size());
    }

    private Long actorId() {
        return authenticatedUserContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
    }
}
