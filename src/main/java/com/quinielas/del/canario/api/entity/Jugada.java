package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jugadas")
public class Jugada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Jugador que registró la jugada (FK → users.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    /** Quiniela a la que pertenece la jugada (FK → quinielas.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiniela_id", nullable = false)
    private Quiniela quiniela;

    /** Puntos acumulados al cierre de la quiniela (null mientras está en curso) */
    @Column(name = "puntos_obtenidos")
    private Integer puntosObtenidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoJugada estado = EstadoJugada.CREADA;

    /**
     * Posición final de esta jugada en el ranking de la quiniela.
     * Se asigna al ejecutar el cierre. null mientras la quiniela no ha cerrado.
     */
    @Column(name = "posicion_final")
    private Integer posicionFinal;

    /**
     * Indica si esta jugada fue declarada ganadora al cierre de la quiniela.
     * false por defecto; se actualiza a true durante el proceso de cierre.
     */
    @Column(name = "es_ganadora", nullable = false)
    private boolean esGanadora = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── Relación 1:N con PronosticoJugado ───────────────────────────
    @OneToMany(mappedBy = "jugada", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PronosticoJugado> pronosticos = new ArrayList<>();

    // ─── Ciclo de vida JPA ────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        }
    }

    // ─── Constructores ────────────────────────────────────────────────
    public Jugada() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public User getUsuario()                               { return usuario; }
    public void setUsuario(User usuario)                   { this.usuario = usuario; }

    public Quiniela getQuiniela()                          { return quiniela; }
    public void setQuiniela(Quiniela quiniela)             { this.quiniela = quiniela; }

    public Integer getPuntosObtenidos()                    { return puntosObtenidos; }
    public void setPuntosObtenidos(Integer p)              { this.puntosObtenidos = p; }

    public EstadoJugada getEstado()                        { return estado; }
    public void setEstado(EstadoJugada estado)             { this.estado = estado; }

    public Integer getPosicionFinal()                      { return posicionFinal; }
    public void setPosicionFinal(Integer p)                { this.posicionFinal = p; }

    public boolean isEsGanadora()                          { return esGanadora; }
    public void setEsGanadora(boolean v)                   { this.esGanadora = v; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    public List<PronosticoJugado> getPronosticos()                     { return pronosticos; }
    public void setPronosticos(List<PronosticoJugado> pronosticos)     { this.pronosticos = pronosticos; }
}

