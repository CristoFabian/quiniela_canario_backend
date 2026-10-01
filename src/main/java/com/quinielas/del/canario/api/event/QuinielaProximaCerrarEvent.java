package com.quinielas.del.canario.api.event;

import java.time.LocalDateTime;

/**
 * Se publica cuando falta ~1 día para el cierre de registro de una quiniela ABIERTA.
 * Disparado por {@code NotificacionScheduledTasks} (tarea programada diaria).
 * Se notifica a todos los jugadores activos (broadcast).
 */
public class QuinielaProximaCerrarEvent {

    private final Long          quinielaId;
    private final String        nombreQuiniela;
    private final LocalDateTime fechaCierre;

    public QuinielaProximaCerrarEvent(Long quinielaId, String nombreQuiniela, LocalDateTime fechaCierre) {
        this.quinielaId     = quinielaId;
        this.nombreQuiniela = nombreQuiniela;
        this.fechaCierre    = fechaCierre;
    }

    public Long getQuinielaId()          { return quinielaId; }
    public String getNombreQuiniela()    { return nombreQuiniela; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
}
