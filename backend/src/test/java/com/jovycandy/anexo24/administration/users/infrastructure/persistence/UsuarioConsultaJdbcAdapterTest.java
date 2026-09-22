package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del adaptador read-only de consulta administrativa de usuarios. */
@ExtendWith(MockitoExtension.class)
class UsuarioConsultaJdbcAdapterTest {

    @Mock
    private JdbcTemplate appJdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private UsuarioConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void listadoSinFiltrosCuentaYConsultaConOffsetYOrdenEstable() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(3L);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenReturn(List.of());

        Pagina<UsuarioAdministracion> pagina = adapter.findPage(
                null, null, null, null, null, 1, 20);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).query(sql.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                params.capture());
        assertThat(sql.getValue()).contains("FROM app24.UsuarioApp u");
        assertThat(sql.getValue()).contains("JOIN app24.PerfilApp p ON p.id = u.perfil_id");
        assertThat(sql.getValue()).contains("u.perfil_id, p.nombre AS perfil_nombre");
        assertThat(sql.getValue()).contains("ORDER BY u.clave ASC, u.id ASC");
        assertThat(sql.getValue()).contains("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        assertThat(sql.getValue()).doesNotContain("password_hash");
        assertThat(params.getValue()).containsExactly(0L, 20);

        ArgumentCaptor<Object[]> paramsTotal = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(anyString(), eq(Long.class), paramsTotal.capture());
        assertThat(paramsTotal.getValue()).isEmpty();
        assertThat(pagina.total()).isEqualTo(3L);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamano()).isEqualTo(20);
        assertThat(pagina.items()).isEmpty();
    }

    @Test
    void filtrosAcumulativosParametrizadosYCountComparteSemantica() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenReturn(List.of());

        Pagina<UsuarioAdministracion> pagina = adapter.findPage(
                "op01", "Juan", "op@example.test", "ACTIVO", 7L, 2, 10);

        ArgumentCaptor<String> sqlListado = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsListado = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).query(sqlListado.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                paramsListado.capture());
        assertThat(sqlListado.getValue()).contains(
                "WHERE u.clave = ? AND u.correo = ? AND u.perfil_id = ? AND u.estado = ?"
                        + " AND u.nombre LIKE ? ESCAPE '\\'");
        assertThat(paramsListado.getValue()).containsExactly(
                "op01", "op@example.test", 7L, "ACTIVO", "%Juan%", 10L, 10);

        ArgumentCaptor<String> sqlCount = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCount = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(sqlCount.capture(), eq(Long.class),
                paramsCount.capture());
        assertThat(sqlCount.getValue()).contains(
                "WHERE u.clave = ? AND u.correo = ? AND u.perfil_id = ? AND u.estado = ?"
                        + " AND u.nombre LIKE ? ESCAPE '\\'");
        assertThat(paramsCount.getValue()).containsExactly(
                "op01", "op@example.test", 7L, "ACTIVO", "%Juan%");
    }

    @Test
    void comodinesDelNombreSeEscapanComoTextoLiteral() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenReturn(List.of());

        adapter.findPage(null, "Juan_20%", null, null, null, 1, 20);

        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(anyString(), eq(Long.class), params.capture());
        assertThat(params.getValue()).containsExactly("%Juan\\_20\\%%");
    }

    @Test
    void backslashDelNombreSeEscapa() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenReturn(List.of());

        adapter.findPage(null, "c:\\carpeta", null, null, null, 1, 20);

        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).queryForObject(anyString(), eq(Long.class), params.capture());
        assertThat(params.getValue()).containsExactly("%c:\\\\carpeta%");
    }

    @Test
    void offsetSeCalculaConAritmeticaLong() {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenReturn(List.of());

        adapter.findPage(null, null, null, null, null, 5, 100);

        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(appJdbcTemplate).query(
                anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                params.capture());
        assertThat(params.getValue()).containsExactly(400L, 100);
    }

    @Test
    void mapeaFilaConVigenciaYPerfil() throws Exception {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        prepararResultSet(true);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<UsuarioAdministracion> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        Pagina<UsuarioAdministracion> pagina = adapter.findPage(
                null, null, null, null, null, 1, 20);

        assertThat(pagina.total()).isEqualTo(1L);
        assertThat(pagina.items()).singleElement().satisfies(usuario -> {
            assertThat(usuario.id()).isEqualTo(7L);
            assertThat(usuario.clave()).isEqualTo("op01");
            assertThat(usuario.nombre()).isEqualTo("Operador Uno");
            assertThat(usuario.correo()).isEqualTo("op@example.test");
            assertThat(usuario.estado()).isEqualTo("ACTIVO");
            assertThat(usuario.vigencia()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(usuario.perfilId()).isEqualTo(3L);
            assertThat(usuario.perfilNombre()).isEqualTo("ADMINISTRADOR");
        });
    }

    @Test
    void mapeaFilaConVigenciaNula() throws Exception {
        when(appJdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        prepararResultSet(false);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<UsuarioAdministracion> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        Pagina<UsuarioAdministracion> pagina = adapter.findPage(
                null, null, null, null, null, 1, 20);

        assertThat(pagina.items()).singleElement().satisfies(usuario -> {
            assertThat(usuario.vigencia()).isNull();
            assertThat(usuario.perfilNombre()).isEqualTo("ADMINISTRADOR");
        });
    }

    @Test
    void findByIdConsultaPorIdYDevuelveUsuario() throws Exception {
        prepararResultSet(false);
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                eq(42L))).thenAnswer(invocation -> {
            RowMapper<UsuarioAdministracion> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        Optional<UsuarioAdministracion> resultado = adapter.findById(42L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(appJdbcTemplate).query(sql.capture(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(), eq(42L));
        assertThat(sql.getValue()).contains("WHERE u.id = ?");
        assertThat(sql.getValue()).doesNotContain("password_hash");
        assertThat(resultado).isPresent();
        assertThat(resultado.get().id()).isEqualTo(7L);
    }

    @Test
    void findByIdSinResultadosDevuelveVacio() {
        when(appJdbcTemplate.<UsuarioAdministracion>query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<UsuarioAdministracion>>any(),
                eq(42L))).thenReturn(List.of());

        Optional<UsuarioAdministracion> resultado = adapter.findById(42L);

        assertThat(resultado).isEmpty();
    }

    private void prepararResultSet(boolean conVigencia) throws Exception {
        when(resultSet.getLong("id")).thenReturn(7L);
        when(resultSet.getString("clave")).thenReturn("op01");
        when(resultSet.getString("nombre")).thenReturn("Operador Uno");
        when(resultSet.getString("correo")).thenReturn("op@example.test");
        when(resultSet.getString("estado")).thenReturn("ACTIVO");
        when(resultSet.getLong("perfil_id")).thenReturn(3L);
        when(resultSet.getString("perfil_nombre")).thenReturn("ADMINISTRADOR");
        when(resultSet.getDate("vigencia")).thenReturn(
                conVigencia ? java.sql.Date.valueOf(LocalDate.of(2026, 12, 31)) : null);
    }
}