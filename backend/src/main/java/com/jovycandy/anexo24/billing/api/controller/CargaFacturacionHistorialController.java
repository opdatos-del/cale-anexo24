package com.jovycandy.anexo24.billing.api.controller;

import com.jovycandy.anexo24.billing.api.dto.CargaFacturacionResumenDto;
import com.jovycandy.anexo24.billing.domain.model.EstadoCargaFacturacionPersistida;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/facturacion/cargas")
public class CargaFacturacionHistorialController {
    private final CargaFacturacionRepository repository;

    public CargaFacturacionHistorialController(CargaFacturacionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FACTURACION_CARGAR')")
    public ResponseEntity<Pagina<CargaFacturacionResumenDto>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (pagina < 1 || tamano < 1 || tamano > 100 || ((desde == null) != (hasta == null))
                || (desde != null && desde.isAfter(hasta))) {
            throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("Los criterios de historial no son válidos.");
        }
        EstadoCargaFacturacionPersistida filtro = null;
        if (estado != null && !estado.isBlank()) {
            try { filtro = EstadoCargaFacturacionPersistida.valueOf(estado.trim().toUpperCase()); }
            catch (IllegalArgumentException exception) {
                throw new BillingUploadExceptionHandler.BillingArchivoInvalidoException("El estado persistido no es válido.");
            }
        }
        var resultado = repository.buscar(principal.userId(), filtro, desde, hasta, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(CargaFacturacionResumenDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }
}
