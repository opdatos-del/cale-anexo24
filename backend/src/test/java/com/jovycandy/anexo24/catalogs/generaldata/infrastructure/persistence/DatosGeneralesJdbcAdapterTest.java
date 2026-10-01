package com.jovycandy.anexo24.catalogs.generaldata.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del adapter singleton de Datos Generales. */
@ExtendWith(MockitoExtension.class)
class DatosGeneralesJdbcAdapterTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private DatosGeneralesJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DatosGeneralesJdbcAdapter(jdbcTemplate);
    }

    @Test
    void devuelveVacioCuandoElSpNoRetornaFilas() {
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(adapter.find()).isEmpty();
    }

    @Test
    void devuelveLaEntidadCuandoElSpRetornaUnaFila() {
        DatosGenerales datos = new DatosGenerales("Empresa", "RFC", "IMMEX", "Calle, Municipio");
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class))).thenReturn(List.of(datos));

        assertThat(adapter.find()).contains(datos);
    }

    @Test
    void rechazaCardinalidadMayorQueUnoSinElegirFilaArbitraria() {
        DatosGenerales primera = new DatosGenerales("Empresa 1", "RFC1", "IMMEX1", "Domicilio 1");
        DatosGenerales segunda = new DatosGenerales("Empresa 2", "RFC2", "IMMEX2", "Domicilio 2");
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class))).thenReturn(List.of(primera, segunda));

        assertThatThrownBy(() -> adapter.find())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La fuente DatosGenerales debe contener como máximo un registro.");
    }

    @Test
    void mapperLeeSoloLaProyeccionContratada() throws Exception {
        when(resultSet.getString("razon_social")).thenReturn("Empresa");
        when(resultSet.getString("rfc")).thenReturn("RFC");
        when(resultSet.getString("registro_immex")).thenReturn("IMMEX");
        when(resultSet.getString("domicilio_fiscal")).thenReturn("Calle, Municipio");

        DatosGenerales datos = DatosGeneralesJdbcAdapter.MAPPER.mapRow(resultSet, 0);

        assertThat(datos).isEqualTo(new DatosGenerales("Empresa", "RFC", "IMMEX", "Calle, Municipio"));
    }
}
