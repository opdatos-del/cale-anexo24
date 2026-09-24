package com.jovycandy.anexo24.administration.profiles.infrastructure.persistence;

import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas de bindings, JSON y traducción de errores del adaptador de commands. */
@ExtendWith(MockitoExtension.class)
class PerfilComandoJdbcAdapterTest {
    @Mock private JdbcTemplate appJdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private PerfilComandoJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PerfilComandoJdbcAdapter(appJdbcTemplate);
    }

    @Test
    void crearEnlazaNombreYDevuelveIdentificadorDeSalida() throws Exception {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("NuevoPerfilId", 9L));

        assertThat(adapter.crear("OPERACION")).isEqualTo(9L);

        CallableStatementCreator creator = capturarCreator();
        when(connection.prepareCall("{call app24.APP24_C_PERFIL_CREAR(?, ?)}")).thenReturn(statement);
        creator.createCallableStatement(connection);
        verify(statement).setString(1, "OPERACION");
        verify(statement).registerOutParameter(2, java.sql.Types.BIGINT);
    }

    @Test
    void salidaCrearNulaOCeroSeRechaza() {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(java.util.Collections.singletonMap("NuevoPerfilId", null));
        assertThatThrownBy(() -> adapter.crear("OPERACION")).isInstanceOf(IllegalStateException.class);
        reset(appJdbcTemplate);
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of("NuevoPerfilId", 0L));
        assertThatThrownBy(() -> adapter.crear("OPERACION")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cambiarEstadoYReemplazarPermisosEnlazanFechaYJsonUnicode() throws Exception {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of());
        LocalDate fecha = LocalDate.of(2026, 9, 24);

        adapter.cambiarEstado(7L, "INACTIVO", fecha);
        CallableStatementCreator estadoCreator = capturarCreator();
        when(connection.prepareCall("{call app24.APP24_C_PERFIL_CAMBIAR_ESTADO(?, ?, ?)}")).thenReturn(statement);
        estadoCreator.createCallableStatement(connection);
        verify(statement).setLong(1, 7L);
        verify(statement).setString(2, "INACTIVO");
        verify(statement).setDate(3, java.sql.Date.valueOf(fecha));
        clearInvocations(statement);

        adapter.reemplazarPermisos(7L, List.of(2L, 10L), fecha);
        CallableStatementCreator permisosCreator = capturarCreator();
        when(connection.prepareCall("{call app24.APP24_C_PERFIL_REEMPLAZAR_PERMISOS(?, ?, ?)}")).thenReturn(statement);
        permisosCreator.createCallableStatement(connection);
        verify(statement).setLong(1, 7L);
        verify(statement).setNString(2, "[2,10]");
        verify(statement).setDate(3, java.sql.Date.valueOf(fecha));
    }

    @Test
    void serializaConjuntosVaciosYUnitariosComoArraysJson() throws Exception {
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of());
        CallableStatementCreator emptyCreator;
        adapter.reemplazarPermisos(7L, List.of(), LocalDate.of(2026, 9, 24));
        ArgumentCaptor<CallableStatementCreator> emptyCaptor = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(emptyCaptor.capture(), anyList());
        emptyCreator = emptyCaptor.getValue();
        when(connection.prepareCall("{call app24.APP24_C_PERFIL_REEMPLAZAR_PERMISOS(?, ?, ?)}")).thenReturn(statement);
        emptyCreator.createCallableStatement(connection);
        verify(statement).setNString(2, "[]");

        reset(appJdbcTemplate, statement);
        when(appJdbcTemplate.call(any(), anyList())).thenReturn(Map.of());
        adapter.reemplazarPermisos(7L, List.of(1L), LocalDate.of(2026, 9, 24));
        ArgumentCaptor<CallableStatementCreator> oneCaptor = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate).call(oneCaptor.capture(), anyList());
        oneCaptor.getValue().createCallableStatement(connection);
        verify(statement).setNString(2, "[1]");
    }

    @Test
    void jsonNoSerializableSeConvierteEnSolicitudInvalidaSinAccederAJdbc() {
        List<Object> ciclica = new ArrayList<>();
        ciclica.add(ciclica);
        @SuppressWarnings("unchecked")
        List<Long> actividadIds = (List<Long>) (List<?>) ciclica;

        assertThatThrownBy(() -> adapter.reemplazarPermisos(7L, actividadIds, LocalDate.now()))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("No fue posible serializar las actividades.");
        verifyNoInteractions(appJdbcTemplate);
    }

    @Test
    void codigosSqlConocidosSeTraducenADominioYElDesconocidoSePreserva() {
        assertThatThrownBy(() -> fallaConCodigo(51102)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fallaConCodigo(51104)).isInstanceOf(RecursoDuplicadoException.class);
        assertThatThrownBy(() -> fallaConCodigo(2601)).isInstanceOf(RecursoDuplicadoException.class);
        assertThatThrownBy(() -> fallaConCodigo(2627)).isInstanceOf(RecursoDuplicadoException.class);
        assertThatThrownBy(() -> fallaConCodigo(51107)).isInstanceOf(EstadoIncompatibleException.class);
        assertThatThrownBy(() -> fallaConCodigo(51108)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> fallaConCodigo(51150)).isInstanceOf(IllegalStateException.class)
                .hasMessage("El command afectó un número inconsistente de filas");
        assertThatThrownBy(() -> fallaConCodigo(59999)).isInstanceOf(DataAccessResourceFailureException.class);
    }

    private CallableStatementCreator capturarCreator() {
        ArgumentCaptor<CallableStatementCreator> captor = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(appJdbcTemplate, atLeastOnce()).call(captor.capture(), anyList());
        return captor.getAllValues().getLast();
    }

    private void fallaConCodigo(int codigo) {
        reset(appJdbcTemplate);
        SQLException sqlException = new SQLException("fallo", "S0001", codigo);
        when(appJdbcTemplate.call(any(), anyList()))
                .thenThrow(new DataAccessResourceFailureException("fallo", sqlException));
        adapter.actualizarNombre(7L, "NOMBRE");
    }
}
