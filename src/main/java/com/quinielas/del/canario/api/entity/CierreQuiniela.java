package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro permanente e inmutable del cierre de una quiniela.
 *
 * <p>Una vez persistido este registro, el cierre se considera ejecutado y
 * no puede volver a dispararse (control de idempotencia).
 *
 * <p>Los ganadores se almacenan en la entidad GanadorQuiniela; este registro
 * actúa como cabecera de auditoría.
 */
@Entity
@Table(name = "cierre_quiniela",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_cierre_quiniela_id",
               columnNames = {"quiniela_id"}))
public class CierreQuiniela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Quiniela que se cierra (relación única: una quiniela solo se cierra una vez). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiniela_id", nullable = false, unique = true)
    private Quiniela quiniela;

    /** Puntaje máximo alcanzado por al menos un jugador. */
    @Column(name = "puntaje_maximo", nullable = false)
    private int puntajeMaximo;

    /** Número total de jugadas ACTIVAS evaluadas al momento del cierre. */
    @Column(name = "total_jugadas_elegibles", nullable = false)
    private int totalJugadasElegibles;

    /** Número de ganadores (> 1 indica empate definitivo). */
    @Column(name = "total_ganadores", nullable = false)
    private int totalGanadores;

    /**
     * Indica si el resultado final fue un empate entre dos o más jugadas.
     * true  = más de un ganador con el mismo puntaje y sin desempate posible.
     * false = un único ganador determinado.
     */
    @Column(name = "es_empate", nullable = false)
    private boolean esEmpate;

    /**
     * Descripción del criterio de desempate aplicado, o "EMPATE_DEFINITIVO"
     * si todos los finalistas tenían exactamente el mismo puntaje y timestamp.
     * Ejemplo: "PRIMER_TICKET_REGISTRADO" | "EMPATE_DEFINITIVO"
     */
    @Column(name = "criterio_desempate", length = 60)
    private String criterioDesempate;

    /** Administrador que ejecutó el cierre. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por", nullable = false)
    private User cerradoPor;

    /** Momento exacto en que se ejecutó el cierre (zona horaria México). */
    @Column(name = "fecha_cierre", nullable = false, updatable = false)
    private LocalDateTime fechaCierre;

    // ─── Constructores ────────────────────────────────────────────────
    public CierreQuiniela() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public Quiniela getQuiniela()                          { return quiniela; }
    public void setQuiniela(Quiniela quiniela)             { this.quiniela = quiniela; }

    public int getPuntajeMaximo()                          { return puntajeMaximo; }
    public void setPuntajeMaximo(int v)                    { this.puntajeMaximo = v; }

    public int getTotalJugadasElegibles()                  { return totalJugadasElegibles; }
    public void setTotalJugadasElegibles(int v)            { this.totalJugadasElegibles = v; }

    public int getTotalGanadores()                         { return totalGanadores; }
    public void setTotalGanadores(int v)                   { this.totalGanadores = v; }

    public boolean isEsEmpate()                            { return esEmpate; }
    public void setEsEmpate(boolean v)                     { this.esEmpate = v; }

    public String getCriterioDesempate()                   { return criterioDesempate; }
    public void setCriterioDesempate(String v)             { this.criterioDesempate = v; }

    public User getCerradoPor()                            { return cerradoPor; }
    public void setCerradoPor(User v)                      { this.cerradoPor = v; }

    public LocalDateTime getFechaCierre()                  { return fechaCierre; }
    public void setFechaCierre(LocalDateTime v)            { this.fechaCierre = v; }
}


