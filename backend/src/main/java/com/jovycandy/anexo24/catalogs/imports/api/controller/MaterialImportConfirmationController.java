package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.ConfirmacionCargaMaterialResponse;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaMaterialUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmación autoritativa de cargas de materiales. */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones/materiales")
public class MaterialImportConfirmationController {

    private final ConfirmarCargaMaterialUseCase useCase;

    public MaterialImportConfirmationController(ConfirmarCargaMaterialUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada mediante el staging legacy existente.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmación
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('MATERIALES_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaMaterialResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es válida.");
        return ResponseEntity.ok(ConfirmacionCargaMaterialResponse.from(useCase.ejecutar(cargaId)));
    }
}
