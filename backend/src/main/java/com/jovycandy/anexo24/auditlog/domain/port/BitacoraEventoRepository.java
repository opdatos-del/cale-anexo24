package com.jovycandy.anexo24.auditlog.domain.port;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;

/** Puerto append-only para registrar eventos de Bitácora. */
public interface BitacoraEventoRepository {

    /**
     * Agrega un evento de Bitácora sin exponer actualización ni borrado.
     *
     * @param evento evento validado que se registrará
     */
    void registrar(BitacoraEvento evento);
}
