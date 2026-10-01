package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de cada ganador de una quiniela.
 *
 * <p>Si hubo empate definitivo, existirán múltiples registros con el
 * mismo {@code puntosObtenidos} vinculados al mismo {@link CierreQuiniela}.
 *
 * <p>Los campos de puntos, posición y criterio son inmutables una vez
 * persistidos. Los campos de premio ({@code montoPremio}, {@code estadoPremio},
 * comprobante, etc.) sí se actualizan durante el flujo de entrega del premio.
 */
@Entity
@Table(name = "ganador_quiniela")
public class GanadorQuiniela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cierre al que pertenece este ganador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cierre_quiniela_id", nullable = false)
    private CierreQuiniela cierreQuiniela;

    /** Jugada (ticket) ganadora. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugada_id", nullable = false)
    private Jugada jugada;

    /** Usuario dueño de la jugada ganadora. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    /** Puntos obtenidos por esta jugada al momento del cierre (snapshot). */
    @Column(name = "puntos_obtenidos", nullable = false)
    private int puntosObtenidos;

    /**
     * Posición final dentro de los ganadores.
     * En un empate definitivo todos los ganadores tienen posición 1.
     * En desempate el único ganador tiene posición 1.
     */
    @Column(name = "posicion", nullable = false)
    private int posicion;

    /**
     * Nombre completo del ganador al momento del cierre (snapshot inmutable).
     * Formato: "Nombre ApellidoPaterno ApellidoMaterno"
     * Se guarda para que la auditoría sea independiente de cambios futuros en el perfil.
     */
    @Column(name = "nombre_completo", length = 250)
    private String nombreCompleto;

    /**
     * Criterio que determinó este ganador:
     * "MAYOR_PUNTAJE" | "DESEMPATE_TIPO_DIFICIL" | "DESEMPATE_TOTAL_ACIERTOS"
     * | "DESEMPATE_ULTIMO_PARTIDO" | "EMPATE_DEFINITIVO"
     */
    @Column(name = "criterio_aplicado", nullable = false, length = 60)
    private String criterioAplicado;

    // ─── Entrega del premio monetario ─────────────────────────────────

    /**
     * Monto del premio que corresponde a este ganador.
     * Se calcula al cierre repartiendo {@code Quiniela.bolsaAcumulada} en
     * partes iguales entre todos los ganadores (registros de este cierre).
     */
    @Column(name = "monto_premio", precision = 10, scale = 2)
    private BigDecimal montoPremio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_premio", nullable = false, length = 20)
    private EstadoPremio estadoPremio = EstadoPremio.PENDIENTE;

    /**
     * Nombre del archivo del comprobante de la transferencia/pago del premio,
     * subido por el administrador. Se guarda en uploads/comprobantes.
     */
    @Column(name = "comprobante_premio_url", length = 255)
    private String comprobantePremioUrl;

    /**
     * Comprobante adicional que puede utilizar la administración para mostrar a
     * otros jugadores que el pago del premio efectivamente se realizó.
     * Se almacena también como nombre/URL relativo en uploads/comprobantes.
     */
    @Column(name = "comprobante_premio_otros_url", length = 255)
    private String comprobantePremioOtrosUrl;

    /** Administrador que subió el comprobante y marcó el premio como pagado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagado_por")
    private User pagadoPor;

    /** Momento en que el administrador registró el pago del premio. */
    @Column(name = "fecha_pago_premio")
    private LocalDateTime fechaPagoPremio;

    /** Momento en que el jugador confirmó haber recibido el premio. */
    @Column(name = "fecha_confirmacion_jugador")
    private LocalDateTime fechaConfirmacionJugador;

    // ─── Constructores ────────────────────────────────────────────────
    public GanadorQuiniela() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public CierreQuiniela getCierreQuiniela()              { return cierreQuiniela; }
    public void setCierreQuiniela(CierreQuiniela v)        { this.cierreQuiniela = v; }

    public Jugada getJugada()                              { return jugada; }
    public void setJugada(Jugada jugada)                   { this.jugada = jugada; }

    public User getUsuario()                               { return usuario; }
    public void setUsuario(User usuario)                   { this.usuario = usuario; }

    public int getPuntosObtenidos()                        { return puntosObtenidos; }
    public void setPuntosObtenidos(int v)                  { this.puntosObtenidos = v; }

    public int getPosicion()                               { return posicion; }
    public void setPosicion(int v)                         { this.posicion = v; }

    public String getNombreCompleto()                      { return nombreCompleto; }
    public void setNombreCompleto(String v)                { this.nombreCompleto = v; }

    public String getCriterioAplicado()                    { return criterioAplicado; }
    public void setCriterioAplicado(String v)              { this.criterioAplicado = v; }

    public BigDecimal getMontoPremio()                     { return montoPremio; }
    public void setMontoPremio(BigDecimal v)               { this.montoPremio = v; }

    public EstadoPremio getEstadoPremio()                  { return estadoPremio; }
    public void setEstadoPremio(EstadoPremio v)            { this.estadoPremio = v; }

    public String getComprobantePremioUrl()                { return comprobantePremioUrl; }
    public void setComprobantePremioUrl(String v)          { this.comprobantePremioUrl = v; }

    public String getComprobantePremioOtrosUrl()           { return comprobantePremioOtrosUrl; }
    public void setComprobantePremioOtrosUrl(String v)     { this.comprobantePremioOtrosUrl = v; }

    public User getPagadoPor()                             { return pagadoPor; }
    public void setPagadoPor(User v)                       { this.pagadoPor = v; }

    public LocalDateTime getFechaPagoPremio()              { return fechaPagoPremio; }
    public void setFechaPagoPremio(LocalDateTime v)        { this.fechaPagoPremio = v; }

    public LocalDateTime getFechaConfirmacionJugador()     { return fechaConfirmacionJugador; }
    public void setFechaConfirmacionJugador(LocalDateTime v) { this.fechaConfirmacionJugador = v; }
}

