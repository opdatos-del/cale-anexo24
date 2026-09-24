package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAcceso;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC de usuarios mediante SP. */
@ExtendWith(MockitoExtension.class)
class UsuarioJdbcAdapterTest {

    @Mock private JdbcTemplate appJdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private UsuarioJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void toStringDelModeloAuthNoExponeHash() {
        UsuarioApp usuario = new UsuarioApp(7L, "op01", "Operador", "op@test", "hash-ficticio",
                "ACTIVO", null, 3L);

        assertThat(usuario.toString())
                .contains("passwordHash=REDACTED")
                .doesNotContain("hash-ficticio");
    }

    @Test
    void findByClaveConsumeSpYMapeaPasswordInternamente() throws Exception {
        UsuarioApp usuario = new UsuarioApp(7L, "op01", "Operador", "op@test", "hash-interno",
                "ACTIVO", null, 3L);
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("usuario", List.of(usuario)));

        assertThat(adapter.findByClave("op01")).contains(usuario);

        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_Q_USUARIO_POR_CLAVE(?)}")).thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setString(1, "op01");
    }

    @Test
    void findByClaveSinFilasDevuelveVacio() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("usuario", List.of()));
        assertThat(adapter.findByClave("inexistente")).isEmpty();
    }

    @Test
    void findAccesoMapeaVariosPermisosYConservaPerfilInactivo() {
        List<Object[]> filas = List.of(
                new Object[]{7L, "INACTIVO", "A_PERMISO"},
                new Object[]{7L, "INACTIVO", null},
                new Object[]{7L, "INACTIVO", "B_PERMISO"});
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("acceso", filas));

        UsuarioAcceso acceso = adapter.findAccesoByUsuario(42L).orElseThrow();

        assertThat(acceso.perfilId()).isEqualTo(7L);
        assertThat(acceso.perfilEstado()).isEqualTo("INACTIVO");
        assertThat(acceso.permisos()).containsExactly("A_PERMISO", "B_PERMISO");
    }

    @Test
    void findAccesoSinFilasDevuelveVacio() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("acceso", List.of()));
        assertThat(adapter.findAccesoByUsuario(42L)).isEmpty();
    }

    @Test
    void findAccesoConPerfilSinPermisosConservaFila() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(
                Map.of("acceso", List.<Object[]>of(new Object[]{7L, "ACTIVO", null})));
        assertThat(adapter.findAccesoByUsuario(42L)).get().satisfies(acceso -> {
            assertThat(acceso.perfilId()).isEqualTo(7L);
            assertThat(acceso.permisos()).isEmpty();
        });
    }
}
