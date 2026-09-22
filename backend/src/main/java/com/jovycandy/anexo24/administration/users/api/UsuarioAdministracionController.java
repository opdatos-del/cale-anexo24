package com.jovycandy.anexo24.administration.users.api;

import com.jovycandy.anexo24.administration.users.api.dto.ActualizarUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CrearUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.UsuarioAdministracionDto;
import com.jovycandy.anexo24.administration.users.application.command.ActualizarUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CrearUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.query.ListarUsuariosUseCase;
import com.jovycandy.anexo24.administration.users.application.query.ObtenerUsuarioUseCase;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.shared.api.ApiError;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controlador read-only de consulta administrativa de usuarios. */
@RestController
@RequestMapping("/api/v1/administracion/usuarios")
public class UsuarioAdministracionController {

    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    private final CrearUsuarioUseCase crearUsuarioUseCase;
    private final ActualizarUsuarioUseCase actualizarUsuarioUseCase;

    /**
     * Construye el controlador con los casos de uso de lectura.
     *
     * @param listarUsuariosUseCase  caso de uso de listado
     * @param obtenerUsuarioUseCase  caso de uso de detalle
     */
    public UsuarioAdministracionController(ListarUsuariosUseCase listarUsuariosUseCase,
                                           ObtenerUsuarioUseCase obtenerUsuarioUseCase,
                                           CrearUsuarioUseCase crearUsuarioUseCase,
                                           ActualizarUsuarioUseCase actualizarUsuarioUseCase) {
        this.listarUsuariosUseCase = listarUsuariosUseCase;
        this.obtenerUsuarioUseCase = obtenerUsuarioUseCase;
        this.crearUsuarioUseCase = crearUsuarioUseCase;
        this.actualizarUsuarioUseCase = actualizarUsuarioUseCase;
    }

    /**
     * Lista usuarios paginados con filtros opcionales y orden estable.
     *
     * @param clave    clave exacta opcional
     * @param nombre   nombre parcial opcional
     * @param correo   correo exacto opcional
     * @param estado   estado opcional (ACTIVO/INACTIVO)
     * @param perfilId perfil exacto opcional
     * @param pagina   número de página base 1
     * @param tamano   tamaño de página; rango 1-100
     * @return página de usuarios de administración
     */
    @Operation(
            summary = "Listar usuarios de administración",
            description = "Consulta read-only paginada con filtros opcionales y orden "
                    + "estable por clave e identificador.")
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
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<Pagina<UsuarioAdministracionDto>> listar(
            @Parameter(description = "Clave exacta", in = ParameterIn.QUERY)
            @RequestParam(required = false) String clave,
            @Parameter(description = "Nombre parcial (case-insensitive)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Correo exacto", in = ParameterIn.QUERY)
            @RequestParam(required = false) String correo,
            @Parameter(description = "Estado exacto: ACTIVO o INACTIVO", in = ParameterIn.QUERY)
            @RequestParam(required = false) String estado,
            @Parameter(description = "Identificador exacto del perfil", in = ParameterIn.QUERY)
            @RequestParam(required = false) Long perfilId,
            @Parameter(description = "Número de página base 1", example = "1", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20",
                    in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<UsuarioAdministracion> paginaDominio = listarUsuariosUseCase.ejecutar(
                clave, nombre, correo, estado, perfilId, pagina, tamano);
        Pagina<UsuarioAdministracionDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(UsuarioAdministracionDto::from).toList(),
                paginaDominio.total(),
                paginaDominio.pagina(),
                paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }

    /**
     * Obtiene el detalle read-only de un usuario.
     *
     * @param id identificador del usuario
     * @return usuario de administración
     */
    @Operation(
            summary = "Obtener usuario de administración",
            description = "Detalle read-only de un usuario con su perfil asignado; "
                    + "sin secretos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> crear(
            @Valid @org.springframework.web.bind.annotation.RequestBody CrearUsuarioRequest request,
            HttpServletRequest servletRequest) {
        UsuarioAdministracion usuario = crearUsuarioUseCase.ejecutar(request, correlationId(servletRequest));
        return ResponseEntity.created(java.net.URI.create("/api/v1/administracion/usuarios/" + usuario.id()))
                .body(UsuarioAdministracionDto.from(usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> actualizar(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody ActualizarUsuarioRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(UsuarioAdministracionDto.from(
                actualizarUsuarioUseCase.ejecutar(id, request, correlationId(servletRequest))));
    }

    private String correlationId(HttpServletRequest servletRequest) {
        Object valor = servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return valor == null ? null : valor.toString();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> obtener(
            @Parameter(description = "Identificador del usuario", example = "1",
                    in = ParameterIn.PATH, required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(UsuarioAdministracionDto.from(obtenerUsuarioUseCase.ejecutar(id)));
    }
}