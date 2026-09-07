package com.quinielas.del.canario.api.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Configuración global de Jackson para formatear fechas en las respuestas JSON.
 * Solo aplica a la serialización (respuestas); los request bodies siguen aceptando
 * el formato ISO estándar (yyyy-MM-dd'T'HH:mm).
 *
 * Formato de salida: dd/MM/yyyy HH:mm
 * Ejemplo: 2026-04-10T20:00:00  →  10/04/2026 20:00
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeFormatCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(com.quinielas.del.canario.api.util.FechaUtil.PATRON);

        return builder -> builder
                .serializers(new LocalDateTimeSerializer(formatter))
                .featuresToDisable(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                );
    }
}
