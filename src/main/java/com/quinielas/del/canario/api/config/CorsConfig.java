package com.quinielas.del.canario.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (frontend) → configurable en application.properties
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers que puede enviar el frontend
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Permite que el navegador lea el header Authorization en la respuesta
        config.setExposedHeaders(List.of("Authorization"));

        // Permite el envío de cookies/credenciales entre dominios
        config.setAllowCredentials(true);

        // Tiempo en segundos que el navegador cachea la respuesta del preflight (OPTIONS)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

