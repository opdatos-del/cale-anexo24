package com.jovycandy.anexo24.catalogs.structures.api;

import com.jovycandy.anexo24.catalogs.structures.api.dto.EstructuraDetalleDto;
import com.jovycandy.anexo24.catalogs.structures.application.query.ListarEstructurasUseCase;
import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;
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

/** Controlador del catálogo de líneas BOM. */
@RestController
@RequestMapping("/api/v1/catalogos/estructuras")
public class StructureController {

    private final ListarEstructurasUseCase listarEstructurasUseCase;

    /**
     * Constructor con el caso de uso de estructuras.
     *
     * @param listarEstructurasUseCase caso de uso de consulta BOM
     */
    public StructureController(ListarEstructurasUseCase listarEstructurasUseCase) {
        this.listarEstructurasUseCase = listarEstructurasUseCase;
    }

    /**
     * Lista líneas BOM paginadas desde el procedimiento APP24.
     *
     * @param producto clave funcional opcional del producto
     * @param material clave funcional opcional del material
     * @param pagina número de página base 1
     * @param tamano tamaño de página; rango 1-100
     * @return página de líneas BOM
     */
    @Operation(summary = "Consultar estructuras",
            description = "Consulta paginada de líneas BOM del Módulo C.")
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
    @PreAuthorize("hasAuthority('ESTRUCTURAS_CONSULTAR')")
    public ResponseEntity<Pagina<EstructuraDetalleDto>> listar(
            @Parameter(description = "Clave del producto", in = ParameterIn.QUERY)
            @RequestParam(required = false) String producto,
            @Parameter(description = "Clave del material", in = ParameterIn.QUERY)
            @RequestParam(required = false) String material,
            @Parameter(description = "Número de página base 1", example = "1",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<EstructuraDetalle> paginaDominio = listarEstructurasUseCase
                .ejecutar(producto, material, pagina, tamano);
        Pagina<EstructuraDetalleDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(EstructuraDetalleDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
