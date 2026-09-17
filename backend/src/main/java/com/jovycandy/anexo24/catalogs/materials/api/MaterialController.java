package com.jovycandy.anexo24.catalogs.materials.api;

import com.jovycandy.anexo24.catalogs.materials.api.dto.MaterialDto;
import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.catalogs.materials.domain.port.MaterialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador del catálogo de materiales (RF-010).
 *
 * <p>Consulta paginada y con filtro; requiere el permiso
 * {@code MATERIALES_CONSULTAR}.</p>
 */
@RestController
@RequestMapping("/api/v1/catalogos/materiales")
public class MaterialController {

    private final MaterialRepository materialRepository;

    /**
     * Constructor con el puerto de materiales.
     *
     * @param materialRepository puerto de consulta de materiales
     */
    public MaterialController(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    /**
     * Lista materiales paginados.
     *
     * @param filtro texto opcional para filtrar clave, descripción o fracción
     * @param pagina número de página (default 1)
     * @param tamano tamaño de página (default 20, máximo 100)
     * @return página de materiales
     */
    @GetMapping
    @PreAuthorize("hasAuthority('MATERIALES_CONSULTAR')")
    public ResponseEntity<Pagina<MaterialDto>> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        int tamanoReal = Math.min(Math.max(tamano, 1), 100);
        int paginaReal = Math.max(pagina, 1);
        Pagina<Material> paginaDominio = materialRepository.findPage(filtro, paginaReal, tamanoReal);
        Pagina<MaterialDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(MaterialDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}