package com.jovycandy.anexo24.operations.pediments.api;

import com.jovycandy.anexo24.operations.pediments.api.dto.CargaPedimentoResponse;
import com.jovycandy.anexo24.operations.pediments.api.dto.PedimentoErrorsResponse;
import com.jovycandy.anexo24.operations.pediments.application.command.ExcelPedimentoParser;
import com.jovycandy.anexo24.operations.pediments.application.usecase.CargarPedimentosUseCase;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.port.CargaPedimentoRepository;
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
import java.util.Locale;

/** API V1 de upload, validación y preview de staging de pedimentos. */
@RestController
@RequestMapping("/api/v1/operaciones/pedimentos")
public class PedimentoController {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private final ExcelPedimentoParser parser;
    private final CargarPedimentosUseCase useCase;
    private final CargaPedimentoRepository repository;

    public PedimentoController(ExcelPedimentoParser parser, CargarPedimentosUseCase useCase,
                               CargaPedimentoRepository repository) {
        this.parser = parser;
        this.useCase = useCase;
        this.repository = repository;
    }

    @PostMapping(value = "/cargas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PEDIMENTOS_CARGAR')")
    public ResponseEntity<CargaPedimentoResponse> cargar(
            @RequestPart("archivo") MultipartFile archivo,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest request) {
        byte[] bytes = readAndValidate(archivo);
        String correlationId = correlationId(request);
        CargaPedimentoArchivo parsed = parser.parsear(archivo.getOriginalFilename(), sha256(bytes), bytes);
        long id = useCase.ejecutar(parsed, principal.userId(), correlationId);
        CargaPedimentoResponse response = repository.findById(id, 1, 100)
                .map(detail -> CargaPedimentoResponse.from(detail, ExcelPedimentoParser.CAMPOS_CONFIRMADOS, 1, 100))
                .orElseThrow(() -> new IllegalStateException("La carga recién creada no está disponible"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cargas/{id}")
    @PreAuthorize("hasAuthority('PEDIMENTOS_CARGAR')")
    public ResponseEntity<CargaPedimentoResponse> obtener(@PathVariable long id,
                                                           @RequestParam(defaultValue = "1") int pagina,
                                                           @RequestParam(defaultValue = "100") int tamano) {
        validarPaginacion(id, pagina, tamano);
        return repository.findById(id, pagina, tamano)
                .map(detail -> ResponseEntity.ok(CargaPedimentoResponse.from(detail,
                        ExcelPedimentoParser.CAMPOS_CONFIRMADOS, pagina, tamano)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cargas/{id}/errores")
    @PreAuthorize("hasAuthority('PEDIMENTOS_CARGAR')")
    public ResponseEntity<PedimentoErrorsResponse> errores(@PathVariable long id,
                                                           @RequestParam(defaultValue = "1") int pagina,
                                                           @RequestParam(defaultValue = "100") int tamano) {
        validarPaginacion(id, pagina, tamano);
        return ResponseEntity.ok(PedimentoErrorsResponse.from(id, pagina, tamano,
                repository.findErrors(id, pagina, tamano)));
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES)
            throw new PedimentoUploadExceptionHandler.PedimentoArchivoInvalidoException("El archivo está vacío o supera el límite de 10 MiB.");
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase(Locale.ROOT).endsWith(".xls") || name.toLowerCase(Locale.ROOT).endsWith(".xlsx")))
            throw new PedimentoUploadExceptionHandler.PedimentoArchivoInvalidoException("Sólo se permiten archivos .xls y .xlsx.");
        try {
            byte[] bytes = file.getBytes();
            boolean xls = name.toLowerCase(Locale.ROOT).endsWith(".xls");
            boolean magic = xls ? bytes.length >= 4 && (bytes[0] & 255) == 0xD0 && (bytes[1] & 255) == 0xCF
                    && (bytes[2] & 255) == 0x11 && (bytes[3] & 255) == 0xE0
                    : bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
            if (!magic) throw new PedimentoUploadExceptionHandler.PedimentoArchivoInvalidoException("La firma del archivo no corresponde a su extensión.");
            return bytes;
        } catch (IOException exception) {
            throw new PedimentoUploadExceptionHandler.PedimentoArchivoInvalidoException("No fue posible leer el archivo enviado.");
        }
    }

    private void validarPaginacion(long id, int pagina, int tamano) {
        if (id < 1 || pagina < 1 || tamano < 1 || tamano > 100)
            throw new PedimentoUploadExceptionHandler.PedimentoArchivoInvalidoException("Los parámetros de paginación no son válidos.");
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return value == null ? java.util.UUID.randomUUID().toString() : value.toString();
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
