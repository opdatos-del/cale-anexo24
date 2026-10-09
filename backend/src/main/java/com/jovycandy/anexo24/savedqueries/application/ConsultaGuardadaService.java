package com.jovycandy.anexo24.savedqueries.application;

import tools.jackson.databind.JsonNode;
import com.jovycandy.anexo24.auditlog.application.RegistrarEventoBitacoraService;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.savedqueries.domain.port.ConsultaGuardadaRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserContext;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso owner-scoped para presets estructurados sin capacidad de ejecución. */
@Service
public class ConsultaGuardadaService {
    private final ConsultaGuardadaRepository repository;
    private final ConsultaGuardadaCriteriosValidator criteriosValidator;
    private final AuthenticatedUserContext userContext;
    private final RegistrarEventoBitacoraService bitacora;

    public ConsultaGuardadaService(ConsultaGuardadaRepository repository, ConsultaGuardadaCriteriosValidator criteriosValidator,
                                   AuthenticatedUserContext userContext, RegistrarEventoBitacoraService bitacora) {
        this.repository = repository;
        this.criteriosValidator = criteriosValidator;
        this.userContext = userContext;
        this.bitacora = bitacora;
    }

    @Transactional(transactionManager = "appTransactionManager", readOnly = true)
    public List<ConsultaGuardada> listar(ConsultaGuardadaAlcance alcance) {
        return repository.listar(actorId(), alcance);
    }

    @Transactional(transactionManager = "appTransactionManager")
    public ConsultaGuardada crear(String nombre, String descripcion, ConsultaGuardadaAlcance alcance, JsonNode criterios, String correlationId) {
        String criteriosJson = criteriosValidator.validarYSerializar(alcance, criterios);
        ConsultaGuardada creada = repository.crear(actorId(), nombreValido(nombre), descripcionValida(descripcion), alcance, criteriosJson);
        auditar(creada, BitacoraAccion.CONSULTA_GUARDADA_CREAR, correlationId);
        return creada;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public ConsultaGuardada actualizar(Long id, String nombre, String descripcion, ConsultaGuardadaAlcance alcance, JsonNode criterios, String correlationId) {
        if (id == null || id <= 0) throw new SolicitudInvalidaException("El identificador es inválido.");
        String criteriosJson = criteriosValidator.validarYSerializar(alcance, criterios);
        ConsultaGuardada actualizada = repository.actualizar(id, actorId(), nombreValido(nombre), descripcionValida(descripcion), alcance, criteriosJson);
        auditar(actualizada, BitacoraAccion.CONSULTA_GUARDADA_ACTUALIZAR, correlationId);
        return actualizada;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public void eliminar(Long id, String correlationId) {
        if (id == null || id <= 0) throw new SolicitudInvalidaException("El identificador es inválido.");
        Long actor = actorId();
        repository.eliminar(id, actor);
        bitacora.registrar(new BitacoraEvento(actor, BitacoraModulo.SISTEMA, BitacoraAccion.CONSULTA_GUARDADA_ELIMINAR,
                BitacoraResultado.EXITO, "consultaGuardadaId=" + id, correlationId));
    }

    private void auditar(ConsultaGuardada consulta, BitacoraAccion accion, String correlationId) {
        bitacora.registrar(new BitacoraEvento(actorId(), BitacoraModulo.SISTEMA, accion, BitacoraResultado.EXITO,
                "consultaGuardadaId=" + consulta.id() + ";alcance=" + consulta.alcance(), correlationId));
    }

    private Long actorId() {
        return userContext.currentUser().map(principal -> principal.userId())
                .orElseThrow(() -> new IllegalStateException("Actor autenticado no disponible"));
    }

    private String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 80) throw new SolicitudInvalidaException("El nombre es inválido.");
        return nombre.trim();
    }

    private String descripcionValida(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) return null;
        if (descripcion.trim().length() > 250) throw new SolicitudInvalidaException("La descripción es inválida.");
        return descripcion.trim();
    }
}
