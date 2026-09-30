package com.jovycandy.anexo24.catalogs.auxiliary.warehouses.api;

import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.api.dto.AlmacenDto;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.application.query.ListarAlmacenesUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.model.Almacen;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta HTTP del catálogo de almacenes. */
@RestController
@RequestMapping("/api/v1/catalogos/almacenes")
public class AlmacenController {
    private final ListarAlmacenesUseCase useCase;

    public AlmacenController(ListarAlmacenesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<AlmacenDto>> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Almacen> resultado = useCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(AlmacenDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }
}
