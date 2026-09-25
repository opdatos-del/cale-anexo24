package com.jovycandy.anexo24.billing.api.controller;

import com.jovycandy.anexo24.billing.api.dto.CargaFacturacionDetalleResponse;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturacion/cargas")
public class CargaFacturacionDetalleController {
    private final CargaFacturacionRepository repository;

    public CargaFacturacionDetalleController(CargaFacturacionRepository repository) { this.repository = repository; }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURACION_CARGAR')")
    public ResponseEntity<CargaFacturacionDetalleResponse> obtener(@PathVariable long id,
                                                                    @RequestParam(defaultValue = "1") int pagina,
                                                                    @RequestParam(defaultValue = "100") int tamano) {
        if (id <= 0 || pagina < 1 || tamano < 1 || tamano > 100)
            throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("Los parámetros de paginación no son válidos.");
        return repository.findById(id, pagina, tamano)
                .map(detail -> ResponseEntity.ok(CargaFacturacionDetalleResponse.from(detail, pagina, tamano)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
