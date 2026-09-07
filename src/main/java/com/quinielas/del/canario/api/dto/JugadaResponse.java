package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoJugada;
import com.quinielas.del.canario.api.entity.Jugada;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class JugadaResponse {

    private Long          id;
    private Long          quinielaId;
    private String        quinielaNombre;
    private BigDecimal    costoQuiniela;
    private Integer       puntosObtenidos;
    private EstadoJugada  estado;
    private LocalDateTime createdAt;
    private int           totalPronosticos;
    /** Posición final en el ranking de la quiniela. null mientras no ha cerrado. */
    private Integer       posicionFinal;
    /** Indica si esta jugada fue declarada ganadora al cierre de la quiniela. */
    private boolean       esGanadora;

    /** Pronósticos incluidos solo en la vista de detalle; null en listados. */
    private List<PronosticoJugadoResponse> pronosticos;

    public JugadaResponse() {}

    // ─── Factory: cabecera (listar) ───────────────────────────────────
    public static JugadaResponse from(Jugada j) {
        JugadaResponse r = new JugadaResponse();
        r.id               = j.getId();
        r.quinielaId       = j.getQuiniela().getId();
        r.quinielaNombre   = j.getQuiniela().getNombre();
        r.costoQuiniela    = j.getQuiniela().getCosto();
        r.puntosObtenidos  = j.getPuntosObtenidos();
        r.estado           = j.getEstado();
        r.createdAt        = j.getCreatedAt();
        r.totalPronosticos = j.getPronosticos().size();
        r.posicionFinal    = j.getPosicionFinal();
        r.esGanadora       = j.isEsGanadora();
        return r;
    }

    // ─── Factory: cabecera + detalle de pronósticos ───────────────────
    public static JugadaResponse fromDetalle(Jugada j) {
        JugadaResponse r = from(j);
        r.pronosticos = j.getPronosticos().stream()
                .map(PronosticoJugadoResponse::from)
                .collect(Collectors.toList());
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public Long getQuinielaId()                            { return quinielaId; }
    public void setQuinielaId(Long quinielaId)             { this.quinielaId = quinielaId; }

    public String getQuinielaNombre()                      { return quinielaNombre; }
    public void setQuinielaNombre(String quinielaNombre)   { this.quinielaNombre = quinielaNombre; }

    public BigDecimal getCostoQuiniela()                   { return costoQuiniela; }
    public void setCostoQuiniela(BigDecimal costoQuiniela) { this.costoQuiniela = costoQuiniela; }

    public Integer getPuntosObtenidos()                    { return puntosObtenidos; }
    public void setPuntosObtenidos(Integer p)              { this.puntosObtenidos = p; }

    public EstadoJugada getEstado()                        { return estado; }
    public void setEstado(EstadoJugada estado)             { this.estado = estado; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    public int getTotalPronosticos()                       { return totalPronosticos; }
    public void setTotalPronosticos(int totalPronosticos)  { this.totalPronosticos = totalPronosticos; }

    public Integer getPosicionFinal()                      { return posicionFinal; }
    public void setPosicionFinal(Integer v)                { this.posicionFinal = v; }

    public boolean isEsGanadora()                          { return esGanadora; }
    public void setEsGanadora(boolean v)                   { this.esGanadora = v; }

    public List<PronosticoJugadoResponse> getPronosticos()              { return pronosticos; }
    public void setPronosticos(List<PronosticoJugadoResponse> pronosticos) { this.pronosticos = pronosticos; }
}

