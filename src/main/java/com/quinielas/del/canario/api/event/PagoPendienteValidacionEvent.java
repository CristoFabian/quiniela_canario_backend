package com.quinielas.del.canario.api.event;

import java.math.BigDecimal;

/**
 * Se publica cuando un jugador registra un pago que queda en estado PENDIENTE
 * (requiere validación manual del administrador).
 * Disparado por {@code PagoService.crearPago}.
 */
public class PagoPendienteValidacionEvent {

    private final Long        pagoId;
    private final String      usuarioNombre;
    private final BigDecimal  monto;

    public PagoPendienteValidacionEvent(Long pagoId, String usuarioNombre, BigDecimal monto) {
        this.pagoId        = pagoId;
        this.usuarioNombre = usuarioNombre;
        this.monto         = monto;
    }

    public Long getPagoId()             { return pagoId; }
    public String getUsuarioNombre()    { return usuarioNombre; }
    public BigDecimal getMonto()        { return monto; }
}
