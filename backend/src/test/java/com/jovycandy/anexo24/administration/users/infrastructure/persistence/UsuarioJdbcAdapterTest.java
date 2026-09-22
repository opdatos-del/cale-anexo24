package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAcceso;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC de usuarios. */
@ExtendWith(MockitoExtension.class)
class UsuarioJdbcAdapterTest {

    @Mock
    private JdbcTemplate appJdbcTemplate;

    private UsuarioJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void encuentraAccesoConPerfilActivoYPermisosSinFiltrarEstado() {
        when(appJdbcTemplate.<Object[]>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L)))
                .thenReturn(List.of(
                        fila(7L, "ACTIVO", "OPERACIONES_CONSULTAR"),
                        fila(7L, "ACTIVO", "BITACORA_CONSULTAR")));

        Optional<UsuarioAcceso> acceso = adapter.findAccesoByUsuario(42L);

        assertThat(acceso).isPresent();
        assertThat(acceso.get().perfilId()).isEqualTo(7L);
        assertThat(acceso.get().perfilEstado()).isEqualTo("ACTIVO");
        assertThat(acceso.get().perfilActivo()).isTrue();
        assertThat(acceso.get().permisos())
                .containsExactly("OPERACIONES_CONSULTAR", "BITACORA_CONSULTAR");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(appJdbcTemplate).query(sql.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L));
        assertThat(sql.getValue()).contains("JOIN app24.PerfilApp p ON p.id = u.perfil_id");
        assertThat(sql.getValue()).contains("LEFT JOIN app24.PerfilActividad pa ON pa.perfil_id = p.id");
        assertThat(sql.getValue()).contains("LEFT JOIN app24.Actividad a ON a.id = pa.actividad_id");
        assertThat(sql.getValue()).contains("WHERE u.id = ?");
        assertThat(sql.getValue()).contains("ORDER BY a.clave ASC");
        assertThat(sql.getValue()).doesNotContain("p.estado =");
        assertThat(sql.getValue()).doesNotContain("u.estado");
    }

    @Test
    void perfilActivoSinPermisosConservaAccesoConListaVacia() {
        when(appJdbcTemplate.<Object[]>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L)))
                .thenReturn(List.<Object[]>of(fila(7L, "ACTIVO", null)));

        Optional<UsuarioAcceso> acceso = adapter.findAccesoByUsuario(42L);

        assertThat(acceso).isPresent();
        assertThat(acceso.get().perfilActivo()).isTrue();
        assertThat(acceso.get().permisos()).isEmpty();
    }

    @Test
    void perfilInactivoSePropagaSinRechazoEnElAdaptador() {
        when(appJdbcTemplate.<Object[]>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L)))
                .thenReturn(List.<Object[]>of(fila(7L, "INACTIVO", "OPERACIONES_CONSULTAR")));

        Optional<UsuarioAcceso> acceso = adapter.findAccesoByUsuario(42L);

        assertThat(acceso).isPresent();
        assertThat(acceso.get().perfilEstado()).isEqualTo("INACTIVO");
        assertThat(acceso.get().perfilActivo()).isFalse();
        assertThat(acceso.get().permisos()).containsExactly("OPERACIONES_CONSULTAR");
    }

    @Test
    void sinFilasDevuelveVacio() {
        when(appJdbcTemplate.<Object[]>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L)))
                .thenReturn(List.of());

        Optional<UsuarioAcceso> acceso = adapter.findAccesoByUsuario(42L);

        assertThat(acceso).isEmpty();
    }

    @Test
    void omitePermisosNulosEnFilasMixtas() {
        when(appJdbcTemplate.<Object[]>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L)))
                .thenReturn(List.of(
                        fila(7L, "ACTIVO", null),
                        fila(7L, "ACTIVO", "BITACORA_CONSULTAR"),
                        fila(7L, "ACTIVO", null)));

        Optional<UsuarioAcceso> acceso = adapter.findAccesoByUsuario(42L);

        assertThat(acceso).isPresent();
        assertThat(acceso.get().permisos()).containsExactly("BITACORA_CONSULTAR");
    }

    @Test
    void propagaErroresDeAccesoADatos() {
        RuntimeException error = new RuntimeException("BD no disponible");
        when(appJdbcTemplate.<Object[]>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<Object[]>>any(), eq(42L)))
                .thenThrow(error);

        assertThatThrownBy(() -> adapter.findAccesoByUsuario(42L)).isSameAs(error);
    }

    private Object[] fila(Long perfilId, String perfilEstado, String permiso) {
        return new Object[]{perfilId, perfilEstado, permiso};
    }
}