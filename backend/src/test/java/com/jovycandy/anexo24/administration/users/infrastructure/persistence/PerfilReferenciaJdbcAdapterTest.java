package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC de referencia de perfiles. */
@ExtendWith(MockitoExtension.class)
class PerfilReferenciaJdbcAdapterTest {

    private static final String SQL_ESTADO_POR_ID =
            "SELECT estado FROM app24.PerfilApp WHERE id = ?";

    @Mock
    private JdbcTemplate appJdbcTemplate;

    private PerfilReferenciaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PerfilReferenciaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void encuentraPerfilActivo() {
        prepararConsulta(List.of("ACTIVO"));

        Optional<String> estado = adapter.findEstadoById(7L);

        assertThat(estado).contains("ACTIVO");
        verificarConsulta(7L);
    }

    @Test
    void encuentraPerfilInactivo() {
        prepararConsulta(List.of("INACTIVO"));

        Optional<String> estado = adapter.findEstadoById(7L);

        assertThat(estado).contains("INACTIVO");
        verificarConsulta(7L);
    }

    @Test
    void sinFilasDevuelveVacio() {
        prepararConsulta(List.of());

        Optional<String> estado = adapter.findEstadoById(7L);

        assertThat(estado).isEmpty();
        verificarConsulta(7L);
    }

    private void prepararConsulta(List<String> estados) {
        when(appJdbcTemplate.<String>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(7L)))
                .thenReturn(estados);
    }

    private void verificarConsulta(Long perfilId) {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(appJdbcTemplate).query(sql.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(perfilId));
        assertThat(sql.getValue()).isEqualTo(SQL_ESTADO_POR_ID);
    }
}
