package com.quinielas.del.canario.api.dto;

import com.quinielas.del.canario.api.entity.EstadoPremio;
import com.quinielas.del.canario.api.entity.GanadorQuiniela;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa el premio monetario de un ganador de quiniela:
 * monto a entregar, estado de la entrega (PENDIENTE / PAGADO / CONFIRMADO),
 * comprobante de la transferencia y fechas de auditoría.
 */
public class PremioResponse {

    private Long           ganadorId;
    private Long           quinielaId;
    private String         nombreQuiniela;
    private Long           jugadaId;
    private Long           usuarioId;
    private String         nombreCompleto;
    private int            puntosObtenidos;
    private BigDecimal     montoPremio;
    private EstadoPremio   estadoPremio;
    private boolean        tieneComprobante;
    private boolean        tieneComprobanteOtros;
    private String         comprobantePremioOtrosUrl;
    private String         pagadoPorUsername;
    private LocalDateTime  fechaPagoPremio;
    private LocalDateTime  fechaConfirmacionJugador;

    public static PremioResponse from(GanadorQuiniela g) {
        PremioResponse r = new PremioResponse();
        r.ganadorId          = g.getId();
        r.quinielaId         = g.getCierreQuiniela().getQuiniela().getId();
        r.nombreQuiniela     = g.getCierreQuiniela().getQuiniela().getNombre();
        r.jugadaId           = g.getJugada().getId();
        r.usuarioId          = g.getUsuario().getId();
        r.nombreCompleto     = g.getNombreCompleto();
        r.puntosObtenidos    = g.getPuntosObtenidos();
        r.montoPremio        = g.getMontoPremio();
        r.estadoPremio       = g.getEstadoPremio();
        r.tieneComprobante   = g.getComprobantePremioUrl() != null && !g.getComprobantePremioUrl().isBlank();
        r.tieneComprobanteOtros = g.getComprobantePremioOtrosUrl() != null && !g.getComprobantePremioOtrosUrl().isBlank();
        r.comprobantePremioOtrosUrl = g.getComprobantePremioOtrosUrl();
        r.pagadoPorUsername  = g.getPagadoPor() != null ? g.getPagadoPor().getUsername() : null;
        r.fechaPagoPremio    = g.getFechaPagoPremio();
        r.fechaConfirmacionJugador = g.getFechaConfirmacionJugador();
        return r;
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getGanadorId()                          { return ganadorId; }
    public void setGanadorId(Long v)                    { this.ganadorId = v; }

    public Long getQuinielaId()                         { return quinielaId; }
    public void setQuinielaId(Long v)                   { this.quinielaId = v; }

    public String getNombreQuiniela()                   { return nombreQuiniela; }
    public void setNombreQuiniela(String v)             { this.nombreQuiniela = v; }

    public Long getJugadaId()                           { return jugadaId; }
    public void setJugadaId(Long v)                     { this.jugadaId = v; }

    public Long getUsuarioId()                          { return usuarioId; }
    public void setUsuarioId(Long v)                    { this.usuarioId = v; }

    public String getNombreCompleto()                   { return nombreCompleto; }
    public void setNombreCompleto(String v)             { this.nombreCompleto = v; }

    public int getPuntosObtenidos()                     { return puntosObtenidos; }
    public void setPuntosObtenidos(int v)               { this.puntosObtenidos = v; }

    public BigDecimal getMontoPremio()                  { return montoPremio; }
    public void setMontoPremio(BigDecimal v)            { this.montoPremio = v; }

    public EstadoPremio getEstadoPremio()                { return estadoPremio; }
    public void setEstadoPremio(EstadoPremio v)          { this.estadoPremio = v; }

    public boolean isTieneComprobante()                  { return tieneComprobante; }
    public void setTieneComprobante(boolean v)           { this.tieneComprobante = v; }

    public boolean isTieneComprobanteOtros()            { return tieneComprobanteOtros; }
    public void setTieneComprobanteOtros(boolean v)      { this.tieneComprobanteOtros = v; }

    public String getComprobantePremioOtrosUrl()        { return comprobantePremioOtrosUrl; }
    public void setComprobantePremioOtrosUrl(String v)  { this.comprobantePremioOtrosUrl = v; }

    public String getPagadoPorUsername()                 { return pagadoPorUsername; }
    public void setPagadoPorUsername(String v)           { this.pagadoPorUsername = v; }

    public LocalDateTime getFechaPagoPremio()            { return fechaPagoPremio; }
    public void setFechaPagoPremio(LocalDateTime v)      { this.fechaPagoPremio = v; }

    public LocalDateTime getFechaConfirmacionJugador()   { return fechaConfirmacionJugador; }
    public void setFechaConfirmacionJugador(LocalDateTime v) { this.fechaConfirmacionJugador = v; }
}

