package com.jovycandy.anexo24.operations.entries.application.query;

import com.jovycandy.anexo24.operations.entries.domain.model.EntradaLinea;
import com.jovycandy.anexo24.operations.entries.domain.port.EntradaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Caso de uso de consulta paginada de líneas de Entradas. */
@Service
public class ListarEntradasUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;
    private static final int PEDIMENTO_MAXIMO = 20;
    private static final int CLAVE_PEDIMENTO_MAXIMA = 5;
    private static final int FRACCION_MAXIMA = 15;
    private static final int NUMERO_PARTE_MAXIMO = 50;

    private final EntradaRepository entradaRepository;

    /**
     * Constructor con el puerto de Entradas.
     *
     * @param entradaRepository puerto de consulta de Entradas
     */
    public ListarEntradasUseCase(EntradaRepository entradaRepository) {
        this.entradaRepository = entradaRepository;
    }

    /**
     * Consulta líneas de entrada con rango obligatorio y filtros acumulativos.
     *
     * @param desde fecha inicial inclusiva sobre la fecha de pago
     * @param hasta fecha final inclusiva sobre la fecha de pago
     * @param pedimento número de pedimento opcional
     * @param clavePedimento clave de pedimento opcional
     * @param fraccion fracción arancelaria opcional
     * @param numeroParte número de parte opcional
     * @param pagina número de página base 1
     * @param tamano cantidad solicitada de elementos
     * @return página de líneas de entrada
     */
    public Pagina<EntradaLinea> ejecutar(LocalDate desde, LocalDate hasta, String pedimento,
                                         String clavePedimento, String fraccion, String numeroParte,
                                         int pagina, int tamano) {
        validarRango(desde, hasta);
        validarPaginacion(pagina, tamano);

        String pedimentoNormalizado = normalizarTexto(pedimento);
        String clavePedimentoNormalizada = normalizarTexto(clavePedimento);
        String fraccionNormalizada = normalizarTexto(fraccion);
        String numeroParteNormalizado = normalizarTexto(numeroParte);

        validarLongitud(pedimentoNormalizado, PEDIMENTO_MAXIMO, "pedimento");
        validarLongitud(clavePedimentoNormalizada, CLAVE_PEDIMENTO_MAXIMA, "clavePedimento");
        validarLongitud(fraccionNormalizada, FRACCION_MAXIMA, "fraccion");
        validarLongitud(numeroParteNormalizado, NUMERO_PARTE_MAXIMO, "numeroParte");

        return entradaRepository.findPage(
                desde,
                hasta,
                pedimentoNormalizado,
                clavePedimentoNormalizada,
                fraccionNormalizada,
                numeroParteNormalizado,
                pagina,
                tamano);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new SolicitudInvalidaException("Los parámetros desde y hasta son obligatorios.");
        }
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("El parámetro desde no puede ser posterior a hasta.");
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
