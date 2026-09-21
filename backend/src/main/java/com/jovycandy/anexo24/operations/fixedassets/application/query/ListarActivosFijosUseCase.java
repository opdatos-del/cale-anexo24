package com.jovycandy.anexo24.operations.fixedassets.application.query;

import com.jovycandy.anexo24.operations.fixedassets.domain.model.ActivoFijo;
import com.jovycandy.anexo24.operations.fixedassets.domain.port.ActivoFijoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Caso de uso de consulta paginada de partidas marcadas como activos fijos. */
@Service
public class ListarActivosFijosUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;
    private static final int PEDIMENTO_MAXIMO = 20;
    private static final int CLAVE_PEDIMENTO_MAXIMA = 5;
    private static final int NUMERO_PARTE_MAXIMO = 50;
    private static final int DESCRIPCION_MAXIMA = 250;
    private static final int SERIE_MAXIMA = 50;
    private static final int MARCA_MAXIMA = 50;
    private static final int MODELO_MAXIMO = 50;

    private final ActivoFijoRepository activoFijoRepository;

    /**
     * Constructor con el puerto de Activos Fijos.
     *
     * @param activoFijoRepository puerto de consulta de activos fijos
     */
    public ListarActivosFijosUseCase(ActivoFijoRepository activoFijoRepository) {
        this.activoFijoRepository = activoFijoRepository;
    }

    /**
     * Consulta activos fijos con rango opcional en pareja y filtros acumulativos.
     *
     * @param desde fecha inicial inclusiva de importación
     * @param hasta fecha final inclusiva de importación
     * @param pedimento número de pedimento opcional
     * @param clavePedimento clave de pedimento opcional
     * @param numeroParte número de parte opcional
     * @param descripcion descripción histórica opcional
     * @param serie número de serie histórico opcional
     * @param marca marca histórica opcional
     * @param modelo modelo histórico opcional
     * @param pagina número de página base 1
     * @param tamano cantidad solicitada de elementos
     * @return página de partidas activas
     */
    public Pagina<ActivoFijo> ejecutar(LocalDate desde, LocalDate hasta, String pedimento,
                                       String clavePedimento, String numeroParte, String descripcion,
                                       String serie, String marca, String modelo, int pagina, int tamano) {
        validarRango(desde, hasta);
        validarPaginacion(pagina, tamano);

        String pedimentoNormalizado = normalizarTexto(pedimento);
        String clavePedimentoNormalizada = normalizarTexto(clavePedimento);
        String numeroParteNormalizado = normalizarTexto(numeroParte);
        String descripcionNormalizada = normalizarTexto(descripcion);
        String serieNormalizada = normalizarTexto(serie);
        String marcaNormalizada = normalizarTexto(marca);
        String modeloNormalizado = normalizarTexto(modelo);

        validarLongitud(pedimentoNormalizado, PEDIMENTO_MAXIMO, "pedimento");
        validarLongitud(clavePedimentoNormalizada, CLAVE_PEDIMENTO_MAXIMA, "clavePedimento");
        validarLongitud(numeroParteNormalizado, NUMERO_PARTE_MAXIMO, "numeroParte");
        validarLongitud(descripcionNormalizada, DESCRIPCION_MAXIMA, "descripcion");
        validarLongitud(serieNormalizada, SERIE_MAXIMA, "serie");
        validarLongitud(marcaNormalizada, MARCA_MAXIMA, "marca");
        validarLongitud(modeloNormalizado, MODELO_MAXIMO, "modelo");

        return activoFijoRepository.findPage(
                desde, hasta, pedimentoNormalizado, clavePedimentoNormalizada,
                numeroParteNormalizado, descripcionNormalizada, serieNormalizada,
                marcaNormalizada, modeloNormalizado, pagina, tamano);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if ((desde == null) != (hasta == null)) {
            throw new SolicitudInvalidaException(
                    "Los parámetros desde y hasta deben informarse juntos.");
        }
        if (desde != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    "El parámetro desde no puede ser posterior a hasta.");
        }
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }

    private void validarLongitud(String valor, int maximo, String parametro) {
        if (valor != null && valor.length() > maximo) {
            throw new SolicitudInvalidaException(
                    "El parámetro " + parametro + " supera la longitud permitida.");
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
