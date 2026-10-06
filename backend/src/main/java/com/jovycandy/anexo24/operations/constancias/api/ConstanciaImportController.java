package com.jovycandy.anexo24.operations.constancias.api;

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

/**
 * API de staging y previsualizacion de constancias. Reutiliza el motor generico de importacion
 * (parser + staging) pero expone endpoints de OPERACIONES, nunca bajo /catalogos.
 */
@RestController
@RequestMapping("/api/v1/operaciones/constancias/importaciones")
public class ConstanciaImportController {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final CatalogImportType TIPO = CatalogImportType.CONSTANCIA;

    private final ExcelCatalogImportParser parser;
    private final CargarCatalogoUseCase useCase;
    private final CatalogImportRepository repository;

    public ConstanciaImportController(ExcelCatalogImportParser parser, CargarCatalogoUseCase useCase,
                                CatalogImportRepository repository) {
        this.parser = parser;
        this.useCase = useCase;
        this.repository = repository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSTANCIAS_CARGAR')")
    public ResponseEntity<CatalogImportResponse> cargar(@RequestPart("archivo") MultipartFile file,
                                                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                        HttpServletRequest request) {
        byte[] bytes = read(file);
        CatalogImportArchivo parsed = parser.parsear(TIPO, file.getOriginalFilename(), sha256(bytes), bytes);
        long id = useCase.ejecutar(parsed, principal.userId(), correlationId(request));
        return repository.findById(TIPO, id, 1, 100)
                .map(detail -> ResponseEntity.ok(CatalogImportResponse.from(detail, 100)))
                .orElseThrow(() -> new IllegalStateException("La carga recien creada no esta disponible"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSTANCIAS_CARGAR')")
    public ResponseEntity<CatalogImportResponse> obtener(@PathVariable long id,
                                                         @RequestParam(defaultValue = "1") int pagina,
                                                         @RequestParam(defaultValue = "100") int tamano) {
        validarPaginacion(id, pagina, tamano);
        return repository.findById(TIPO, id, pagina, tamano)
                .map(detail -> ResponseEntity.ok(CatalogImportResponse.from(detail, tamano)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/errores")
    @PreAuthorize("hasAuthority('CONSTANCIAS_CARGAR')")
    public ResponseEntity<List<CatalogImportResponse.Error>> errores(@PathVariable long id,
                                                                      @RequestParam(defaultValue = "1") int pagina,
                                                                      @RequestParam(defaultValue = "100") int tamano) {
        validarPaginacion(id, pagina, tamano);
        return repository.findById(TIPO, id, 1, 1)
                .map(detail -> ResponseEntity.ok(repository.findErrors(TIPO, id, pagina, tamano).stream()
                        .map(error -> new CatalogImportResponse.Error(error.hoja(), error.fila(), error.columna(),
                                error.valorEnmascarado(), error.codigo(), error.mensaje()))
                        .toList()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES)
            throw new ConstanciaImportExceptionHandler.ArchivoInvalidoException("El archivo esta vacio o supera 10 MiB.");
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase(Locale.ROOT).endsWith(".xls") || name.toLowerCase(Locale.ROOT).endsWith(".xlsx")))
            throw new ConstanciaImportExceptionHandler.ArchivoInvalidoException("Solo se permiten archivos .xls y .xlsx.");
        try {
            byte[] bytes = file.getBytes();
            boolean xls = name.toLowerCase(Locale.ROOT).endsWith(".xls");
            boolean magic = xls ? bytes.length >= 4 && (bytes[0] & 255) == 0xD0 && (bytes[1] & 255) == 0xCF
                    && (bytes[2] & 255) == 0x11 && (bytes[3] & 255) == 0xE0
                    : bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
            if (!magic) throw new ConstanciaImportExceptionHandler.ArchivoInvalidoException("La firma no corresponde a la extension.");
            return bytes;
        } catch (IOException exception) {
            throw new ConstanciaImportExceptionHandler.ArchivoInvalidoException("No fue posible leer el archivo.");
        }
    }

    private void validarPaginacion(long id, int pagina, int tamano) {
        if (id < 1 || pagina < 1 || tamano < 1 || tamano > 100)
            throw new ConstanciaImportExceptionHandler.ArchivoInvalidoException("Los parametros de paginacion no son validos.");
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }

    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 no esta disponible", exception); }
    }
}
