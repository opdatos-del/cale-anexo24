package com.jovycandy.anexo24.administration.users.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Proyección de acceso de un usuario para autenticación (CU-001).
 *
 * <p>Une el perfil asignado y sus permisos sin filtrar el estado del
 * perfil: la decisión de rechazar por perfil inactivo queda en la capa
 * de aplicación (FASE 1 de administración).</p>
 *
 * @param perfilId     identificador del perfil asignado
 * @param perfilEstado estado del perfil (ACTIVO o INACTIVO)
 * @param permisos     actividades habilitadas; puede estar vacía
 */
public record UsuarioAcceso(
        Long perfilId,
        String perfilEstado,
        List<String> permisos) {

    /**
     * Compact constructor con invariantes del modelo.
     *
     * <p>Los permisos se copian de forma inmutable para que los
     * consumidores no alteren la colección interna.</p>
     *
     * @throws IllegalArgumentException si el perfil es inválido o los permisos son nulos
     */
    public UsuarioAcceso {
        if (perfilId == null || perfilId <= 0) {
            throw new IllegalArgumentException("perfilId debe ser un identificador positivo");
        }
        if (perfilEstado == null || perfilEstado.isBlank()) {
            throw new IllegalArgumentException("perfilEstado es obligatorio");
        }
        permisos = List.copyOf(Objects.requireNonNull(permisos, "permisos es obligatorio"));
    }

    /**
     * Indica si el perfil asignado está activo.
     *
     * <p>La comparación ignora mayúsculas porque el esquema {@code app24}
     * persiste estados textuales (ACTIVO/INACTIVO).</p>
     *
     * @return {@code true} si el perfil está en estado ACTIVO
     */
    public boolean perfilActivo() {
        return "ACTIVO".equalsIgnoreCase(perfilEstado);
    }
}