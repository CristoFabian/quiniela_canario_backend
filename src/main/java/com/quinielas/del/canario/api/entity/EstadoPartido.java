package com.quinielas.del.canario.api.entity;

public enum EstadoPartido {
    /** Partido aún no comenzado */
    PENDIENTE,
    /** Partido en curso */
    EN_JUEGO,
    /** Partido terminado */
    FINALIZADO,
    /** Partido suspendido definitivamente */
    SUSPENDIDO,
    /** Partido pospuesto a otra fecha */
    POSPUESTO
}

