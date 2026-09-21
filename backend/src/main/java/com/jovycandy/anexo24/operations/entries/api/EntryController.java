package com.jovycandy.anexo24.operations.entries.api;

import com.jovycandy.anexo24.operations.entries.api.dto.EntradaLineaDto;
import com.jovycandy.anexo24.operations.entries.application.query.ListarEntradasUseCase;
import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
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

/** Controlador de consulta de Entradas e Importaciones. */
@RestController
@RequestMapping("/api/v1/operaciones/entradas")
public class EntryController {

    private final ListarEntradasUseCase listarEntradasUseCase;

    /**
     * Constructor con el caso de uso de Entradas.
     *
     * @param listarEntradasUseCase caso de uso de consulta de Entradas
     */
    public EntryController(ListarEntradasUseCase listarEntradasUseCase) {
        this.listarEntradasUseCase = listarEntradasUseCase;
    }

    /**
     * Lista líneas de entrada paginadas mediante el SP APP24 read-only.
     * El rango se aplica a la fecha de pago (`Importaciones.Fecha`).
     *
     * @param desde fecha inicial inclusiva en formato ISO
     * @param hasta fecha final inclusiva en formato ISO
     * @param pedimento número de pedimento opcional
     * @param clavePedimento clave de pedimento opcional
     * @param fraccion fracción arancelaria opcional
     * @param numeroParte número de parte opcional
     * @param pagina número de página base 1
     * @param tamano tamaño de página; rango 1-100
     * @return página de líneas de Entrada
     */
    @Operation(summary = "Consultar entradas",
            description = "Consulta paginada de líneas de importación. "
                    + "El rango se aplica a la fecha de pago.")
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
    public ResponseEntity<Pagina<EntradaLineaDto>> listar(
            @Parameter(description = "Fecha inicial inclusiva de pago; formato YYYY-MM-DD",
                    in = ParameterIn.QUERY, required = true)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final inclusiva de pago; formato YYYY-MM-DD",
                    in = ParameterIn.QUERY, required = true)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Número de pedimento", in = ParameterIn.QUERY)
            @RequestParam(required = false) String pedimento,
            @Parameter(description = "Clave de pedimento", in = ParameterIn.QUERY)
            @RequestParam(required = false) String clavePedimento,
            @Parameter(description = "Fracción arancelaria", in = ParameterIn.QUERY)
            @RequestParam(required = false) String fraccion,
            @Parameter(description = "Número de parte", in = ParameterIn.QUERY)
            @RequestParam(required = false) String numeroParte,
            @Parameter(description = "Número de página base 1", example = "1",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<EntradaLinea> paginaDominio = listarEntradasUseCase.ejecutar(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano);
        Pagina<EntradaLineaDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(EntradaLineaDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
