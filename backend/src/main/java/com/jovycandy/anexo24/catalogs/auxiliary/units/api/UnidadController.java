package com.jovycandy.anexo24.catalogs.auxiliary.units.api;

import com.jovycandy.anexo24.catalogs.auxiliary.units.api.dto.UnidadDto;
import com.jovycandy.anexo24.catalogs.auxiliary.units.application.query.ListarUnidadesUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.units.domain.model.Unidad;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta HTTP del catálogo de unidades. */
@RestController
@RequestMapping("/api/v1/catalogos/unidades")
public class UnidadController {
    private final ListarUnidadesUseCase useCase;

    public UnidadController(ListarUnidadesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<UnidadDto>> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Unidad> resultado = useCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(UnidadDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }
}
