package com.quinielas.del.canario.api.entity;

public enum EstadoQuiniela {
    /** Recién creada, aún no publicada */
    CREADA,
    /** Abierta para que los jugadores participen */
    ABIERTA,
    /** Los partidos ya comenzaron */
    EN_JUEGO,
    /** Todos los partidos terminaron */
    FINALIZADA
}

