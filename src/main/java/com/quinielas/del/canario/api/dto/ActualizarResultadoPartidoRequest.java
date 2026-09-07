package com.quinielas.del.canario.api.dto;

public class ActualizarResultadoPartidoRequest {

    private Integer marcadorLocal;
    private Integer marcadorVisitante;
    private Integer totalCorners;
    private Boolean ambosMarcan;

    // ─── Getters / Setters ────────────────────────────────────────────
    public Integer getMarcadorLocal()                { return marcadorLocal; }
    public void setMarcadorLocal(Integer ml)         { this.marcadorLocal = ml; }

    public Integer getMarcadorVisitante()            { return marcadorVisitante; }
    public void setMarcadorVisitante(Integer mv)     { this.marcadorVisitante = mv; }

    public Integer getTotalCorners()                 { return totalCorners; }
    public void setTotalCorners(Integer tc)          { this.totalCorners = tc; }

    public Boolean getAmbosMarcan()                  { return ambosMarcan; }
    public void setAmbosMarcan(Boolean am)           { this.ambosMarcan = am; }
}

