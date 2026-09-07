package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

/**
 * Registro de cada ganador de una quiniela.
 *
 * <p>Si hubo empate definitivo, existirán múltiples registros con el
 * mismo {@code puntosObtenidos} vinculados al mismo {@link CierreQuiniela}.
 *
 * <p>Este registro es inmutable una vez persistido: los puntos y posición
 * NO deben recalcularse después del cierre.
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
}

