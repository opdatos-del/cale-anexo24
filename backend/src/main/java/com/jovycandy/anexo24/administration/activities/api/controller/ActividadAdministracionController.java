package com.jovycandy.anexo24.administration.activities.api.controller;

import com.jovycandy.anexo24.administration.activities.api.dto.ActividadAdministracionDto;
import com.jovycandy.anexo24.administration.activities.application.query.ListarActividadesUseCase;
import com.jovycandy.anexo24.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Controlador del catálogo técnico de actividades para administración de perfiles. */
@RestController
@RequestMapping("/api/v1/administracion/actividades")
public class ActividadAdministracionController {

    private final ListarActividadesUseCase listarActividadesUseCase;

    /**
     * Construye el controlador con el caso de uso de listado.
     *
     * @param listarActividadesUseCase caso de uso de listado
     */
    public ActividadAdministracionController(ListarActividadesUseCase listarActividadesUseCase) {
        this.listarActividadesUseCase = listarActividadesUseCase;
    }

    @Operation(
            summary = "Listar actividades de administración",
            description = "Consulta read-only del catálogo técnico ordenado por clave e identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
    public ResponseEntity<List<ActividadAdministracionDto>> listar() {
        List<ActividadAdministracionDto> actividades = listarActividadesUseCase.ejecutar().stream()
                .map(ActividadAdministracionDto::from)
                .toList();
        return ResponseEntity.ok(actividades);
    }
}
