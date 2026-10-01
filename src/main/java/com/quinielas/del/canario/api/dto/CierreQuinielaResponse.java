package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.CierreQuiniela;
import com.quinielas.del.canario.api.entity.EstadoPremio;
import com.quinielas.del.canario.api.entity.GanadorQuiniela;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private BigDecimal    bolsaAcumulada;
    private BigDecimal    porcentajeComision;
    private BigDecimal    montoComision;
    private BigDecimal    premioTotalRepartido;
    private List<GanadorResponse> ganadores;

    public CierreQuinielaResponse() {}

    // ─── Factory ─────────────────────────────────────────────────────
    public static CierreQuinielaResponse from(CierreQuiniela cierre,
                                              List<GanadorQuiniela> ganadoresList,
                                              Map<Long, String> telefonosPorUsuarioId) {
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
        r.bolsaAcumulada        = cierre.getBolsaAcumuladaSnapshot();
        r.porcentajeComision    = cierre.getPorcentajeComision();
        r.montoComision         = cierre.getMontoComision();
        r.premioTotalRepartido  = cierre.getPremioTotalRepartido();
        r.ganadores             = ganadoresList.stream()
                                    .map(g -> GanadorResponse.from(g,
                                            telefonosPorUsuarioId.get(g.getUsuario().getId())))
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

    public BigDecimal    getBolsaAcumulada()          { return bolsaAcumulada; }
    public void          setBolsaAcumulada(BigDecimal v) { this.bolsaAcumulada = v; }

    public BigDecimal    getPorcentajeComision()      { return porcentajeComision; }
    public void          setPorcentajeComision(BigDecimal v) { this.porcentajeComision = v; }

    public BigDecimal    getMontoComision()           { return montoComision; }
    public void          setMontoComision(BigDecimal v) { this.montoComision = v; }

    public BigDecimal    getPremioTotalRepartido()    { return premioTotalRepartido; }
    public void          setPremioTotalRepartido(BigDecimal v) { this.premioTotalRepartido = v; }

    public List<GanadorResponse> getGanadores()               { return ganadores; }
    public void setGanadores(List<GanadorResponse> ganadores) { this.ganadores = ganadores; }

    // ─── Clase interna: detalle de cada ganador ───────────────────────
    public static class GanadorResponse {
        private Long   jugadaId;
        private Long   usuarioId;
        /** Nombre completo del ganador (snapshot guardado al momento del cierre). */
        private String nombreCompleto;
        private String email;
        private String telefono;
        private int    puntosObtenidos;
        private int    posicion;
        private String criterioAplicado;
        private BigDecimal montoPremio;
        private EstadoPremio estadoPremio;
        private LocalDateTime fechaPagoPremio;
        private LocalDateTime fechaConfirmacionJugador;
        private boolean tieneComprobanteOtros;
        private String comprobantePremioOtrosUrl;

        public static GanadorResponse from(GanadorQuiniela g, String telefono) {
            GanadorResponse r = new GanadorResponse();
            r.jugadaId        = g.getJugada().getId();
            r.usuarioId       = g.getUsuario().getId();
            r.nombreCompleto  = g.getNombreCompleto();
            r.email           = g.getUsuario().getEmail();
            r.telefono        = telefono;
            r.puntosObtenidos = g.getPuntosObtenidos();
            r.posicion        = g.getPosicion();
            r.criterioAplicado = g.getCriterioAplicado();
            r.montoPremio      = g.getMontoPremio();
            r.estadoPremio     = g.getEstadoPremio();
            r.fechaPagoPremio  = g.getFechaPagoPremio();
            r.fechaConfirmacionJugador = g.getFechaConfirmacionJugador();
            r.tieneComprobanteOtros = g.getComprobantePremioOtrosUrl() != null && !g.getComprobantePremioOtrosUrl().isBlank();
            r.comprobantePremioOtrosUrl = g.getComprobantePremioOtrosUrl();
            return r;
        }

        public Long   getJugadaId()              { return jugadaId; }
        public void   setJugadaId(Long v)        { this.jugadaId = v; }
        public Long   getUsuarioId()             { return usuarioId; }
        public void   setUsuarioId(Long v)       { this.usuarioId = v; }
        public String getNombreCompleto()        { return nombreCompleto; }
        public void   setNombreCompleto(String v){ this.nombreCompleto = v; }
        public String getEmail()                 { return email; }
        public void   setEmail(String v)         { this.email = v; }
        public String getTelefono()              { return telefono; }
        public void   setTelefono(String v)      { this.telefono = v; }
        public int    getPuntosObtenidos()       { return puntosObtenidos; }
        public void   setPuntosObtenidos(int v)  { this.puntosObtenidos = v; }
        public int    getPosicion()              { return posicion; }
        public void   setPosicion(int v)         { this.posicion = v; }
        public String getCriterioAplicado()      { return criterioAplicado; }
        public void   setCriterioAplicado(String v) { this.criterioAplicado = v; }
        public BigDecimal getMontoPremio()       { return montoPremio; }
        public void   setMontoPremio(BigDecimal v) { this.montoPremio = v; }
        public EstadoPremio getEstadoPremio()    { return estadoPremio; }
        public void   setEstadoPremio(EstadoPremio v) { this.estadoPremio = v; }
        public LocalDateTime getFechaPagoPremio()  { return fechaPagoPremio; }
        public void   setFechaPagoPremio(LocalDateTime v) { this.fechaPagoPremio = v; }
        public LocalDateTime getFechaConfirmacionJugador() { return fechaConfirmacionJugador; }
        public void   setFechaConfirmacionJugador(LocalDateTime v) { this.fechaConfirmacionJugador = v; }
        public boolean isTieneComprobanteOtros() { return tieneComprobanteOtros; }
        public void setTieneComprobanteOtros(boolean v) { this.tieneComprobanteOtros = v; }
        public String getComprobantePremioOtrosUrl() { return comprobantePremioOtrosUrl; }
        public void setComprobantePremioOtrosUrl(String v) { this.comprobantePremioOtrosUrl = v; }
    }
}

