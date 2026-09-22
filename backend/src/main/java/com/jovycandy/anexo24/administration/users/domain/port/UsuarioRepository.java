package com.jovycandy.anexo24.administration.users.domain.port;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAcceso;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;

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
     * Obtiene la proyección de acceso del usuario: perfil asignado y
     * permisos, sin filtrar el estado del perfil.
     *
     * <p>Devuelve vacío cuando el usuario no existe o su registro es
     * inconsistente (sin perfil asignado).</p>
     *
     * @param usuarioId identificador del usuario
     * @return acceso del usuario o vacío
     */
    Optional<UsuarioAcceso> findAccesoByUsuario(Long usuarioId);
}