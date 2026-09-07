package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.PronosticoJugado;

public class PronosticoJugadoResponse {

    private Long   id;
    private Long   jugadaId;
    private Long   partidoId;
    private String equipoLocal;
    private String equipoVisitante;
    private Long   tipoPronosticoId;
    private String tipoPronosticoCodigo;
    private String tipoPronosticoNombre;
    private Long   opcionPronosticoId;
    private String opcionPronosticoCodigo;
    private String opcionPronosticoDescripcion;
    private Integer puntosObtenidos;

    public PronosticoJugadoResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static PronosticoJugadoResponse from(PronosticoJugado p) {
        PronosticoJugadoResponse r = new PronosticoJugadoResponse();
        r.id                         = p.getId();
        r.jugadaId                   = p.getJugada().getId();
        r.partidoId                  = p.getPartido().getId();
        r.equipoLocal                = p.getPartido().getEquipoLocal();
        r.equipoVisitante            = p.getPartido().getEquipoVisitante();
        r.tipoPronosticoId           = p.getTipoPronostico().getId();
        r.tipoPronosticoCodigo       = p.getTipoPronostico().getCodigo();
        r.tipoPronosticoNombre       = p.getTipoPronostico().getNombre();
        r.opcionPronosticoId         = p.getOpcionPronostico().getId();
        r.opcionPronosticoCodigo     = p.getOpcionPronostico().getCodigo();
        r.opcionPronosticoDescripcion = p.getOpcionPronostico().getDescripcion();
        r.puntosObtenidos            = p.getPuntosObtenidos();
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                          { return id; }
    public void setId(Long id)                                   { this.id = id; }

    public Long getJugadaId()                                    { return jugadaId; }
    public void setJugadaId(Long jugadaId)                       { this.jugadaId = jugadaId; }

    public Long getPartidoId()                                   { return partidoId; }
    public void setPartidoId(Long partidoId)                     { this.partidoId = partidoId; }

    public String getEquipoLocal()                               { return equipoLocal; }
    public void setEquipoLocal(String equipoLocal)               { this.equipoLocal = equipoLocal; }

    public String getEquipoVisitante()                           { return equipoVisitante; }
    public void setEquipoVisitante(String equipoVisitante)       { this.equipoVisitante = equipoVisitante; }

    public Long getTipoPronosticoId()                            { return tipoPronosticoId; }
    public void setTipoPronosticoId(Long tipoPronosticoId)       { this.tipoPronosticoId = tipoPronosticoId; }

    public String getTipoPronosticoCodigo()                      { return tipoPronosticoCodigo; }
    public void setTipoPronosticoCodigo(String c)                { this.tipoPronosticoCodigo = c; }

    public String getTipoPronosticoNombre()                      { return tipoPronosticoNombre; }
    public void setTipoPronosticoNombre(String n)                { this.tipoPronosticoNombre = n; }

    public Long getOpcionPronosticoId()                          { return opcionPronosticoId; }
    public void setOpcionPronosticoId(Long opcionPronosticoId)   { this.opcionPronosticoId = opcionPronosticoId; }

    public String getOpcionPronosticoCodigo()                    { return opcionPronosticoCodigo; }
    public void setOpcionPronosticoCodigo(String c)              { this.opcionPronosticoCodigo = c; }

    public String getOpcionPronosticoDescripcion()               { return opcionPronosticoDescripcion; }
    public void setOpcionPronosticoDescripcion(String d)         { this.opcionPronosticoDescripcion = d; }

    public Integer getPuntosObtenidos()                          { return puntosObtenidos; }
    public void setPuntosObtenidos(Integer puntosObtenidos)      { this.puntosObtenidos = puntosObtenidos; }
}

