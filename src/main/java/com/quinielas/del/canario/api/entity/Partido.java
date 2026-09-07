package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "partidos")
public class Partido {

    // ─── Campos ───────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → quinielas.id (lado propietario de la relación). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_quiniela", nullable = false)
    private Quiniela quiniela;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "equipo_local", nullable = false, length = 100)
    private String equipoLocal;

    @Column(name = "equipo_visitante", nullable = false, length = 100)
    private String equipoVisitante;

    /** Nullable: se registra al finalizar el partido. */
    @Column(name = "marcador_local")
    private Integer marcadorLocal;

    /** Nullable: se registra al finalizar el partido. */
    @Column(name = "marcador_visitante")
    private Integer marcadorVisitante;

    /** Nullable: total de córners en el partido. */
    @Column(name = "total_corners")
    private Integer totalCorners;

    /** Nullable: indica si ambos equipos marcaron. */
    @Column(name = "ambos_marcan")
    private Boolean ambosMarcan;

    @Column(name = "fecha_partido", nullable = false)
    private LocalDateTime fechaPartido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPartido estado = EstadoPartido.PENDIENTE;

    // ─── Constructores ────────────────────────────────────────────────
    public Partido() {}

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Quiniela getQuiniela()                    { return quiniela; }
    public void setQuiniela(Quiniela quiniela)       { this.quiniela = quiniela; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String d)             { this.descripcion = d; }

    public String getEquipoLocal()                   { return equipoLocal; }
    public void setEquipoLocal(String el)            { this.equipoLocal = el; }

    public String getEquipoVisitante()               { return equipoVisitante; }
    public void setEquipoVisitante(String ev)        { this.equipoVisitante = ev; }

    public Integer getMarcadorLocal()                { return marcadorLocal; }
    public void setMarcadorLocal(Integer ml)         { this.marcadorLocal = ml; }

    public Integer getMarcadorVisitante()            { return marcadorVisitante; }
    public void setMarcadorVisitante(Integer mv)     { this.marcadorVisitante = mv; }

    public Integer getTotalCorners()                 { return totalCorners; }
    public void setTotalCorners(Integer tc)          { this.totalCorners = tc; }

    public Boolean getAmbosMarcan()                  { return ambosMarcan; }
    public void setAmbosMarcan(Boolean am)           { this.ambosMarcan = am; }

    public LocalDateTime getFechaPartido()           { return fechaPartido; }
    public void setFechaPartido(LocalDateTime fp)    { this.fechaPartido = fp; }

    public EstadoPartido getEstado()                 { return estado; }
    public void setEstado(EstadoPartido estado)      { this.estado = estado; }
}

