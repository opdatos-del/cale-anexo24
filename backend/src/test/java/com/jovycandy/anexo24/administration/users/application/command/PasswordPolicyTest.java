package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void aceptaPasswordValido() {
        assertThatCode(() -> policy.validar("Abcdefgh1!")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Abcdef1!", "Abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwx1!", "abcdefgh1!", "Abcdefghi!", "Abcdefghi1"})
    void rechazaPasswordFueraDePolitica(String password) {
        assertThatThrownBy(() -> policy.validar(password))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La contraseña no cumple la política de seguridad.");
    }
}
