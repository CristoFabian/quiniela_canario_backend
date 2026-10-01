package com.quinielas.del.canario.api.entity;

/**
 * Categoría de una regla del juego, usada para agrupar/ordenar las reglas
 * al mostrarlas al jugador (por ejemplo, en una pantalla de "Reglas del juego").
 */
public enum CategoriaRegla {
    /** Reglas generales: estructura de la quiniela, cómo participar, etc. */
    GENERAL,
    /** Reglas sobre cómo se otorgan los puntos por cada tipo de pronóstico. */
    PUNTUACION,
    /** Reglas sobre la cadena de desempate al determinar ganadores. */
    DESEMPATE,
    /** Reglas sobre el reparto de premios/bolsa acumulada. */
    PREMIOS,
    /** Reglas sobre pagos, comprobantes y plazos. */
    PAGOS
}

