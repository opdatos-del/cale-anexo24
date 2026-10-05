package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.ConfirmacionCargaSubmaquilaResponse;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaSubmaquilaUseCase;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de confirmación autoritativa de cargas de constancias de transferencia.
 *
 * <p>La confirmación crea las salidas y partidas en {@code CALE_IMMEX} mediante el SP
 * legacy {@code dbo.CARGA_SUBMAQUILA}. A diferencia de otros contratos, este SP es
 * estrictamente INSERT-only y no deduplica: reintentar una carga ya confirmada crearía
 * salidas duplicadas, por lo que el estado {@code CONFIRMADA} es terminal y el wrapper
 * rechaza cualquier reintento.</p>
 */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones/submaquilas")
public class SubmaquilaImportConfirmationController {

    private final ConfirmarCargaSubmaquilaUseCase useCase;

    public SubmaquilaImportConfirmationController(ConfirmarCargaSubmaquilaUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Confirma una carga previsualizada de constancias de transferencia. Cada renglón del
     * archivo se materializa como una partida y cada grupo folio/fecha/submaquilador como
     * una salida en {@code dbo.SALIDAS} / {@code dbo.PSALIDAS}.
     *
     * @param cargaId identificador de la carga
     * @return resumen de la confirmación
     */
    @PostMapping("/{cargaId}/confirmacion")
    @PreAuthorize("hasAuthority('SUBMAQUILA_CONFIRMAR')")
    public ResponseEntity<ConfirmacionCargaSubmaquilaResponse> confirmar(@PathVariable long cargaId) {
        if (cargaId < 1) throw new SolicitudInvalidaException("La carga indicada no es válida.");
        return ResponseEntity.ok(ConfirmacionCargaSubmaquilaResponse.from(useCase.ejecutar(cargaId)));
    }
}
