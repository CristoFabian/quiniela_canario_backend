package com.quinielas.del.canario.api.event;

import com.quinielas.del.canario.api.entity.EstadoPago;

/**
 * Se publica cuando el administrador aprueba o rechaza un pago.
 * Disparado por {@code PagoService.validarPago}.
 */
public class PagoValidadoEvent {

    private final Long        pagoId;
    private final Long        usuarioId;
    private final EstadoPago  nuevoEstado;
    private final String      quinielaNombre;

    public PagoValidadoEvent(Long pagoId, Long usuarioId, EstadoPago nuevoEstado, String quinielaNombre) {
        this.pagoId         = pagoId;
        this.usuarioId      = usuarioId;
        this.nuevoEstado    = nuevoEstado;
        this.quinielaNombre = quinielaNombre;
    }

    public Long getPagoId()             { return pagoId; }
    public Long getUsuarioId()          { return usuarioId; }
    public EstadoPago getNuevoEstado()  { return nuevoEstado; }
    public String getQuinielaNombre()   { return quinielaNombre; }
}
