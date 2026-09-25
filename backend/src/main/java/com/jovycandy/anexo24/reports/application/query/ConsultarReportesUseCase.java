package com.jovycandy.anexo24.reports.application.query;

import com.jovycandy.anexo24.auditlog.api.dto.BitacoraRegistroDto;
import com.jovycandy.anexo24.auditlog.application.query.ListarBitacoraUseCase;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.operations.entries.api.dto.EntradaLineaDto;
import com.jovycandy.anexo24.operations.entries.application.query.ListarEntradasUseCase;
import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
import com.jovycandy.anexo24.operations.exits.api.dto.SalidaLineaDto;
import com.jovycandy.anexo24.operations.exits.application.query.ListarSalidasUseCase;
import com.jovycandy.anexo24.operations.exits.domain.model.SalidaLinea;
import com.jovycandy.anexo24.operations.usedmaterials.api.dto.MaterialUtilizadoDto;
import com.jovycandy.anexo24.operations.usedmaterials.application.query.ListarMaterialesUtilizadosUseCase;
import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** Caso de uso que compone los reportes a partir de consultas existentes. */
@Service
public class ConsultarReportesUseCase {

    static final int TAMANO_PAGINA_EXPORTACION = 100;
    static final int MAXIMO_FILAS_EXPORTACION = 10_000;

    private final ListarEntradasUseCase listarEntradasUseCase;
    private final ListarSalidasUseCase listarSalidasUseCase;
    private final ListarMaterialesUtilizadosUseCase listarMaterialesUtilizadosUseCase;
    private final ListarBitacoraUseCase listarBitacoraUseCase;

    public ConsultarReportesUseCase(ListarEntradasUseCase listarEntradasUseCase,
            ListarSalidasUseCase listarSalidasUseCase,
            ListarMaterialesUtilizadosUseCase listarMaterialesUtilizadosUseCase,
            ListarBitacoraUseCase listarBitacoraUseCase) {
        this.listarEntradasUseCase = listarEntradasUseCase;
        this.listarSalidasUseCase = listarSalidasUseCase;
        this.listarMaterialesUtilizadosUseCase = listarMaterialesUtilizadosUseCase;
        this.listarBitacoraUseCase = listarBitacoraUseCase;
    }

    public Pagina<EntradaLineaDto> entradas(LocalDate desde, LocalDate hasta, String pedimento,
            String clavePedimento, String fraccion, String numeroParte, int pagina, int tamano) {
        Pagina<EntradaLinea> resultado = listarEntradasUseCase.ejecutar(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano);
        return convertir(resultado, EntradaLineaDto::from);
    }

    public Pagina<SalidaLineaDto> salidas(LocalDate desde, LocalDate hasta, String pedimento,
            String clavePedimento, String fraccion, String numeroParte, int pagina, int tamano) {
        Pagina<SalidaLinea> resultado = listarSalidasUseCase.ejecutar(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano);
        return convertir(resultado, SalidaLineaDto::from);
    }

    public Pagina<MaterialUtilizadoDto> materialesUtilizados(LocalDate desde, LocalDate hasta,
            String material, String producto, String pedimentoSalida, String clavePedimentoSalida,
            int pagina, int tamano) {
        Pagina<MaterialUtilizado> resultado = listarMaterialesUtilizadosUseCase.ejecutar(
                desde, hasta, material, producto, pedimentoSalida, clavePedimentoSalida, pagina, tamano);
        return convertir(resultado, MaterialUtilizadoDto::from);
    }

    public Pagina<BitacoraRegistroDto> bitacora(Instant desde, Instant hasta, Long usuarioId,
            BitacoraModulo modulo, BitacoraResultado resultado, String correlationId, int pagina, int tamano) {
        Pagina<BitacoraRegistro> registros = listarBitacoraUseCase.ejecutar(
                desde, hasta, usuarioId, modulo, resultado, correlationId, pagina, tamano);
        return convertir(registros, BitacoraRegistroDto::from);
    }

    /** Obtiene todas las entradas filtradas aptas para exportación. */
    public List<EntradaLineaDto> exportarEntradas(LocalDate desde, LocalDate hasta, String pedimento,
            String clavePedimento, String fraccion, String numeroParte) {
        return recolectar(pagina -> entradas(desde, hasta, pedimento, clavePedimento, fraccion,
                numeroParte, pagina, TAMANO_PAGINA_EXPORTACION));
    }

    /** Obtiene todas las salidas filtradas aptas para exportación. */
    public List<SalidaLineaDto> exportarSalidas(LocalDate desde, LocalDate hasta, String pedimento,
            String clavePedimento, String fraccion, String numeroParte) {
        return recolectar(pagina -> salidas(desde, hasta, pedimento, clavePedimento, fraccion,
                numeroParte, pagina, TAMANO_PAGINA_EXPORTACION));
    }

    /** Obtiene todos los materiales utilizados filtrados aptos para exportación. */
    public List<MaterialUtilizadoDto> exportarMaterialesUtilizados(LocalDate desde, LocalDate hasta,
            String material, String producto, String pedimentoSalida, String clavePedimentoSalida) {
        return recolectar(pagina -> materialesUtilizados(desde, hasta, material, producto,
                pedimentoSalida, clavePedimentoSalida, pagina, TAMANO_PAGINA_EXPORTACION));
    }

    /** Obtiene todos los eventos de bitácora filtrados aptos para exportación. */
    public List<BitacoraRegistroDto> exportarBitacora(Instant desde, Instant hasta, Long usuarioId,
            BitacoraModulo modulo, BitacoraResultado resultado, String correlationId) {
        return recolectar(pagina -> bitacora(desde, hasta, usuarioId, modulo, resultado,
                correlationId, pagina, TAMANO_PAGINA_EXPORTACION));
    }

    private <T> List<T> recolectar(IntFunction<Pagina<T>> consultarPagina) {
        Pagina<T> primeraPagina = consultarPagina.apply(1);
        if (primeraPagina.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportación excede el máximo de "
                    + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primeraPagina.total() == 0) return List.of();

        List<T> resultados = new ArrayList<>((int) primeraPagina.total());
        resultados.addAll(primeraPagina.items());
        for (int pagina = 2; pagina <= primeraPagina.totalPaginas(); pagina++) {
            resultados.addAll(consultarPagina.apply(pagina).items());
        }
        return resultados;
    }

    private <T, R> Pagina<R> convertir(Pagina<T> pagina, java.util.function.Function<T, R> mapper) {
        return new Pagina<>(pagina.items().stream().map(mapper).toList(),
                pagina.total(), pagina.pagina(), pagina.tamano());
    }
}
