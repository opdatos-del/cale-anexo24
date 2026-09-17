package com.jovycandy.anexo24.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración web compartida.
 *
 * <p>Permite el origen del frontend de desarrollo (Angular en
 * {@code http://localhost:4200}) mientras se definen los cambios de
 * origen permitidos por ambiente.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registra políticas CORS para el desarrollo local.
     *
     * @param registry registro de configuración CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}