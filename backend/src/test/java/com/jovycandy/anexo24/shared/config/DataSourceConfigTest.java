package com.jovycandy.anexo24.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de los gestores transaccionales explícitos. */
@ExtendWith(MockitoExtension.class)
class DataSourceConfigTest {

    @Mock
    private DataSource primaryDataSource;

    @Mock
    private DataSource appDataSource;

    private DataSourceConfig config;

    @BeforeEach
    void setUp() {
        config = new DataSourceConfig();
    }

    @Test
    void transactionManagerUsaOrigenPrimario() {
        JdbcTransactionManager transactionManager = config.transactionManager(primaryDataSource);

        assertThat(transactionManager.getDataSource()).isSameAs(primaryDataSource);
    }

    @Test
    void appTransactionManagerUsaOrigenSecundario() {
        JdbcTransactionManager appTransactionManager = config.appTransactionManager(appDataSource);

        assertThat(appTransactionManager.getDataSource()).isSameAs(appDataSource);
    }

    @Test
    void gestionaOrigenesDistintos() {
        JdbcTransactionManager transactionManager = config.transactionManager(primaryDataSource);
        JdbcTransactionManager appTransactionManager = config.appTransactionManager(appDataSource);

        assertThat(transactionManager).isNotSameAs(appTransactionManager);
        assertThat(transactionManager.getDataSource())
                .isNotSameAs(appTransactionManager.getDataSource());
    }

    @Test
    void excepcionEnAppTransactionManagerProvocaRollback() throws Exception {
        Connection conexion = mock(Connection.class);
        when(appDataSource.getConnection()).thenReturn(conexion);
        when(conexion.getAutoCommit()).thenReturn(true);
        JdbcTransactionManager appTransactionManager = config.appTransactionManager(appDataSource);
        TransactionTemplate plantilla = new TransactionTemplate(appTransactionManager);

        assertThatThrownBy(() -> plantilla.execute(status -> {
            throw new IllegalStateException("operación fallida");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("operación fallida");

        verify(conexion).setAutoCommit(false);
        verify(conexion).rollback();
        verify(conexion, never()).commit();
        verify(conexion).setAutoCommit(true);
    }
}