package com.jovycandy.anexo24.catalogs.products.api;

import com.jovycandy.anexo24.catalogs.products.api.dto.ProductoDto;
import com.jovycandy.anexo24.catalogs.products.application.query.ListarProductosUseCase;
import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;
import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controlador del catálogo de Productos. */
@RestController
@RequestMapping("/api/v1/catalogos/productos")
public class ProductController {

    private final ListarProductosUseCase listarProductosUseCase;

    /**
     * Constructor con el caso de uso de Productos.
     *
     * @param listarProductosUseCase caso de uso de consulta de productos
     */
    public ProductController(ListarProductosUseCase listarProductosUseCase) {
        this.listarProductosUseCase = listarProductosUseCase;
    }

    /**
     * Lista productos paginados desde el procedimiento de consulta APP24.
     *
     * @param filtro texto opcional para código, nombre o fracción
     * @param pagina número de página base 1
     * @param tamano tamaño de página; rango 1-100
     * @return página de productos
     */
    @Operation(summary = "Consultar productos",
            description = "Consulta paginada del catálogo de productos del Módulo C.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_CONSULTAR')")
    public ResponseEntity<Pagina<ProductoDto>> listar(
            @Parameter(description = "Texto para buscar por código, nombre o fracción",
                    in = ParameterIn.QUERY)
            @RequestParam(required = false) String filtro,
            @Parameter(description = "Número de página base 1", example = "1",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Producto> paginaDominio = listarProductosUseCase.ejecutar(filtro, pagina, tamano);
        Pagina<ProductoDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(ProductoDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
