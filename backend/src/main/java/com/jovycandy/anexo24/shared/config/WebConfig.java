package com.jovycandy.anexo24.shared.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración web compartida.
 *
 * <p>Permite los orígenes del frontend de desarrollo (cualquier puerto
 * local, ya que {@code ng serve} cambia de puerto si 4200 está ocupado).
 * Expone un {@link CorsConfigurationSource} que también consume Spring
 * Security, de modo que el preflight {@code OPTIONS} se resuelve antes
 * de la autorización. En producción debe definirse
 * {@code app.cors.allowed-origin-patterns} con el dominio real.</p>
 */
@Configuration
public class WebConfig {

    /** Patrones de origen permitidos (por defecto, solo desarrollo local). */
    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private List<String> allowedOriginPatterns;

    /**
     * Política CORS aplicada tanto por Spring Security como por MVC.
     *
     * @return fuente de configuración CORS para {@code /api/v1/**}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }
}