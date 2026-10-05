package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.ConfirmacionCargaAgenteResponse;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaAgenteUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmación autoritativa de cargas de agentes aduanales. */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones/agentes")
public class AgentImportConfirmationController {

    private final ConfirmarCargaAgenteUseCase useCase;

    public AgentImportConfirmationController(ConfirmarCargaAgenteUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de agentes aduanales. Incorpora únicamente
     * los agentes válidos nuevos; los agentes existentes con la misma clave se
     * mantienen sin cambios porque el SP legacy {@code dbo.CARGAAgentes} es INSERT-only
     * y los omite silenciosamente.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmación
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('AGENTES_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaAgenteResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es válida.");
        return ResponseEntity.ok(ConfirmacionCargaAgenteResponse.from(useCase.ejecutar(cargaId)));
    }
}