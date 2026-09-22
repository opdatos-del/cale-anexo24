package com.jovycandy.anexo24.administration.users.domain.port;

import java.util.Optional;

/** Consulta mínima del estado de un perfil asignable a usuario. */
public interface PerfilReferenciaRepository {
    Optional<String> findEstadoById(Long perfilId);
}
