package com.jovycandy.anexo24.operations.usedmaterials.application.query;

import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.operations.usedmaterials.domain.port.MaterialUtilizadoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Caso de uso de consulta paginada de materiales utilizados. */
@Service
public class ListarMaterialesUtilizadosUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;
    private static final int MATERIAL_MAXIMO = 50;
    private static final int PRODUCTO_MAXIMO = 50;
    private static final int PEDIMENTO_SALIDA_MAXIMO = 50;
    private static final int CLAVE_PEDIMENTO_SALIDA_MAXIMA = 5;

    private final MaterialUtilizadoRepository materialUtilizadoRepository;

    /**
     * Constructor con el puerto del histórico de materiales utilizados.
     *
     * @param materialUtilizadoRepository puerto de consulta histórica
     */
    public ListarMaterialesUtilizadosUseCase(MaterialUtilizadoRepository materialUtilizadoRepository) {
        this.materialUtilizadoRepository = materialUtilizadoRepository;
    }

    /**
     * Consulta materiales utilizados con rango obligatorio y filtros acumulativos.
     *
     * @param desde fecha inicial inclusiva de salida
     * @param hasta fecha final inclusiva de salida
     * @param material código de material opcional
     * @param producto código de producto opcional
     * @param pedimentoSalida documento de salida opcional
     * @param clavePedimentoSalida clave de pedimento de salida opcional
     * @param pagina número de página base 1
     * @param tamano cantidad solicitada de elementos
     * @return página de materiales utilizados
     */
    public Pagina<MaterialUtilizado> ejecutar(LocalDate desde, LocalDate hasta, String material,
                                              String producto, String pedimentoSalida,
                                              String clavePedimentoSalida, int pagina, int tamano) {
        validarRango(desde, hasta);
        validarPaginacion(pagina, tamano);

        String materialNormalizado = normalizarTexto(material);
        String productoNormalizado = normalizarTexto(producto);
        String pedimentoSalidaNormalizado = normalizarTexto(pedimentoSalida);
        String clavePedimentoSalidaNormalizada = normalizarTexto(clavePedimentoSalida);

        validarLongitud(materialNormalizado, MATERIAL_MAXIMO, "material");
        validarLongitud(productoNormalizado, PRODUCTO_MAXIMO, "producto");
        validarLongitud(pedimentoSalidaNormalizado, PEDIMENTO_SALIDA_MAXIMO, "pedimentoSalida");
        validarLongitud(clavePedimentoSalidaNormalizada, CLAVE_PEDIMENTO_SALIDA_MAXIMA,
                "clavePedimentoSalida");

        return materialUtilizadoRepository.findPage(
                desde,
                hasta,
                materialNormalizado,
                productoNormalizado,
                pedimentoSalidaNormalizado,
                clavePedimentoSalidaNormalizada,
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
