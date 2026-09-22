package com.jovycandy.anexo24.auditlog.api;

import com.jovycandy.anexo24.auditlog.api.dto.BitacoraRegistroDto;
import com.jovycandy.anexo24.auditlog.application.query.ListarBitacoraUseCase;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
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

import java.time.Instant;
import java.time.OffsetDateTime;

/** Controlador read-only de consulta de eventos propios de Bitácora. */
@RestController
@RequestMapping("/api/v1/bitacora")
public class BitacoraController {

    private final ListarBitacoraUseCase listarBitacoraUseCase;

    /**
     * Construye el controlador con el caso de uso de lectura.
     *
     * @param listarBitacoraUseCase caso de uso de consulta de Bitácora
     */
    public BitacoraController(ListarBitacoraUseCase listarBitacoraUseCase) {
        this.listarBitacoraUseCase = listarBitacoraUseCase;
    }

    /**
     * Consulta eventos propios de la aplicación con rango UTC y filtros exactos.
     *
     * @param desde         instante inicial inclusivo ISO-8601 con offset
     * @param hasta         instante final inclusivo ISO-8601 con offset
     * @param usuarioId     actor opcional
     * @param modulo        módulo opcional
     * @param resultado     resultado opcional
     * @param correlationId correlación exacta opcional
     * @param pagina        número de página base 1
     * @param tamano        tamaño de página; rango 1-100
     * @return página de eventos de Bitácora
     */
    @Operation(
            summary = "Consultar bitácora de la aplicación",
            description = "Consulta read-only de eventos propios con rango UTC inclusivo, "
                    + "filtros exactos y paginación estable.")
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
    @PreAuthorize("hasAuthority('BITACORA_CONSULTAR')")
    public ResponseEntity<Pagina<BitacoraRegistroDto>> listar(
            @Parameter(description = "Instante UTC inicial inclusivo; ISO-8601 con offset",
                    example = "2026-09-22T00:00:00Z", in = ParameterIn.QUERY, required = true)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @Parameter(description = "Instante UTC final inclusivo; ISO-8601 con offset",
                    example = "2026-09-22T07:00:00-06:00", in = ParameterIn.QUERY, required = true)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @Parameter(description = "Identificador exacto del usuario", in = ParameterIn.QUERY)
            @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Módulo controlado del evento", in = ParameterIn.QUERY)
            @RequestParam(required = false) BitacoraModulo modulo,
            @Parameter(description = "Resultado controlado del evento", in = ParameterIn.QUERY)
            @RequestParam(required = false) BitacoraResultado resultado,
            @Parameter(description = "Correlation ID exacto", in = ParameterIn.QUERY)
            @RequestParam(required = false) String correlationId,
            @Parameter(description = "Número de página base 1", example = "1", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Instant desdeUtc = desde == null ? null : desde.toInstant();
        Instant hastaUtc = hasta == null ? null : hasta.toInstant();
        Pagina<BitacoraRegistro> paginaDominio = listarBitacoraUseCase.ejecutar(
                desdeUtc, hastaUtc, usuarioId, modulo, resultado, correlationId, pagina, tamano);
        Pagina<BitacoraRegistroDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(BitacoraRegistroDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
