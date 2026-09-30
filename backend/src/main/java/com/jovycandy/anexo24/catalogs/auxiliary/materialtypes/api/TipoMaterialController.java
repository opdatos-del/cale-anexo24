package com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.api;

import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.api.dto.TipoMaterialDto;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.application.query.ListarTiposMaterialUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.domain.model.TipoMaterial;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta HTTP del catálogo de tipos de material. */
@RestController
@RequestMapping("/api/v1/catalogos/tipos-material")
public class TipoMaterialController {
    private final ListarTiposMaterialUseCase useCase;

    public TipoMaterialController(ListarTiposMaterialUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<TipoMaterialDto>> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<TipoMaterial> resultado = useCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(TipoMaterialDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }
}
