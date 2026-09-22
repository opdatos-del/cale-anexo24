package com.jovycandy.anexo24.administration.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Solicitud HTTP para crear un usuario administrativo. */
public record CrearUsuarioRequest(
        @NotBlank @Size(max = 30) String clave,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank
                @Size(min = 10, max = 50)
                @Pattern(regexp = "(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{Alnum}\\s]).{10,50}",
                        message = "La contraseña no cumple la política de seguridad.")
                String password,
        LocalDate vigencia,
        @NotNull @Positive Long perfilId) {
}
