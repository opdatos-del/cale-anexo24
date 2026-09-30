package com.jovycandy.anexo24.catalogs.businessparties.clients.api;

import com.jovycandy.anexo24.catalogs.businessparties.clients.api.dto.ClienteDto;
import com.jovycandy.anexo24.catalogs.businessparties.clients.application.query.ListarClientesUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** API read-only de cliente. */
@RestController @RequestMapping("/api/v1/catalogos/clientes")
public class ClienteController {
    private final ListarClientesUseCase useCase;
    public ClienteController(ListarClientesUseCase useCase) { this.useCase=useCase; }
    @GetMapping @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<ClienteDto>> listar(@RequestParam(required=false) String filtro,@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="20") int tamano) {
        var result=useCase.ejecutar(filtro,pagina,tamano);
        return ResponseEntity.ok(new Pagina<>(result.items().stream().map(ClienteDto::from).toList(),result.total(),result.pagina(),result.tamano()));
    }
}
