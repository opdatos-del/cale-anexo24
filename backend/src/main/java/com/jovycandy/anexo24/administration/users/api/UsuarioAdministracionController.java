package com.jovycandy.anexo24.administration.users.api;

import com.jovycandy.anexo24.administration.users.api.dto.ActualizarUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CrearUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CambiarEstadoUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CambiarPerfilUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.CambiarVigenciaUsuarioRequest;
import com.jovycandy.anexo24.administration.users.api.dto.UsuarioAdministracionDto;
import com.jovycandy.anexo24.administration.users.api.dto.RestablecerPasswordUsuarioRequest;
import com.jovycandy.anexo24.administration.users.application.command.ActualizarUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CrearUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CambiarEstadoUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CambiarPerfilUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.CambiarVigenciaUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.RestablecerPasswordUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.application.command.model.ActualizarUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CrearUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CambiarEstadoUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CambiarPerfilUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.CambiarVigenciaUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.command.model.RestablecerPasswordUsuarioCommand;
import com.jovycandy.anexo24.administration.users.application.query.ListarUsuariosUseCase;
import com.jovycandy.anexo24.administration.users.application.query.ObtenerUsuarioUseCase;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Controlador de administración de usuarios. */
@RestController
@RequestMapping("/api/v1/administracion/usuarios")
public class UsuarioAdministracionController {

    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    private final CrearUsuarioUseCase crearUsuarioUseCase;
    private final ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    private final CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;
    private final CambiarPerfilUsuarioUseCase cambiarPerfilUsuarioUseCase;
    private final CambiarVigenciaUsuarioUseCase cambiarVigenciaUsuarioUseCase;
    private final RestablecerPasswordUsuarioUseCase restablecerPasswordUsuarioUseCase;

    /**
     * Construye el controlador con los casos de uso de usuarios.
     *
     * @param listarUsuariosUseCase caso de uso de listado
     * @param obtenerUsuarioUseCase caso de uso de detalle
     * @param crearUsuarioUseCase caso de uso de creación
     * @param actualizarUsuarioUseCase caso de uso de edición
     */
    public UsuarioAdministracionController(ListarUsuariosUseCase listarUsuariosUseCase,
                                           ObtenerUsuarioUseCase obtenerUsuarioUseCase,
                                           CrearUsuarioUseCase crearUsuarioUseCase,
                                           ActualizarUsuarioUseCase actualizarUsuarioUseCase,
                                           CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase,
                                           CambiarPerfilUsuarioUseCase cambiarPerfilUsuarioUseCase,
                                           CambiarVigenciaUsuarioUseCase cambiarVigenciaUsuarioUseCase,
                                           RestablecerPasswordUsuarioUseCase restablecerPasswordUsuarioUseCase) {
        this.listarUsuariosUseCase = listarUsuariosUseCase;
        this.obtenerUsuarioUseCase = obtenerUsuarioUseCase;
        this.crearUsuarioUseCase = crearUsuarioUseCase;
        this.actualizarUsuarioUseCase = actualizarUsuarioUseCase;
        this.cambiarEstadoUsuarioUseCase = cambiarEstadoUsuarioUseCase;
        this.cambiarPerfilUsuarioUseCase = cambiarPerfilUsuarioUseCase;
        this.cambiarVigenciaUsuarioUseCase = cambiarVigenciaUsuarioUseCase;
        this.restablecerPasswordUsuarioUseCase = restablecerPasswordUsuarioUseCase;
    }

    @Operation(
            summary = "Listar usuarios de administración",
            description = "Consulta read-only paginada con filtros opcionales y orden estable por clave e identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<Pagina<UsuarioAdministracionDto>> listar(
            @Parameter(description = "Clave exacta", in = ParameterIn.QUERY) @RequestParam(required = false) String clave,
            @Parameter(description = "Nombre parcial (case-insensitive)", in = ParameterIn.QUERY) @RequestParam(required = false) String nombre,
            @Parameter(description = "Correo exacto", in = ParameterIn.QUERY) @RequestParam(required = false) String correo,
            @Parameter(description = "Estado exacto: ACTIVO o INACTIVO", in = ParameterIn.QUERY) @RequestParam(required = false) String estado,
            @Parameter(description = "Identificador exacto del perfil", in = ParameterIn.QUERY) @RequestParam(required = false) Long perfilId,
            @Parameter(description = "Número de página base 1", example = "1", in = ParameterIn.QUERY) @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Elementos por página; rango 1-100", example = "20", in = ParameterIn.QUERY) @RequestParam(defaultValue = "20") int tamano) {
        Pagina<UsuarioAdministracion> paginaDominio = listarUsuariosUseCase.ejecutar(
                clave, nombre, correo, estado, perfilId, pagina, tamano);
        Pagina<UsuarioAdministracionDto> paginaDto = new Pagina<>(
                paginaDominio.items().stream().map(UsuarioAdministracionDto::from).toList(),
                paginaDominio.total(), paginaDominio.pagina(), paginaDominio.tamano());
        return ResponseEntity.ok(paginaDto);
    }

    @Operation(
            summary = "Obtener usuario de administración",
            description = "Detalle read-only de un usuario con su perfil asignado; sin secretos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> obtener(
            @Parameter(description = "Identificador del usuario", example = "1", in = ParameterIn.PATH, required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(UsuarioAdministracionDto.from(obtenerUsuarioUseCase.ejecutar(id)));
    }

    @Operation(
            summary = "Crear usuario de administración",
            description = "Crea un usuario ACTIVO con perfil existente y ACTIVO. La contraseña se almacena sólo como bcrypt y la auditoría es transaccional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Perfil no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Duplicado o perfil incompatible", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> crear(@Valid @RequestBody CrearUsuarioRequest request,
                                                            HttpServletRequest servletRequest) {
        CrearUsuarioCommand command = new CrearUsuarioCommand(request.clave(), request.nombre(), request.correo(),
                request.password(), request.vigencia(), request.perfilId());
        UsuarioAdministracion usuario = crearUsuarioUseCase.ejecutar(command, correlationId(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/administracion/usuarios/" + usuario.id()))
                .body(UsuarioAdministracionDto.from(usuario));
    }

    @Operation(
            summary = "Actualizar datos básicos de usuario",
            description = "Actualiza únicamente nombre y correo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Correo duplicado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> actualizar(@PathVariable Long id,
                                                                 @Valid @RequestBody ActualizarUsuarioRequest request,
                                                                 HttpServletRequest servletRequest) {
        ActualizarUsuarioCommand command = new ActualizarUsuarioCommand(request.nombre(), request.correo());
        return ResponseEntity.ok(UsuarioAdministracionDto.from(
                actualizarUsuarioUseCase.ejecutar(id, command, correlationId(servletRequest))));
    }

    @Operation(summary = "Cambiar estado de usuario", description = "Cambia exclusivamente estado ACTIVO/INACTIVO; protege al último administrador efectivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "Estado incompatible"),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible")
    })
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> cambiarEstado(@PathVariable Long id,
                                                                    @Valid @RequestBody CambiarEstadoUsuarioRequest request,
                                                                    HttpServletRequest servletRequest) {
        UsuarioAdministracion usuario = cambiarEstadoUsuarioUseCase.ejecutar(id,
                new CambiarEstadoUsuarioCommand(request.estado()), correlationId(servletRequest));
        return ResponseEntity.ok(UsuarioAdministracionDto.from(usuario));
    }

    @Operation(summary = "Cambiar perfil de usuario", description = "Asigna un perfil existente y ACTIVO; protege al último administrador efectivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario o perfil no encontrado"),
            @ApiResponse(responseCode = "409", description = "Estado incompatible"),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible")
    })
    @PatchMapping("/{id}/perfil")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> cambiarPerfil(@PathVariable Long id,
                                                                    @Valid @RequestBody CambiarPerfilUsuarioRequest request,
                                                                    HttpServletRequest servletRequest) {
        UsuarioAdministracion usuario = cambiarPerfilUsuarioUseCase.ejecutar(id,
                new CambiarPerfilUsuarioCommand(request.perfilId()), correlationId(servletRequest));
        return ResponseEntity.ok(UsuarioAdministracionDto.from(usuario));
    }

    @Operation(summary = "Cambiar vigencia de usuario", description = "Actualiza vigencia; null representa cuenta sin vencimiento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vigencia actualizada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "Estado incompatible"),
            @ApiResponse(responseCode = "503", description = "Servicio de datos no disponible")
    })
    @PatchMapping("/{id}/vigencia")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<UsuarioAdministracionDto> cambiarVigencia(@PathVariable Long id,
                                                                      @Valid @RequestBody CambiarVigenciaUsuarioRequest request,
                                                                      HttpServletRequest servletRequest) {
        UsuarioAdministracion usuario = cambiarVigenciaUsuarioUseCase.ejecutar(id,
                new CambiarVigenciaUsuarioCommand(request.vigencia()), correlationId(servletRequest));
        return ResponseEntity.ok(UsuarioAdministracionDto.from(usuario));
    }

    @Operation(
            summary = "Restablecer contraseña de usuario",
            description = "Reemplaza la contraseña de un usuario mediante hash BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña restablecida"),
            @ApiResponse(responseCode = "400", description = "Solicitud o contraseña inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Dependencia no disponible", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ResponseEntity<Void> restablecerPassword(@PathVariable Long id,
                                                     @Valid @RequestBody RestablecerPasswordUsuarioRequest request,
                                                     HttpServletRequest servletRequest) {
        restablecerPasswordUsuarioUseCase.ejecutar(id,
                new RestablecerPasswordUsuarioCommand(request.password()), correlationId(servletRequest));
        return ResponseEntity.noContent().build();
    }

    private String correlationId(HttpServletRequest servletRequest) {
        Object valor = servletRequest.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return valor == null ? null : valor.toString();
    }

}
