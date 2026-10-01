package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.PronosticoJugado;

/**
 * Pronóstico de una jugada con el detalle del partido, para que el administrador
 * pueda ver qué eligió el jugador y el resultado real que explica su puntaje.
 */
public class PronosticoDetalleAdminResponse {

    private Long    id;
    private Long    partidoId;
    private String  equipoLocal;
    private String  equipoVisitante;
    private String  estadoPartido;

    /** Resultado real del partido (null mientras no se haya capturado). */
    private Integer marcadorLocal;
    private Integer marcadorVisitante;
    private Integer totalCorners;
    private Boolean ambosMarcan;

    private String  tipoPronosticoCodigo;
    private String  tipoPronosticoNombre;
    private String  opcionPronosticoCodigo;
    private String  opcionPronosticoDescripcion;

    /** Puntos obtenidos (null hasta que se evalúe). */
    private Integer puntosObtenidos;

    /** true si ya se comparó contra el resultado real del partido. */
    private boolean evaluado;

    public PronosticoDetalleAdminResponse() {}

    public static PronosticoDetalleAdminResponse from(PronosticoJugado p) {
        PronosticoDetalleAdminResponse r = new PronosticoDetalleAdminResponse();
        r.id                          = p.getId();
        r.partidoId                   = p.getPartido().getId();
        r.equipoLocal                 = p.getPartido().getEquipoLocal();
        r.equipoVisitante             = p.getPartido().getEquipoVisitante();
        r.estadoPartido               = p.getPartido().getEstado().name();
        r.marcadorLocal               = p.getPartido().getMarcadorLocal();
        r.marcadorVisitante           = p.getPartido().getMarcadorVisitante();
        r.totalCorners                = p.getPartido().getTotalCorners();
        r.ambosMarcan                 = p.getPartido().getAmbosMarcan();
        r.tipoPronosticoCodigo        = p.getTipoPronostico().getCodigo();
        r.tipoPronosticoNombre        = p.getTipoPronostico().getNombre();
        r.opcionPronosticoCodigo      = p.getOpcionPronostico().getCodigo();
        r.opcionPronosticoDescripcion = p.getOpcionPronostico().getDescripcion();
        r.puntosObtenidos             = p.getPuntosObtenidos();
        r.evaluado                    = p.isEvaluado();
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                             { return id; }
    public void setId(Long v)                                       { this.id = v; }

    public Long getPartidoId()                                      { return partidoId; }
    public void setPartidoId(Long v)                                { this.partidoId = v; }

    public String getEquipoLocal()                                  { return equipoLocal; }
    public void setEquipoLocal(String v)                            { this.equipoLocal = v; }

    public String getEquipoVisitante()                              { return equipoVisitante; }
    public void setEquipoVisitante(String v)                        { this.equipoVisitante = v; }

    public String getEstadoPartido()                                { return estadoPartido; }
    public void setEstadoPartido(String v)                          { this.estadoPartido = v; }

    public Integer getMarcadorLocal()                               { return marcadorLocal; }
    public void setMarcadorLocal(Integer v)                         { this.marcadorLocal = v; }

    public Integer getMarcadorVisitante()                           { return marcadorVisitante; }
    public void setMarcadorVisitante(Integer v)                     { this.marcadorVisitante = v; }

    public Integer getTotalCorners()                                { return totalCorners; }
    public void setTotalCorners(Integer v)                          { this.totalCorners = v; }

    public Boolean getAmbosMarcan()                                 { return ambosMarcan; }
    public void setAmbosMarcan(Boolean v)                           { this.ambosMarcan = v; }

    public String getTipoPronosticoCodigo()                        { return tipoPronosticoCodigo; }
    public void setTipoPronosticoCodigo(String v)                  { this.tipoPronosticoCodigo = v; }

    public String getTipoPronosticoNombre()                        { return tipoPronosticoNombre; }
    public void setTipoPronosticoNombre(String v)                  { this.tipoPronosticoNombre = v; }

    public String getOpcionPronosticoCodigo()                      { return opcionPronosticoCodigo; }
    public void setOpcionPronosticoCodigo(String v)                { this.opcionPronosticoCodigo = v; }

    public String getOpcionPronosticoDescripcion()                 { return opcionPronosticoDescripcion; }
    public void setOpcionPronosticoDescripcion(String v)           { this.opcionPronosticoDescripcion = v; }

    public Integer getPuntosObtenidos()                            { return puntosObtenidos; }
    public void setPuntosObtenidos(Integer v)                      { this.puntosObtenidos = v; }

    public boolean isEvaluado()                                    { return evaluado; }
    public void setEvaluado(boolean v)                             { this.evaluado = v; }
}
