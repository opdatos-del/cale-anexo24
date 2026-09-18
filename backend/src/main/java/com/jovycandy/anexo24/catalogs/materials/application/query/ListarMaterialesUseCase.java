package com.jovycandy.anexo24.catalogs.materials.application.query;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.catalogs.materials.domain.port.MaterialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.stereotype.Service;

/** Caso de uso de consulta paginada del catálogo de materiales. */
@Service
public class ListarMaterialesUseCase {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;

    private final MaterialRepository materialRepository;

    /**
     * Constructor con el puerto de materiales.
     *
     * @param materialRepository puerto de consulta del catálogo
     */
    public ListarMaterialesUseCase(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    /**
     * Consulta materiales aplicando los límites funcionales de paginación.
     *
     * @param filtro texto opcional para clave, descripción o fracción
     * @param pagina número de página base 1
     * @param tamano cantidad solicitada de elementos
     * @return página de materiales
     */
    public Pagina<Material> ejecutar(String filtro, int pagina, int tamano) {
        validarPaginacion(pagina, tamano);
        return materialRepository.findPage(normalizarFiltro(filtro), pagina, tamano);
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new IllegalArgumentException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }

    private String normalizarFiltro(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return null;
        }
        return filtro.trim();
    }
}
