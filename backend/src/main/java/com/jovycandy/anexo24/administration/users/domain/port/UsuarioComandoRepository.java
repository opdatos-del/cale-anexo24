package com.jovycandy.anexo24.administration.users.domain.port;

import java.time.LocalDate;

/** Puerto de mutaciones administrativas mediante commands almacenados. */
public interface UsuarioComandoRepository {
    Long crear(String clave, String nombre, String correo, String passwordHash, LocalDate vigencia, Long perfilId);
    void actualizarDatos(Long usuarioId, String nombre, String correo);
    void actualizarEstado(Long usuarioId, String estado, Long actorId, LocalDate fechaActual);
    void actualizarPerfil(Long usuarioId, Long perfilId, Long actorId, LocalDate fechaActual);
    void actualizarVigencia(Long usuarioId, LocalDate vigencia, Long actorId, LocalDate fechaActual);
    void restablecerPassword(Long usuarioId, String passwordHash);
}
