package com.jovycandy.anexo24.security.api.dto;

import java.util.List;

/**
 * Respuesta de inicio de sesión.
 *
 * @param token   token JWT de acceso
 * @param expiraEn minutos de vigencia del token
 * @param usuario nombre del usuario autenticado
 * @param permisos permisos asignados al usuario
 */
public record LoginResponse(
        String token,
        long expiraEn,
        String usuario,
        List<String> permisos) {
}