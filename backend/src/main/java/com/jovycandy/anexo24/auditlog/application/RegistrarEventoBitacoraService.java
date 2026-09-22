package com.jovycandy.anexo24.auditlog.application;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraEventoRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Caso de uso interno para agregar eventos de Bitácora. */
@Service
public class RegistrarEventoBitacoraService {

    private final BitacoraEventoRepository repository;

    /**
     * Construye el caso de uso con su puerto append-only.
     *
     * @param repository persistencia interna de Bitácora
     */
    public RegistrarEventoBitacoraService(BitacoraEventoRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra un evento previamente validado por su modelo de dominio.
     *
     * @param evento evento interno de Bitácora
     */
    public void registrar(BitacoraEvento evento) {
        repository.registrar(Objects.requireNonNull(evento, "evento es obligatorio"));
    }
}
