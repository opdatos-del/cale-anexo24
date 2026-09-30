package com.jovycandy.anexo24.catalogs.auxiliary.categories.api;

import com.jovycandy.anexo24.catalogs.auxiliary.categories.api.dto.CategoriaDto;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.application.query.ListarCategoriasUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.domain.model.Categoria;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta HTTP del catálogo de categorías. */
@RestController
@RequestMapping("/api/v1/catalogos/categorias")
public class CategoriaController {
    private final ListarCategoriasUseCase useCase;

    public CategoriaController(ListarCategoriasUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOGOS_AUX_CONSULTAR')")
    public ResponseEntity<Pagina<CategoriaDto>> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Categoria> resultado = useCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(CategoriaDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }
}
