package com.jovycandy.anexo24.shared.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
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
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        validateRequiredProperties("spring.datasource", properties);
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
    @ConfigurationProperties("app.datasource.hikari")
    public DataSource appDataSource(
            @Qualifier("appDataSourceProperties") DataSourceProperties properties) {
        validateRequiredProperties("app.datasource", properties);
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

    /**
     * Fuerza la resolución de todas las variables obligatorias del perfil productivo.
     *
     * @return marcador de configuración validada
     */
    @Bean
    @Profile("prod")
    public Object productionConfigurationRequired(
            @Value("${DB_URL}") String dbUrl,
            @Value("${DB_USERNAME}") String dbUsername,
            @Value("${DB_PASSWORD}") String dbPassword,
            @Value("${APP_DB_URL}") String appDbUrl,
            @Value("${APP_DB_USERNAME}") String appDbUsername,
            @Value("${APP_DB_PASSWORD}") String appDbPassword,
            @Value("${JWT_SECRET}") String jwtSecret,
            @Value("${JWT_EXPIRATION_MINUTES}") String jwtExpirationMinutes,
            @Value("${APP_CORS_ALLOWED_ORIGINS}") String corsAllowedOrigins) {
        return new Object();
    }

    /**
     * Valida propiedades de conexión antes de crear pools que fallarían de forma diferida.
     *
     * @param prefix prefijo de propiedades del origen
     * @param properties propiedades enlazadas del origen
     */
    private static void validateRequiredProperties(String prefix, DataSourceProperties properties) {
        validateRequiredProperty(prefix + ".url", properties.getUrl());
        validateRequiredProperty(prefix + ".username", properties.getUsername());
        validateRequiredProperty(prefix + ".password", properties.getPassword());
    }

    /**
     * Rechaza valores vacíos o placeholders que Spring no pudo resolver.
     *
     * @param property nombre de propiedad requerido
     * @param value valor recibido
     */
    private static void validateRequiredProperty(String property, String value) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException("Configuración obligatoria ausente: " + property);
        }
    }

}
