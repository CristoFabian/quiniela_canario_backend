package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pronostico_jugado",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_pronostico_jugada_partido_tipo",
               columnNames = {"jugada_id", "partido_id", "tipo_pronostico_id"}))
public class PronosticoJugado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Jugada a la que pertenece este pronóstico (FK → jugadas.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugada_id", nullable = false)
    private Jugada jugada;

    /** Partido sobre el que se pronostica (FK → partidos.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    /** Tipo de pronóstico (FK → tipo_pronostico.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_pronostico_id", nullable = false)
    private TipoPronostico tipoPronostico;

    /** Opción elegida por el jugador (FK → opcion_pronostico.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_pronostico_id", nullable = false)
    private OpcionPronostico opcionPronostico;

    /** Puntos obtenidos en este pronóstico (null hasta que se evalúe) */
    @Column(name = "puntos_obtenidos")
    private Integer puntosObtenidos;

    /**
     * Indica si este pronóstico ya fue evaluado contra el resultado real del partido.
     * false = pendiente de evaluar, true = ya evaluado.
     */
    @Column(nullable = false)
    private boolean evaluado = false;

    // ─── Constructores ────────────────────────────────────────────────
    public PronosticoJugado() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                        { return id; }
    public void setId(Long id)                                 { this.id = id; }

    public Jugada getJugada()                                  { return jugada; }
    public void setJugada(Jugada jugada)                       { this.jugada = jugada; }

    public Partido getPartido()                                { return partido; }
    public void setPartido(Partido partido)                    { this.partido = partido; }

    public TipoPronostico getTipoPronostico()                  { return tipoPronostico; }
    public void setTipoPronostico(TipoPronostico tp)           { this.tipoPronostico = tp; }

    public OpcionPronostico getOpcionPronostico()              { return opcionPronostico; }
    public void setOpcionPronostico(OpcionPronostico op)       { this.opcionPronostico = op; }

    public Integer getPuntosObtenidos()                        { return puntosObtenidos; }
    public void setPuntosObtenidos(Integer p)                  { this.puntosObtenidos = p; }

    public boolean isEvaluado()                                { return evaluado; }
    public void setEvaluado(boolean evaluado)                  { this.evaluado = evaluado; }
}

