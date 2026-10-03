package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.ConfirmacionCargaProductoResponse;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaProductoUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmación autoritativa de cargas de productos. */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones/productos")
public class ProductImportConfirmationController {

    private final ConfirmarCargaProductoUseCase useCase;

    public ProductImportConfirmationController(ConfirmarCargaProductoUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de productos. Incorpora únicamente los
     * productos válidos nuevos; los existentes con la misma clave se ignoran
     * silenciosamente porque el SP legacy es INSERT-only.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmación
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('PRODUCTOS_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaProductoResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es válida.");
        return ResponseEntity.ok(ConfirmacionCargaProductoResponse.from(useCase.ejecutar(cargaId)));
    }
}