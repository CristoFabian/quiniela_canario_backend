package com.quinielas.del.canario.api.entity;

/**
 * Catálogo de eventos de negocio que generan una {@link Notificacion}.
 * El prefijo indica el rol destinatario habitual (no se aplica en código,
 * es solo documentación para quien agregue nuevos tipos).
 */
public enum TipoNotificacion {

    // ─── Administrador ─────────────────────────────────────────────
    /** Existe un pago nuevo en estado PENDIENTE esperando validación. */
    PAGO_PENDIENTE_VALIDACION,
    /** Un jugador confirmó la recepción del comprobante de pago de su premio. */
    PREMIO_CONFIRMADO_JUGADOR,

    // ─── Jugador ────────────────────────────────────────────────────
    /** Una nueva quiniela pasó a estado ABIERTA y ya admite jugadas. */
    QUINIELA_DISPONIBLE,
    /** Falta ~1 día para que cierre el registro de una quiniela ABIERTA. */
    QUINIELA_PROXIMA_CERRAR,
    /** El administrador aprobó un pago del jugador. */
    PAGO_APROBADO,
    /** El administrador rechazó un pago del jugador. */
    PAGO_RECHAZADO,
    /** Una jugada del jugador resultó ganadora al cerrar la quiniela. */
    JUGADA_GANADORA,
    /** El administrador subió el comprobante de pago del premio del jugador. */
    PREMIO_PAGADO,
    /** Se agregó una nueva regla del juego. */
    REGLA_AGREGADA,
    /** Se editó una regla del juego existente. */
    REGLA_EDITADA,
    /** Se eliminó/desactivó una regla del juego. */
    REGLA_ELIMINADA,
    /** Una regla del juego previamente desactivada volvió a estar activa. */
    REGLA_REACTIVADA
}
