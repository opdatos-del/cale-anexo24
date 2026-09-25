package com.jovycandy.anexo24.auditlog.domain.model;

/** Acciones controladas registradas en la Bitácora de la aplicación. */
public enum BitacoraAccion {
    LOGIN_OK,
    LOGIN_FALLIDO,
    USUARIO_CREADO,
    USUARIO_ACTUALIZADO,
    USUARIO_ESTADO_CAMBIADO,
    USUARIO_PERFIL_CAMBIADO,
    USUARIO_VIGENCIA_CAMBIADA,
    USUARIO_PASSWORD_RESTABLECIDA,
    PERFIL_CREADO,
    PERFIL_ACTUALIZADO,
    PERFIL_ESTADO_CAMBIADO,
    PERFIL_PERMISOS_CAMBIADOS,
    CARGA_VALIDADA,
    CARGA_CON_ERRORES
}
