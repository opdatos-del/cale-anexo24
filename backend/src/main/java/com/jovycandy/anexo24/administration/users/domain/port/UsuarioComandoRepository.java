package com.jovycandy.anexo24.administration.users.domain.port;

import java.time.LocalDate;

/** Puerto de mutaciones administrativas, separado de autenticación y consultas. */
public interface UsuarioComandoRepository {
    boolean existsByClave(String clave);
    boolean existsByCorreo(String correo);
    boolean existsByCorreoExceptoUsuario(String correo, Long usuarioId);
    Long crear(String clave, String nombre, String correo, String passwordHash, LocalDate vigencia, Long perfilId);
    int actualizarDatos(Long usuarioId, String nombre, String correo);
}
