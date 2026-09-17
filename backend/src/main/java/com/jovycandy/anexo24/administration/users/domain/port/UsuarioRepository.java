package com.jovycandy.anexo24.administration.users.domain.port;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de acceso a usuarios del esquema complementario.
 *
 * <p>Implementado por infraestructura con JdbcTemplate contra
 * ANEXO24_DEV; el dominio no conoce detalles de persistencia.</p>
 */
public interface UsuarioRepository {

    /**
     * Busca un usuario por su clave de acceso.
     *
     * @param clave clave de acceso
     * @return usuario encontrado o vacío
     */
    Optional<UsuarioApp> findByClave(String clave);

    /**
     * Obtiene los permisos (claves de actividades) del usuario.
     *
     * @param usuarioId identificador del usuario
     * @return lista de claves de permiso
     */
    List<String> findPermisosByUsuario(Long usuarioId);
}