package com.quinielas.del.canario.api.entity;

public enum EstadoJugada {
    /** Jugada registrada, esperando confirmación de pago */
    CREADA,
    /** Pago pendiente de confirmación (puede ser por validación manual o proceso automático) */
    PENDIENTE_VALIDACION,
    /** Pago confirmado, jugada participando en la quiniela */
    ACTIVA,
    /** Jugada rechazada (pago no confirmado o incidencia) */
    RECHAZADA,
    /** Jugada expirada (la quiniela cerró sin confirmar pago) */
    EXPIRADA,
    /** Jugada evaluada y cerrada al finalizar la quiniela */
    FINALIZADA
}

