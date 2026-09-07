package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.CierreQuiniela;
import com.quinielas.del.canario.api.entity.GanadorQuiniela;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Respuesta del proceso de cierre de una quiniela.
 * Contiene la auditoría del cierre y la lista de ganadores.
 */
public class CierreQuinielaResponse {

    private Long          quinielaId;
    private String        nombreQuiniela;
    private int           puntajeMaximo;
    private int           totalJugadasElegibles;
    private int           totalGanadores;
    private boolean       esEmpate;
    private String        criterioDesempate;
    private String        cerradoPorUsername;
    private LocalDateTime fechaCierre;
    private List<GanadorResponse> ganadores;

    public CierreQuinielaResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static CierreQuinielaResponse from(CierreQuiniela cierre,
                                              List<GanadorQuiniela> ganadoresList) {
        CierreQuinielaResponse r = new CierreQuinielaResponse();
        r.quinielaId            = cierre.getQuiniela().getId();
        r.nombreQuiniela        = cierre.getQuiniela().getNombre();
        r.puntajeMaximo         = cierre.getPuntajeMaximo();
        r.totalJugadasElegibles = cierre.getTotalJugadasElegibles();
        r.totalGanadores        = cierre.getTotalGanadores();
        r.esEmpate              = cierre.isEsEmpate();
        r.criterioDesempate     = cierre.getCriterioDesempate();
        r.cerradoPorUsername    = cierre.getCerradoPor() != null
                                  ? cierre.getCerradoPor().getUsername() : null;
        r.fechaCierre           = cierre.getFechaCierre();
        r.ganadores             = ganadoresList.stream()
                                    .map(GanadorResponse::from)
                                    .collect(Collectors.toList());
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long          getQuinielaId()              { return quinielaId; }
    public void          setQuinielaId(Long v)        { this.quinielaId = v; }

    public String        getNombreQuiniela()          { return nombreQuiniela; }
    public void          setNombreQuiniela(String v)  { this.nombreQuiniela = v; }

    public int           getPuntajeMaximo()           { return puntajeMaximo; }
    public void          setPuntajeMaximo(int v)      { this.puntajeMaximo = v; }

    public int           getTotalJugadasElegibles()   { return totalJugadasElegibles; }
    public void          setTotalJugadasElegibles(int v) { this.totalJugadasElegibles = v; }

    public int           getTotalGanadores()          { return totalGanadores; }
    public void          setTotalGanadores(int v)     { this.totalGanadores = v; }

    public boolean       isEsEmpate()                 { return esEmpate; }
    public void          setEsEmpate(boolean v)       { this.esEmpate = v; }

    public String        getCriterioDesempate()       { return criterioDesempate; }
    public void          setCriterioDesempate(String v) { this.criterioDesempate = v; }

    public String        getCerradoPorUsername()      { return cerradoPorUsername; }
    public void          setCerradoPorUsername(String v) { this.cerradoPorUsername = v; }

    public LocalDateTime getFechaCierre()             { return fechaCierre; }
    public void          setFechaCierre(LocalDateTime v) { this.fechaCierre = v; }

    public List<GanadorResponse> getGanadores()               { return ganadores; }
    public void setGanadores(List<GanadorResponse> ganadores) { this.ganadores = ganadores; }

    // ─── Clase interna: detalle de cada ganador ───────────────────────
    public static class GanadorResponse {
        private Long   jugadaId;
        private Long   usuarioId;
        /** Nombre completo del ganador (snapshot guardado al momento del cierre). */
        private String nombreCompleto;
        private int    puntosObtenidos;
        private int    posicion;
        private String criterioAplicado;

        public static GanadorResponse from(GanadorQuiniela g) {
            GanadorResponse r = new GanadorResponse();
            r.jugadaId        = g.getJugada().getId();
            r.usuarioId       = g.getUsuario().getId();
            r.nombreCompleto  = g.getNombreCompleto();
            r.puntosObtenidos = g.getPuntosObtenidos();
            r.posicion        = g.getPosicion();
            r.criterioAplicado = g.getCriterioAplicado();
            return r;
        }

        public Long   getJugadaId()              { return jugadaId; }
        public void   setJugadaId(Long v)        { this.jugadaId = v; }
        public Long   getUsuarioId()             { return usuarioId; }
        public void   setUsuarioId(Long v)       { this.usuarioId = v; }
        public String getNombreCompleto()        { return nombreCompleto; }
        public void   setNombreCompleto(String v){ this.nombreCompleto = v; }
        public int    getPuntosObtenidos()       { return puntosObtenidos; }
        public void   setPuntosObtenidos(int v)  { this.puntosObtenidos = v; }
        public int    getPosicion()              { return posicion; }
        public void   setPosicion(int v)         { this.posicion = v; }
        public String getCriterioAplicado()      { return criterioAplicado; }
        public void   setCriterioAplicado(String v) { this.criterioAplicado = v; }
    }
}

