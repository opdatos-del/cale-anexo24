package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.ConfirmacionCargaProveedorResponse;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaProveedorUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmación autoritativa de cargas de proveedores. */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones/proveedores")
public class ProviderImportConfirmationController {

    private final ConfirmarCargaProveedorUseCase useCase;

    public ProviderImportConfirmationController(ConfirmarCargaProveedorUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de proveedores. Incorpora únicamente los
     * proveedores válidos nuevos; los existentes con la misma clave se ignoran
     * silenciosamente porque el SP legacy es INSERT-only.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmación
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('PROVEEDORES_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaProveedorResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es válida.");
        return ResponseEntity.ok(ConfirmacionCargaProveedorResponse.from(useCase.ejecutar(cargaId)));
    }
}