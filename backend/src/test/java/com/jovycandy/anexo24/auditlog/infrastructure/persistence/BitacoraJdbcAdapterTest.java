package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del insert append-only de Bitácora contra app24. */
@ExtendWith(MockitoExtension.class)
class BitacoraJdbcAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BitacoraJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BitacoraJdbcAdapter(jdbcTemplate);
    }

    @Test
    void insertaColumnasYValoresAprobadosEnOrden() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        BitacoraEvento evento = new BitacoraEvento(
                null,
                BitacoraModulo.SEGURIDAD,
                BitacoraAccion.LOGIN_OK,
                BitacoraResultado.EXITO,
                "Acceso autenticado",
                "req-01");

        adapter.registrar(evento);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sql.capture(), parametros.capture());
        assertThat(sql.getValue().trim()).isEqualTo("""
                INSERT INTO app24.BitacoraEvento
                    (usuario_id, modulo, accion, detalle, correlacion_id, resultado)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trim());
        assertThat(parametros.getValue()).containsExactly(
                null,
                "SEGURIDAD",
                "LOGIN_OK",
                "Acceso autenticado",
                "req-01",
                "EXITO");
    }

    @Test
    void aceptaUnaFilaAfectada() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        adapter.registrar(eventoValido());
    }

    @Test
    void rechazaCeroFilasAfectadas() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

        assertThatIllegalStateException().isThrownBy(() -> adapter.registrar(eventoValido()));
    }

    @Test
    void rechazaMasDeUnaFilaAfectada() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(2);

        assertThatIllegalStateException().isThrownBy(() -> adapter.registrar(eventoValido()));
    }

    private BitacoraEvento eventoValido() {
        return new BitacoraEvento(
                42L,
                BitacoraModulo.OPERACIONES,
                BitacoraAccion.LOGIN_FALLIDO,
                BitacoraResultado.FALLO,
                null,
                null);
    }
}
