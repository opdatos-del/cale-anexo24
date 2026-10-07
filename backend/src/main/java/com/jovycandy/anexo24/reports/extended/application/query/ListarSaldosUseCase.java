package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;
import com.jovycandy.anexo24.reports.extended.domain.port.SaldoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista entradas del reporte legacy Saldos (LEGACY-038) sin ejecutar descargos ni
 * recalcular saldos.
 *
 * <p>Reglas de filtro demostradas en el SP:</p>
 * <ul>
 *   <li>DOCUMENT_OVERRIDES_DATE_RANGE = YES: si documento no es vacio, anula
 *   @DESDE/@HASTA.</li>
 *   <li>DATE_FILTER_COLUMN = Importaciones.Fecha, BETWEEN inclusivo.</li>
 *   <li>DOCUMENT_FILTER_COLUMN = Importaciones.Numero_ped, igualdad exacta.</li>
 * </ul>
 */
@Service
public class ListarSaldosUseCase {

    private static final int TAMANO_PAGINA_EXPORTACION = 100;
    private static final int MAXIMO_FILAS_EXPORTACION = 10_000;

    private final SaldoRepository repository;

    public ListarSaldosUseCase(SaldoRepository repository) {
        this.repository = repository;
    }

    public Pagina<Saldo> ejecutar(LocalDate desde, LocalDate hasta, String documento,
                                   int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de saldos no es válida.");
        }
        if (desde == null || hasta == null) {
            throw new SolicitudInvalidaException("El rango de fechas es obligatorio para saldos.");
        }
        return repository.findPage(desde, hasta, normalizarDocumento(documento), pagina, tamano);
    }

    /** Obtiene todas las filas filtradas aptas para exportación. */
    public List<Saldo> exportar(LocalDate desde, LocalDate hasta, String documento) {
        String docNormalizado = normalizarDocumento(documento);
        Pagina<Saldo> primeraPagina = repository.findPage(desde, hasta, docNormalizado, 1,
                TAMANO_PAGINA_EXPORTACION);
        if (primeraPagina.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportación excede el máximo de "
                    + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primeraPagina.total() == 0) return List.of();

        List<Saldo> resultados = new ArrayList<>((int) primeraPagina.total());
        resultados.addAll(primeraPagina.items());
        for (int pagina = 2; pagina <= primeraPagina.totalPaginas(); pagina++) {
            resultados.addAll(repository.findPage(desde, hasta, docNormalizado, pagina,
                    TAMANO_PAGINA_EXPORTACION).items());
        }
        return resultados;
    }

    private String normalizarDocumento(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
