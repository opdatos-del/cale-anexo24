package com.jovycandy.anexo24.administration.profiles.application.query;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/** Caso de uso de listado paginado read-only de perfiles. */
@Service
public class ListarPerfilesUseCase {

    private static final int NOMBRE_MAXIMO = 80;
    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;
    private static final Set<String> ESTADOS_VALIDOS = Set.of("ACTIVO", "INACTIVO");

    private final PerfilConsultaRepository perfilConsultaRepository;

    /**
     * Construye el caso de uso de listado.
     *
     * @param perfilConsultaRepository puerto read-only de perfiles
     */
    public ListarPerfilesUseCase(PerfilConsultaRepository perfilConsultaRepository) {
        this.perfilConsultaRepository = perfilConsultaRepository;
    }

    /**
     * Lista perfiles con filtros normalizados y paginación validada.
     *
     * @param nombre filtro opcional parcial por nombre
     * @param estado filtro opcional exacto por estado (ACTIVO/INACTIVO)
     * @param pagina número de página base 1
     * @param tamano tamaño de página; rango 1-100
     * @return página de perfiles administrativos
     * @throws SolicitudInvalidaException si algún parámetro es inválido
     */
    public Pagina<PerfilAdministracion> ejecutar(String nombre, String estado, int pagina, int tamano) {
        validarPaginacion(pagina, tamano);
        return perfilConsultaRepository.findPage(normalizarNombre(nombre), normalizarEstado(estado), pagina, tamano);
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        String normalizado = nombre.trim();
        if (normalizado.length() > NOMBRE_MAXIMO) {
            throw new SolicitudInvalidaException(
                    "El parámetro nombre supera la longitud máxima de " + NOMBRE_MAXIMO + " caracteres.");
        }
        return normalizado;
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new SolicitudInvalidaException("El parámetro estado solo admite ACTIVO o INACTIVO.");
        }
        return normalizado;
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }
}
