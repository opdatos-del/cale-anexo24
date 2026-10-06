package com.jovycandy.anexo24.operations.constancias.api;

import com.jovycandy.anexo24.operations.constancias.api.dto.ConfirmacionCargaConstanciaResponse;
import com.jovycandy.anexo24.operations.constancias.application.usecase.ConfirmarCargaConstanciaUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmacion autoritativa de cargas de constancias (operacion, no catalogo). */
@RestController
@RequestMapping("/api/v1/operaciones/constancias")
public class ConstanciaImportConfirmationController {

    private final ConfirmarCargaConstanciaUseCase useCase;

    public ConstanciaImportConfirmationController(ConfirmarCargaConstanciaUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de constancias ejecutando el SP legacy dbo.CARGACONSTANCIAS a
     * traves del wrapper dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR. La operacion puede generar
     * productos, salidas, psalidas y dirigido.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmacion
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('CONSTANCIAS_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaConstanciaResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es valida.");
        return ResponseEntity.ok(ConfirmacionCargaConstanciaResponse.from(useCase.ejecutar(cargaId)));
    }
}
