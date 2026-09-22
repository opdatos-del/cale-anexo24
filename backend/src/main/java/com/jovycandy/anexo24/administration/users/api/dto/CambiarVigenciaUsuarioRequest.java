package com.jovycandy.anexo24.administration.users.api.dto;

import java.time.LocalDate;

/** Solicitud HTTP para cambiar la vigencia; {@code null} representa vigencia indefinida. */
public record CambiarVigenciaUsuarioRequest(LocalDate vigencia) {
}
