package com.jovycandy.anexo24.operations.fixedassets.api;

import com.jovycandy.anexo24.operations.fixedassets.api.dto.ActivoFijoDto;
import com.jovycandy.anexo24.operations.fixedassets.application.query.ListarActivosFijosUseCase;
import com.jovycandy.anexo24.operations.fixedassets.domain.model.ActivoFijo;
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

/** Controlador de consulta de partidas de importación marcadas como activos fijos. */
@RestController
@RequestMapping("/api/v1/operaciones/activos-fijos")
public class FixedAssetController {

    private final ListarActivosFijosUseCase listarActivosFijosUseCase;

    /**
     * Constructor con el caso de uso de Activos Fijos.
     *
     * @param listarActivosFijosUseCase caso de uso de consulta de activos fijos
     */
    public FixedAssetController(ListarActivosFijosUseCase listarActivosFijosUseCase) {
        this.listarActivosFijosUseCase = listarActivosFijosUseCase;
    }

    /**
     * Lista partidas activas paginadas mediante el SP APP24 read-only.
     * El rango opcional se aplica a `IMPORTACIONES.Fecha`.
     *
     * @param desde fecha inicial inclusiva en formato ISO; se informa junto con hasta
     * @param hasta fecha final inclusiva en formato ISO; se informa junto con desde
     * @param pedimento número de pedimento opcional
     * @param clavePedimento clave de pedimento opcional
     * @param numeroParte número de parte opcional
     * @param descripcion descripción histórica opcional
     * @param serie número de serie histórico opcional
     * @param marca marca histórica opcional
     * @param modelo modelo histórico opcional
     * @param pagina número de página base 1
     * @param tamano tamaño de página; rango 1-100
     * @return página de partidas marcadas como activo fijo
     */
    @Operation(summary = "Consultar activos fijos",
            description = "Consulta paginada de partidas de importación marcadas como activos. "
                    + "El rango de fecha es opcional y, cuando se informa, se aplica a la fecha de importación.")
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
    public ResponseEntity<Pagina<ActivoFijoDto>> listar(
            @Parameter(description = "Fecha inicial inclusiva de importación; formato YYYY-MM-DD",
                    in = ParameterIn.QUERY)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final inclusiva de importación; formato YYYY-MM-DD",
                    in = ParameterIn.QUERY)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Número de pedimento", in = ParameterIn.QUERY)
            @RequestParam(required = false) String pedimento,
            @Parameter(description = "Clave de pedimento", in = ParameterIn.QUERY)
            @RequestParam(required = false) String clavePedimento,
            @Parameter(description = "Número de parte", in = ParameterIn.QUERY)
            @RequestParam(required = false) String numeroParte,
            @Parameter(description = "Descripción histórica", in = ParameterIn.QUERY)
            @RequestParam(required = false) String descripcion,
            @Parameter(description = "Número de serie histórico", in = ParameterIn.QUERY)
            @RequestParam(required = false) String serie,
            @Parameter(description = "Marca histórica", in = ParameterIn.QUERY)
            @RequestParam(required = false) String marca,
            @Parameter(description = "Modelo histórico", in = ParameterIn.QUERY)
            @RequestParam(required = false) String modelo,
            @Parameter(description = "Número de página base 1", example = "1",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<ActivoFijo> paginaDominio = listarActivosFijosUseCase.ejecutar(
                desde, hasta, pedimento, clavePedimento, numeroParte, descripcion,
                serie, marca, modelo, pagina, tamano);
        Pagina<ActivoFijoDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(ActivoFijoDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
