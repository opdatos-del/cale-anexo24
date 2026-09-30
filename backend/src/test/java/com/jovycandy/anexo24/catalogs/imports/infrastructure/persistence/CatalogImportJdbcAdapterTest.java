package com.jovycandy.anexo24.catalogs.imports.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Asegura que cada catálogo usa su command SP versionado y no SQL inline. */
@ExtendWith(MockitoExtension.class)
class CatalogImportJdbcAdapterTest {
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    private CatalogImportJdbcAdapter adapter;

    @BeforeEach
    void setUp() { adapter = new CatalogImportJdbcAdapter(jdbcTemplate); }

    @Test
    void materialUsaElCommandVersionadoConOnceParametros() throws Exception {
        assertCommand(CatalogImportType.MATERIAL, "app24.APP24_C_CATALOGO_MATERIAL_CARGA_CREAR");
    }

    @Test
    void productoUsaElCommandVersionadoConOnceParametros() throws Exception {
        assertCommand(CatalogImportType.PRODUCTO, "app24.APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR");
    }

    private void assertCommand(CatalogImportType type, String procedure) throws Exception {
        when(jdbcTemplate.call(any(CallableStatementCreator.class), anyList())).thenReturn(Map.of("CargaId", 9L));
        when(connection.prepareCall("{call " + procedure + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(statement);

        assertThat(adapter.save(new CatalogImportArchivo(type, "synthetic.xlsx", "a".repeat(64), "V1",
                List.of("clave"), List.of(), List.of(), false), 7L, "corr-1")).isEqualTo(9L);

        ArgumentCaptor<CallableStatementCreator> creator = ArgumentCaptor.forClass(CallableStatementCreator.class);
        verify(jdbcTemplate).call(creator.capture(), anyList());
        creator.getValue().createCallableStatement(connection);
        verify(connection).prepareCall("{call " + procedure + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
        verify(statement).setLong(3, 7L);
        verify(statement).setString(8, "corr-1");
        verify(statement).registerOutParameter(11, java.sql.Types.BIGINT);
    }
}
