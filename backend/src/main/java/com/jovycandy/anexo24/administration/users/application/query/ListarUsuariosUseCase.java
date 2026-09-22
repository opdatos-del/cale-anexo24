package com.jovycandy.anexo24.administration.users.application.query;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Caso de uso de listado paginado read-only de usuarios (administración).
 *
 * <p>Normaliza y valida filtros antes de delegar al puerto de consulta;
 * no conoce JDBC (FASE 2).</p>
 */
@Service
public class ListarUsuariosUseCase {

    private static final int CLAVE_MAXIMA = 30;
    private static final int NOMBRE_MAXIMO = 120;
    private static final int CORREO_MAXIMO = 150;
    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 100;
    private static final Set<String> ESTADOS_VALIDOS = Set.of("ACTIVO", "INACTIVO");

    private final UsuarioConsultaRepository usuarioConsultaRepository;

    /**
     * Construye el caso de uso de listado.
     *
     * @param usuarioConsultaRepository puerto read-only de usuarios
     */
    public ListarUsuariosUseCase(UsuarioConsultaRepository usuarioConsultaRepository) {
        this.usuarioConsultaRepository = usuarioConsultaRepository;
    }

    /**
     * Lista usuarios con filtros normalizados y paginación validada.
     *
     * @param clave    filtro opcional exacto por clave
     * @param nombre   filtro opcional parcial por nombre
     * @param correo   filtro opcional exacto por correo
     * @param estado   filtro opcional exacto por estado (ACTIVO/INACTIVO)
     * @param perfilId filtro opcional exacto por perfil
     * @param pagina   número de página base 1
     * @param tamano   tamaño de página; rango 1-100
     * @return página de usuarios administrativos
     * @throws SolicitudInvalidaException si algún parámetro es inválido
     */
    public Pagina<UsuarioAdministracion> ejecutar(
            String clave,
            String nombre,
            String correo,
            String estado,
            Long perfilId,
            int pagina,
            int tamano) {
        validarPaginacion(pagina, tamano);
        validarPerfilId(perfilId);

        return usuarioConsultaRepository.findPage(
                normalizar(clave, CLAVE_MAXIMA, "clave"),
                normalizar(nombre, NOMBRE_MAXIMO, "nombre"),
                normalizar(correo, CORREO_MAXIMO, "correo"),
                normalizarEstado(estado),
                perfilId,
                pagina,
                tamano);
    }

    /**
     * Normaliza un filtro opcional: {@code null}/blank se vuelven {@code null};
     * el resto se recorta y se valida su longitud.
     *
     * @param valor     valor crudo del filtro
     * @param longitudMaxima límite del DDL de UsuarioApp
     * @param campo     nombre del parámetro para el mensaje de error
     * @return valor normalizado o {@code null}
     * @throws SolicitudInvalidaException si supera la longitud máxima
     */
    private String normalizar(String valor, int longitudMaxima, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim();
        if (normalizado.length() > longitudMaxima) {
            throw new SolicitudInvalidaException(
                    "El parámetro " + campo + " supera la longitud máxima de "
                            + longitudMaxima + " caracteres.");
        }
        return normalizado;
    }

    /**
     * Normaliza el filtro de estado a mayúsculas limitado a ACTIVO/INACTIVO.
     *
     * @param estado valor crudo del filtro
     * @return estado normalizado o {@code null}
     * @throws SolicitudInvalidaException si el estado no está permitido
     */
    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new SolicitudInvalidaException(
                    "El parámetro estado solo admite ACTIVO o INACTIVO.");
        }
        return normalizado;
    }

    /**
     * Valida el filtro de perfil cuando está presente.
     *
     * @param perfilId identificador del perfil
     * @throws SolicitudInvalidaException si es menor o igual a cero
     */
    private void validarPerfilId(Long perfilId) {
        if (perfilId != null && perfilId <= 0) {
            throw new SolicitudInvalidaException("El parámetro perfilId debe ser positivo.");
        }
    }

    /**
     * Valida la paginación sin ajustes silenciosos.
     *
     * @param pagina número de página
     * @param tamano tamaño de página
     * @throws SolicitudInvalidaException si algún valor es inválido
     */
    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < PAGINA_MINIMA) {
            throw new SolicitudInvalidaException("El parámetro pagina debe ser mayor o igual que 1.");
        }
        if (tamano < TAMANO_MINIMO || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("El parámetro tamano debe estar entre 1 y 100.");
        }
    }
}