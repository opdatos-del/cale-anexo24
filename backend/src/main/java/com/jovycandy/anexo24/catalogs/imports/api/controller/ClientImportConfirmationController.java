package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.ConfirmacionCargaClienteResponse;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaClienteUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmación autoritativa de cargas de clientes. */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones/clientes")
public class ClientImportConfirmationController {

    private final ConfirmarCargaClienteUseCase useCase;

    public ClientImportConfirmationController(ConfirmarCargaClienteUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de clientes. Incorpora únicamente los
     * clientes válidos nuevos; los existentes con la misma clave se ignoran
     * silenciosamente porque el SP legacy es INSERT-only.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmación
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('CLIENTES_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaClienteResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es válida.");
        return ResponseEntity.ok(ConfirmacionCargaClienteResponse.from(useCase.ejecutar(cargaId)));
    }
}