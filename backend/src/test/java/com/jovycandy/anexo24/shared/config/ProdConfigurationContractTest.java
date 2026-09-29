package com.jovycandy.anexo24.shared.config;

import com.jovycandy.anexo24.security.JwtTokenService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas del contrato de propiedades obligatorio para el perfil productivo. */
class ProdConfigurationContractTest {

    private static final String JWT_SECRET = "prod-contract-secret-with-at-least-32-bytes";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(DataSourceConfig.class, JwtTokenService.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:sqlserver://primary.example:1433;databaseName=CALE_IMMEX",
                    "spring.datasource.username=primary_user",
                    "spring.datasource.password=primary_password",
                    "DB_URL=jdbc:sqlserver://primary.example:1433;databaseName=CALE_IMMEX",
                    "DB_USERNAME=primary_user",
                    "DB_PASSWORD=primary_password",
                    "spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    "spring.datasource.hikari.maximum-pool-size=30",
                    "spring.datasource.hikari.minimum-idle=5",
                    "app.datasource.url=jdbc:sqlserver://app.example:1433;databaseName=ANEXO24",
                    "app.datasource.username=app_user",
                    "app.datasource.password=app_password",
                    "APP_DB_URL=jdbc:sqlserver://app.example:1433;databaseName=ANEXO24",
                    "APP_DB_USERNAME=app_user",
                    "APP_DB_PASSWORD=app_password",
                    "app.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    "app.datasource.hikari.maximum-pool-size=10",
                    "app.datasource.hikari.minimum-idle=2",
                    "jwt.secret=" + JWT_SECRET,
                    "jwt.expiration-minutes=480");

    @Test
    void construyeLosDosOrigenesJwtYPoolsConContratoProd() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DataSourceProperties primaryProperties = context.getBean("primaryDataSourceProperties",
                    DataSourceProperties.class);
            DataSourceProperties appProperties = context.getBean("appDataSourceProperties",
                    DataSourceProperties.class);
            HikariDataSource primaryDataSource = context.getBean("primaryDataSource", HikariDataSource.class);
            HikariDataSource appDataSource = context.getBean("appDataSource", HikariDataSource.class);

            assertThat(primaryProperties.getUrl()).contains("CALE_IMMEX");
            assertThat(appProperties.getUrl()).contains("ANEXO24");
            assertThat(primaryDataSource.getMaximumPoolSize()).isEqualTo(30);
            assertThat(primaryDataSource.getMinimumIdle()).isEqualTo(5);
            assertThat(appDataSource.getMaximumPoolSize()).isEqualTo(10);
            assertThat(appDataSource.getMinimumIdle()).isEqualTo(2);
            assertThat(context.getBean(JwtTokenService.class).expirationMinutes()).isEqualTo(480);
        });
    }

    @Test
    void fallaCuandoFaltaJwtSecret() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(DataSourceConfig.class, JwtTokenService.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:sqlserver://primary.example:1433;databaseName=CALE_IMMEX",
                        "spring.datasource.username=primary_user",
                        "spring.datasource.password=primary_password",
                        "DB_URL=jdbc:sqlserver://primary.example:1433;databaseName=CALE_IMMEX",
                        "DB_USERNAME=primary_user",
                        "DB_PASSWORD=primary_password",
                        "spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                        "app.datasource.url=jdbc:sqlserver://app.example:1433;databaseName=ANEXO24",
                        "app.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                        "APP_DB_URL=jdbc:sqlserver://app.example:1433;databaseName=ANEXO24",
                        "APP_DB_USERNAME=app_user",
                        "APP_DB_PASSWORD=app_password",
                        "jwt.expiration-minutes=480")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Configuración obligatoria ausente: jwt.secret (JWT_SECRET)");
                });
    }

    @Test
    void fallaCuandoFaltaAppDbUrl() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(DataSourceConfig.class, JwtTokenService.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:sqlserver://primary.example:1433;databaseName=CALE_IMMEX",
                        "spring.datasource.username=primary_user",
                        "spring.datasource.password=primary_password",
                        "DB_URL=jdbc:sqlserver://primary.example:1433;databaseName=CALE_IMMEX",
                        "DB_USERNAME=primary_user",
                        "DB_PASSWORD=primary_password",
                        "spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                        "app.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                        "jwt.secret=" + JWT_SECRET,
                        "jwt.expiration-minutes=480")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Configuración obligatoria ausente: app.datasource.url");
                });
    }
}
