package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.catalogs.imports.api.dto.CatalogImportResponse;
import com.jovycandy.anexo24.catalogs.imports.application.command.ExcelCatalogImportParser;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.CargarCatalogoUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** API V1 de staging de materiales y productos sin confirmación legacy. */
@RestController
@RequestMapping("/api/v1/catalogos/importaciones")
public class CatalogImportController {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private final ExcelCatalogImportParser parser;
    private final CargarCatalogoUseCase useCase;
    private final CatalogImportRepository repository;

    public CatalogImportController(ExcelCatalogImportParser parser, CargarCatalogoUseCase useCase,
                                   CatalogImportRepository repository) {
        this.parser = parser;
        this.useCase = useCase;
        this.repository = repository;
    }

    @PostMapping(value = "/materiales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MATERIALES_CARGAR')")
    public ResponseEntity<CatalogImportResponse> materiales(@RequestPart("archivo") MultipartFile file,
                                                              @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                              HttpServletRequest request) {
        return cargar(CatalogImportType.MATERIAL, file, principal, request);
    }

    @PostMapping(value = "/productos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCTOS_CARGAR')")
    public ResponseEntity<CatalogImportResponse> productos(@RequestPart("archivo") MultipartFile file,
                                                              @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                              HttpServletRequest request) {
        return cargar(CatalogImportType.PRODUCTO, file, principal, request);
    }

    @PostMapping(value = "/clientes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENTES_CARGAR')")
    public ResponseEntity<CatalogImportResponse> clientes(@RequestPart("archivo") MultipartFile file,
                                                              @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                              HttpServletRequest request) {
        return cargar(CatalogImportType.CLIENTE, file, principal, request);
    }

    @PostMapping(value = "/proveedores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROVEEDORES_CARGAR')")
    public ResponseEntity<CatalogImportResponse> proveedores(@RequestPart("archivo") MultipartFile file,
                                                              @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                              HttpServletRequest request) {
        return cargar(CatalogImportType.PROVEEDOR, file, principal, request);
    }

    @GetMapping("/materiales/{id}")
    @PreAuthorize("hasAuthority('MATERIALES_CARGAR')")
    public ResponseEntity<CatalogImportResponse> obtenerMaterial(@PathVariable long id,
                                                                   @RequestParam(defaultValue = "1") int pagina,
                                                                   @RequestParam(defaultValue = "100") int tamano) {
        return obtener(CatalogImportType.MATERIAL, id, pagina, tamano);
    }

    @GetMapping("/productos/{id}")
    @PreAuthorize("hasAuthority('PRODUCTOS_CARGAR')")
    public ResponseEntity<CatalogImportResponse> obtenerProducto(@PathVariable long id,
                                                                   @RequestParam(defaultValue = "1") int pagina,
                                                                   @RequestParam(defaultValue = "100") int tamano) {
        return obtener(CatalogImportType.PRODUCTO, id, pagina, tamano);
    }

    @GetMapping("/clientes/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_CARGAR')")
    public ResponseEntity<CatalogImportResponse> obtenerCliente(@PathVariable long id,
                                                                 @RequestParam(defaultValue = "1") int pagina,
                                                                 @RequestParam(defaultValue = "100") int tamano) {
        return obtener(CatalogImportType.CLIENTE, id, pagina, tamano);
    }

    @GetMapping("/proveedores/{id}")
    @PreAuthorize("hasAuthority('PROVEEDORES_CARGAR')")
    public ResponseEntity<CatalogImportResponse> obtenerProveedor(@PathVariable long id,
                                                                   @RequestParam(defaultValue = "1") int pagina,
                                                                   @RequestParam(defaultValue = "100") int tamano) {
        return obtener(CatalogImportType.PROVEEDOR, id, pagina, tamano);
    }

    @GetMapping("/materiales/{id}/errores")
    @PreAuthorize("hasAuthority('MATERIALES_CARGAR')")
    public ResponseEntity<List<CatalogImportResponse.Error>> erroresMaterial(@PathVariable long id,
                                                                               @RequestParam(defaultValue = "1") int pagina,
                                                                               @RequestParam(defaultValue = "100") int tamano) {
        return errores(CatalogImportType.MATERIAL, id, pagina, tamano);
    }

    @GetMapping("/productos/{id}/errores")
    @PreAuthorize("hasAuthority('PRODUCTOS_CARGAR')")
    public ResponseEntity<List<CatalogImportResponse.Error>> erroresProducto(@PathVariable long id,
                                                                               @RequestParam(defaultValue = "1") int pagina,
                                                                               @RequestParam(defaultValue = "100") int tamano) {
        return errores(CatalogImportType.PRODUCTO, id, pagina, tamano);
    }

    @GetMapping("/clientes/{id}/errores")
    @PreAuthorize("hasAuthority('CLIENTES_CARGAR')")
    public ResponseEntity<List<CatalogImportResponse.Error>> erroresCliente(@PathVariable long id,
                                                                             @RequestParam(defaultValue = "1") int pagina,
                                                                             @RequestParam(defaultValue = "100") int tamano) {
        return errores(CatalogImportType.CLIENTE, id, pagina, tamano);
    }

    @GetMapping("/proveedores/{id}/errores")
    @PreAuthorize("hasAuthority('PROVEEDORES_CARGAR')")
    public ResponseEntity<List<CatalogImportResponse.Error>> erroresProveedor(@PathVariable long id,
                                                                               @RequestParam(defaultValue = "1") int pagina,
                                                                               @RequestParam(defaultValue = "100") int tamano) {
        return errores(CatalogImportType.PROVEEDOR, id, pagina, tamano);
    }

    private ResponseEntity<CatalogImportResponse> cargar(CatalogImportType type, MultipartFile file,
                                                          AuthenticatedUserPrincipal principal, HttpServletRequest request) {
        byte[] bytes = read(file);
        CatalogImportArchivo parsed = parser.parsear(type, file.getOriginalFilename(), sha256(bytes), bytes);
        String correlationId = correlationId(request);
        long id = useCase.ejecutar(parsed, principal.userId(), correlationId);
        return repository.findById(type, id, 1, 100)
                .map(detail -> ResponseEntity.ok(CatalogImportResponse.from(detail, 100)))
                .orElseThrow(() -> new IllegalStateException("La carga recién creada no está disponible"));
    }

    private ResponseEntity<CatalogImportResponse> obtener(CatalogImportType type, long id, int pagina, int tamano) {
        validarPaginacion(id, pagina, tamano);
        return repository.findById(type, id, pagina, tamano)
                .map(detail -> ResponseEntity.ok(CatalogImportResponse.from(detail, tamano)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<List<CatalogImportResponse.Error>> errores(CatalogImportType type, long id, int pagina, int tamano) {
        validarPaginacion(id, pagina, tamano);
        return repository.findById(type, id, 1, 1)
                .map(detail -> ResponseEntity.ok(repository.findErrors(type, id, pagina, tamano).stream()
                        .map(error -> new CatalogImportResponse.Error(error.hoja(), error.fila(), error.columna(),
                                error.valorEnmascarado(), error.codigo(), error.mensaje()))
                        .toList()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES)
            throw new CatalogImportExceptionHandler.ArchivoInvalidoException("El archivo está vacío o supera 10 MiB.");
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase(Locale.ROOT).endsWith(".xls") || name.toLowerCase(Locale.ROOT).endsWith(".xlsx")))
            throw new CatalogImportExceptionHandler.ArchivoInvalidoException("Sólo se permiten archivos .xls y .xlsx.");
        try {
            byte[] bytes = file.getBytes();
            boolean xls = name.toLowerCase(Locale.ROOT).endsWith(".xls");
            boolean magic = xls ? bytes.length >= 4 && (bytes[0] & 255) == 0xD0 && (bytes[1] & 255) == 0xCF
                    && (bytes[2] & 255) == 0x11 && (bytes[3] & 255) == 0xE0
                    : bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
            if (!magic) throw new CatalogImportExceptionHandler.ArchivoInvalidoException("La firma no corresponde a la extensión.");
            return bytes;
        } catch (IOException exception) {
            throw new CatalogImportExceptionHandler.ArchivoInvalidoException("No fue posible leer el archivo.");
        }
    }

    private void validarPaginacion(long id, int pagina, int tamano) {
        if (id < 1 || pagina < 1 || tamano < 1 || tamano > 100)
            throw new CatalogImportExceptionHandler.ArchivoInvalidoException("Los parámetros de paginación no son válidos.");
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }

    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 no está disponible", exception); }
    }
}
