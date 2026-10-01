package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Jugador que realizó el pago (FK → users.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    /**
     * Jugadas cubiertas por este pago.
     * Un pago puede cubrir una o varias jugadas del mismo jugador.
     * Tabla intermedia: pago_jugadas (pago_id, jugada_id)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pago_jugadas",
            joinColumns        = @JoinColumn(name = "pago_id"),
            inverseJoinColumns = @JoinColumn(name = "jugada_id")
    )
    private List<Jugada> jugadas = new ArrayList<>();

    /** Monto que el jugador declara haber pagado */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /**
     * Nombre del archivo del comprobante de pago (p.ej. usuario1_20260410123045.jpg).
     * Opcional: el jugador puede subirlo después.
     */
    @Column(name = "comprobante_url", length = 255)
    private String comprobanteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    /** Administrador que aprobó o rechazó el pago (FK → users.id, nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por")
    private User validadoPor;

    /** Observación del administrador al validar (motivo de rechazo, etc.) */
    @Column(length = 500)
    private String observacion;

    /**
     * Indica que el comprobante fue enviado por WhatsApp y el administrador
     * lo registró manualmente en lugar de que el jugador lo subiera en la plataforma.
     */
    @Column(name = "comprobante_whatsapp", nullable = false)
    private boolean comprobanteWhatsapp = false;

    /**
     * true mientras el comprobante fue enviado por WhatsApp y aún no existe un
     * archivo cargado en la plataforma; el administrador debe subirlo él mismo.
     * Se pone en false automáticamente en cuanto se adjunta un archivo.
     */
    @Column(name = "comprobante_admin", nullable = false)
    private boolean comprobanteAdmin = false;

    /** Evita acreditar dos veces un pago vencido como saldo a favor. */
    @Column(name = "saldo_acreditado", nullable = false)
    private boolean saldoAcreditado = false;

    /** Monto de este pago que fue acreditado al saldo del jugador. */
    @Column(name = "monto_saldo_acreditado", precision = 10, scale = 2)
    private BigDecimal montoSaldoAcreditado;

    /**
     * Pago rechazado del que deriva este reintento.
     * {@code null} si es el intento original (primer pago).
     * Permite rastrear el historial completo de intentos de pago.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_origen_id")
    private Pago pagoOrigen;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    // ─── Ciclo de vida ─────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        }
    }

    // ─── Constructores ─────────────────────────────────────────────────
    public Pago() {}

    // ─── Getters / Setters ─────────────────────────────────────────────
    public Long getId()                                  { return id; }
    public void setId(Long id)                           { this.id = id; }

    public User getUsuario()                             { return usuario; }
    public void setUsuario(User usuario)                 { this.usuario = usuario; }

    public List<Jugada> getJugadas()                     { return jugadas; }
    public void setJugadas(List<Jugada> jugadas)         { this.jugadas = jugadas; }

    public BigDecimal getMonto()                         { return monto; }
    public void setMonto(BigDecimal monto)               { this.monto = monto; }

    public String getComprobanteUrl()                    { return comprobanteUrl; }
    public void setComprobanteUrl(String c)              { this.comprobanteUrl = c; }

    public EstadoPago getEstado()                        { return estado; }
    public void setEstado(EstadoPago estado)             { this.estado = estado; }

    public User getValidadoPor()                         { return validadoPor; }
    public void setValidadoPor(User validadoPor)         { this.validadoPor = validadoPor; }

    public String getObservacion()                       { return observacion; }
    public void setObservacion(String observacion)       { this.observacion = observacion; }

    public boolean isComprobanteWhatsapp()               { return comprobanteWhatsapp; }
    public void setComprobanteWhatsapp(boolean c)        { this.comprobanteWhatsapp = c; }

    public boolean isComprobanteAdmin()                   { return comprobanteAdmin; }
    public void setComprobanteAdmin(boolean c)            { this.comprobanteAdmin = c; }

    public boolean isSaldoAcreditado()                   { return saldoAcreditado; }
    public void setSaldoAcreditado(boolean s)             { this.saldoAcreditado = s; }

    public BigDecimal getMontoSaldoAcreditado()           { return montoSaldoAcreditado; }
    public void setMontoSaldoAcreditado(BigDecimal m)     { this.montoSaldoAcreditado = m; }

    public Pago getPagoOrigen()                          { return pagoOrigen; }
    public void setPagoOrigen(Pago pagoOrigen)           { this.pagoOrigen = pagoOrigen; }

    public LocalDateTime getFechaCreacion()              { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)       { this.fechaCreacion = fc; }

    public LocalDateTime getFechaValidacion()            { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime fv)     { this.fechaValidacion = fv; }
}

