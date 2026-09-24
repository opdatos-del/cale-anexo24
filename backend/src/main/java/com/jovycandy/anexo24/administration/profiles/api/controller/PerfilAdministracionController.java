package com.jovycandy.anexo24.administration.profiles.api.controller;

import com.jovycandy.anexo24.administration.profiles.api.dto.ActualizarNombrePerfilRequest;
import com.jovycandy.anexo24.administration.profiles.api.dto.CambiarEstadoPerfilRequest;
import com.jovycandy.anexo24.administration.profiles.api.dto.CrearPerfilRequest;
import com.jovycandy.anexo24.administration.profiles.api.dto.PerfilAdministracionDto;
import com.jovycandy.anexo24.administration.profiles.api.dto.PerfilPermisosDetalleDto;
import com.jovycandy.anexo24.administration.profiles.api.dto.ReemplazarPermisosPerfilRequest;
import com.jovycandy.anexo24.administration.profiles.application.command.ActualizarNombrePerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.CambiarEstadoPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.CrearPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.ReemplazarPermisosPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.model.ActualizarNombrePerfilCommand;
import com.jovycandy.anexo24.administration.profiles.application.command.model.CambiarEstadoPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.application.command.model.CrearPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.application.command.model.ReemplazarPermisosPerfilCommand;
import com.jovycandy.anexo24.administration.profiles.application.query.ListarPerfilesUseCase;
import com.jovycandy.anexo24.administration.profiles.application.query.ObtenerPermisosPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import com.jovycandy.anexo24.shared.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Controlador de administración de perfiles. */
@RestController
@RequestMapping("/api/v1/administracion/perfiles")
public class PerfilAdministracionController {
    private final ListarPerfilesUseCase listarPerfilesUseCase;
    private final CrearPerfilUseCase crearPerfilUseCase;
    private final ActualizarNombrePerfilUseCase actualizarNombrePerfilUseCase;
    private final CambiarEstadoPerfilUseCase cambiarEstadoPerfilUseCase;
    private final ObtenerPermisosPerfilUseCase obtenerPermisosPerfilUseCase;
    private final ReemplazarPermisosPerfilUseCase reemplazarPermisosPerfilUseCase;

    public PerfilAdministracionController(ListarPerfilesUseCase listarPerfilesUseCase, CrearPerfilUseCase crearPerfilUseCase,
            ActualizarNombrePerfilUseCase actualizarNombrePerfilUseCase, CambiarEstadoPerfilUseCase cambiarEstadoPerfilUseCase,
            ObtenerPermisosPerfilUseCase obtenerPermisosPerfilUseCase,
            ReemplazarPermisosPerfilUseCase reemplazarPermisosPerfilUseCase) {
        this.listarPerfilesUseCase = listarPerfilesUseCase; this.crearPerfilUseCase = crearPerfilUseCase;
        this.actualizarNombrePerfilUseCase = actualizarNombrePerfilUseCase; this.cambiarEstadoPerfilUseCase = cambiarEstadoPerfilUseCase;
        this.obtenerPermisosPerfilUseCase = obtenerPermisosPerfilUseCase;
        this.reemplazarPermisosPerfilUseCase = reemplazarPermisosPerfilUseCase;
    }

    @Operation(summary = "Listar perfiles de administración", description = "Consulta read-only paginada con filtros opcionales y orden estable por nombre e identificador.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Consulta realizada"), @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USUARIOS_ADMINISTRAR','PERFILES_ADMINISTRAR')")
    public ResponseEntity<Pagina<PerfilAdministracionDto>> listar(
            @Parameter(description = "Nombre parcial (case-insensitive)", in = ParameterIn.QUERY) @RequestParam(required = false) String nombre,
            @Parameter(description = "Estado exacto: ACTIVO o INACTIVO", in = ParameterIn.QUERY) @RequestParam(required = false) String estado,
            @Parameter(description = "Número de página base 1", example = "1", in = ParameterIn.QUERY) @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20", in = ParameterIn.QUERY) @RequestParam(defaultValue = "20") int tamano) {
        Pagina<PerfilAdministracion> resultado = listarPerfilesUseCase.ejecutar(nombre, estado, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(PerfilAdministracionDto::from).toList(), resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @Operation(summary = "Crear perfil", description = "Crea un perfil ACTIVO sin permisos implícitos.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Perfil creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    @PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
    public ResponseEntity<PerfilAdministracionDto> crear(@Valid @RequestBody CrearPerfilRequest request, HttpServletRequest servletRequest) {
        PerfilAdministracion perfil = crearPerfilUseCase.ejecutar(new CrearPerfilCommand(request.nombre()), correlationId(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/administracion/perfiles/" + perfil.id())).body(PerfilAdministracionDto.from(perfil));
    }

    @Operation(summary = "Actualizar nombre de perfil", description = "Actualiza exclusivamente el nombre del perfil.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Perfil actualizado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Perfil inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
    public ResponseEntity<PerfilAdministracionDto> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarNombrePerfilRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(PerfilAdministracionDto.from(actualizarNombrePerfilUseCase.ejecutar(id, new ActualizarNombrePerfilCommand(request.nombre()), correlationId(servletRequest))));
    }

    @Operation(summary = "Cambiar estado de perfil", description = "Cambia entre ACTIVO e INACTIVO y aplica el guardrail de administradores efectivos.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Estado actualizado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Perfil inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Guardrail de administrador efectivo", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
    public ResponseEntity<PerfilAdministracionDto> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoPerfilRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(PerfilAdministracionDto.from(cambiarEstadoPerfilUseCase.ejecutar(id, new CambiarEstadoPerfilCommand(request.estado()), correlationId(servletRequest))));
    }

    @Operation(summary = "Consultar permisos de perfil", description = "Obtiene permisos ordenados por clave e identificador.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Consulta realizada"), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Perfil inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
    public ResponseEntity<PerfilPermisosDetalleDto> permisos(@PathVariable Long id) {
        return ResponseEntity.ok(PerfilPermisosDetalleDto.from(obtenerPermisosPerfilUseCase.ejecutar(id)));
    }

    @Operation(summary = "Reemplazar permisos de perfil", description = "Reemplaza atómicamente el conjunto final de actividades; una lista vacía es válida.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Permisos reemplazados"), @ApiResponse(responseCode = "400", description = "IDs inválidos o repetidos", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Perfil o actividad inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Guardrail de administrador efectivo", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PutMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('PERFILES_ADMINISTRAR')")
    public ResponseEntity<PerfilPermisosDetalleDto> reemplazarPermisos(@PathVariable Long id, @Valid @RequestBody ReemplazarPermisosPerfilRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(PerfilPermisosDetalleDto.from(reemplazarPermisosPerfilUseCase.ejecutar(id, new ReemplazarPermisosPerfilCommand(request.actividadIds()), correlationId(servletRequest))));
    }

    private String correlationId(HttpServletRequest servletRequest) {
        Object valor = servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return valor == null ? null : valor.toString();
    }
}
