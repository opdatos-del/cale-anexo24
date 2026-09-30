package com.jovycandy.anexo24.catalogs.auxiliary.application;

import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;

/** Reglas compartidas de paginación para consultas auxiliares read-only. */
public final class PaginacionCatalogo {
    public static final int TAMANO_MAXIMO = 100;

    private PaginacionCatalogo() {
    }

    public static void validar(int pagina, int tamano) {
        if (pagina < 1) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }

    public static String normalizarFiltro(String filtro) {
        return filtro == null || filtro.isBlank() ? null : filtro.trim();
    }
}
