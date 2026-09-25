package com.jovycandy.anexo24.billing.api.controller;

import com.jovycandy.anexo24.billing.api.dto.CargaFacturacionResponse;
import com.jovycandy.anexo24.billing.application.command.ExcelFacturacionParser;
import com.jovycandy.anexo24.billing.application.usecase.CargarFacturacionUseCase;
import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/facturacion/cargas")
public class CargaFacturacionController {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_BATCH_BYTES = 50L * 1024 * 1024;
    private static final int MAX_FILES = 5;
    private final ExcelFacturacionParser parser;
    private final CargarFacturacionUseCase useCase;

    public CargaFacturacionController(ExcelFacturacionParser parser, CargarFacturacionUseCase useCase) {
        this.parser = parser;
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FACTURACION_CARGAR')")
    public ResponseEntity<CargaFacturacionResponse> cargar(
            @RequestPart("archivos") List<MultipartFile> archivos,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest request) {
        if (archivos == null || archivos.isEmpty() || archivos.size() > MAX_FILES)
            throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("Debe enviar de 1 a 5 archivos.");
        long totalSize = archivos.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > MAX_BATCH_BYTES) throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("El lote supera el límite total de 50 MiB.");

        List<byte[]> contents = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        for (MultipartFile file : archivos) {
            byte[] bytes = read(file);
            validate(file, bytes);
            contents.add(bytes);
            hashes.add(sha256(bytes));
        }
        Object correlation = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        String correlationId = correlation == null ? UUID.randomUUID().toString() : correlation.toString();
        List<ArchivoFacturacion> parsedFiles = new ArrayList<>();
        for (int i = 0; i < archivos.size(); i++) {
            parsedFiles.add(parser.parsear(archivos.get(i).getOriginalFilename(), hashes.get(i), contents.get(i)));
        }
        List<Long> ids = useCase.ejecutarLote(parsedFiles, principal.userId(), correlationId);

        List<CargaFacturacionResponse.Carga> cargas = new ArrayList<>();
        for (int i = 0; i < archivos.size(); i++) {
            ArchivoFacturacion parsed = parsedFiles.get(i);
            long id = ids.get(i);
            int invalid = invalidRows(parsed);
            int valid = Math.max(0, parsed.filas() - invalid);
            String status = parsed.fallida() ? "FALLIDA" : parsed.errores().isEmpty() ? "VALIDADA" : "CON_ERRORES";
            if (parsed.fallida()) { valid = 0; invalid = parsed.filas(); }
            CargaFacturacionResponse.Preview preview = new CargaFacturacionResponse.Preview(
                    parsed.columnas(), parsed.preview().stream().map(fila -> filaComoMapa(parsed.columnas(), fila)).toList());
            List<CargaFacturacionResponse.Error> errors = parsed.errores().stream().map(error ->
                    new CargaFacturacionResponse.Error(error.hoja(), error.fila(), error.columna(),
                            error.valorEnmascarado(), error.codigo(), error.mensaje())).toList();
            cargas.add(new CargaFacturacionResponse.Carga(id, parsed.nombre(), parsed.hash(), status,
                    parsed.filas(), valid, invalid, preview, errors));
        }
        return ResponseEntity.ok(new CargaFacturacionResponse(cargas, correlationId,
                "FACTURACION_TEMPLATE_V1_PROVISIONAL", false));
    }

    private int invalidRows(ArchivoFacturacion archivo) {
        if (archivo.errores().stream().anyMatch(error -> error.fila() == null)) return archivo.filas();
        return (int) archivo.errores().stream()
                .map(error -> error.hoja() + "\u0000" + error.fila()).distinct().count();
    }

    private Map<String, String> filaComoMapa(List<String> columns, ArchivoFacturacion.Fila fila) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String value = i < fila.celdas().size() ? fila.celdas().get(i) : "";
            result.put(columns.get(i), value.isBlank() ? null : value);
        }
        return result;
    }

    private byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES)
            throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("El archivo está vacío o supera el límite de 10 MiB.");
        try {
            byte[] data = file.getBytes();
            if (data.length == 0 || data.length > MAX_FILE_BYTES)
                throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("El archivo excede el límite permitido.");
            return data;
        } catch (IOException e) {
            throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("No fue posible leer el archivo enviado.");
        }
    }

    private void validate(MultipartFile file, byte[] bytes) {
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase(Locale.ROOT).endsWith(".xls") || name.toLowerCase(Locale.ROOT).endsWith(".xlsx")))
            throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("Solo se permiten archivos .xls y .xlsx.");
        boolean xls = name.toLowerCase(Locale.ROOT).endsWith(".xls");
        String mime = file.getContentType();
        if (mime != null && !mime.isBlank()) {
            String normalizedMime = mime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            String expected = xls ? "application/vnd.ms-excel"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (!normalizedMime.equals(expected) && !normalizedMime.equals("application/octet-stream"))
                throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("El tipo MIME no corresponde a la extensión del archivo.");
        }
        boolean magic = xls ? bytes.length >= 8 && (bytes[0] & 255) == 0xD0 && (bytes[1] & 255) == 0xCF
                && (bytes[2] & 255) == 0x11 && (bytes[3] & 255) == 0xE0
                : bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
        if (!magic) throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("La firma del archivo no corresponde a su extensión.");
    }

    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 no está disponible", e); }
    }
}
