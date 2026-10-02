package com.jovycandy.anexo24.reports.api.controller;

import com.jovycandy.anexo24.auditlog.api.dto.BitacoraRegistroDto;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.operations.entries.api.dto.EntradaLineaDto;
import com.jovycandy.anexo24.operations.exits.api.dto.SalidaLineaDto;
import com.jovycandy.anexo24.operations.usedmaterials.api.dto.MaterialUtilizadoDto;
import com.jovycandy.anexo24.reports.application.query.ConsultarReportesUseCase;
import com.jovycandy.anexo24.reports.extended.api.dto.AnalisisDescargaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.CompulsaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.LineaF4Dto;
import com.jovycandy.anexo24.reports.extended.api.dto.OperacionBloqueadaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.OperacionDirigidaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.RectificacionDto;
import com.jovycandy.anexo24.reports.extended.api.dto.VencimientoDto;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnalisisDescargasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesBloqueadasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarCompulsaUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarLineasF4UseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesDirigidasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarRectificacionesUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarVencimientosUseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.model.OperacionBloqueada;
import com.jovycandy.anexo24.reports.extended.domain.model.Compulsa;
import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
import com.jovycandy.anexo24.reports.extended.domain.model.Rectificacion;
import com.jovycandy.anexo24.reports.extended.domain.model.Vencimiento;
import com.jovycandy.anexo24.reports.infrastructure.export.ExportadorXlsxReportes;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/** Expone los reportes V1 derivados de las consultas read-only existentes. */
@RestController
@RequestMapping("/api/v1/reportes")
public class ReportesController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ConsultarReportesUseCase consultarReportesUseCase;
    private final ExportadorXlsxReportes exportadorXlsxReportes;
    private final ListarAnalisisDescargasUseCase listarAnalisisDescargasUseCase;
    private final ListarOperacionesBloqueadasUseCase listarOperacionesBloqueadasUseCase;
    private final ListarCompulsaUseCase listarCompulsaUseCase;
    private final ListarOperacionesDirigidasUseCase listarOperacionesDirigidasUseCase;
    private final ListarRectificacionesUseCase listarRectificacionesUseCase;
    private final ListarVencimientosUseCase listarVencimientosUseCase;
    private final ListarLineasF4UseCase listarLineasF4UseCase;

    public ReportesController(ConsultarReportesUseCase consultarReportesUseCase,
            ExportadorXlsxReportes exportadorXlsxReportes,
            ListarAnalisisDescargasUseCase listarAnalisisDescargasUseCase,
            ListarOperacionesBloqueadasUseCase listarOperacionesBloqueadasUseCase,
            ListarCompulsaUseCase listarCompulsaUseCase,
            ListarOperacionesDirigidasUseCase listarOperacionesDirigidasUseCase,
            ListarRectificacionesUseCase listarRectificacionesUseCase,
            ListarVencimientosUseCase listarVencimientosUseCase,
            ListarLineasF4UseCase listarLineasF4UseCase) {
        this.consultarReportesUseCase = consultarReportesUseCase;
        this.exportadorXlsxReportes = exportadorXlsxReportes;
        this.listarAnalisisDescargasUseCase = listarAnalisisDescargasUseCase;
        this.listarOperacionesBloqueadasUseCase = listarOperacionesBloqueadasUseCase;
        this.listarCompulsaUseCase = listarCompulsaUseCase;
        this.listarOperacionesDirigidasUseCase = listarOperacionesDirigidasUseCase;
        this.listarRectificacionesUseCase = listarRectificacionesUseCase;
        this.listarVencimientosUseCase = listarVencimientosUseCase;
        this.listarLineasF4UseCase = listarLineasF4UseCase;
    }

    @GetMapping("/entradas")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<EntradaLineaDto>> entradas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String pedimento, @RequestParam(required = false) String clavePedimento,
            @RequestParam(required = false) String fraccion, @RequestParam(required = false) String numeroParte,
            @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(consultarReportesUseCase.entradas(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano));
    }

    @GetMapping("/salidas")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<SalidaLineaDto>> salidas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String pedimento, @RequestParam(required = false) String clavePedimento,
            @RequestParam(required = false) String fraccion, @RequestParam(required = false) String numeroParte,
            @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(consultarReportesUseCase.salidas(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte, pagina, tamano));
    }

    @GetMapping("/materiales-utilizados")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<MaterialUtilizadoDto>> materialesUtilizados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String material, @RequestParam(required = false) String producto,
            @RequestParam(required = false) String pedimentoSalida,
            @RequestParam(required = false) String clavePedimentoSalida,
            @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(consultarReportesUseCase.materialesUtilizados(
                desde, hasta, material, producto, pedimentoSalida, clavePedimentoSalida, pagina, tamano));
    }

    @GetMapping("/bitacora")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<BitacoraRegistroDto>> bitacora(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) BitacoraModulo modulo,
            @RequestParam(required = false) BitacoraResultado resultado,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(consultarReportesUseCase.bitacora(
                aInstant(desde), aInstant(hasta), usuarioId, modulo, resultado, correlationId, pagina, tamano));
    }

    @GetMapping("/analisis-descargas")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<AnalisisDescargaDto>> analisisDescargas(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<AnalisisDescarga> resultado = listarAnalisisDescargasUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(AnalisisDescargaDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/operaciones-bloqueadas")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<OperacionBloqueadaDto>> operacionesBloqueadas(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<OperacionBloqueada> resultado = listarOperacionesBloqueadasUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(OperacionBloqueadaDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/compulsa")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<CompulsaDto>> compulsa(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Compulsa> resultado = listarCompulsaUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(CompulsaDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/dirigidos")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<OperacionDirigidaDto>> dirigidos(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<OperacionDirigida> resultado = listarOperacionesDirigidasUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(OperacionDirigidaDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/rectificaciones")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<RectificacionDto>> rectificaciones(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Rectificacion> resultado = listarRectificacionesUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(RectificacionDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/vencimientos")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<VencimientoDto>> vencimientos(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Vencimiento> resultado = listarVencimientosUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(VencimientoDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/f4")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<LineaF4Dto>> f4(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<LineaF4> resultado = listarLineasF4UseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(LineaF4Dto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/entradas/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarEntradas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String pedimento, @RequestParam(required = false) String clavePedimento,
            @RequestParam(required = false) String fraccion, @RequestParam(required = false) String numeroParte) {
        List<EntradaLineaDto> resultado = consultarReportesUseCase.exportarEntradas(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte);
        return archivo("entradas", List.of("Importación", "Partida", "Pedimento", "Clave pedimento", "Fecha entrada", "Fracción", "Unidad comercial", "Cantidad comercial", "Número parte", "Fecha pago"),
                resultado.stream().map(item -> fila(item.importacionId(), item.partidaId(), item.pedimento(), item.clavePedimento(), item.fechaEntrada(), item.fraccion(), item.unidadComercial(), item.cantidadComercial(), item.numeroParte(), item.fechaPago())).toList());
    }

    @GetMapping("/salidas/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarSalidas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String pedimento, @RequestParam(required = false) String clavePedimento,
            @RequestParam(required = false) String fraccion, @RequestParam(required = false) String numeroParte) {
        List<SalidaLineaDto> resultado = consultarReportesUseCase.exportarSalidas(
                desde, hasta, pedimento, clavePedimento, fraccion, numeroParte);
        return archivo("salidas", List.of("Salida", "Partida", "Pedimento", "Clave pedimento", "Fracción", "Unidad comercial", "Cantidad", "Número parte", "Fecha pago"),
                resultado.stream().map(item -> fila(item.salidaId(), item.partidaId(), item.pedimento(), item.clavePedimento(), item.fraccion(), item.unidadComercial(), item.cantidad(), item.numeroParte(), item.fechaPago())).toList());
    }

    @GetMapping("/materiales-utilizados/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarMaterialesUtilizados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String material, @RequestParam(required = false) String producto,
            @RequestParam(required = false) String pedimentoSalida, @RequestParam(required = false) String clavePedimentoSalida) {
        List<MaterialUtilizadoDto> resultado = consultarReportesUseCase.exportarMaterialesUtilizados(
                desde, hasta, material, producto, pedimentoSalida, clavePedimentoSalida);
        return archivo("materiales-utilizados", List.of("Descarga", "Entrada", "Partida entrada", "Salida", "Partida salida", "Pedimento entrada", "Pedimento salida", "Material", "Descripción material", "Producto", "Descripción producto", "Cantidad incorporada", "Merma", "Desperdicio", "Total descargado", "Unidad", "Fecha"),
                resultado.stream().map(item -> fila(item.descargaId(), item.entradaId(), item.partidaEntradaId(), item.salidaId(), item.partidaSalidaId(), item.pedimentoEntrada(), item.pedimentoSalida(), item.materialCode(), item.materialDescription(), item.productCode(), item.productDescription(), item.cantidadIncorporada(), item.cantidadMerma(), item.cantidadDesperdicio(), item.cantidadTotalDescargada(), item.unidad(), item.fecha())).toList());
    }

    @GetMapping("/bitacora/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarBitacora(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) BitacoraModulo modulo,
            @RequestParam(required = false) BitacoraResultado resultado, @RequestParam(required = false) String correlationId) {
        List<BitacoraRegistroDto> registros = consultarReportesUseCase.exportarBitacora(
                aInstant(desde), aInstant(hasta), usuarioId, modulo, resultado, correlationId);
        return archivo("bitacora", List.of("ID", "Fecha", "Usuario ID", "Usuario", "Módulo", "Acción", "Detalle", "Resultado", "Correlation ID"),
                registros.stream().map(item -> fila(item.id(), item.fecha(), item.usuarioId(), item.usuario(), item.modulo(), item.accion(), item.detalle(), item.resultado(), item.correlationId())).toList());
    }

    @GetMapping("/f4/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarF4(@RequestParam(required = false) String filtro) {
        List<LineaF4> resultado = listarLineasF4UseCase.exportar(filtro);
        return archivo("f4", List.of("Tipo descarga", "F4", "Fecha", "Importación", "Clave", "Incorporado", "Saldo"),
                resultado.stream().map(item -> fila(item.tipoDescarga(), item.f4(), item.fecha(), item.importacion(), item.clave(), item.incorporado(), item.saldo())).toList());
    }

    private ResponseEntity<byte[]> archivo(String nombre, List<String> encabezados, List<List<Object>> filas) {
        if (filas.isEmpty()) return ResponseEntity.noContent().build();
        byte[] contenido = exportadorXlsxReportes.exportar(nombre, encabezados, filas);
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre + ".xlsx").build().toString())
                .body(contenido);
    }

    private List<Object> fila(Object... valores) {
        return Arrays.asList(valores);
    }

    private Instant aInstant(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
