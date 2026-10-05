package com.jovycandy.anexo24.operations.actas.api;

import com.jovycandy.anexo24.operations.actas.api.dto.ConfirmacionCargaActaResponse;
import com.jovycandy.anexo24.operations.actas.application.usecase.ConfirmarCargaActaUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de confirmacion autoritativa de cargas de actas (operacion, no catalogo). */
@RestController
@RequestMapping("/api/v1/operaciones/actas")
public class ActaImportConfirmationController {

    private final ConfirmarCargaActaUseCase useCase;

    public ActaImportConfirmationController(ConfirmarCargaActaUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de actas ejecutando el SP legacy dbo.CARGAACTAS a
     * traves del wrapper dbo.APP24_C_ACTA_CARGA_CONFIRMAR. La operacion puede generar
     * SALIDAS, PSALIDAS y, por barrido global, filas DIRIGIDO.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmacion
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('ACTAS_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaActaResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es valida.");
        return ResponseEntity.ok(ConfirmacionCargaActaResponse.from(useCase.ejecutar(cargaId)));
    }
}
