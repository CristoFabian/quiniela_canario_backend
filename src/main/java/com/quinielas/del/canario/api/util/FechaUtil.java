package com.quinielas.del.canario.api.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para formatear fechas de forma consistente en toda la API.
 * Usado principalmente en mensajes de error y respuestas de texto.
 * El mismo formato se aplica globalmente a la serialización JSON vía JacksonConfig.
 */
public final class FechaUtil {

    public static final String PATRON = "dd/MM/yyyy HH:mm";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATRON);

    private FechaUtil() {}

    /**
     * Formatea un {@code LocalDateTime} con el patrón {@code dd/MM/yyyy HH:mm}.
     * Ejemplo: 2026-04-10T20:00:00  →  10/04/2026 20:00
     *
     * @param fecha fecha a formatear; si es {@code null} devuelve "N/D"
     */
    public static String format(LocalDateTime fecha) {
        return fecha != null ? fecha.format(FORMATTER) : "N/D";
    }
}

