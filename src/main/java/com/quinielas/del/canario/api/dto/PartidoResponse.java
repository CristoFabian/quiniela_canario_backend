package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoPartido;
import com.quinielas.del.canario.api.entity.Partido;

import java.time.LocalDateTime;

public class PartidoResponse {

    private Long id;
    private Long quinielaId;
    private String descripcion;
    private String equipoLocal;
    private String equipoVisitante;
    private Integer marcadorLocal;
    private Integer marcadorVisitante;
    private Integer totalCorners;
    private Boolean ambosMarcan;
    private LocalDateTime fechaPartido;
    private EstadoPartido estado;

    public PartidoResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static PartidoResponse from(Partido partido) {
        PartidoResponse r = new PartidoResponse();
        r.id               = partido.getId();
        r.quinielaId       = partido.getQuiniela().getId();
        r.descripcion      = partido.getDescripcion();
        r.equipoLocal      = partido.getEquipoLocal();
        r.equipoVisitante  = partido.getEquipoVisitante();
        r.marcadorLocal    = partido.getMarcadorLocal();
        r.marcadorVisitante = partido.getMarcadorVisitante();
        r.totalCorners     = partido.getTotalCorners();
        r.ambosMarcan      = partido.getAmbosMarcan();
        r.fechaPartido     = partido.getFechaPartido();
        r.estado           = partido.getEstado();
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Long getQuinielaId()                      { return quinielaId; }
    public void setQuinielaId(Long quinielaId)       { this.quinielaId = quinielaId; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String d)             { this.descripcion = d; }

    public String getEquipoLocal()                   { return equipoLocal; }
    public void setEquipoLocal(String el)            { this.equipoLocal = el; }

    public String getEquipoVisitante()               { return equipoVisitante; }
    public void setEquipoVisitante(String ev)        { this.equipoVisitante = ev; }

    public Integer getMarcadorLocal()                { return marcadorLocal; }
    public void setMarcadorLocal(Integer ml)         { this.marcadorLocal = ml; }

    public Integer getMarcadorVisitante()            { return marcadorVisitante; }
    public void setMarcadorVisitante(Integer mv)     { this.marcadorVisitante = mv; }

    public Integer getTotalCorners()                 { return totalCorners; }
    public void setTotalCorners(Integer tc)          { this.totalCorners = tc; }

    public Boolean getAmbosMarcan()                  { return ambosMarcan; }
    public void setAmbosMarcan(Boolean am)           { this.ambosMarcan = am; }

    public LocalDateTime getFechaPartido()           { return fechaPartido; }
    public void setFechaPartido(LocalDateTime fp)    { this.fechaPartido = fp; }

    public EstadoPartido getEstado()                 { return estado; }
    public void setEstado(EstadoPartido estado)      { this.estado = estado; }
}

