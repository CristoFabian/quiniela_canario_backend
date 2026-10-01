package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoPago;
import com.quinielas.del.canario.api.entity.Pago;
import com.quinielas.del.canario.api.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PagoResponse {

    private Long         id;
    private Long         usuarioId;
    private String       usuarioUsername;
    private String       nombreCompleto;
    private List<JugadaResumenEnPago> jugadas;
    private BigDecimal   monto;
    private String       comprobanteUrl;
    private EstadoPago   estado;
    private String       observacion;
    private boolean      comprobanteWhatsapp;
    private boolean      comprobanteAdmin;
    private Long         pagoOrigenId;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaValidacion;
    private int          totalJugadas;
    private boolean      saldoAcreditado;
    private BigDecimal   montoSaldoAcreditado;
    private BigDecimal   saldoJugador;

    public PagoResponse() {}

    // ─── Factory ──────────────────────────────────────────────────────
    public static PagoResponse from(Pago p) {
        PagoResponse r = new PagoResponse();
        r.id                  = p.getId();
        r.usuarioId           = p.getUsuario().getId();
        r.usuarioUsername     = p.getUsuario().getUsername();
        r.nombreCompleto      = obtenerNombreCompleto(p.getUsuario());
        r.monto               = p.getMonto();
        r.comprobanteUrl      = p.getComprobanteUrl();
        r.estado              = p.getEstado();
        r.observacion         = p.getObservacion();
        r.comprobanteWhatsapp = p.isComprobanteWhatsapp();
        r.comprobanteAdmin    = p.isComprobanteAdmin();
        r.pagoOrigenId        = p.getPagoOrigen() != null ? p.getPagoOrigen().getId() : null;
        r.fechaCreacion       = p.getFechaCreacion();
        r.fechaValidacion     = p.getFechaValidacion();
        r.jugadas             = p.getJugadas().stream()
                                  .map(JugadaResumenEnPago::from)
                                  .collect(Collectors.toList());
        r.totalJugadas        = r.jugadas.size();
        r.saldoAcreditado     = p.isSaldoAcreditado();
        r.montoSaldoAcreditado = p.getMontoSaldoAcreditado();
        r.saldoJugador        = p.getUsuario().getPerfil() != null
                                  ? p.getUsuario().getPerfil().getSaldoAFavor()
                                  : BigDecimal.ZERO;
        return r;
    }

    // ─── Resumen de jugada dentro de un pago ──────────────────────────
    private static String obtenerNombreCompleto(User usuario) {
        if (usuario == null) return null;
        if (usuario.getPerfil() == null) return usuario.getUsername();

        String nombre = usuario.getPerfil().getNombre();
        String apellidoPaterno = usuario.getPerfil().getApellidoPaterno();
        String apellidoMaterno = usuario.getPerfil().getApellidoMaterno();

        StringBuilder sb = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) sb.append(nombre.trim());
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(apellidoPaterno.trim());
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(apellidoMaterno.trim());
        }

        String nombreCompleto = sb.toString().trim();
        return nombreCompleto.isEmpty() ? usuario.getUsername() : nombreCompleto;
    }

    public static class JugadaResumenEnPago {
        private Long       id;
        private Long       quinielaId;
        private String     quinielaNombre;
        private String     estadoQuiniela;
        /** Costo de la quiniela; permite al admin verificar que el monto del pago es correcto. */
        private java.math.BigDecimal costoQuiniela;
        /** Fecha de cierre de la quiniela; referencia clave para validar urgencia. */
        private java.time.LocalDateTime fechaCierreQuiniela;
        /** Pronósticos registrados de los 8 esperados. */
        private int        totalPronosticos;
        private String     estado;

        public static JugadaResumenEnPago from(com.quinielas.del.canario.api.entity.Jugada j) {
            JugadaResumenEnPago r = new JugadaResumenEnPago();
            r.id                  = j.getId();
            r.quinielaId          = j.getQuiniela().getId();
            r.quinielaNombre      = j.getQuiniela().getNombre();
            r.estadoQuiniela      = j.getQuiniela().getEstado().name();
            r.costoQuiniela       = j.getQuiniela().getCosto();
            r.fechaCierreQuiniela = j.getQuiniela().getFechaCierre();
            r.totalPronosticos    = j.getPronosticos().size();
            r.estado              = j.getEstado().name();
            return r;
        }

        public Long getId()                                        { return id; }
        public void setId(Long id)                                 { this.id = id; }
        public Long getQuinielaId()                                { return quinielaId; }
        public void setQuinielaId(Long q)                          { this.quinielaId = q; }
        public String getQuinielaNombre()                          { return quinielaNombre; }
        public void setQuinielaNombre(String n)                    { this.quinielaNombre = n; }
        public String getEstadoQuiniela()                          { return estadoQuiniela; }
        public void setEstadoQuiniela(String e)                    { this.estadoQuiniela = e; }
        public java.math.BigDecimal getCostoQuiniela()             { return costoQuiniela; }
        public void setCostoQuiniela(java.math.BigDecimal c)       { this.costoQuiniela = c; }
        public java.time.LocalDateTime getFechaCierreQuiniela()    { return fechaCierreQuiniela; }
        public void setFechaCierreQuiniela(java.time.LocalDateTime f) { this.fechaCierreQuiniela = f; }
        public int getTotalPronosticos()                           { return totalPronosticos; }
        public void setTotalPronosticos(int t)                     { this.totalPronosticos = t; }
        public String getEstado()                                  { return estado; }
        public void setEstado(String e)                            { this.estado = e; }
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public Long getUsuarioId()                             { return usuarioId; }
    public void setUsuarioId(Long usuarioId)               { this.usuarioId = usuarioId; }

    public String getUsuarioUsername()                     { return usuarioUsername; }
    public void setUsuarioUsername(String u)               { this.usuarioUsername = u; }

    public String getNombreCompleto()                      { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto)   { this.nombreCompleto = nombreCompleto; }

    public List<JugadaResumenEnPago> getJugadas()          { return jugadas; }
    public void setJugadas(List<JugadaResumenEnPago> j)    { this.jugadas = j; }

    public BigDecimal getMonto()                           { return monto; }
    public void setMonto(BigDecimal monto)                 { this.monto = monto; }

    public String getComprobanteUrl()                      { return comprobanteUrl; }
    public void setComprobanteUrl(String c)                { this.comprobanteUrl = c; }

    public EstadoPago getEstado()                          { return estado; }
    public void setEstado(EstadoPago estado)               { this.estado = estado; }


    public String getObservacion()                         { return observacion; }
    public void setObservacion(String observacion)         { this.observacion = observacion; }

    public boolean isComprobanteWhatsapp()                 { return comprobanteWhatsapp; }
    public void setComprobanteWhatsapp(boolean c)          { this.comprobanteWhatsapp = c; }

    public boolean isComprobanteAdmin()                    { return comprobanteAdmin; }
    public void setComprobanteAdmin(boolean c)             { this.comprobanteAdmin = c; }

    public Long getPagoOrigenId()                          { return pagoOrigenId; }
    public void setPagoOrigenId(Long pagoOrigenId)         { this.pagoOrigenId = pagoOrigenId; }

    public LocalDateTime getFechaCreacion()                { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)         { this.fechaCreacion = fc; }

    public LocalDateTime getFechaValidacion()              { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime fv)       { this.fechaValidacion = fv; }

    public int getTotalJugadas()                           { return totalJugadas; }
    public void setTotalJugadas(int totalJugadas)          { this.totalJugadas = totalJugadas; }
    public boolean isSaldoAcreditado()                     { return saldoAcreditado; }
    public void setSaldoAcreditado(boolean s)              { this.saldoAcreditado = s; }
    public BigDecimal getMontoSaldoAcreditado()            { return montoSaldoAcreditado; }
    public void setMontoSaldoAcreditado(BigDecimal m)      { this.montoSaldoAcreditado = m; }
    public BigDecimal getSaldoJugador()                    { return saldoJugador; }
    public void setSaldoJugador(BigDecimal s)              { this.saldoJugador = s; }
}

