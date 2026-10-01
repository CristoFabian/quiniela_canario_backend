package com.quinielas.del.canario.api.event;

import java.math.BigDecimal;

/**
 * Se publica por cada jugada ganadora al cerrar una quiniela.
 * Disparado por {@code CierreQuinielaService.cerrarQuiniela}.
 */
public class JugadaGanadoraEvent {

    private final Long       ganadorId;
    private final Long       usuarioId;
    private final String     quinielaNombre;
    private final BigDecimal montoPremio;

    public JugadaGanadoraEvent(Long ganadorId, Long usuarioId, String quinielaNombre, BigDecimal montoPremio) {
        this.ganadorId      = ganadorId;
        this.usuarioId      = usuarioId;
        this.quinielaNombre = quinielaNombre;
        this.montoPremio    = montoPremio;
    }

    public Long getGanadorId()        { return ganadorId; }
    public Long getUsuarioId()        { return usuarioId; }
    public String getQuinielaNombre() { return quinielaNombre; }
    public BigDecimal getMontoPremio() { return montoPremio; }
}
