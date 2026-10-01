package com.quinielas.del.canario.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Regla del juego administrable: le permite al administrador documentar,
 * agregar, editar, reordenar y desactivar/eliminar las reglas que se le
 * muestran al jugador (puntuación, desempate, premios, pagos, etc.),
 * sin necesidad de tocar código.
 */
@Entity
@Table(name = "regla_juego")
public class ReglaJuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título corto de la regla. Ej: "Puntos por tipo de pronóstico". */
    @Column(nullable = false, length = 150)
    private String titulo;

    /** Contenido/explicación completa de la regla (texto largo, admite markdown simple). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    /** Categoría para agrupar las reglas al mostrarlas. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaRegla categoria = CategoriaRegla.GENERAL;

    /** Orden de despliegue dentro de su categoría (menor = primero). */
    @Column(nullable = false)
    private int orden = 0;

    /** Eliminación lógica: false = oculta/eliminada para el jugador. */
    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // ─── Constructores ────────────────────────────────────────────────
    public ReglaJuego() {}

    public ReglaJuego(String titulo, String descripcion, CategoriaRegla categoria,
                       int orden, boolean activo) {
        this.titulo      = titulo;
        this.descripcion = descripcion;
        this.categoria   = categoria;
        this.orden       = orden;
        this.activo      = activo;
    }

    @PrePersist
    protected void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaCreacion = ahora;
        this.fechaActualizacion = ahora;
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public String getTitulo()                              { return titulo; }
    public void setTitulo(String titulo)                   { this.titulo = titulo; }

    public String getDescripcion()                         { return descripcion; }
    public void setDescripcion(String descripcion)         { this.descripcion = descripcion; }

    public CategoriaRegla getCategoria()                   { return categoria; }
    public void setCategoria(CategoriaRegla categoria)     { this.categoria = categoria; }

    public int getOrden()                                  { return orden; }
    public void setOrden(int orden)                        { this.orden = orden; }

    public boolean isActivo()                              { return activo; }
    public void setActivo(boolean activo)                  { this.activo = activo; }

    public LocalDateTime getFechaCreacion()                { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime f)          { this.fechaCreacion = f; }

    public LocalDateTime getFechaActualizacion()           { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime f)     { this.fechaActualizacion = f; }
}

