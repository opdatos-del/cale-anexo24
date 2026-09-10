package com.jovycandy.anexo24.shared.api;

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
 * <p>Valida la cadena completa HTTP → Spring Boot → SQL Server,
 * respondiendo el estado de la aplicación y de la base de datos.</p>
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    /**
     * Plantilla JDBC para verificar la conectividad con la base de datos.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param jdbcTemplate plantilla JDBC administrada por Spring
     */
    public SystemStatusController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Reporta el estado técnico del sistema.
     *
     * <p>Ejecuta {@code SELECT 1} contra la base de datos. Si la consulta
     * falla, la base de datos se reporta como "DOWN".</p>
     *
     * @return estado del sistema con HTTP 200 (OK) si la base de datos
     *         responde, o 503 (Service Unavailable) si no
     */
    @GetMapping("/status")
    public ResponseEntity<SystemStatus> status() {
        String database = "DOWN";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            database = "UP";
        } catch (DataAccessException e) {
            // base de datos inalcanzable -> el estado permanece en "DOWN"
        }
        SystemStatus body = new SystemStatus("anexo24", "UP", database);
        HttpStatus status = "UP".equals(database) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }
}