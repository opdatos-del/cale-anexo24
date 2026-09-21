package com.jovycandy.anexo24.operations.usedmaterials.api;

import com.jovycandy.anexo24.operations.usedmaterials.api.dto.MaterialUtilizadoDto;
import com.jovycandy.anexo24.operations.usedmaterials.application.query.ListarMaterialesUtilizadosUseCase;
import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Controlador de consulta del histórico de materiales utilizados. */
@RestController
@RequestMapping("/api/v1/operaciones/materiales-utilizados")
public class UsedMaterialController {

    private final ListarMaterialesUtilizadosUseCase listarMaterialesUtilizadosUseCase;

    /**
     * Constructor con el caso de uso de materiales utilizados.
     *
     * @param listarMaterialesUtilizadosUseCase caso de uso de consulta histórica
     */
    public UsedMaterialController(ListarMaterialesUtilizadosUseCase listarMaterialesUtilizadosUseCase) {
        this.listarMaterialesUtilizadosUseCase = listarMaterialesUtilizadosUseCase;
    }

    /**
     * Lista filas históricas de descarga paginadas mediante el SP APP24 read-only.
     * El rango se aplica a la fecha de salida (`SALIDAS.Fecha`).
     *
     * @param desde fecha inicial inclusiva en formato ISO
     * @param hasta fecha final inclusiva en formato ISO
     * @param material código de material opcional
     * @param producto código de producto opcional
     * @param pedimentoSalida documento de salida opcional
     * @param clavePedimentoSalida clave de pedimento de salida opcional
     * @param pagina número de página base 1
     * @param tamano tamaño de página; rango 1-100
     * @return página de materiales utilizados
     */
    @Operation(summary = "Consultar materiales utilizados",
            description = "Consulta paginada del histórico de descargas. "
                    + "El rango se aplica a la fecha de salida.")
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
    @PreAuthorize("hasAuthority('OPERACIONES_CONSULTAR')")
    public ResponseEntity<Pagina<MaterialUtilizadoDto>> listar(
            @Parameter(description = "Fecha inicial inclusiva de salida; formato YYYY-MM-DD",
                    in = ParameterIn.QUERY, required = true)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final inclusiva de salida; formato YYYY-MM-DD",
                    in = ParameterIn.QUERY, required = true)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Código de material", in = ParameterIn.QUERY)
            @RequestParam(required = false) String material,
            @Parameter(description = "Código de producto", in = ParameterIn.QUERY)
            @RequestParam(required = false) String producto,
            @Parameter(description = "Documento de salida", in = ParameterIn.QUERY)
            @RequestParam(required = false) String pedimentoSalida,
            @Parameter(description = "Clave de pedimento de salida", in = ParameterIn.QUERY)
            @RequestParam(required = false) String clavePedimentoSalida,
            @Parameter(description = "Número de página base 1", example = "1",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<MaterialUtilizado> paginaDominio = listarMaterialesUtilizadosUseCase.ejecutar(
                desde, hasta, material, producto, pedimentoSalida, clavePedimentoSalida, pagina, tamano);
        Pagina<MaterialUtilizadoDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(MaterialUtilizadoDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
