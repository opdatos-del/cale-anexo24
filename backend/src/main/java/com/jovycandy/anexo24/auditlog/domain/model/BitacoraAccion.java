package com.jovycandy.anexo24.auditlog.domain.model;

/** Acciones de Bitácora aprobadas para autenticación y administración de usuarios. */
public enum BitacoraAccion {
    LOGIN_OK,
    LOGIN_FALLIDO,
    USUARIO_CREADO,
    USUARIO_ACTUALIZADO,
    USUARIO_ESTADO_CAMBIADO,
    USUARIO_PERFIL_CAMBIADO,
    USUARIO_VIGENCIA_CAMBIADA,
    USUARIO_PASSWORD_RESTABLECIDA
}
