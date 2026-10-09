package com.jovycandy.anexo24.savedqueries.api;

import com.jovycandy.anexo24.savedqueries.api.dto.ConsultaGuardadaDto;
import com.jovycandy.anexo24.savedqueries.api.dto.GuardarConsultaRequest;
import com.jovycandy.anexo24.savedqueries.application.ConsultaGuardadaService;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** API de metadatos de presets propios; no expone operaciones de ejecución. */
@RestController
@RequestMapping("/api/v1/consultas-guardadas")
@PreAuthorize("isAuthenticated()")
public class ConsultaGuardadaController {
    private final ConsultaGuardadaService service;
    public ConsultaGuardadaController(ConsultaGuardadaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ConsultaGuardadaDto>> listar(@RequestParam(required = false) ConsultaGuardadaAlcance alcance) {
        return ResponseEntity.ok(service.listar(alcance).stream().map(ConsultaGuardadaDto::from).toList());
    }

    @PostMapping
    public ResponseEntity<ConsultaGuardadaDto> crear(@Valid @RequestBody GuardarConsultaRequest request, HttpServletRequest servletRequest) {
        var creada = service.crear(request.nombre(), request.descripcion(), request.alcance(), request.criterios(), correlationId(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/consultas-guardadas/" + creada.id())).body(ConsultaGuardadaDto.from(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultaGuardadaDto> actualizar(@PathVariable Long id, @Valid @RequestBody GuardarConsultaRequest request, HttpServletRequest servletRequest) {
        var actualizada = service.actualizar(id, request.nombre(), request.descripcion(), request.alcance(), request.criterios(), correlationId(servletRequest));
        return ResponseEntity.ok(ConsultaGuardadaDto.from(actualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpServletRequest servletRequest) {
        service.eliminar(id, correlationId(servletRequest));
        return ResponseEntity.noContent().build();
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return value == null ? null : value.toString();
    }
}
