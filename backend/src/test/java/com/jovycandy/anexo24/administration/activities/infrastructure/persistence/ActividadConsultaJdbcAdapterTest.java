package com.jovycandy.anexo24.administration.activities.infrastructure.persistence;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
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

/** Pruebas del adaptador read-only de actividades mediante SP. */
@ExtendWith(MockitoExtension.class)
class ActividadConsultaJdbcAdapterTest {

    @Mock private JdbcTemplate appJdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    @Mock private ResultSet resultSet;
    private ActividadConsultaJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ActividadConsultaJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void listadoInvocaSoloElProcedimientoAlmacenado() throws Exception {
        ActividadAdministracion actividad = new ActividadAdministracion(
                1L, "PERFILES_ADMINISTRAR", "Administrar perfiles", "/perfiles", "ADMINISTRAR");
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("items", List.of(actividad)));

        List<ActividadAdministracion> resultado = adapter.findAll();

        assertThat(resultado).containsExactly(actividad);
        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(creator.capture(), anyList());
        when(connection.prepareCall("{call app24.APP24_Q_ACTIVIDADES_LISTAR}")).thenReturn(statement);
        creator.getValue().createCallableStatement(connection);
        verify(connection).prepareCall("{call app24.APP24_Q_ACTIVIDADES_LISTAR}");
    }

    @Test
    void mapperLeeLosCamposDelContrato() throws Exception {
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("clave")).thenReturn("PERFILES_ADMINISTRAR");
        when(resultSet.getString("nombre")).thenReturn("Administrar perfiles");
        when(resultSet.getString("recurso")).thenReturn("/perfiles");
        when(resultSet.getString("accion")).thenReturn("ADMINISTRAR");

        Field field = ActividadConsultaJdbcAdapter.class.getDeclaredField("MAPPER");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        RowMapper<ActividadAdministracion> mapper = (RowMapper<ActividadAdministracion>) field.get(null);

        ActividadAdministracion actividad = mapper.mapRow(resultSet, 0);

        assertThat(actividad).isEqualTo(new ActividadAdministracion(
                1L, "PERFILES_ADMINISTRAR", "Administrar perfiles", "/perfiles", "ADMINISTRAR"));
    }

    @Test
    void listadoSinResultadosDevuelveListaVacia() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of());

        assertThat(adapter.findAll()).isEmpty();
    }
}
