package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "quinielas")
public class Quiniela {

    /** Máximo de partidos permitidos por quiniela (regla de negocio). */
    public static final int MAX_PARTIDOS = 8;

    // ─── Campos ───────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoQuiniela estado = EstadoQuiniela.CREADA;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    /** Usuario que creó la quiniela (FK → users.id). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private User creadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;

    /**
     * Bolsa acumulada: suma de los montos de todos los pagos APROBADOS
     * asociados a las jugadas que corresponden a esta quiniela.
     * Se recalcula automáticamente cada vez que un pago cambia de estado.
     */
    @Column(name = "bolsa_acumulada", nullable = false, precision = 10, scale = 2)
    private BigDecimal bolsaAcumulada = BigDecimal.ZERO;

    /**
     * true una vez que ya se notificó a los jugadores que esta quiniela está próxima
     * a cerrar (falta ~1 día). Evita que la tarea programada envíe el aviso más de una vez.
     */
    @Column(name = "aviso_cierre_enviado", nullable = false)
    private boolean avisoCierreEnviado = false;

    // ─── Relación 1:N con Partido ─────────────────────────────────────
    @OneToMany(mappedBy = "quiniela", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Partido> partidos = new ArrayList<>();

    // ─── Constructores ────────────────────────────────────────────────
    public Quiniela() {}

    // ─── Ciclo de vida JPA ────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now(ZoneId.of("America/Mexico_City"));
        }
    }

    // ─── Regla de negocio: máximo 8 partidos ─────────────────────────
    /**
     * Agrega un partido a la quiniela respetando el límite de {@value MAX_PARTIDOS}.
     * Lanza {@link IllegalStateException} si ya se alcanzó el límite.
     */
    public void agregarPartido(Partido partido) {
        if (partidos.size() >= MAX_PARTIDOS) {
            throw new IllegalStateException(
                    "La quiniela ya tiene el máximo permitido de " + MAX_PARTIDOS + " partidos");
        }
        partido.setQuiniela(this);
        partidos.add(partido);
    }

    /** Devuelve una vista no modificable de la lista de partidos. */
    public List<Partido> getPartidos() {
        return Collections.unmodifiableList(partidos);
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String descripcion)   { this.descripcion = descripcion; }

    public BigDecimal getCosto()                     { return costo; }
    public void setCosto(BigDecimal costo)           { this.costo = costo; }

    public EstadoQuiniela getEstado()                { return estado; }
    public void setEstado(EstadoQuiniela estado)     { this.estado = estado; }

    public LocalDateTime getFechaInicio()            { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fi)     { this.fechaInicio = fi; }

    public LocalDateTime getFechaCierre()            { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fc)     { this.fechaCierre = fc; }

    public User getCreadoPor()                       { return creadoPor; }
    public void setCreadoPor(User creadoPor)         { this.creadoPor = creadoPor; }

    public LocalDateTime getFechaCreacion()          { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fc)   { this.fechaCreacion = fc; }

    public BigDecimal getBolsaAcumulada()                    { return bolsaAcumulada; }
    public void setBolsaAcumulada(BigDecimal bolsaAcumulada) { this.bolsaAcumulada = bolsaAcumulada; }

    public boolean isAvisoCierreEnviado()                    { return avisoCierreEnviado; }
    public void setAvisoCierreEnviado(boolean avisoCierreEnviado) { this.avisoCierreEnviado = avisoCierreEnviado; }
}

