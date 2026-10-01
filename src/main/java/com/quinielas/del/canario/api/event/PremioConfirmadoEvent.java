package com.quinielas.del.canario.api.event;

/**
 * Se publica cuando el jugador confirma que ya recibió su premio (comprobante verificado).
 * Disparado por {@code PremioService.confirmarRecepcion}. Notifica a los administradores.
 */
public class PremioConfirmadoEvent {

    private final Long   ganadorId;
    private final String usuarioNombre;
    private final String quinielaNombre;

    public PremioConfirmadoEvent(Long ganadorId, String usuarioNombre, String quinielaNombre) {
        this.ganadorId      = ganadorId;
        this.usuarioNombre  = usuarioNombre;
        this.quinielaNombre = quinielaNombre;
    }

    public Long getGanadorId()        { return ganadorId; }
    public String getUsuarioNombre()  { return usuarioNombre; }
    public String getQuinielaNombre() { return quinielaNombre; }
}
