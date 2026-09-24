package com.jovycandy.anexo24.administration.users.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrearUsuarioRequestValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void aceptaPasswordNoVacioParaDelegarPoliticaAApplication() {
        assertThat(violaciones("Abcdefgh1!")).isEmpty();
    }

    @Test
    void rechazaPasswordVacio() {
        assertThat(violaciones("   ")).isNotEmpty();
    }

    @Test
    void toStringNoExponePassword() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "op01", "Nombre", "correo@test", "secreto-ficticio", null, 1L);

        assertThat(request.toString())
                .contains("password=REDACTED")
                .doesNotContain("secreto-ficticio");
    }

    private Set<?> violaciones(String password) {
        return validator.validate(new CrearUsuarioRequest("op01", "Nombre", "correo@test", password, null, 1L));
    }
}
