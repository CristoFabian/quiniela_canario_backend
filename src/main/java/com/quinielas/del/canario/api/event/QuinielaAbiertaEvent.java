package com.quinielas.del.canario.api.event;

/**
 * Se publica cuando una quiniela pasa a estado ABIERTA (disponible para jugar).
 * Disparado por {@code QuinielaService.actualizarEstado}. Se notifica a todos
 * los jugadores activos (broadcast).
 */
public class QuinielaAbiertaEvent {

    private final Long   quinielaId;
    private final String nombreQuiniela;

    public QuinielaAbiertaEvent(Long quinielaId, String nombreQuiniela) {
        this.quinielaId     = quinielaId;
        this.nombreQuiniela = nombreQuiniela;
    }

    public Long getQuinielaId()        { return quinielaId; }
    public String getNombreQuiniela()  { return nombreQuiniela; }
}
