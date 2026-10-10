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
import com.jovycandy.anexo24.reports.extended.api.dto.CompulsaDetalleDto;
import com.jovycandy.anexo24.reports.extended.api.dto.LineaF4Dto;
import com.jovycandy.anexo24.reports.extended.api.dto.OperacionBloqueadaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.OperacionDirigidaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.RectificacionDto;
import com.jovycandy.anexo24.reports.extended.api.dto.RectificacionDetalleDto;
import com.jovycandy.anexo24.reports.extended.api.dto.Anexo30EntradaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.Anexo30FraccionDto;
import com.jovycandy.anexo24.reports.extended.api.dto.Anexo30DescargaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.Anexo30ComparativaDto;
import com.jovycandy.anexo24.reports.extended.api.dto.Anexo30InventarioInicialDto;
import com.jovycandy.anexo24.reports.extended.api.dto.SaldoDto;
import com.jovycandy.anexo24.reports.extended.api.dto.VencimientoDto;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnalisisDescargasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesBloqueadasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarCompulsaUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarCompulsaDetalleUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarLineasF4UseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesDirigidasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarRectificacionesUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarRectificacionesDetalleUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30EntradasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30FraccionesUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30DescargasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30ComparativaUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnexo30InventarioInicialUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarSaldosUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarVencimientosUseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.model.OperacionBloqueada;
import com.jovycandy.anexo24.reports.extended.domain.model.Compulsa;
import com.jovycandy.anexo24.reports.extended.domain.model.CompulsaDetalle;
import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
import com.jovycandy.anexo24.reports.extended.domain.model.Rectificacion;
import com.jovycandy.anexo24.reports.extended.domain.model.RectificacionDetalle;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Entrada;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Fraccion;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Descarga;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Comparativa;
import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30InventarioInicial;
import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;
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
    private final ListarCompulsaDetalleUseCase listarCompulsaDetalleUseCase;
    private final ListarOperacionesDirigidasUseCase listarOperacionesDirigidasUseCase;
    private final ListarRectificacionesUseCase listarRectificacionesUseCase;
    private final ListarRectificacionesDetalleUseCase listarRectificacionesDetalleUseCase;
    private final ListarVencimientosUseCase listarVencimientosUseCase;
    private final ListarLineasF4UseCase listarLineasF4UseCase;
    private final ListarAnexo30EntradasUseCase listarAnexo30EntradasUseCase;
    private final ListarAnexo30FraccionesUseCase listarAnexo30FraccionesUseCase;
    private final ListarAnexo30DescargasUseCase listarAnexo30DescargasUseCase;
    private final ListarAnexo30ComparativaUseCase listarAnexo30ComparativaUseCase;
    private final ListarAnexo30InventarioInicialUseCase listarAnexo30InventarioInicialUseCase;
    private final ListarSaldosUseCase listarSaldosUseCase;

    public ReportesController(ConsultarReportesUseCase consultarReportesUseCase,
            ExportadorXlsxReportes exportadorXlsxReportes,
            ListarAnalisisDescargasUseCase listarAnalisisDescargasUseCase,
            ListarOperacionesBloqueadasUseCase listarOperacionesBloqueadasUseCase,
            ListarCompulsaUseCase listarCompulsaUseCase,
            ListarCompulsaDetalleUseCase listarCompulsaDetalleUseCase,
            ListarOperacionesDirigidasUseCase listarOperacionesDirigidasUseCase,
            ListarRectificacionesUseCase listarRectificacionesUseCase,
            ListarRectificacionesDetalleUseCase listarRectificacionesDetalleUseCase,
            ListarVencimientosUseCase listarVencimientosUseCase,
            ListarLineasF4UseCase listarLineasF4UseCase,
            ListarAnexo30EntradasUseCase listarAnexo30EntradasUseCase,
            ListarAnexo30FraccionesUseCase listarAnexo30FraccionesUseCase,
            ListarAnexo30DescargasUseCase listarAnexo30DescargasUseCase,
            ListarAnexo30ComparativaUseCase listarAnexo30ComparativaUseCase,
            ListarAnexo30InventarioInicialUseCase listarAnexo30InventarioInicialUseCase,
            ListarSaldosUseCase listarSaldosUseCase) {
        this.consultarReportesUseCase = consultarReportesUseCase;
        this.exportadorXlsxReportes = exportadorXlsxReportes;
        this.listarAnalisisDescargasUseCase = listarAnalisisDescargasUseCase;
        this.listarOperacionesBloqueadasUseCase = listarOperacionesBloqueadasUseCase;
        this.listarCompulsaUseCase = listarCompulsaUseCase;
        this.listarCompulsaDetalleUseCase = listarCompulsaDetalleUseCase;
        this.listarOperacionesDirigidasUseCase = listarOperacionesDirigidasUseCase;
        this.listarRectificacionesUseCase = listarRectificacionesUseCase;
        this.listarRectificacionesDetalleUseCase = listarRectificacionesDetalleUseCase;
        this.listarVencimientosUseCase = listarVencimientosUseCase;
        this.listarLineasF4UseCase = listarLineasF4UseCase;
        this.listarAnexo30EntradasUseCase = listarAnexo30EntradasUseCase;
        this.listarAnexo30FraccionesUseCase = listarAnexo30FraccionesUseCase;
        this.listarAnexo30DescargasUseCase = listarAnexo30DescargasUseCase;
        this.listarAnexo30ComparativaUseCase = listarAnexo30ComparativaUseCase;
        this.listarAnexo30InventarioInicialUseCase = listarAnexo30InventarioInicialUseCase;
        this.listarSaldosUseCase = listarSaldosUseCase;
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

    @GetMapping("/analisis-descargas/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAnalisisDescargas(@RequestParam(required = false) String filtro) {
        List<AnalisisDescarga> resultado = listarAnalisisDescargasUseCase.exportar(filtro);
        return archivo("analisis-descargas", List.of("Descarga", "Pedimento entrada", "Partida entrada",
                "Pedimento salida", "Partida salida", "Material", "Producto", "Fecha importaciÃ³n",
                "Fecha salida", "Fecha vencimiento", "Cantidad importada", "Cantidad exportada",
                "Cantidad incorporada", "Merma", "Desperdicio", "Unidad"),
                resultado.stream().map(item -> fila(item.descargaId(), item.pedimentoEntrada(), item.partidaEntrada(),
                        item.pedimentoSalida(), item.partidaSalida(), item.material(), item.producto(),
                        item.fechaImportacion(), item.fechaSalida(), item.fechaVencimiento(), item.cantidadImportada(),
                        item.cantidadExportada(), item.cantidadIncorporada(), item.cantidadMerma(),
                        item.cantidadDesperdicio(), item.unidad())).toList());
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


    @GetMapping("/operaciones-bloqueadas/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarOperacionesBloqueadas(@RequestParam(required = false) String filtro) {
        List<OperacionBloqueada> resultado = listarOperacionesBloqueadasUseCase.exportar(filtro);
        return archivo("operaciones-bloqueadas", List.of("Fecha bloqueo", "Pedimento exportacion", "Clave exportacion", "Producto", "Cantidad producto", "Pedimento importacion", "Material", "Cantidad material", "Incorporado", "Desperdicio", "Merma", "Folio"),
                resultado.stream().map(item -> fila(item.fechaBloqueo(), item.pedimentoExportacion(), item.claveExportacion(), item.producto(), item.cantidadProducto(), item.pedimentoImportacion(), item.material(), item.cantidadMaterial(), item.incorporado(), item.desperdicio(), item.merma(), item.folio())).toList());
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

    @GetMapping("/compulsa/detalle")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<CompulsaDetalleDto>> compulsaDetalle(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<CompulsaDetalle> resultado = listarCompulsaDetalleUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(CompulsaDetalleDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }


    @GetMapping("/compulsa/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarCompulsa(@RequestParam(required = false) String filtro) {
        List<Compulsa> resultado = listarCompulsaUseCase.exportar(filtro);
        return archivo("compulsa", List.of("Pedimento Glosa", "Pedimento Anexo 24", "Fecha Glosa", "Fecha Anexo 24", "Clave Glosa", "Clave Anexo 24", "Fraccion Glosa", "Fraccion Anexo 24"),
                resultado.stream().map(item -> fila(item.pedimentoGlosa(), item.pedimentoAnexo24(), item.fechaGlosa(), item.fechaAnexo24(), item.claveGlosa(), item.claveAnexo24(), item.fraccionGlosa(), item.fraccionAnexo24())).toList());
    }

    @GetMapping("/compulsa/detalle/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarCompulsaDetalle(@RequestParam(required = false) String filtro) {
        List<CompulsaDetalle> resultado = listarCompulsaDetalleUseCase.exportar(filtro);
        return archivo("compulsa-detalle", List.of("Pedimento Glosa", "SEC Glosa", "Pedimento A24", "SEC A24", "Clave pedimento Glosa", "Clave pedimento A24", "Estado clave", "Fecha Glosa", "Fecha A24", "Estado fechas", "Fraccion Glosa", "Fraccion A24", "Estado fraccion", "Pais OD Glosa", "Pais OD A24", "Estado pais OD", "Pais CV Glosa", "Pais CV A24", "Estado pais CV", "Valor aduana Glosa", "Valor aduana A24", "Estado valor aduana", "Valor comercial Glosa", "Valor comercial A24", "Estado valor comercial", "Cantidad UMC Glosa", "Cantidad UMC A24", "Estado cantidad comercial", "Cantidad UMT Glosa", "Cantidad UMT A24", "Estado cantidad tarifa", "Tipo operacion Glosa", "Tipo pedimento Glosa"),
                resultado.stream().map(item -> fila(item.pedimentoGlosa(), item.secGlosa(), item.pedimentoA24(), item.secA24(), item.clavePedimentoGlosa(), item.clavePedimentoA24(), item.statusClavePedimento(), item.fechaGlosa(), item.fechaA24(), item.statusFechas(), item.fraccionGlosa(), item.fraccionA24(), item.statusFraccion(), item.paisOdGlosa(), item.paisOdA24(), item.statusPaisOd(), item.paisCvGlosa(), item.paisCvA24(), item.statusPaisCv(), item.valorAduanaGlosa(), item.valorAduanaA24(), item.statusValorAduana(), item.valorComercialGlosa(), item.valorComercialA24(), item.statusValorComercial(), item.cantidadUmcGlosa(), item.cantidadUmcA24(), item.statusCantidadComercial(), item.cantidadUmtGlosa(), item.cantidadUmtA24(), item.statusCantidadTarifa(), item.tipoOperacionGlosa(), item.tipoPedimentoGlosa())).toList());
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

    @GetMapping("/dirigidos/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarDirigidos(@RequestParam(required = false) String filtro) {
        List<OperacionDirigida> resultado = listarOperacionesDirigidasUseCase.exportar(filtro);
        return archivo("dirigidos", List.of("Salida", "Partida salida", "Documento", "Fecha salida",
                "Clave pedimento", "Secuencia", "Producto", "Cantidad", "Factura", "Descargo", "Dirigido",
                "Valor descarga dÃ³lares", "Valor descarga pesos", "Tiene estructura", "NÃºmero materiales"),
                resultado.stream().map(item -> fila(item.salidaKey(), item.psalidaKey(), item.documento(),
                        item.fechaSalida(), item.clavePedimento(), item.secuencia(), item.producto(), item.cantidad(),
                        item.factura(), item.descargo(), item.dirigido(), item.valorDescargaDolares(),
                        item.valorDescargaPesos(), item.tieneEstructura(), item.numeroMateriales())).toList());
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

    @GetMapping("/rectificaciones/detalle")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<RectificacionDetalleDto>> rectificacionesDetalle(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<RectificacionDetalle> resultado = listarRectificacionesDetalleUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(RectificacionDetalleDto::from).toList(),
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


    @GetMapping("/rectificaciones/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarRectificaciones(@RequestParam(required = false) String filtro) {
        List<Rectificacion> resultado = listarRectificacionesUseCase.exportar(filtro);
        return archivo("rectificaciones", List.of("Pedimento", "Rectificaciones relacionadas"), resultado.stream().map(item -> fila(item.pedimento(), item.total())).toList());
    }

    @GetMapping("/rectificaciones/detalle/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarRectificacionesDetalle(@RequestParam(required = false) String filtro) {
        List<RectificacionDetalle> resultado = listarRectificacionesDetalleUseCase.exportar(filtro);
        return archivo("rectificaciones-detalle", List.of("Pedimento", "Clave pedimento", "Descarga", "Pedimento original", "Existe pedimento", "Clave pedimento original", "Descarga original", "Estado"), resultado.stream().map(item -> fila(item.pedimento(), item.clavePedimento(), item.descarga(), item.pedimentoOriginal(), item.existePedimento(), item.clavePedimentoOriginal(), item.descargaOriginal(), item.status())).toList());
    }

    @GetMapping("/vencimientos/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarVencimientos(@RequestParam(required = false) String filtro) {
        List<Vencimiento> resultado = listarVencimientosUseCase.exportar(filtro);
        return archivo("vencimientos", List.of("Pedimento", "Fecha base", "Clave pedimento", "Clave", "Desperdicio", "Aplicado", "Factura", "Vencimiento"), resultado.stream().map(item -> fila(item.pedimento(), item.fechaBase(), item.clavePedimento(), item.clave(), item.desperdicio(), item.aplicado(), item.factura(), item.vencimiento())).toList());
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

    @GetMapping("/saldos")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<SaldoDto>> saldos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String documento,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Saldo> resultado = listarSaldosUseCase.ejecutar(desde, hasta, documento, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(SaldoDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/saldos/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarSaldos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String documento) {
        List<Saldo> resultado = listarSaldosUseCase.exportar(desde, hasta, documento);
        return archivo("saldos", List.of("Documento", "Fecha de Pago", "Clave Pedimento",
                "Tipo de Operacion", "tc", "Clave", "Descripcion", "Fraccion",
                "Cant. Importado", "Unidad", "Saldo", "Valor Aduanal de Saldo",
                "Valor dolares del saldo", "Pais origen", "Temporalidad(Meses)",
                "Categoria", "Fecha de Vencimiento", "PedimentoOriginal", "Descarga",
                "lote", "Complemento 1", "Complemento 2", "Complemento 3",
                "Desperdiciado", "Saldodesperdicio", "COVE", "Factura",
                "Tipo Material", "pu_vad", "pu_vdo", "val_aduanal", "val_dolares",
                "saldo en UMT", "unidadt", "valor en pesos", "Saldo en valor pesos", "NICO"),
                resultado.stream().map(item -> fila(item.documento(), item.fechaPago(), item.clavePedimento(),
                        item.tipoOperacion(), item.tcMonetaria(), item.clave(), item.descripcion(),
                        item.fraccion(), item.cantImportado(), item.unidad(), item.saldo(),
                        item.valorAduanalDeSaldo(), item.valorDolaresDelSaldo(), item.paisOrigen(),
                        item.temporalidadMeses(), item.categoria(), item.fechaVencimiento(),
                        item.pedimentoOriginal(), item.descarga(), item.lote(), item.complemento1(),
                        item.complemento2(), item.complemento3(), item.desperdiciado(), item.saldodesperdicio(),
                        item.cove(), item.factura(), item.tipoMaterial(), item.puVad(), item.puVdo(),
                        item.valAduanal(), item.valDolares(), item.saldoEnUMT(), item.unidadt(),
                        item.valorEnPesos(), item.saldoEnValorPesos(), item.nico())).toList());
    }

    @GetMapping("/anexo30-revision-entradas")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<Anexo30EntradaDto>> anexo30RevisionEntradas(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Anexo30Entrada> resultado = listarAnexo30EntradasUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(Anexo30EntradaDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/anexo30-revision-fracciones")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<Anexo30FraccionDto>> anexo30RevisionFracciones(@RequestParam(required = false) String filtro, @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Anexo30Fraccion> resultado = listarAnexo30FraccionesUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(Anexo30FraccionDto::from).toList(), resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/anexo30-revision-descargas")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<Anexo30DescargaDto>> anexo30RevisionDescargas(@RequestParam(required = false) String filtro, @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Anexo30Descarga> resultado = listarAnexo30DescargasUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(Anexo30DescargaDto::from).toList(), resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/anexo30-revision-comparativa")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<Anexo30ComparativaDto>> anexo30RevisionComparativa(@RequestParam(required = false) String filtro, @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Anexo30Comparativa> resultado = listarAnexo30ComparativaUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(Anexo30ComparativaDto::from).toList(), resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/anexo30-revision-inventario-inicial")
    @PreAuthorize("hasAuthority('REPORTES_GENERAR')")
    public ResponseEntity<Pagina<Anexo30InventarioInicialDto>> anexo30RevisionInventarioInicial(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pagina<Anexo30InventarioInicial> resultado = listarAnexo30InventarioInicialUseCase.ejecutar(filtro, pagina, tamano);
        return ResponseEntity.ok(new Pagina<>(resultado.items().stream().map(Anexo30InventarioInicialDto::from).toList(),
                resultado.total(), resultado.pagina(), resultado.tamano()));
    }

    @GetMapping("/anexo30-revision-inventario-inicial/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAnexo30RevisionInventarioInicial(@RequestParam(required = false) String filtro) {
        List<Anexo30InventarioInicial> resultado = listarAnexo30InventarioInicialUseCase.exportar(filtro);
        return archivo("anexo30-revision-inventario-inicial",
                List.of("Patente", "Número de pedimento", "Clave sección aduanera", "Fecha de selección",
                        "Fracción arancelaria", "Valor comercial histórico", "Activo fijo"),
                resultado.stream().map(item -> fila(item.patente(), item.numeroPedimento(), item.claveSeccionAduanera(),
                        item.fechaSeleccion(), item.fraccion(), item.valorComercialHistorico(), item.identificadorActivoFijo())).toList());
    }

    @GetMapping("/anexo30-revision-entradas/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAnexo30RevisionEntradas(@RequestParam(required = false) String filtro) {
        List<Anexo30Entrada> resultado = listarAnexo30EntradasUseCase.exportar(filtro);
        return archivo("anexo30-revision-entradas", List.of("Entrada", "Descarga", "Tipo operacion", "Pedimento",
                "Pedimento original", "Fecha", "Fecha original", "Clave pedimento", "Fraccion",
                "Valor comercial", "IVA FP21", "IVA FP22", "Saldo", "Operacion", "Partida", "ESAF"),
                resultado.stream().map(item -> fila(item.entradaKey(), item.descarga(), item.tipoOperacion(),
                        item.pedimento(), item.pedimentoOriginal(), item.fecha(), item.fechaOriginal(),
                        item.clavePedimento(), item.fraccion(), item.valorComercial(), item.ivaFp21(),
                        item.ivaFp22(), item.saldo(), item.operacion(), item.partida(), item.esaf())).toList());
    }

    @GetMapping("/anexo30-revision-fracciones/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAnexo30RevisionFracciones(@RequestParam(required = false) String filtro) {
        List<Anexo30Fraccion> resultado = listarAnexo30FraccionesUseCase.exportar(filtro);
        return archivo("anexo30-revision-fracciones", List.of("Fraccion key", "Tipo", "Clave pedimento",
                "Ejercicio", "Periodo", "Fraccion", "Valor", "AF", "Archivo"),
                resultado.stream().map(item -> fila(item.fraccionKey(), item.tipo(), item.clavePedimento(),
                        item.ejercicio(), item.periodo(), item.fraccion(), item.valor(), item.af(),
                        item.archivo())).toList());
    }

    @GetMapping("/anexo30-revision-descargas/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAnexo30RevisionDescargas(@RequestParam(required = false) String filtro) {
        List<Anexo30Descarga> resultado = listarAnexo30DescargasUseCase.exportar(filtro);
        return archivo("anexo30-revision-descargas", List.of("Descarga key", "Entrada key", "Fraccion key",
                "Pedimento", "Pedimento original", "Fecha entrada", "Clave pedimento entrada",
                "Fraccion entrada", "Valor comercial entrada", "Saldo persistido A31", "ESAF", "Partida",
                "Fraccion descarga", "Valor descargado", "Tipo A31", "Clave pedimento A31", "Ejercicio",
                "Periodo", "Fraccion A31", "Valor A31", "AF", "Archivo"),
                resultado.stream().map(item -> fila(item.descargaKey(), item.entradaKey(), item.fraccionKey(),
                        item.pedimento(), item.pedimentoOriginal(), item.fechaEntrada(), item.clavePedimentoEntrada(),
                        item.fraccionEntrada(), item.valorComercialEntrada(), item.saldoPersistidoA31(), item.esaf(),
                        item.partida(), item.fraccionDescarga(), item.valorDescargado(), item.tipoA31(),
                        item.clavePedimentoA31(), item.ejercicio(), item.periodo(), item.fraccionA31(),
                        item.valorA31(), item.af(), item.archivo())).toList());
    }

    @GetMapping("/anexo30-revision-comparativa/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAnexo30RevisionComparativa(@RequestParam(required = false) String filtro) {
        List<Anexo30Comparativa> resultado = listarAnexo30ComparativaUseCase.exportar(filtro);
        return archivo("anexo30-revision-comparativa", List.of("Clave pedimento", "Ejercicio", "Periodo",
                "Fraccion", "Valor A31", "Valor A24", "Diferencia", "IVA21 total", "IVA22 total",
                "Valor total", "IVA descargado A31", "IVA descargado A24"),
                resultado.stream().map(item -> fila(item.clavePedimento(), item.ejercicio(), item.periodo(),
                        item.fraccion(), item.valorA31(), item.valorA24(), item.diferencia(), item.iva21Total(),
                        item.iva22Total(), item.valorTotal(), item.ivaDescargadoA31(),
                        item.ivaDescargadoA24())).toList());
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
        return archivo("entradas", List.of("ImportaciÃ³n", "Partida", "Pedimento", "Clave pedimento", "Fecha entrada", "FracciÃ³n", "Unidad comercial", "Cantidad comercial", "NÃºmero parte", "Fecha pago"),
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
        return archivo("salidas", List.of("Salida", "Partida", "Pedimento", "Clave pedimento", "FracciÃ³n", "Unidad comercial", "Cantidad", "NÃºmero parte", "Fecha pago"),
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
        return archivo("materiales-utilizados", List.of("Descarga", "Entrada", "Partida entrada", "Salida", "Partida salida", "Pedimento entrada", "Pedimento salida", "Material", "DescripciÃ³n material", "Producto", "DescripciÃ³n producto", "Cantidad incorporada", "Merma", "Desperdicio", "Total descargado", "Unidad", "Fecha"),
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
        return archivo("bitacora", List.of("ID", "Fecha", "Usuario ID", "Usuario", "MÃ³dulo", "AcciÃ³n", "Detalle", "Resultado", "Correlation ID"),
                registros.stream().map(item -> fila(item.id(), item.fecha(), item.usuarioId(), item.usuario(), item.modulo(), item.accion(), item.detalle(), item.resultado(), item.correlationId())).toList());
    }

    @GetMapping("/f4/exportacion")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarF4(@RequestParam(required = false) String filtro) {
        List<LineaF4> resultado = listarLineasF4UseCase.exportar(filtro);
        return archivo("f4", List.of("Tipo descarga", "F4", "Fecha", "ImportaciÃ³n", "Clave", "Incorporado", "Saldo"),
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
