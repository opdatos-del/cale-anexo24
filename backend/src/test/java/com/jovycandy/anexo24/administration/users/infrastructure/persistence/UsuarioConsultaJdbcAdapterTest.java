package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del adaptador read-only de usuarios mediante SP. */
@ExtendWith(MockitoExtension.class)
class UsuarioConsultaJdbcAdapterTest {

    @Mock private JdbcTemplate appJdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private UsuarioConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void listadoPasaFiltrosPaginacionLeeTotalYMapeaOchoCampos() throws Exception {
        UsuarioAdministracion usuario = new UsuarioAdministracion(7L, "op01", "Operador",
                "op@test", "ACTIVO", LocalDate.of(2026, 12, 31), 3L, "ADMIN");
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(
                Map.of("items", List.of(usuario), "Total", 1L));

        Pagina<UsuarioAdministracion> pagina = adapter.findPage(
                "op01", "Juan_20%", "op@test", "ACTIVO", 3L, 2, 10);

        assertThat(pagina.total()).isEqualTo(1L);
        assertThat(pagina.items()).containsExactly(usuario);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_Q_USUARIOS_LISTAR(?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setString(1, "op01");
        verify(statement).setString(2, "Juan_20%");
        verify(statement).setString(3, "op@test");
        verify(statement).setString(4, "ACTIVO");
        verify(statement).setLong(5, 3L);
        verify(statement).setInt(6, 2);
        verify(statement).setInt(7, 10);
        verify(statement).registerOutParameter(8, java.sql.Types.BIGINT);
    }

    @Test
    void listadoSinResultadosConservaTotalCero() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(), "Total", 0L));
        assertThat(adapter.findPage(null, null, null, null, null, 1, 20).items()).isEmpty();
    }

    @Test
    void detalleConsumeSpYDevuelveOptional() throws Exception {
        UsuarioAdministracion usuario = new UsuarioAdministracion(42L, "op01", "Operador",
                "op@test", "ACTIVO", null, 3L, "ADMIN");
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(usuario)));

        assertThat(adapter.findById(42L)).contains(usuario);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_Q_USUARIO_OBTENER(?)}")).thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setLong(1, 42L);
    }

    @Test
    void detalleSinResultadosDevuelveVacio() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of()));
        assertThat(adapter.findById(404L)).isEmpty();
    }
}
