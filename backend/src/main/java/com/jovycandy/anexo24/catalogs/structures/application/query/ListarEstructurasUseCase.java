package com.jovycandy.anexo24.catalogs.structures.application.query;

import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;
import com.jovycandy.anexo24.catalogs.structures.domain.port.EstructuraRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada de líneas BOM. */
@Service
public class ListarEstructurasUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;

    private final EstructuraRepository estructuraRepository;

    /**
     * Constructor con el puerto de estructuras.
     *
     * @param estructuraRepository puerto de consulta de estructuras
     */
    public ListarEstructurasUseCase(EstructuraRepository estructuraRepository) {
        this.estructuraRepository = estructuraRepository;
    }

    /**
     * Consulta líneas BOM aplicando las reglas de paginación y normalización.
     *
     * @param producto clave funcional opcional del producto
     * @param material clave funcional opcional del material
     * @param pagina número de página base 1
     * @param tamano cantidad solicitada de elementos
     * @return página de líneas BOM
     */
    public Pagina<EstructuraDetalle> ejecutar(String producto, String material,
                                               int pagina, int tamano) {
        validarPaginacion(pagina, tamano);
        return estructuraRepository.findPage(
                normalizarTexto(producto), normalizarTexto(material), pagina, tamano);
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
