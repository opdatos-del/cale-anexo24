package com.jovycandy.anexo24.catalogs.businessparties.providers.api;

import com.jovycandy.anexo24.catalogs.businessparties.providers.api.dto.ProveedorDto;
import com.jovycandy.anexo24.catalogs.businessparties.providers.application.query.ListarProveedoresUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** API read-only de proveedor. */
@RestController @RequestMapping("/api/v1/catalogos/proveedores")
public class ProveedorController {
    private final ListarProveedoresUseCase useCase;
    public ProveedorController(ListarProveedoresUseCase useCase) { this.useCase=useCase; }
    @GetMapping @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<ProveedorDto>> listar(@RequestParam(required=false) String filtro,@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="20") int tamano) {
        var result=useCase.ejecutar(filtro,pagina,tamano);
        return ResponseEntity.ok(new Pagina<>(result.items().stream().map(ProveedorDto::from).toList(),result.total(),result.pagina(),result.tamano()));
    }
}
