package com.jovycandy.anexo24.administration.users.domain.port;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;

import java.util.Optional;

/**
 * Puerto read-only de consulta administrativa de usuarios.
 *
 * <p>Separado del {@code UsuarioRepository} de autenticación: la lectura
 * administrativa y la autenticación no comparten puerto (FASE 2).</p>
 */
public interface UsuarioConsultaRepository {

    /**
     * Lista usuarios paginados con filtros opcionales.
     *
     * @param clave    filtro opcional exacto por clave
     * @param nombre   filtro opcional parcial por nombre
     * @param correo   filtro opcional exacto por correo
     * @param estado   filtro opcional exacto por estado
     * @param perfilId filtro opcional exacto por perfil
     * @param pagina   número de página base 1
     * @param tamano   tamaño de página
     * @return página de usuarios administrativos
     */
    Pagina<UsuarioAdministracion> findPage(
            String clave,
            String nombre,
            String correo,
            String estado,
            Long perfilId,
            int pagina,
            int tamano);

    /**
     * Obtiene el detalle read-only de un usuario.
     *
     * @param id identificador del usuario
     * @return usuario o vacío si no existe
     */
    Optional<UsuarioAdministracion> findById(Long id);
}