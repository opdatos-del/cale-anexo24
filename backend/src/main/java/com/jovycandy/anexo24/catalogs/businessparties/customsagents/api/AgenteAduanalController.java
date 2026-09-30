package com.jovycandy.anexo24.catalogs.businessparties.customsagents.api;

import com.jovycandy.anexo24.catalogs.businessparties.customsagents.api.dto.AgenteAduanalDto;
import com.jovycandy.anexo24.catalogs.businessparties.customsagents.application.query.ListarAgentesAduanalesUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** API read-only de agente aduanal. */
@RestController @RequestMapping("/api/v1/catalogos/agentes-aduanales")
public class AgenteAduanalController {
    private final ListarAgentesAduanalesUseCase useCase;
    public AgenteAduanalController(ListarAgentesAduanalesUseCase useCase) { this.useCase=useCase; }
    @GetMapping @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<AgenteAduanalDto>> listar(@RequestParam(required=false) String filtro,@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="20") int tamano) {
        var result=useCase.ejecutar(filtro,pagina,tamano);
        return ResponseEntity.ok(new Pagina<>(result.items().stream().map(AgenteAduanalDto::from).toList(),result.total(),result.pagina(),result.tamano()));
    }
}
