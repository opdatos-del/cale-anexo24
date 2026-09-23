package com.jovycandy.anexo24.administration.profiles.api.controller;

import com.jovycandy.anexo24.administration.profiles.api.dto.PerfilAdministracionDto;
import com.jovycandy.anexo24.administration.profiles.application.query.ListarPerfilesUseCase;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
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

/** Controlador de administración de perfiles. */
@RestController
@RequestMapping("/api/v1/administracion/perfiles")
public class PerfilAdministracionController {

    private final ListarPerfilesUseCase listarPerfilesUseCase;

    /**
     * Construye el controlador con el caso de uso de listado.
     *
     * @param listarPerfilesUseCase caso de uso de listado
     */
    public PerfilAdministracionController(ListarPerfilesUseCase listarPerfilesUseCase) {
        this.listarPerfilesUseCase = listarPerfilesUseCase;
    }

    @Operation(
            summary = "Listar perfiles de administración",
            description = "Consulta read-only paginada con filtros opcionales y orden estable por nombre e identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USUARIOS_ADMINISTRAR','PERFILES_ADMINISTRAR')")
    public ResponseEntity<Pagina<PerfilAdministracionDto>> listar(
            @Parameter(description = "Nombre parcial (case-insensitive)", in = ParameterIn.QUERY) @RequestParam(required = false) String nombre,
            @Parameter(description = "Estado exacto: ACTIVO o INACTIVO", in = ParameterIn.QUERY) @RequestParam(required = false) String estado,
            @Parameter(description = "Número de página base 1", example = "1", in = ParameterIn.QUERY) @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20", in = ParameterIn.QUERY) @RequestParam(defaultValue = "20") int tamano) {
        Pagina<PerfilAdministracion> paginaDominio = listarPerfilesUseCase.ejecutar(nombre, estado, pagina, tamano);
        Pagina<PerfilAdministracionDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(PerfilAdministracionDto::from).toList(),
                paginaDominio.total(), paginaDominio.pagina(), paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }
}
