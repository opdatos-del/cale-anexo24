package com.jovycandy.anexo24.catalogs.generaldata.api;

import com.jovycandy.anexo24.catalogs.generaldata.api.dto.DatosGeneralesDto;
import com.jovycandy.anexo24.catalogs.generaldata.application.query.ObtenerDatosGeneralesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta HTTP read-only de los datos generales de la empresa. */
@RestController
@RequestMapping("/api/v1/catalogos/datos-generales")
public class DatosGeneralesController {
    private final ObtenerDatosGeneralesUseCase useCase;

    public DatosGeneralesController(ObtenerDatosGeneralesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<DatosGeneralesDto> obtener() {
        return useCase.ejecutar()
                .map(DatosGeneralesDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
