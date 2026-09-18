package com.jovycandy.anexo24.shared.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint técnico de verificación de salud del sistema.
 *
 * <p>Valida la cadena completa HTTP → Spring Boot → SQL Server para las
 * dos fuentes de datos configuradas.</p>
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final JdbcTemplate moduleCJdbcTemplate;
    private final JdbcTemplate applicationJdbcTemplate;

    /**
     * Constructor con las plantillas de ambos orígenes de datos.
     *
     * @param moduleCJdbcTemplate plantilla del Módulo C
     * @param applicationJdbcTemplate plantilla del esquema app24
     */
    public SystemStatusController(
            JdbcTemplate moduleCJdbcTemplate,
            @Qualifier("appJdbcTemplate") JdbcTemplate applicationJdbcTemplate) {
        this.moduleCJdbcTemplate = moduleCJdbcTemplate;
        this.applicationJdbcTemplate = applicationJdbcTemplate;
    }

    /**
     * Reporta el estado técnico del sistema y sus bases de datos.
     *
     * @return estado HTTP 200 si ambas fuentes responden; 503 en otro caso
     */
    @GetMapping("/status")
    public ResponseEntity<SystemStatus> status() {
        String moduleC = checkDatabase(moduleCJdbcTemplate);
        String application = checkDatabase(applicationJdbcTemplate);
        boolean healthy = "UP".equals(moduleC) && "UP".equals(application);

        SystemStatus body = new SystemStatus(
                "anexo24",
                healthy ? "UP" : "DOWN",
                moduleC,
                application);
        HttpStatus status = healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Ejecuta una consulta mínima contra una fuente de datos.
     *
     * @param jdbcTemplate plantilla que se desea comprobar
     * @return {@code UP} si responde o {@code DOWN} si falla
     */
    private String checkDatabase(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (DataAccessException exception) {
            return "DOWN";
        }
    }
}
