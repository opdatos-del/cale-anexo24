package com.jovycandy.anexo24.catalogs.products.application.query;

import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;
import com.jovycandy.anexo24.catalogs.products.domain.port.ProductoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada del catálogo de Productos. */
@Service
public class ListarProductosUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;

    private final ProductoRepository productoRepository;

    /**
     * Constructor con el puerto de productos.
     *
     * @param productoRepository puerto de consulta del catálogo
     */
    public ListarProductosUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Consulta productos aplicando los límites funcionales de paginación.
     *
     * @param filtro texto opcional para código, nombre o fracción
     * @param pagina número de página base 1
     * @param tamano cantidad solicitada de elementos
     * @return página de productos
     */
    public Pagina<Producto> ejecutar(String filtro, int pagina, int tamano) {
        validarPaginacion(pagina, tamano);
        return productoRepository.findPage(normalizarFiltro(filtro), pagina, tamano);
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }

    private String normalizarFiltro(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return null;
        }
        return filtro.trim();
    }
}
