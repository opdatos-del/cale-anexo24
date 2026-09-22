package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adaptador JDBC de comandos administrativos de usuarios. */
@ExtendWith(MockitoExtension.class)
class UsuarioComandoJdbcAdapterTest {

    @Mock
    private JdbcTemplate appJdbcTemplate;

    private UsuarioComandoJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioComandoJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void existsByClaveConsultaDeFormaParametrizada() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);

        boolean existe = adapter.existsByClave("op01");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(sql.capture(), eq(Long.class), parametros.capture());
        assertThat(sql.getValue()).isEqualTo(
                "SELECT COUNT(*) FROM app24.UsuarioApp WHERE clave = ?");
        assertThat(parametros.getValue()).containsExactly("op01");
        assertThat(existe).isTrue();
    }

    @Test
    void existsByCorreoConsultaDeFormaParametrizada() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);

        boolean existe = adapter.existsByCorreo("op@example.test");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(sql.capture(), eq(Long.class), parametros.capture());
        assertThat(sql.getValue()).isEqualTo(
                "SELECT COUNT(*) FROM app24.UsuarioApp WHERE correo = ?");
        assertThat(parametros.getValue()).containsExactly("op@example.test");
        assertThat(existe).isFalse();
    }

    @Test
    void existsByCorreoExceptoUsuarioConsultaDeFormaParametrizada() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);

        boolean existe = adapter.existsByCorreoExceptoUsuario("op@example.test", 42L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(sql.capture(), eq(Long.class), parametros.capture());
        assertThat(sql.getValue()).isEqualTo(
                "SELECT COUNT(*) FROM app24.UsuarioApp WHERE correo = ? AND id <> ?");
        assertThat(parametros.getValue()).containsExactly("op@example.test", 42L);
        assertThat(existe).isTrue();
    }

    @Test
    void crearInsertaUsuarioActivoConHashYDevuelveIdGenerado() {
        LocalDate vigencia = LocalDate.of(2026, 12, 31);
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(7L);

        Long id = adapter.crear(
                "op01", "Operador Uno", "op@example.test", "$2a$hash", vigencia, 3L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(sql.capture(), eq(Long.class), parametros.capture());
        assertThat(sql.getValue()).contains("INSERT INTO app24.UsuarioApp")
                .contains("(clave, nombre, correo, password_hash, estado, vigencia, perfil_id)")
                .contains("OUTPUT INSERTED.id")
                .contains("VALUES (?, ?, ?, ?, 'ACTIVO', ?, ?)");
        assertThat(parametros.getValue()).containsExactly(
                "op01", "Operador Uno", "op@example.test", "$2a$hash", vigencia, 3L);
        assertThat(id).isEqualTo(7L);
    }

    @Test
    void crearConvierteClaveDuplicadaEnRecursoDuplicado() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenThrow(new DuplicateKeyException("Clave duplicada"));

        assertThatThrownBy(() -> adapter.crear(
                "op01", "Operador Uno", "op@example.test", "hash", null, 3L))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void crearPropagaErroresDeAccesoADatosDistintosDeDuplicado() {
        DataAccessResourceFailureException error =
                new DataAccessResourceFailureException("Base de datos no disponible");
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenThrow(error);

        assertThatThrownBy(() -> adapter.crear(
                "op01", "Operador Uno", "op@example.test", "hash", null, 3L))
                .isSameAs(error);
    }

    @Test
    void actualizarDatosModificaSoloNombreYCorreoPorId() {
        when(appJdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        int filasActualizadas = adapter.actualizarDatos(42L, "Operador Dos", "nuevo@example.test");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).update(sql.capture(), parametros.capture());
        assertThat(sql.getValue()).contains("UPDATE app24.UsuarioApp")
                .contains("SET nombre = ?, correo = ?")
                .contains("WHERE id = ?")
                .doesNotContain("clave")
                .doesNotContain("password_hash")
                .doesNotContain("estado")
                .doesNotContain("vigencia")
                .doesNotContain("perfil_id");
        assertThat(parametros.getValue()).containsExactly(
                "Operador Dos", "nuevo@example.test", 42L);
        assertThat(filasActualizadas).isEqualTo(1);
    }

    @Test
    void actualizarDatosConvierteClaveDuplicadaEnRecursoDuplicado() {
        when(appJdbcTemplate.update(anyString(), any(Object[].class)))
                .thenThrow(new DuplicateKeyException("Correo duplicado"));

        assertThatThrownBy(() -> adapter.actualizarDatos(42L, "Operador Dos", "op@example.test"))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void updatesSensiblesModificanSoloSuColumnaYGuardrailEsParametrizado() {
        when(appJdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        adapter.actualizarEstado(42L, "INACTIVO");
        adapter.actualizarPerfil(42L, 7L);
        adapter.actualizarVigencia(42L, null);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate, org.mockito.Mockito.times(3)).update(sql.capture(), parametros.capture());
        assertThat(sql.getAllValues()).allSatisfy(valor -> assertThat(valor).contains("UPDATE app24.UsuarioApp").contains("WHERE id = ?"));
        assertThat(sql.getAllValues().get(0)).contains("SET estado = ?").doesNotContain("password_hash").doesNotContain("clave");
        assertThat(sql.getAllValues().get(1)).contains("SET perfil_id = ?").doesNotContain("estado").doesNotContain("vigencia");
        assertThat(sql.getAllValues().get(2)).contains("SET vigencia = ?").doesNotContain("estado").doesNotContain("perfil_id");
        assertThat(parametros.getAllValues().get(2)).containsExactly(null, 42L);

        when(appJdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
        assertThat(adapter.existsConCapacidadAdministrativa(LocalDate.of(2026, 1, 1))).isTrue();
        ArgumentCaptor<String> guardrailSql = ArgumentCaptor.forClass(String.class);
        verify(appJdbcTemplate).queryForObject(guardrailSql.capture(), eq(Integer.class), any(Object[].class));
        assertThat(guardrailSql.getValue()).contains("app24.UsuarioApp", "app24.PerfilApp", "app24.PerfilActividad", "app24.Actividad")
                .contains("USUARIOS_ADMINISTRAR", "PERFILES_ADMINISTRAR", "u.estado = 'ACTIVO'", "p.estado = 'ACTIVO'", "u.vigencia IS NULL OR u.vigencia >= ?");
    }

    @Test
    void actualizarDatosPropagaErroresDeAccesoADatosDistintosDeDuplicado() {
        DataAccessResourceFailureException error =
                new DataAccessResourceFailureException("Base de datos no disponible");
        when(appJdbcTemplate.update(anyString(), any(Object[].class))).thenThrow(error);

        assertThatThrownBy(() -> adapter.actualizarDatos(42L, "Operador Dos", "op@example.test"))
                .isSameAs(error);
    }
}
