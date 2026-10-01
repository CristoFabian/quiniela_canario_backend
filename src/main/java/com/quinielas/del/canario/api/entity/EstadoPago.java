package com.quinielas.del.canario.api.entity;

public enum EstadoPago {
    /** Pago registrado por el jugador, pendiente de revisión del administrador */
    PENDIENTE,
    /** Pago verificado y aprobado; las jugadas asociadas pasan a ACTIVA */
    APROBADO,
    /** Pago rechazado; las jugadas asociadas regresan a CREADA para reintentar */
    RECHAZADO,
    /** Estado administrativo posterior a la revisión, reservado para pagos cuya compensación ya fue resuelta. */
    VENCIDO
}

