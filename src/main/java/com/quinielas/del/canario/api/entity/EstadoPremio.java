package com.quinielas.del.canario.api.entity;

/**
 * Estado de la entrega del premio monetario a un ganador de quiniela.
 */
public enum EstadoPremio {
    /** Premio calculado al cierre, aún no se ha entregado ni subido comprobante. */
    PENDIENTE,
    /** El administrador subió el comprobante de la transferencia/pago del premio. */
    PAGADO,
    /** El jugador confirmó que recibió el premio (verificó el comprobante/dinero). */
    CONFIRMADO
}

