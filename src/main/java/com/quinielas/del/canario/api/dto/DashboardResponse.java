package com.quinielas.del.canario.api.dto;

import java.util.List;

public class DashboardResponse {

    // ─── Contadores ───────────────────────────────────────────────────
    private long totalQuinielas;
    private long quinielasCreadas;
    private long quinielasAbiertas;
    private long quinielasEnJuego;
    private long quinielasFinalizadas;
    private long totalJugadores;
    private long jugadoresActivos;
    private long jugadoresInactivos;
    private long totalAdmins;

    // ─── Listas de detalle ────────────────────────────────────────────
    /** Quinielas en estado ABIERTA (disponibles para participar). */
    private List<QuinielaResponse> quinielasActivasDetalle;

    /** Quinielas en estado EN_JUEGO (partidos en curso). */
    private List<QuinielaResponse> quinielasEnJuegoDetalle;

    public DashboardResponse() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public long getTotalQuinielas()                              { return totalQuinielas; }
    public void setTotalQuinielas(long totalQuinielas)           { this.totalQuinielas = totalQuinielas; }

    public long getQuinielasCreadas()                            { return quinielasCreadas; }
    public void setQuinielasCreadas(long quinielasCreadas)       { this.quinielasCreadas = quinielasCreadas; }

    public long getQuinielasAbiertas()                           { return quinielasAbiertas; }
    public void setQuinielasAbiertas(long quinielasAbiertas)     { this.quinielasAbiertas = quinielasAbiertas; }

    public long getQuinielasEnJuego()                            { return quinielasEnJuego; }
    public void setQuinielasEnJuego(long quinielasEnJuego)       { this.quinielasEnJuego = quinielasEnJuego; }

    public long getQuinielasFinalizadas()                        { return quinielasFinalizadas; }
    public void setQuinielasFinalizadas(long quinielasFinalizadas){ this.quinielasFinalizadas = quinielasFinalizadas; }

    public long getTotalJugadores()                              { return totalJugadores; }
    public void setTotalJugadores(long totalJugadores)           { this.totalJugadores = totalJugadores; }

    public long getJugadoresActivos()                            { return jugadoresActivos; }
    public void setJugadoresActivos(long jugadoresActivos)       { this.jugadoresActivos = jugadoresActivos; }

    public long getJugadoresInactivos()                          { return jugadoresInactivos; }
    public void setJugadoresInactivos(long jugadoresInactivos)   { this.jugadoresInactivos = jugadoresInactivos; }

    public long getTotalAdmins()                                 { return totalAdmins; }
    public void setTotalAdmins(long totalAdmins)                 { this.totalAdmins = totalAdmins; }

    public List<QuinielaResponse> getQuinielasActivasDetalle()                       { return quinielasActivasDetalle; }
    public void setQuinielasActivasDetalle(List<QuinielaResponse> quinielasActivasDetalle) { this.quinielasActivasDetalle = quinielasActivasDetalle; }

    public List<QuinielaResponse> getQuinielasEnJuegoDetalle()                       { return quinielasEnJuegoDetalle; }
    public void setQuinielasEnJuegoDetalle(List<QuinielaResponse> quinielasEnJuegoDetalle) { this.quinielasEnJuegoDetalle = quinielasEnJuegoDetalle; }
}
