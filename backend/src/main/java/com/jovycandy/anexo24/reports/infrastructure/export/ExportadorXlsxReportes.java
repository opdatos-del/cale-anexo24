package com.jovycandy.anexo24.reports.infrastructure.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.List;

/** Genera libros XLSX de reportes sin evaluar contenido proporcionado por usuarios. */
@Component
public class ExportadorXlsxReportes {

    /**
     * Genera un libro con encabezados y filas tabulares.
     *
     * @param nombreHoja nombre de la hoja
     * @param encabezados columnas del reporte
     * @param filas valores de las filas
     * @return contenido binario XLSX
     */
    public byte[] exportar(String nombreHoja, List<String> encabezados, List<List<Object>> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet(nombreHoja);
            var encabezado = hoja.createRow(0);
            for (int columna = 0; columna < encabezados.size(); columna++) {
                encabezado.createCell(columna).setCellValue(encabezados.get(columna));
            }
            for (int filaIndice = 0; filaIndice < filas.size(); filaIndice++) {
                var fila = hoja.createRow(filaIndice + 1);
                List<Object> valores = filas.get(filaIndice);
                for (int columna = 0; columna < valores.size(); columna++) {
                    escribirCelda(fila.createCell(columna), valores.get(columna));
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar el archivo XLSX.", exception);
        }
    }

    private void escribirCelda(Cell celda, Object valor) {
        if (valor == null) return;
        if (valor instanceof String texto) {
            celda.setCellValue(sanitizarFormula(texto));
        } else if (valor instanceof BigDecimal decimal) {
            celda.setCellValue(decimal.toPlainString());
        } else if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
        } else if (valor instanceof TemporalAccessor) {
            celda.setCellValue(valor.toString());
        } else if (valor instanceof Enum<?> enumeracion) {
            celda.setCellValue(enumeracion.name());
        } else {
            celda.setCellValue(sanitizarFormula(valor.toString()));
        }
    }

    private String sanitizarFormula(String valor) {
        if (valor.isEmpty()) return valor;
        char inicial = valor.charAt(0);
        return inicial == '=' || inicial == '+' || inicial == '-' || inicial == '@' ? "'" + valor : valor;
    }
}
