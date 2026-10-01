package com.quinielas.del.canario.api.event;

/**
 * Se publica cuando el administrador sube el comprobante de pago de un premio
 * (el premio pasa a estado PAGADO). Disparado por {@code PremioService.subirComprobantePremio}.
 */
public class PremioPagadoEvent {

    private final Long ganadorId;
    private final Long usuarioId;
    private final String quinielaNombre;

    public PremioPagadoEvent(Long ganadorId, Long usuarioId, String quinielaNombre) {
        this.ganadorId      = ganadorId;
        this.usuarioId      = usuarioId;
        this.quinielaNombre = quinielaNombre;
    }

    public Long getGanadorId()        { return ganadorId; }
    public Long getUsuarioId()        { return usuarioId; }
    public String getQuinielaNombre() { return quinielaNombre; }
}
