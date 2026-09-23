package com.jovycandy.anexo24.administration.profiles.infrastructure.persistence;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del adaptador read-only de perfiles mediante SP. */
@ExtendWith(MockitoExtension.class)
class PerfilConsultaJdbcAdapterTest {

    @Mock private JdbcTemplate appJdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    @Mock private ResultSet resultSet;
    private PerfilConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PerfilConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void listadoPasaFiltrosPaginacionYLeeTotal() throws Exception {
        PerfilAdministracion perfil = new PerfilAdministracion(7L, "ADMIN", "ACTIVO", 3L);
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(perfil), "Total", 1L));

        Pagina<PerfilAdministracion> pagina = adapter.findPage("Administr", "ACTIVO", 2, 10);

        assertThat(pagina.items()).containsExactly(perfil);
        assertThat(pagina.total()).isEqualTo(1L);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_Q_PERFILES_LISTAR(?, ?, ?, ?, ?)}"))
                .thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(statement).setString(1, "Administr");
        verify(statement).setString(2, "ACTIVO");
        verify(statement).setInt(3, 2);
        verify(statement).setInt(4, 10);
        verify(statement).registerOutParameter(5, java.sql.Types.BIGINT);
    }

    @Test
    void mapperLeeCantidadPermisosComoLong() throws Exception {
        when(resultSet.getLong("id")).thenReturn(7L);
        when(resultSet.getString("nombre")).thenReturn("ADMIN");
        when(resultSet.getString("estado")).thenReturn("ACTIVO");
        when(resultSet.getLong("cantidad_permisos")).thenReturn(3L);

        Field field = PerfilConsultaJdbcAdapter.class.getDeclaredField("MAPPER");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        RowMapper<PerfilAdministracion> mapper = (RowMapper<PerfilAdministracion>) field.get(null);

        PerfilAdministracion perfil = mapper.mapRow(resultSet, 0);

        verify(resultSet).getLong("cantidad_permisos");
        assertThat(perfil).isEqualTo(new PerfilAdministracion(7L, "ADMIN", "ACTIVO", 3L));
    }

    @Test
    void listadoSinResultadosConservaTotalCero() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(), "Total", 0L));

        Pagina<PerfilAdministracion> pagina = adapter.findPage(null, null, 1, 20);

        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.total()).isZero();
    }
}
