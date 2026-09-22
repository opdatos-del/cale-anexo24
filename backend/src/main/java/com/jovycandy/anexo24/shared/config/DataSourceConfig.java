package com.jovycandy.anexo24.shared.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Configuración de los dos orígenes de datos de la aplicación.
 *
 * <p>El origen primario conecta al Módulo C (CALE_IMMEX) usando las
 * variables {@code DB_*}; el secundario conecta al esquema
 * complementario {@code app24} en ANEXO24_DEV con {@code APP_DB_*}
 * (ver {@code docs/03-diseno/modelo-datos.md}).</p>
 */
@Configuration
public class DataSourceConfig {

    /**
     * Propiedades del origen de datos primario (Módulo C).
     *
     * @return propiedades enlazadas desde {@code spring.datasource.*}
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Origen de datos primario hacia CALE_IMMEX.
     *
     * @param properties propiedades del origen primario
     * @return origen de datos del Módulo C
     */
    @Bean
    @Primary
    public DataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    /**
     * Plantilla JDBC del Módulo C.
     *
     * @param dataSource origen de datos primario
     * @return plantilla JDBC para consultas del Módulo C
     */
    @Bean
    public JdbcTemplate jdbcTemplate(
            @Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Propiedades del origen de datos de la aplicación.
     *
     * @return propiedades enlazadas desde {@code app.datasource.*}
     */
    @Bean
    @ConfigurationProperties("app.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Origen de datos secundario hacia ANEXO24_DEV.
     *
     * @param properties propiedades del origen secundario
     * @return origen de datos del esquema {@code app24}
     */
    @Bean
    public DataSource appDataSource(
            @Qualifier("appDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    /**
     * Plantilla JDBC del esquema complementario.
     *
     * @param dataSource origen de datos secundario
     * @return plantilla JDBC para consultas del esquema {@code app24}
     */
    @Bean
    public JdbcTemplate appJdbcTemplate(
            @Qualifier("appDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Gestor transaccional del origen primario (Módulo C).
     *
     * <p>Reemplaza al auto-configurado; los comandos del Módulo C lo
     * usan por defecto con {@code @Transactional}.</p>
     *
     * @param dataSource origen de datos primario
     * @return gestor transaccional JDBC del Módulo C
     */
    @Bean
    @Primary
    public JdbcTransactionManager transactionManager(
            @Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    /**
     * Gestor transaccional del esquema complementario.
     *
     * <p>Los comandos de administración futura lo referencian
     * explícitamente con {@code @Transactional(transactionManager = "appTransactionManager")}
     * para no mezclar transacciones entre orígenes de datos.</p>
     *
     * @param dataSource origen de datos secundario
     * @return gestor transaccional JDBC del esquema {@code app24}
     */
    @Bean
    public JdbcTransactionManager appTransactionManager(
            @Qualifier("appDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}